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

    // Suggestion UI GET
    @GetMapping("/suggestion-ui")
    public String suggestionPage(Model model) {
        model.addAttribute("incident", new Incident());
        // Get all incident descriptions for dropdown
        List<String> incidentDescriptions = incidentService.getAllIncidents()
                .stream()
                .map(Incident::getDescription)
                .distinct()
                .collect(Collectors.toList());
        model.addAttribute("incidentDescriptions", incidentDescriptions);
        return "suggestions";
    }

    // Suggestion POST
    @PostMapping("/suggestion")
    public String getSuggestion(@ModelAttribute Incident incident, Model model) {
        // Find all suggestions (solutions) for any keyword in the incident description
        List<Incident> solutions = incidentService.findSolutions(incident.getDescription());
        solutions.forEach(s -> System.out.println("Solutions : "+s));
        List<Incident> relevantIncidents = incidentService.getAllIncidents();
        relevantIncidents.forEach(inc -> System.out.println("All Incidents : "+inc));
        // Filter solutions where the description matches (case-insensitive)
        String inputDescription = incident.getDescription().toLowerCase();
        List<Incident> relevantSolutions = solutions.stream()
                .filter(s -> s.getDescription() != null && 
                           (s.getDescription().toLowerCase().contains(inputDescription) ||
                            inputDescription.contains(s.getDescription().toLowerCase())))
                .collect(Collectors.toList());
                relevantSolutions.forEach(s -> System.out.println("Relevant Solutions : "+s));
        
        List<String> suggestions = relevantSolutions.stream()
            .map(Incident::getSolution)
            .filter(sol -> sol != null && !sol.isEmpty())
            .distinct()
            .collect(Collectors.toList());
            suggestions.forEach(s -> System.out.println("Suggestions: "+s));

        // If no existing solutions found, get AI suggestion
        String aiSuggestion = null;
        if (suggestions.isEmpty()) {
            aiSuggestion = openAISuggestionService.getSuggestion(incident.getDescription());
        }

        model.addAttribute("incident", incident);
        model.addAttribute("suggestions", suggestions);
        model.addAttribute("aiSuggestion", aiSuggestion);
        // Repopulate incidentDescriptions for dropdown
        List<String> incidentDescriptions = incidentService.getAllIncidents()
                .stream()
                .map(Incident::getDescription)
                .distinct()
                .collect(Collectors.toList());
                incidentDescriptions.forEach(desc -> System.out.println("Incident Description: "+desc));
        model.addAttribute("incidentDescriptions", incidentDescriptions);
        return "suggestions";
    }

    // Manual solution POST
    @PostMapping("/manual-solution")
    public String submitManualSolution(@RequestParam String description, @RequestParam String manualSolution, Model model) {
        // Save manual solution as a new incident or update existing
        Incident incident = incidentService.handleIncident(description);
        incidentService.updateIncidentSolution(incident.getId(), manualSolution);
        model.addAttribute("incident", incident);
        model.addAttribute("manualSolution", manualSolution);
        model.addAttribute("successMessage", "Solution saved successfully!");
        // Repopulate incidentDescriptions for dropdown
        List<String> incidentDescriptions = incidentService.getAllIncidents()
                .stream()
                .map(Incident::getDescription)
                .distinct()
                .collect(Collectors.toList());
        model.addAttribute("incidentDescriptions", incidentDescriptions);
        return "suggestions";
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
        // Redirect to the solutions UI, optionally with the current description filter
        if (description != null && !description.isEmpty()) {
            return "redirect:/api/incidents/solutions-ui?description=" + description;
        }
        return "redirect:/api/incidents/solutions-ui";
    }

    // Select suggestion and update solution
    @PostMapping("/select-suggestion")
    public String selectSuggestion(@RequestParam String description, @RequestParam String selectedSuggestion, Model model) {
        Incident incident = incidentService.handleIncident(description);
        incidentService.updateIncidentSolution(incident.getId(), selectedSuggestion);
        return "redirect:/api/incidents/solutions-ui?description=" + description;
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
            if ("solved".equals(status)) {
                filteredIncidents = filteredIncidents.stream()
                    .filter(incident -> incident.getSolution() != null && !incident.getSolution().trim().isEmpty())
                    .collect(Collectors.toList());
            } else if ("pending".equals(status)) {
                filteredIncidents = filteredIncidents.stream()
                    .filter(incident -> incident.getSolution() == null || incident.getSolution().trim().isEmpty())
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
        long solvedIncidents = allIncidents.stream()
            .filter(incident -> incident.getSolution() != null && !incident.getSolution().trim().isEmpty())
            .count();
        long pendingIncidents = totalIncidents - solvedIncidents;
        long subIncidents = allIncidents.stream()
            .filter(incident -> incident.getParentIncidentId() != null)
            .count();
        
        model.addAttribute("incidents", filteredIncidents);
        model.addAttribute("totalIncidents", totalIncidents);
        model.addAttribute("solvedIncidents", solvedIncidents);
        model.addAttribute("pendingIncidents", pendingIncidents);
        model.addAttribute("subIncidents", subIncidents);
        model.addAttribute("searchTerm", search);
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        
        return "table";
    }
}