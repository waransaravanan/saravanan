package com.example.incidenttracker.service;

import com.example.incidenttracker.model.Incident;
import com.example.incidenttracker.repository.IncidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    public Incident getParentIncident(Long parentIncidentId) {
        if (parentIncidentId == null) {
            return null;
        }
        return incidentRepository.findById(parentIncidentId).orElse(null);
    }

    /**
     * Resolves an incident with a solution and creates a new incident for any additional blocker
     * This handles the scenario where solving one issue reveals another blocker that needs to be tracked separately
     * 
     * @param incidentId The ID of the incident being resolved
     * @param solution The solution applied to resolve the incident
     * @param newBlockerDescription Description of the new blocker discovered (optional)
     * @return The newly created incident for the blocker, or null if no blocker was specified
     */
    public Incident resolveIncidentAndCreateBlockerIncident(Long incidentId, String solution, String newBlockerDescription) {
        // First, update the original incident with the solution
        Incident resolvedIncident = updateIncidentSolution(incidentId, solution);
        
        if (resolvedIncident == null) {
            throw new IllegalArgumentException("Incident with ID " + incidentId + " not found");
        }
        
        // Add a log entry indicating the incident was resolved but a new blocker was found
        String logEntry = "[" + LocalDateTime.now().format(FORMATTER) + "] Incident resolved. New blocker identified: " + newBlockerDescription;
        resolvedIncident.addSolutionLogEntry(logEntry);
        resolvedIncident.setUpdatedAt(LocalDateTime.now());
        incidentRepository.save(resolvedIncident);
        
        // Create new incident for the blocker if description is provided
        if (newBlockerDescription != null && !newBlockerDescription.trim().isEmpty()) {
            Incident newBlockerIncident = new Incident();
            newBlockerIncident.setDescription(newBlockerDescription.trim());
            newBlockerIncident.setSolution(null);
            newBlockerIncident.setParentIncidentId(incidentId); // Link to original incident
            newBlockerIncident.setIncidentNumber(generateIncidentNumber());
            newBlockerIncident.setCreatedAt(LocalDateTime.now());
            newBlockerIncident.setStatus("OPEN");
            newBlockerIncident.setUpdatedAt(LocalDateTime.now());
            
            // Add creation log entry with reference to parent incident
            String creationLog = "[" + LocalDateTime.now().format(FORMATTER) + "] New blocker incident created from resolved incident " + resolvedIncident.getIncidentNumber() + ": " + newBlockerDescription.trim();
            newBlockerIncident.addSolutionLogEntry(creationLog);
            
            return incidentRepository.save(newBlockerIncident);
        }
        
        return null;
    }

    /**
     * Creates a follow-up incident when a resolved incident leads to discovering a new issue
     * This is specifically for the use case where one solution is applied but reveals another problem
     * 
     * @param parentIncidentId The ID of the parent incident that was resolved
     * @param newIssueDescription Description of the new issue discovered
     * @param parentSolutionApplied The solution that was applied to the parent incident
     * @return The newly created follow-up incident
     */
    public Incident createFollowUpIncident(Long parentIncidentId, String newIssueDescription, String parentSolutionApplied) {
        Optional<Incident> parentIncident = incidentRepository.findById(parentIncidentId);
        
        if (!parentIncident.isPresent()) {
            throw new IllegalArgumentException("Parent incident with ID " + parentIncidentId + " not found");
        }
        
        Incident parent = parentIncident.get();
        
        // Update parent incident status and add log about the follow-up
        String parentLogEntry = "[" + LocalDateTime.now().format(FORMATTER) + "] Follow-up incident created for new issue: " + newIssueDescription;
        parent.addSolutionLogEntry(parentLogEntry);
        parent.setUpdatedAt(LocalDateTime.now());
        
        // If parent doesn't have a solution yet, add the provided solution
        if (parentSolutionApplied != null && !parentSolutionApplied.trim().isEmpty()) {
            if (parent.getSolution() == null || parent.getSolution().trim().isEmpty()) {
                parent.setSolution(parentSolutionApplied.trim());
                parent.addSolutionLogEntry("[" + LocalDateTime.now().format(FORMATTER) + "] Solution applied: " + parentSolutionApplied.trim());
            }
            // Update status to IN_PROGRESS if it was OPEN
            if ("OPEN".equals(parent.getStatus())) {
                parent.setStatus("IN_PROGRESS");
            }
        }
        
        incidentRepository.save(parent);
        
        // Create new follow-up incident
        Incident followUpIncident = new Incident();
        followUpIncident.setDescription(newIssueDescription.trim());
        followUpIncident.setSolution(null);
        followUpIncident.setParentIncidentId(parentIncidentId);
        followUpIncident.setIncidentNumber(generateIncidentNumber());
        followUpIncident.setCreatedAt(LocalDateTime.now());
        followUpIncident.setStatus("OPEN");
        followUpIncident.setUpdatedAt(LocalDateTime.now());
        
        // Add creation log entry with detailed context
        String creationLog = "[" + LocalDateTime.now().format(FORMATTER) + "] Follow-up incident created from " + parent.getIncidentNumber() + " after applying solution '" + (parentSolutionApplied != null ? parentSolutionApplied.trim() : "N/A") + "'. New issue: " + newIssueDescription.trim();
        followUpIncident.addSolutionLogEntry(creationLog);
        
        return incidentRepository.save(followUpIncident);
    }

    /**
     * Gets all child incidents (subtasks/follow-ups) for a given parent incident
     * 
     * @param parentIncidentId The ID of the parent incident
     * @return List of child incidents
     */
    public List<Incident> getChildIncidents(Long parentIncidentId) {
        return incidentRepository.findByParentIncidentId(parentIncidentId);
    }

    /**
     * Gets the complete incident hierarchy starting from a root incident
     * 
     * @param rootIncidentId The ID of the root incident
     * @return List containing the root incident and all its descendants
     */
    public List<Incident> getIncidentHierarchy(Long rootIncidentId) {
        List<Incident> hierarchy = new ArrayList<>();
        Optional<Incident> rootIncident = incidentRepository.findById(rootIncidentId);
        
        if (rootIncident.isPresent()) {
            hierarchy.add(rootIncident.get());
            addChildrenToHierarchy(rootIncidentId, hierarchy);
        }
        
        return hierarchy;
    }
    
    private void addChildrenToHierarchy(Long parentId, List<Incident> hierarchy) {
        List<Incident> children = getChildIncidents(parentId);
        hierarchy.addAll(children);
        
        // Recursively add grandchildren
        for (Incident child : children) {
            addChildrenToHierarchy(child.getId(), hierarchy);
        }
    }

}