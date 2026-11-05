"""Populate a local reporting API with clearly marked synthetic incidents."""
import argparse
import concurrent.futures
import datetime
import json
import secrets
import time
import urllib.error
import urllib.request
from collections import Counter
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url', default='http://localhost:8080')
parser.add_argument('--count', type=int, default=10000)
parser.add_argument('--workers', type=int, default=6)
args = parser.parse_args()
if not 1 <= args.count <= 100000 or not 1 <= args.workers <= 16:
    parser.error('count must be 1..100000; workers must be 1..16')
if urllib.parse.urlparse(args.base_url).hostname not in ('localhost', '127.0.0.1', '::1'):
    parser.error('This script only populates a local API')


def request(method, path, data=None, token=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    payload = None if data is None else json.dumps(data).encode()
    req = urllib.request.Request(args.base_url.rstrip('/') + path, data=payload,
                                 headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=45) as response:
        return json.load(response)


batch = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ') + '-' + secrets.token_hex(3)
before = request('GET', '/api/incidents?size=1')['totalElements']
email = 'sample-' + batch.lower() + '@example.com'
token = request('POST', '/api/auth/register', {
    'name': 'Synthetic Sample Resident', 'email': email,
    'password': secrets.token_urlsafe(32),
})['token']
categories = ['POTHOLE', 'WATER_LEAK', 'ELECTRICITY', 'STREETLIGHT', 'SEWER', 'ILLEGAL_DUMPING', 'OTHER']
issues = ['Road surface damage', 'Water pipe leak', 'Electricity supply interruption',
          'Streetlight not working', 'Blocked sewer drain', 'Dumped waste', 'Damaged public fixture']
areas = [(-25.7461, 28.1881, 'Pretoria'), (-26.2041, 28.0473, 'Johannesburg'),
         (-23.9045, 29.4689, 'Polokwane'), (-25.4745, 30.9703, 'Mbombela'),
         (-29.8587, 31.0218, 'Durban'), (-33.9249, 18.4241, 'Cape Town')]


def create(number):
    index = (number - 1) % len(categories)
    lat, lon, area = areas[(number - 1) % len(areas)]
    # Deterministic offsets form synthetic map points, not resident locations.
    payload = {
        'category': categories[index],
        'description': f'[SAMPLE DATA {batch} #{number:05d}] {issues[index]} in a simulated {area} area. Synthetic demonstration report; not a verified real incident.',
        'latitude': round(lat + ((number * 37) % 1001 - 500) / 10000, 7),
        'longitude': round(lon + ((number * 53) % 1001 - 500) / 10000, 7),
    }
    try:
        request('POST', '/api/incidents', payload, token)
        return number, categories[index], None
    except urllib.error.HTTPError as error:
        return number, None, 'HTTP ' + str(error.code)
    except Exception as error:
        # Do not retry ambiguous POST failures, which could create duplicates.
        return number, None, type(error).__name__


print(f'Batch {batch}: creating {args.count} incidents with {args.workers} workers; existing total {before}', flush=True)
counts = Counter()
failures = []
started = time.monotonic()
with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as pool:
    for completed, (number, category, error) in enumerate(pool.map(create, range(1, args.count + 1)), 1):
        if error:
            failures.append({'number': number, 'error': error})
        else:
            counts[category] += 1
        if completed % 500 == 0 or completed == args.count:
            print(f'{completed}/{args.count}: {sum(counts.values())} created, {len(failures)} failed ({time.monotonic() - started:.1f}s)', flush=True)
        if completed >= 50 and sum(counts.values()) == 0:
            raise SystemExit('No requests succeeded; stopping population. Inspect the API logs.')
after = request('GET', '/api/incidents?size=1')['totalElements']
summary = {'batch': batch, 'sampleAccountEmail': email, 'requested': args.count,
           'created': sum(counts.values()), 'categories': dict(counts),
           'failures': failures, 'totalBefore': before, 'totalAfter': after,
           'status': 'REPORTED', 'elapsedSeconds': round(time.monotonic() - started, 1)}
Path('target').mkdir(exist_ok=True)
Path('target/seed-summary.json').write_text(json.dumps(summary, indent=2))
print(json.dumps(summary), flush=True)
if failures:
    raise SystemExit(1)
