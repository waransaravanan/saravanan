package com.example.incidenttracker.service;

import com.example.incidenttracker.model.Incident;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class IncidentServiceBlockerTest {

    @Autowired
    private IncidentService incidentService;

    private Incident testIncident;

    @BeforeEach
    void setUp() {
        // Create a test incident
        testIncident = new Incident();
        testIncident.setDescription("SSO authentication not working");
        testIncident = incidentService.saveIncident(testIncident);
    }

    @Test
    void testResolveIncidentAndCreateBlockerIncident() {
        // Test the scenario: SSO issue is resolved, but user permission issue is discovered
        String ssoSolution = "SSO service was restarted and authentication endpoint was fixed";
        String permissionBlocker = "User role/permission issue - user cannot access the main dashboard after SSO login";

        // Resolve the SSO incident and create a new incident for the permission blocker
        Incident blockerIncident = incidentService.resolveIncidentAndCreateBlockerIncident(
            testIncident.getId(), 
            ssoSolution, 
            permissionBlocker
        );

        // Verify the original incident was updated with the solution
        Incident updatedOriginal = incidentService.findIncidentById(testIncident.getId()).orElse(null);
        assertNotNull(updatedOriginal);
        assertEquals(ssoSolution, updatedOriginal.getSolution());
        assertTrue(updatedOriginal.getSolutionLog().stream()
            .anyMatch(log -> log.contains("Incident resolved. New blocker identified")));

        // Verify the new blocker incident was created
        assertNotNull(blockerIncident);
        assertEquals(permissionBlocker, blockerIncident.getDescription());
        assertEquals(testIncident.getId(), blockerIncident.getParentIncidentId());
        assertEquals("OPEN", blockerIncident.getStatus());
        assertNull(blockerIncident.getSolution());
        assertTrue(blockerIncident.getIncidentNumber().startsWith("INC-"));
        assertTrue(blockerIncident.isChildIncident());
        assertFalse(blockerIncident.isRootIncident());
    }

    @Test
    void testCreateFollowUpIncident() {
        // Test the scenario where a solution is applied but reveals another issue
        String appliedSolution = "Reset user password and enabled MFA";
        String followUpIssue = "User role needs to be updated from 'guest' to 'employee' for proper access";

        // Create a follow-up incident
        Incident followUpIncident = incidentService.createFollowUpIncident(
            testIncident.getId(),
            followUpIssue,
            appliedSolution
        );

        // Verify the parent incident was updated
        Incident updatedParent = incidentService.findIncidentById(testIncident.getId()).orElse(null);
        assertNotNull(updatedParent);
        assertEquals(appliedSolution, updatedParent.getSolution());
        assertTrue(updatedParent.getSolutionLog().stream()
            .anyMatch(log -> log.contains("Follow-up incident created")));

        // Verify the follow-up incident
        assertNotNull(followUpIncident);
        assertEquals(followUpIssue, followUpIncident.getDescription());
        assertEquals(testIncident.getId(), followUpIncident.getParentIncidentId());
        assertEquals("OPEN", followUpIncident.getStatus());
        assertTrue(followUpIncident.getSolutionLog().stream()
            .anyMatch(log -> log.contains("Follow-up incident created from")));
        assertTrue(followUpIncident.isChildIncident());
    }

    @Test
    void testGetChildIncidents() {
        // Create multiple child incidents
        Incident child1 = incidentService.createSubIncident(testIncident.getId(), "Permission issue");
        Incident child2 = incidentService.createFollowUpIncident(testIncident.getId(), "Role assignment needed", "SSO fixed");

        // Get all children
        var children = incidentService.getChildIncidents(testIncident.getId());

        assertEquals(2, children.size());
        assertTrue(children.stream().anyMatch(inc -> inc.getId().equals(child1.getId())));
        assertTrue(children.stream().anyMatch(inc -> inc.getId().equals(child2.getId())));
    }

    @Test
    void testGetIncidentHierarchy() {
        // Create a hierarchy: parent -> child -> grandchild
        Incident child = incidentService.createSubIncident(testIncident.getId(), "Child issue");
        Incident grandchild = incidentService.createSubIncident(child.getId(), "Grandchild issue");

        // Get complete hierarchy
        var hierarchy = incidentService.getIncidentHierarchy(testIncident.getId());

        assertEquals(3, hierarchy.size());
        assertEquals(testIncident.getId(), hierarchy.get(0).getId()); // Root first
        assertTrue(hierarchy.stream().anyMatch(inc -> inc.getId().equals(child.getId())));
        assertTrue(hierarchy.stream().anyMatch(inc -> inc.getId().equals(grandchild.getId())));
    }

    @Test
    void testIncidentHelperMethods() {
        // Test new helper methods
        assertTrue(testIncident.isRootIncident());
        assertFalse(testIncident.isChildIncident());
        assertTrue(testIncident.isOpen());
        assertFalse(testIncident.isClosed());
        assertFalse(testIncident.hasSolution());

        // Create child incident and test
        Incident child = incidentService.createSubIncident(testIncident.getId(), "Child issue");
        assertFalse(child.isRootIncident());
        assertTrue(child.isChildIncident());

        // Add solution and test
        incidentService.updateIncidentSolution(child.getId(), "Child solution");
        Incident updatedChild = incidentService.findIncidentById(child.getId()).orElse(null);
        assertTrue(updatedChild.hasSolution());
        assertTrue(updatedChild.isInProgress());
    }
}
