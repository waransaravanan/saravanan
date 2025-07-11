package com.example.incidenttracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.incidenttracker.model.Incident;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByDescriptionContainingIgnoreCase(String description);
    List<Incident> findByDescriptionIgnoreCase(String description);
    //List<Incident> findByUniqueDescription(String description);
}
