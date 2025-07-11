package com.example.incidenttracker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.example.incidenttracker.model.Incident;
import com.example.incidenttracker.service.IncidentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Controller // <-- Change from @RestController to @Controller
@RequestMapping("/api/incidents")
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    @PostMapping
    @ResponseBody // Still return JSON for API
    public ResponseEntity<Incident> createIncident(@RequestBody Incident incident) {
        Incident createdIncident = incidentService.saveIncident(incident);
        return ResponseEntity
                .created(URI.create("/api/incidents/" + createdIncident.getId()))
                .body(createdIncident);
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
            @RequestParam String solution) {
        Incident updated = incidentService.updateIncidentSolution(id, solution);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Thymeleaf UI endpoint (no @ResponseBody)
    @GetMapping("/solutions-ui")
    public String solutionsPage(@RequestParam(required = false) String description, Model model) {
        List<Incident> incidents = incidentService.findSolutions(description != null ? description : "");
        model.addAttribute("incidents", incidents);
        model.addAttribute("description", description);
        return "solutions"; // This matches solutions.html in templates
    }

    @PostMapping("/{id}/solution")
    public String updateSolutionFromForm(
            @PathVariable Long id,
            @RequestParam String solution,
            @RequestParam(required = false) String description, // to preserve search/filter if needed
            Model model) {
        incidentService.updateIncidentSolution(id, solution);
        // Redirect to the solutions UI, optionally with the current description filter
        if (description != null && !description.isEmpty()) {
            return "redirect:/api/incidents/solutions-ui?description=" + description;
        }
        return "redirect:/api/incidents/solutions-ui";
    }
}