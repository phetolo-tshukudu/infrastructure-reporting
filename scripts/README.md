# Sample incident population

With the local API running on port 8080:

```cmd
python scripts/populate_incidents.py --count 10000 --workers 6
```

The script registers a dedicated sample resident and creates incidents through the normal API. All descriptions begin with `[SAMPLE DATA ...]`; locations are synthetic map points around South African cities. Existing incidents are preserved. All added reports start as `REPORTED`.

`--count` controls the batch size and `--workers` controls concurrent requests. Each run creates a new batch. Login credentials and tokens remain in memory and are not saved. A summary containing counts, the batch identifier, sample-account email, and any failures is written to `target/seed-summary.json`.

The script accepts only local API hosts and does not automatically retry ambiguous failed POST requests, which could otherwise create duplicate reports.
