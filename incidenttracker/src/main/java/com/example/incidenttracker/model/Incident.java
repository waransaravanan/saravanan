package com.example.incidenttracker.model;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Entity;
import javax.persistence.Id;


@Entity
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String description;
    private String solution;
    private Long parentIncidentId; // null for main incidents, set for subtasks



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
}
