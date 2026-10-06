package com.community.reporting.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.community.reporting.entities.IncidentUpdate;

public interface IncidentUpdateRepository
        extends JpaRepository<IncidentUpdate, Long> {

    List<IncidentUpdate> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);
    
    @Query("""
    	    SELECT AVG(
    	        TIMESTAMPDIFF(
    	            HOUR,
    	            i.createdAt,
    	            u.createdAt
    	        )
    	    )
    	    FROM IncidentUpdate u
    	    JOIN u.incident i
    	    WHERE u.newStatus = com.community.reporting.enums.IncidentStatus.RESOLVED
    	""")
    	Double calculateAverageResolutionHours();
}