package com.example.incidenttracker.service;

import com.example.incidenttracker.model.Incident;
import com.example.incidenttracker.repository.IncidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class IncidentService {

    @Autowired
    private IncidentRepository incidentRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Incident saveIncident(Incident incident) {
        // Generate incident number if not already set
        if (incident.getIncidentNumber() == null || incident.getIncidentNumber().isEmpty()) {
            incident.setIncidentNumber(generateIncidentNumber());
        }
        
        // Set creation timestamp and status for new incidents
        if (incident.getId() == null) {
            incident.setCreatedAt(LocalDateTime.now());
            incident.setStatus("OPEN");
            // Add creation log entry
            incident.addSolutionLogEntry("[" + LocalDateTime.now().format(FORMATTER) + "] Ticket created: " + incident.getDescription());
        }
        
        incident.setUpdatedAt(LocalDateTime.now());
        return incidentRepository.save(incident);
    }

    private String generateIncidentNumber() {
        // Get the count of all incidents to generate next number
        long count = incidentRepository.count();
        return String.format("INC-%04d", count + 1);
    }

    public List<Incident> findSolutions(String description) {
        var allIncidents = incidentRepository.findAll();
        
        // If no description filter is provided or it's empty, return all incidents
        if (description == null || description.strip().isEmpty()) {
            return allIncidents;
        }
        
        // Filter incidents by description (case-insensitive) and handle null descriptions
        var searchTerm = description.strip().toLowerCase();
        var relevantIncidents = allIncidents.stream()
            .filter(incident -> incident.getDescription() != null && 
                              incident.getDescription().toLowerCase().contains(searchTerm))
            .collect(Collectors.toList());
        
        return relevantIncidents;
    }

    public Optional<Incident> findIncidentById(Long id) {
        return incidentRepository.findById(id);
    }

    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    public Incident handleIncident(String description) {
        var uniqueDescription = description.strip().toLowerCase();
        // Find incidents with a similar (case-insensitive) description
        var similar = incidentRepository.findByDescriptionIgnoreCase(uniqueDescription);

        // Return the first incident that already has a solution
        return similar.stream()
                .filter(inc -> inc.getSolution() != null && !inc.getSolution().isEmpty())
                .findFirst()
                .orElseGet(() -> {
                    // If not found, create and save a new incident
                    var newIncident = new Incident();
                    newIncident.setDescription(description.strip());
                    newIncident.setSolution(null);
                    newIncident.setIncidentNumber(generateIncidentNumber());
                    newIncident.setCreatedAt(LocalDateTime.now());
                    newIncident.setStatus("OPEN");
                    newIncident.addSolutionLogEntry("[" + LocalDateTime.now().format(FORMATTER) + "] Ticket created: " + description.strip());
                    newIncident.setUpdatedAt(LocalDateTime.now());
                    return incidentRepository.save(newIncident);
                });
    }

    public Incident createIncidentWithoutSolution(String description) {
        // Create a new incident without solution (for when user wants to create ticket)
        var newIncident = new Incident();
        newIncident.setDescription(description.strip());
        newIncident.setSolution(null);
        newIncident.setIncidentNumber(generateIncidentNumber());
        newIncident.setCreatedAt(LocalDateTime.now());
        newIncident.setStatus("OPEN");
        newIncident.addSolutionLogEntry("[" + LocalDateTime.now().format(FORMATTER) + "] Ticket created: " + description.strip());
        newIncident.setUpdatedAt(LocalDateTime.now());
        return incidentRepository.save(newIncident);
    }

    public Incident createSubIncident(Long parentIncidentId, String description) {
        if (parentIncidentId == null) {
            throw new IllegalArgumentException("Parent incident ID cannot be null");
        }
        var subIncident = new Incident();
        subIncident.setDescription(description);
        subIncident.setSolution(null);
        subIncident.setParentIncidentId(parentIncidentId);
        subIncident.setIncidentNumber(generateIncidentNumber());
        subIncident.setCreatedAt(LocalDateTime.now());
        subIncident.setStatus("OPEN");
        subIncident.addSolutionLogEntry("[" + LocalDateTime.now().format(FORMATTER) + "] Sub-ticket created: " + description);
        subIncident.setUpdatedAt(LocalDateTime.now());
        return incidentRepository.save(subIncident);
    }

    public Incident updateIncidentSolution(Long id, String solution) {
        return incidentRepository.findById(id)
            .map(incident -> {
                // Add solution to log instead of replacing main solution
                var logEntry = "[" + LocalDateTime.now().format(FORMATTER) + "] Solution added: " + solution;
                incident.addSolutionLogEntry(logEntry);
                
                // Update the main solution field (for display purposes)
                incident.setSolution(solution);
                incident.setUpdatedAt(LocalDateTime.now());
                
                // Update status to IN_PROGRESS if it was OPEN
                if ("OPEN".equals(incident.getStatus())) {
                    incident.setStatus("IN_PROGRESS");
                }
                
                return incidentRepository.save(incident);
            })
            .orElse(null);
    }

    public Incident addSolutionUpdate(Long id, String update) {
        return incidentRepository.findById(id)
            .map(incident -> {
                // Add update to log without changing main solution
                var logEntry = "[" + LocalDateTime.now().format(FORMATTER) + "] Update: " + update;
                incident.addSolutionLogEntry(logEntry);
                incident.setUpdatedAt(LocalDateTime.now());
                
                // Update status to IN_PROGRESS if it was OPEN
                if ("OPEN".equals(incident.getStatus())) {
                    incident.setStatus("IN_PROGRESS");
                }
                
                return incidentRepository.save(incident);
            })
            .orElse(null);
    }

    public Incident closeIncident(Long id) {
        return incidentRepository.findById(id)
            .map(incident -> {
                incident.setStatus("CLOSED");
                incident.setClosedAt(LocalDateTime.now());
                incident.setUpdatedAt(LocalDateTime.now());
                incident.addSolutionLogEntry("[" + LocalDateTime.now().format(FORMATTER) + "] Ticket closed");
                return incidentRepository.save(incident);
            })
            .orElse(null);
    }

}