package com.example.incidenttracker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.example.incidenttracker.model.Incident;
import com.example.incidenttracker.service.IncidentService;
import com.example.incidenttracker.service.OpenAISuggestionService;
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

    @Autowired
    private OpenAISuggestionService openAISuggestionService;

    @PostMapping
    @ResponseBody // Still return JSON for API
    public ResponseEntity<Incident> createIncident(@RequestBody Incident incident) {
        List<Incident> existingIncidents = incidentService.getAllIncidents()
                .stream()
                .filter(i -> i.getDescription().equalsIgnoreCase(incident.getDescription()))
                .collect(Collectors.toList());

        if (existingIncidents.isEmpty()) {
            Incident createdIncident = incidentService.saveIncident(incident);
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
        List<Incident> solutions = incidentService.findSolutions(description);
        return ResponseEntity.ok(solutions);
    }

    @GetMapping
    @ResponseBody // Still return JSON for API
    public ResponseEntity<List<Incident>> getAllIncidents() {
        List<Incident> incidents = incidentService.getAllIncidents();
        return ResponseEntity.ok(incidents);
    }

    @PostMapping("/chat")
    @ResponseBody // Still return JSON for API
    public ResponseEntity<Incident> chatIncident(@RequestBody Map<String, String> payload) {
        String description = payload.get("description");
        if (description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Incident incident = incidentService.handleIncident(description);
        return ResponseEntity.ok(incident);
    }

    @PostMapping("/{parentId}/subtask")
    @ResponseBody // Still return JSON for API
    public ResponseEntity<Incident> createSubtask(
            @PathVariable Long parentId,
            @RequestBody Map<String, String> payload) {
        String description = payload.get("description");
        if (description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Incident subIncident = incidentService.createSubIncident(parentId, description);
        return ResponseEntity.ok(subIncident);
    }

    @PutMapping("/{id}/solution")
    @ResponseBody
    public ResponseEntity<Incident> updateSolution(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String solution = payload.get("solution");
        Incident updated = incidentService.updateIncidentSolution(id, solution);
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
        String update = payload.get("update");
        Incident updated = incidentService.addSolutionUpdate(id, update);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/close")
    @ResponseBody
    public ResponseEntity<Incident> closeIncident(@PathVariable Long id) {
        Incident closed = incidentService.closeIncident(id);
        if (closed != null) {
            return ResponseEntity.ok(closed);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Thymeleaf UI endpoint (no @ResponseBody)
    @GetMapping("/solutions-ui")
    public String solutionsPage(@RequestParam(required = false) String description, Model model) {
        List<Incident> incidents = incidentService.findSolutions(description);
        model.addAttribute("incidents", incidents);
        model.addAttribute("description", description);
        return "solutions"; // This matches solutions.html in templates
    }

    @PostMapping("/solution-by-description")
    public String updateSolutionFromForm(
            @RequestParam String incidentDescription,
            @RequestParam String solution,
            @RequestParam(required = false) String description, // to preserve search/filter if needed
            Model model) {
        // Find or create incident by description and update its solution
        Incident incident = incidentService.handleIncident(incidentDescription);
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
        
        List<Incident> allIncidents = incidentService.getAllIncidents();
        List<Incident> filteredIncidents = allIncidents;
        
        // Apply search filter
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase();
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
        long totalIncidents = allIncidents.size();
        long closedIncidents = allIncidents.stream()
            .filter(incident -> "CLOSED".equals(incident.getStatus()))
            .count();
        long inProgressIncidents = allIncidents.stream()
            .filter(incident -> "IN_PROGRESS".equals(incident.getStatus()))
            .count();
        long openIncidents = allIncidents.stream()
            .filter(incident -> "OPEN".equals(incident.getStatus()) || incident.getStatus() == null)
            .count();
        long subIncidents = allIncidents.stream()
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
        
        return "table";
    }
}