package com.example.incidenttracker.service;

import com.example.incidenttracker.model.Incident;
import com.example.incidenttracker.repository.IncidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IncidentService {

    @Autowired
    private IncidentRepository incidentRepository;

    public Incident saveIncident(Incident incident) {
        return incidentRepository.save(incident);
    }

    public List<Incident> findSolutions(String description) {
        return incidentRepository.findByDescriptionContainingIgnoreCase(description);
    }

    public Optional<Incident> findIncidentById(Long id) {
        return incidentRepository.findById(id);
    }

    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    public Incident handleIncident(String description) {
        String uniqueDescription = description.trim().toLowerCase();
        // Find incidents with a similar (case-insensitive) description
        List<Incident> similar = incidentRepository.findByDescriptionIgnoreCase(uniqueDescription);

        // Return the first incident that already has a solution
        return similar.stream()
                .filter(inc -> inc.getSolution() != null && !inc.getSolution().isEmpty())
                .findFirst()
                .orElseGet(() -> {
                    // If not found, create and save a new incident
                    Incident newIncident = new Incident();
                    newIncident.setDescription(description.trim());
                    newIncident.setSolution(null);
                    return incidentRepository.save(newIncident);
                });
    }

    public Incident createSubIncident(Long parentIncidentId, String description) {
        if (parentIncidentId == null) {
            throw new IllegalArgumentException("Parent incident ID cannot be null");
        }
        Incident subIncident = new Incident();
        subIncident.setDescription(description);
        subIncident.setSolution(null);
        subIncident.setParentIncidentId(parentIncidentId);
        return incidentRepository.save(subIncident);
    }

    public Incident updateIncidentSolution(Long id, String solution) {
        return incidentRepository.findById(id)
            .map(incident -> {
                incident.setSolution(solution);
                return incidentRepository.save(incident);
            })
            .orElse(null);
    }

}