package com.example.incidenttracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.incidenttracker.model.Incident;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByDescriptionContainingIgnoreCase(String description);
    List<Incident> findByDescriptionIgnoreCase(String description);
    List<Incident> findByParentIncidentId(Long parentIncidentId);
    List<Incident> findByParentIncidentIdIsNull(); // Find root incidents (no parent)
    List<Incident> findByStatus(String status);
    //List<Incident> findByUniqueDescription(String description);
}
