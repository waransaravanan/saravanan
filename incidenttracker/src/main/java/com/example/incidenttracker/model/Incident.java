package com.example.incidenttracker.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String incidentNumber; // Format: INC-XXXX
    private String description;
    private String solution;
    private Long parentIncidentId; // null for main incidents, set for subtasks
    
    // New fields for enhanced ticketing
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private String status; // OPEN, IN_PROGRESS, CLOSED
    
    @ElementCollection
    @CollectionTable(name = "incident_solution_log", joinColumns = @JoinColumn(name = "incident_id"))
    private List<String> solutionLog = new ArrayList<>();



// Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public Long getParentIncidentId() {
        return parentIncidentId;
    }

    public void setParentIncidentId(Long parentIncidentId) {
        this.parentIncidentId = parentIncidentId;
    }

    public String getIncidentNumber() {
        return incidentNumber;
    }

    public void setIncidentNumber(String incidentNumber) {
        this.incidentNumber = incidentNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getSolutionLog() {
        return solutionLog;
    }

    public void setSolutionLog(List<String> solutionLog) {
        this.solutionLog = solutionLog;
    }

    public void addSolutionLogEntry(String entry) {
        if (this.solutionLog == null) {
            this.solutionLog = new ArrayList<>();
        }
        this.solutionLog.add(entry);
    }

    /**
     * Checks if this incident is a root incident (has no parent)
     * @return true if this is a root incident, false otherwise
     */
    public boolean isRootIncident() {
        return this.parentIncidentId == null;
    }

    /**
     * Checks if this incident is a child/subtask incident (has a parent)
     * @return true if this is a child incident, false otherwise
     */
    public boolean isChildIncident() {
        return this.parentIncidentId != null;
    }

    /**
     * Checks if this incident is currently open
     * @return true if status is OPEN, false otherwise
     */
    public boolean isOpen() {
        return "OPEN".equals(this.status);
    }

    /**
     * Checks if this incident is currently in progress
     * @return true if status is IN_PROGRESS, false otherwise
     */
    public boolean isInProgress() {
        return "IN_PROGRESS".equals(this.status);
    }

    /**
     * Checks if this incident is closed
     * @return true if status is CLOSED, false otherwise
     */
    public boolean isClosed() {
        return "CLOSED".equals(this.status);
    }

    /**
     * Checks if this incident has a solution
     * @return true if solution is not null and not empty, false otherwise
     */
    public boolean hasSolution() {
        return this.solution != null && !this.solution.trim().isEmpty();
    }
}
