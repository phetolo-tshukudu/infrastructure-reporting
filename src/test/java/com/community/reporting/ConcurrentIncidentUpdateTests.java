package com.community.reporting;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import com.community.reporting.entities.*;
import com.community.reporting.enums.*;
import jakarta.persistence.OptimisticLockException;

class ConcurrentIncidentUpdateTests {
    @Test
    void staleUpdateRollsBackItsHistoryEntry() {
        var configuration = new Configuration()
            .addAnnotatedClass(User.class)
            .addAnnotatedClass(Incident.class)
            .addAnnotatedClass(IncidentUpdate.class)
            .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
            .setProperty("hibernate.connection.url", "jdbc:h2:mem:concurrent_incident;DB_CLOSE_DELAY=-1")
            .setProperty("hibernate.hbm2ddl.auto", "create-drop");
        try (var factory = configuration.buildSessionFactory()) {
            Long incidentId;
            Long workerId;
            try (var setup = factory.openSession()) {
                var transaction = setup.beginTransaction();
                var worker = User.builder().name("Worker").email("worker@example.com")
                    .password("hash").role(Role.WORKER).build();
                setup.persist(worker);
                var incident = Incident.builder().category(IncidentCategory.POTHOLE)
                    .description("Road damage").latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO)
                    .status(IncidentStatus.REPORTED).reportedBy(worker).build();
                setup.persist(incident);
                transaction.commit();
                incidentId = incident.getId();
                workerId = worker.getId();
            }
            try (var first = factory.openSession(); var second = factory.openSession()) {
                var firstTransaction = first.beginTransaction();
                var secondTransaction = second.beginTransaction();
                var firstIncident = first.find(Incident.class, incidentId);
                var staleIncident = second.find(Incident.class, incidentId);
                firstIncident.setStatus(IncidentStatus.VERIFIED);
                first.persist(IncidentUpdate.builder().incident(firstIncident)
                    .oldStatus(IncidentStatus.REPORTED).newStatus(IncidentStatus.VERIFIED)
                    .updatedBy(first.find(User.class, workerId)).build());
                firstTransaction.commit();
                // Both requests read REPORTED, but only one may commit.
                second.persist(IncidentUpdate.builder().incident(staleIncident)
                    .oldStatus(IncidentStatus.REPORTED).newStatus(IncidentStatus.VERIFIED)
                    .updatedBy(second.find(User.class, workerId)).build());
                staleIncident.setStatus(IncidentStatus.VERIFIED);
                assertThrows(OptimisticLockException.class, second::flush);
                secondTransaction.rollback();
            }
            try (var check = factory.openSession()) {
                assertEquals(IncidentStatus.VERIFIED, check.find(Incident.class, incidentId).getStatus());
                assertEquals(1L, check.createQuery("select count(u) from IncidentUpdate u", Long.class).getSingleResult());
            }
        }
    }
}
