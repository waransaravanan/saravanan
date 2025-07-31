# Incident Blocker Tracking - API Usage Examples

This document provides examples of how to use the new incident blocker tracking functionality via REST API calls.

## Scenario: SSO to Permission Issue Chain

### Step 1: Create Initial SSO Incident
```bash
curl -X POST http://localhost:8080/api/incidents \
  -H "Content-Type: application/json" \
  -d '{
    "description": "SSO authentication not working - users cannot login"
  }'
```

Response:
```json
{
  "id": 1,
  "incidentNumber": "INC-0001",
  "description": "SSO authentication not working - users cannot login",
  "solution": null,
  "status": "OPEN",
  "parentIncidentId": null,
  "createdAt": "2025-07-27T10:00:00",
  "solutionLog": [
    "[2025-07-27 10:00:00] Ticket created: SSO authentication not working - users cannot login"
  ]
}
```

### Step 2: Resolve SSO Issue and Create Permission Blocker
```bash
curl -X POST http://localhost:8080/api/incidents/1/resolve-and-create-blocker \
  -H "Content-Type: application/json" \
  -d '{
    "solution": "SSO service was restarted and authentication endpoint was fixed",
    "newBlockerDescription": "User role/permission issue - user cannot access main dashboard after SSO login"
  }'
```

Response:
```json
{
  "id": 2,
  "incidentNumber": "INC-0002",
  "description": "User role/permission issue - user cannot access main dashboard after SSO login",
  "solution": null,
  "status": "OPEN",
  "parentIncidentId": 1,
  "createdAt": "2025-07-27T10:30:00",
  "solutionLog": [
    "[2025-07-27 10:30:00] New blocker incident created from resolved incident INC-0001: User role/permission issue - user cannot access main dashboard after SSO login"
  ]
}
```

### Step 3: Get All Child Incidents
```bash
curl -X GET http://localhost:8080/api/incidents/1/children
```

Response:
```json
[
  {
    "id": 2,
    "incidentNumber": "INC-0002",
    "description": "User role/permission issue - user cannot access main dashboard after SSO login",
    "solution": null,
    "status": "OPEN",
    "parentIncidentId": 1,
    "createdAt": "2025-07-27T10:30:00"
  }
]
```

### Step 4: Create Follow-up Incident for Role Update
```bash
curl -X POST http://localhost:8080/api/incidents/2/create-followup \
  -H "Content-Type: application/json" \
  -d '{
    "newIssueDescription": "User role needs to be updated from guest to employee in Active Directory",
    "parentSolutionApplied": "Verified user permissions in application - role assignment is incorrect"
  }'
```

Response:
```json
{
  "id": 3,
  "incidentNumber": "INC-0003",
  "description": "User role needs to be updated from guest to employee in Active Directory",
  "solution": null,
  "status": "OPEN",
  "parentIncidentId": 2,
  "createdAt": "2025-07-27T11:00:00",
  "solutionLog": [
    "[2025-07-27 11:00:00] Follow-up incident created from INC-0002 after applying solution 'Verified user permissions in application - role assignment is incorrect'. New issue: User role needs to be updated from guest to employee in Active Directory"
  ]
}
```

### Step 5: Get Complete Incident Hierarchy
```bash
curl -X GET http://localhost:8080/api/incidents/1/hierarchy
```

Response:
```json
[
  {
    "id": 1,
    "incidentNumber": "INC-0001",
    "description": "SSO authentication not working - users cannot login",
    "solution": "SSO service was restarted and authentication endpoint was fixed",
    "status": "IN_PROGRESS",
    "parentIncidentId": null,
    "solutionLog": [
      "[2025-07-27 10:00:00] Ticket created: SSO authentication not working - users cannot login",
      "[2025-07-27 10:30:00] Solution added: SSO service was restarted and authentication endpoint was fixed",
      "[2025-07-27 10:30:00] Incident resolved. New blocker identified: User role/permission issue - user cannot access main dashboard after SSO login"
    ]
  },
  {
    "id": 2,
    "incidentNumber": "INC-0002",
    "description": "User role/permission issue - user cannot access main dashboard after SSO login",
    "solution": "Verified user permissions in application - role assignment is incorrect",
    "status": "IN_PROGRESS",
    "parentIncidentId": 1,
    "solutionLog": [
      "[2025-07-27 10:30:00] New blocker incident created from resolved incident INC-0001: User role/permission issue - user cannot access main dashboard after SSO login",
      "[2025-07-27 11:00:00] Solution applied: Verified user permissions in application - role assignment is incorrect",
      "[2025-07-27 11:00:00] Follow-up incident created for new issue: User role needs to be updated from guest to employee in Active Directory"
    ]
  },
  {
    "id": 3,
    "incidentNumber": "INC-0003",
    "description": "User role needs to be updated from guest to employee in Active Directory",
    "solution": null,
    "status": "OPEN",
    "parentIncidentId": 2,
    "solutionLog": [
      "[2025-07-27 11:00:00] Follow-up incident created from INC-0002 after applying solution 'Verified user permissions in application - role assignment is incorrect'. New issue: User role needs to be updated from guest to employee in Active Directory"
    ]
  }
]
```

## Alternative Scenario: Direct Subtask Creation

If you prefer to create subtasks manually without resolving the parent first:

```bash
curl -X POST http://localhost:8080/api/incidents/1/subtask \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Database connection timeout after SSO fix"
  }'
```

## Filtering and Querying

### Get all incidents with filtering
```bash
# Get all open incidents
curl -X GET "http://localhost:8080/api/incidents/table?status=open"

# Get all sub-incidents
curl -X GET "http://localhost:8080/api/incidents/table?type=sub"

# Search for specific terms
curl -X GET "http://localhost:8080/api/incidents/table?search=SSO"
```

## Example Workflow Summary

1. **Initial Report**: User reports SSO issue (INC-0001)
2. **First Resolution**: SSO is fixed, but permission issue discovered → Auto-create INC-0002
3. **Investigation**: Permission issue analyzed, role assignment problem found → Create INC-0003
4. **Final Resolution**: Role updated in AD → Close INC-0003, then INC-0002, then INC-0001

This creates a clear audit trail showing:
- What the original problem was
- How it was resolved
- What new issues were discovered
- How those were tracked and resolved
- Complete timeline of all actions

Each incident maintains its own solution log while being linked to related incidents, making it easy to track the complete resolution process and reuse solutions for similar future problems.
