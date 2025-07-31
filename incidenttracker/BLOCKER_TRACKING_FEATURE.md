# Incident Blocker Tracking Feature

## Overview
This feature implements automatic creation of new incidents when a resolved incident reveals additional blockers or issues that need to be tracked separately. This is particularly useful in scenarios where solving one problem uncovers another problem that requires its own tracking and resolution process.

## Use Case Example
**Scenario**: User reports SSO authentication issue
1. **Initial Incident**: "SSO authentication not working" (INC-0001)
2. **Resolution Applied**: SSO service restarted and authentication endpoint fixed
3. **New Blocker Discovered**: After SSO is fixed, user still cannot access dashboard due to role/permission issues
4. **Automatic Action**: System creates new incident "Role/permission issue - user cannot access dashboard" (INC-0002) linked to the original incident (INC-0001)

## Key Features

### 1. Resolve Incident and Create Blocker
- **Endpoint**: `POST /api/incidents/{id}/resolve-and-create-blocker`
- **Purpose**: Resolves an incident with a solution and simultaneously creates a new incident for any discovered blocker
- **Payload**:
  ```json
  {
    "solution": "SSO service restarted and authentication endpoint fixed",
    "newBlockerDescription": "User role/permission issue - cannot access main dashboard after SSO login"
  }
  ```

### 2. Create Follow-up Incident
- **Endpoint**: `POST /api/incidents/{parentId}/create-followup`
- **Purpose**: Creates a new incident that follows up on a parent incident after a solution is applied
- **Payload**:
  ```json
  {
    "newIssueDescription": "User role needs to be updated from 'guest' to 'employee'",
    "parentSolutionApplied": "Reset user password and enabled MFA"
  }
  ```

### 3. Get Child Incidents
- **Endpoint**: `GET /api/incidents/{id}/children`
- **Purpose**: Retrieves all direct child incidents (subtasks/blockers) for a given incident

### 4. Get Incident Hierarchy
- **Endpoint**: `GET /api/incidents/{id}/hierarchy`
- **Purpose**: Retrieves the complete hierarchy of incidents starting from a root incident

## Database Schema Changes

The `Incident` entity already supports parent-child relationships through the `parentIncidentId` field:

```java
private Long parentIncidentId; // null for main incidents, set for subtasks/blockers
```

## New Service Methods

### `resolveIncidentAndCreateBlockerIncident()`
- Updates the original incident with the provided solution
- Creates a new incident for the blocker if description is provided
- Links the new incident to the original via `parentIncidentId`
- Logs the relationship in both incidents' solution logs

### `createFollowUpIncident()`
- Creates a follow-up incident when a resolved incident leads to discovering a new issue
- Updates parent incident with solution if not already set
- Links the new incident to the parent
- Maintains detailed audit trail in solution logs

### `getChildIncidents()` & `getIncidentHierarchy()`
- Efficiently retrieve related incidents using new repository methods
- Support complex incident hierarchies with multiple levels

## Audit Trail

All actions are logged in the `solutionLog` field with timestamps:
- When an incident is resolved and a blocker is identified
- When a follow-up incident is created
- References between parent and child incidents

Example log entries:
```
[2025-07-27 10:30:15] Incident resolved. New blocker identified: Role/permission issue
[2025-07-27 10:30:15] New blocker incident created from resolved incident INC-0001: Role/permission issue
[2025-07-27 10:35:20] Follow-up incident created from INC-0001 after applying solution 'SSO fixed'. New issue: Role assignment needed
```

## New Model Helper Methods

Added convenience methods to the `Incident` model:
- `isRootIncident()` - Check if incident has no parent
- `isChildIncident()` - Check if incident has a parent
- `isOpen()`, `isInProgress()`, `isClosed()` - Status checking
- `hasSolution()` - Check if incident has a solution

## Usage Examples

### Example 1: SSO to Permission Issue Chain
```java
// Original SSO incident is resolved, but permission issue discovered
Incident blockerIncident = incidentService.resolveIncidentAndCreateBlockerIncident(
    ssoIncidentId, 
    "SSO service restarted and authentication endpoint fixed",
    "User role/permission issue - cannot access main dashboard after SSO login"
);
```

### Example 2: Sequential Issue Discovery
```java
// After fixing password, discover role needs update
Incident followUpIncident = incidentService.createFollowUpIncident(
    originalIncidentId,
    "User role needs to be updated from 'guest' to 'employee'",
    "Reset user password and enabled MFA"
);
```

## Benefits

1. **Automatic Tracking**: No manual intervention needed to create related incidents
2. **Audit Trail**: Complete history of how incidents relate to each other
3. **Hierarchical Organization**: Clear parent-child relationships
4. **Reference Tracking**: Easy to find all related issues for a given incident
5. **Solution Reuse**: Historical solutions are preserved and can be referenced for similar future issues

## Testing

Comprehensive test suite included in `IncidentServiceBlockerTest.java` covering:
- Resolve and create blocker scenarios
- Follow-up incident creation
- Child incident retrieval
- Incident hierarchy traversal
- Helper method functionality
