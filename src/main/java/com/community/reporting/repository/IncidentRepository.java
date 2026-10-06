package com.community.reporting.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.community.reporting.dto.analytics.CategoryCountResponse;
import com.community.reporting.dto.analytics.DailyIncidentCountResponse;
import com.community.reporting.dto.analytics.StatusCountResponse;
import com.community.reporting.entities.Incident;
import com.community.reporting.enums.IncidentCategory;
import com.community.reporting.enums.IncidentStatus;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Page<Incident> findByStatus(
        IncidentStatus status,
        Pageable pageable
    );

    Page<Incident> findByCategory(
        IncidentCategory category,
        Pageable pageable
    );

    Page<Incident> findByStatusAndCategory(
        IncidentStatus status,
        IncidentCategory category,
        Pageable pageable
    );

    Page<Incident> findByReportedById(
        Long userId,
        Pageable pageable
    );
    
    long countByStatus(IncidentStatus status);

    @Query("""
        SELECT new com.community.reporting.dto.analytics.CategoryCountResponse(
            i.category,
            COUNT(i)
        )
        FROM Incident i
        GROUP BY i.category
        ORDER BY COUNT(i) DESC
    """)
    List<CategoryCountResponse> countIncidentsByCategory();

    @Query("""
        SELECT new com.community.reporting.dto.analytics.StatusCountResponse(
            i.status,
            COUNT(i)
        )
        FROM Incident i
        GROUP BY i.status
        ORDER BY COUNT(i) DESC
    """)
    List<StatusCountResponse> countIncidentsByStatus();
    
    @Query(
    	    value = """
    	        SELECT
    	            DATE(created_at) AS incident_date,
    	            COUNT(*) AS incident_count
    	        FROM incidents
    	        GROUP BY DATE(created_at)
    	        ORDER BY DATE(created_at)
    	        """,
    	    nativeQuery = true
    	)
    	List<Object[]> countIncidentsPerDayRaw();
    
    List<Incident> findTop10ByOrderByCreatedAtDesc();
}