package com.example.incidenttracker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.example.incidenttracker.model.Incident;
import com.example.incidenttracker.service.IncidentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller 
@RequestMapping("/api/incidents")
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    @PostMapping
    @ResponseBody // Still return JSON for API
    public ResponseEntity<Incident> createIncident(@RequestBody Incident incident) {
        var existingIncidents = incidentService.getAllIncidents()
                .stream()
                .filter(i -> i.getDescription().equalsIgnoreCase(incident.getDescription()))
                .collect(Collectors.toList());

        if (existingIncidents.isEmpty()) {
            var createdIncident = incidentService.saveIncident(incident);
            return ResponseEntity
                    .created(URI.create("/api/incidents/" + createdIncident.getId()))
                    .body(createdIncident);
        } else {
            // Handle case where incident already exists
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(existingIncidents.get(0));
        }
    }

    @GetMapping("/solutions")
    @ResponseBody // Still return JSON for API
    public ResponseEntity<List<Incident>> getIncidentSolutions(@RequestParam String description) {
        var solutions = incidentService.findSolutions(description);
        return ResponseEntity.ok(solutions);
    }

    @GetMapping
    @ResponseBody // Still return JSON for API
    public ResponseEntity<List<Incident>> getAllIncidents() {
        var incidents = incidentService.getAllIncidents();
        return ResponseEntity.ok(incidents);
    }

    @PostMapping("/chat")
    @ResponseBody // Still return JSON for API
    public ResponseEntity<Incident> chatIncident(@RequestBody Map<String, String> payload) {
        var description = payload.get("description");
        if (description == null || description.strip().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        var incident = incidentService.handleIncident(description);
        return ResponseEntity.ok(incident);
    }

    @PostMapping("/{parentId}/subtask")
    @ResponseBody // Still return JSON for API
    public ResponseEntity<Incident> createSubtask(
            @PathVariable Long parentId,
            @RequestBody Map<String, String> payload) {
        var description = payload.get("description");
        if (description == null || description.strip().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        var subIncident = incidentService.createSubIncident(parentId, description);
        return ResponseEntity.ok(subIncident);
    }

    @PutMapping("/{id}/solution")
    @ResponseBody
    public ResponseEntity<Incident> updateSolution(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        var solution = payload.get("solution");
        var updated = incidentService.updateIncidentSolution(id, solution);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/solution-update")
    @ResponseBody
    public ResponseEntity<Incident> addSolutionUpdate(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        var update = payload.get("update");
        var updated = incidentService.addSolutionUpdate(id, update);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/close")
    @ResponseBody
    public ResponseEntity<Incident> closeIncident(@PathVariable Long id) {
        var closed = incidentService.closeIncident(id);
        if (closed != null) {
            return ResponseEntity.ok(closed);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/resolve-and-create-blocker")
    @ResponseBody
    public ResponseEntity<Incident> resolveIncidentAndCreateBlocker(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        var solution = payload.get("solution");
        var newBlockerDescription = payload.get("newBlockerDescription");
        
        if (solution == null || solution.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            var newBlockerIncident = incidentService.resolveIncidentAndCreateBlockerIncident(id, solution, newBlockerDescription);
            if (newBlockerIncident != null) {
                return ResponseEntity.ok(newBlockerIncident);
            } else {
                // Original incident was resolved but no new blocker was created
                return ResponseEntity.ok().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{parentId}/create-followup")
    @ResponseBody
    public ResponseEntity<Incident> createFollowUpIncident(
            @PathVariable Long parentId,
            @RequestBody Map<String, String> payload) {
        var newIssueDescription = payload.get("newIssueDescription");
        var parentSolutionApplied = payload.get("parentSolutionApplied");
        
        if (newIssueDescription == null || newIssueDescription.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            var followUpIncident = incidentService.createFollowUpIncident(parentId, newIssueDescription, parentSolutionApplied);
            return ResponseEntity.ok(followUpIncident);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/children")
    @ResponseBody
    public ResponseEntity<List<Incident>> getChildIncidents(@PathVariable Long id) {
        var children = incidentService.getChildIncidents(id);
        return ResponseEntity.ok(children);
    }

    @GetMapping("/{id}/hierarchy")
    @ResponseBody
    public ResponseEntity<List<Incident>> getIncidentHierarchy(@PathVariable Long id) {
        var hierarchy = incidentService.getIncidentHierarchy(id);
        return ResponseEntity.ok(hierarchy);
    }

    // Thymeleaf UI endpoint (no @ResponseBody)
    @GetMapping("/solutions-ui")
    public String solutionsPage(@RequestParam(required = false) String description, Model model) {
        var incidents = incidentService.findSolutions(description);
        model.addAttribute("incidents", incidents);
        model.addAttribute("description", description);
        
        // Add parent incident information for child incidents
        var parentIncidentsMap = incidents.stream()
            .filter(incident -> incident.getParentIncidentId() != null)
            .collect(Collectors.toMap(
                Incident::getId,
                incident -> incidentService.getParentIncident(incident.getParentIncidentId())
            ));
        model.addAttribute("parentIncidents", parentIncidentsMap);
        
        return "solutions"; // This matches solutions.html in templates
    }

    @PostMapping("/solution-by-description")
    public String updateSolutionFromForm(
            @RequestParam String incidentDescription,
            @RequestParam String solution,
            @RequestParam(required = false) String description, // to preserve search/filter if needed
            Model model) {
        // Find or create incident by description and update its solution
        var incident = incidentService.handleIncident(incidentDescription);
        incidentService.updateIncidentSolution(incident.getId(), solution);
        // Redirect back to the table page where the user was
        return "redirect:/api/incidents/table";
    }

    // Table view endpoint
    @GetMapping("/table")
    public String tablePage(
            @RequestParam(required = false) String search, 
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            Model model) {
        
        var allIncidents = incidentService.getAllIncidents();
        var filteredIncidents = allIncidents;
        
        // Apply search filter
        if (search != null && !search.strip().isEmpty()) {
            var searchLower = search.toLowerCase();
            filteredIncidents = filteredIncidents.stream()
                .filter(incident -> 
                    (incident.getDescription() != null && incident.getDescription().toLowerCase().contains(searchLower)) ||
                    (incident.getSolution() != null && incident.getSolution().toLowerCase().contains(searchLower)))
                .collect(Collectors.toList());
        }
        
        // Apply status filter
        if (status != null && !status.isEmpty()) {
            if ("closed".equals(status)) {
                filteredIncidents = filteredIncidents.stream()
                    .filter(incident -> "CLOSED".equals(incident.getStatus()))
                    .collect(Collectors.toList());
            } else if ("open".equals(status)) {
                filteredIncidents = filteredIncidents.stream()
                    .filter(incident -> "OPEN".equals(incident.getStatus()) || incident.getStatus() == null)
                    .collect(Collectors.toList());
            } else if ("in_progress".equals(status)) {
                filteredIncidents = filteredIncidents.stream()
                    .filter(incident -> "IN_PROGRESS".equals(incident.getStatus()))
                    .collect(Collectors.toList());
            }
        }
        
        // Apply type filter
        if (type != null && !type.isEmpty()) {
            if ("parent".equals(type)) {
                filteredIncidents = filteredIncidents.stream()
                    .filter(incident -> incident.getParentIncidentId() == null)
                    .collect(Collectors.toList());
            } else if ("sub".equals(type)) {
                filteredIncidents = filteredIncidents.stream()
                    .filter(incident -> incident.getParentIncidentId() != null)
                    .collect(Collectors.toList());
            }
        }
        
        // Calculate statistics
        var totalIncidents = allIncidents.size();
        var closedIncidents = allIncidents.stream()
            .filter(incident -> "CLOSED".equals(incident.getStatus()))
            .count();
        var inProgressIncidents = allIncidents.stream()
            .filter(incident -> "IN_PROGRESS".equals(incident.getStatus()))
            .count();
        var openIncidents = allIncidents.stream()
            .filter(incident -> "OPEN".equals(incident.getStatus()) || incident.getStatus() == null)
            .count();
        var subIncidents = allIncidents.stream()
            .filter(incident -> incident.getParentIncidentId() != null)
            .count();
        
        model.addAttribute("incidents", filteredIncidents);
        model.addAttribute("totalIncidents", totalIncidents);
        model.addAttribute("closedIncidents", closedIncidents);
        model.addAttribute("inProgressIncidents", inProgressIncidents);
        model.addAttribute("openIncidents", openIncidents);
        model.addAttribute("subIncidents", subIncidents);
        model.addAttribute("searchTerm", search);
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        
        // Add parent incident information for child incidents
        var parentIncidentsMap = filteredIncidents.stream()
            .filter(incident -> incident.getParentIncidentId() != null)
            .collect(Collectors.toMap(
                Incident::getId,
                incident -> incidentService.getParentIncident(incident.getParentIncidentId())
            ));
        model.addAttribute("parentIncidents", parentIncidentsMap);
        
        return "table";
    }
}