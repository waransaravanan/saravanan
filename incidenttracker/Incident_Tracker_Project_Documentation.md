# Incident Tracker Project - Complete Functionality Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [System Architecture](#system-architecture)
3. [Core Classes and Methods](#core-classes-and-methods)
4. [Functionality Breakdown](#functionality-breakdown)
5. [API Endpoints](#api-endpoints)
6. [Database Schema](#database-schema)
7. [User Interface Components](#user-interface-components)
8. [Integration Features](#integration-features)

---

## Project Overview

The **Incident Tracker** is a comprehensive rule-based IT support system built using Spring Boot. It provides intelligent chat-based incident reporting, automated solution suggestions, and comprehensive ticket management capabilities.

### Key System Capabilities:
- **Intelligent Chat Interface** with button-driven responses
- **Rule-Based Solution Suggestions** with built-in fallback system
- **Advanced Fuzzy Matching** for similar incident detection
- **Comprehensive Ticket Lifecycle Management**
- **Hierarchical Incident Structure** (parent-child relationships)
- **Real-time Statistics Dashboard**
- **Solution Knowledge Base** with audit trails

---

## System Architecture

### Technology Stack:
- **Backend Framework**: Spring Boot 2.x
- **Database**: H2 (in-memory) with JPA/Hibernate
- **Frontend**: Thymeleaf, Bootstrap 5, JavaScript
- **Suggestion System**: Built-in rule-based suggestion engine
- **Session Management**: HTTP Sessions
- **Build Tool**: Maven

### Package Structure:
```
com.example.incidenttracker/
├── IncidentTrackerApplication.java (Main Application)
├── controller/
│   ├── ChatController.java
│   └── IncidentController.java
├── model/
│   └── Incident.java
├── repository/
│   └── IncidentRepository.java
└── service/
    ├── ChatService.java
    └── IncidentService.java
```

---

## Core Classes and Methods

### 1. IncidentTrackerApplication.java
**Purpose**: Main Spring Boot application entry point

#### Methods:
- `main(String[] args)`: Application startup method
- `hiddenHttpMethodFilter()`: Bean configuration for HTTP method filtering

---

### 2. Incident.java (Model Class)
**Purpose**: Entity model representing IT incidents with solution tracking

#### Attributes:
- `id`: Primary key (Long)
- `incidentNumber`: Unique identifier (String) - Format: INC-XXXX
- `description`: Issue description (String)
- `solution`: Main solution text (String)
- `parentIncidentId`: Reference to parent incident (Long)
- `createdAt`: Creation timestamp (LocalDateTime)
- `updatedAt`: Last modification timestamp (LocalDateTime)
- `closedAt`: Closure timestamp (LocalDateTime)
- `status`: Current status (String) - OPEN, IN_PROGRESS, CLOSED
- `solutionLog`: List of solution updates (List&lt;String&gt;)

#### Methods:
- **Status Check Methods**:
  - `isRootIncident()`: Returns true if incident has no parent
  - `isChildIncident()`: Returns true if incident has a parent
  - `isOpen()`: Returns true if status is "OPEN"
  - `isInProgress()`: Returns true if status is "IN_PROGRESS"
  - `isClosed()`: Returns true if status is "CLOSED"
  - `hasSolution()`: Returns true if solution exists

- **Utility Methods**:
  - `addSolutionLogEntry(String entry)`: Adds timestamped entry to solution log
  - Standard getters and setters for all attributes

---

### 3. IncidentRepository.java (Repository Interface)
**Purpose**: JPA Repository for database operations

#### Methods:
- `findByDescriptionContainingIgnoreCase(String description)`: Search by partial description
- `findByDescriptionIgnoreCase(String description)`: Find exact description match
- `findByParentIncidentId(Long parentId)`: Get child incidents
- `findByParentIncidentIdIsNull()`: Get root incidents only
- `findByStatus(String status)`: Filter by incident status
- Standard JpaRepository methods (save, findById, findAll, delete, etc.)

---

### 4. IncidentService.java (Business Logic Layer)
**Purpose**: Core business logic for incident management

#### Primary Methods:

##### Incident Creation and Management:
- `saveIncident(Incident incident)`: 
  - Generates incident number if not set
  - Sets creation timestamp and status
  - Adds creation log entry
  - Returns saved incident

- `createIncidentWithoutSolution(String description)`:
  - Creates new incident for ticket creation scenarios
  - Sets status to "OPEN"
  - Generates unique incident number
  - Adds timestamped creation log

- `createSubIncident(Long parentId, String description)`:
  - Creates child incident linked to parent
  - Validates parent existence
  - Sets parent-child relationship
  - Returns created sub-incident

##### Solution Management:
- `updateIncidentSolution(Long id, String solution)`:
  - Updates main solution field
  - Adds timestamped solution log entry
  - Changes status from OPEN to IN_PROGRESS
  - Returns updated incident

- `addSolutionUpdate(Long id, String update)`:
  - Adds solution update without changing main solution
  - Maintains complete audit trail
  - Updates incident status if needed
  - Returns updated incident

##### Incident Resolution:
- `closeIncident(Long id)`:
  - Sets status to "CLOSED"
  - Records closure timestamp
  - Adds closure log entry
  - Returns closed incident

##### Advanced Incident Workflows:
- `resolveIncidentAndCreateBlockerIncident(Long id, String solution, String newBlockerDescription)`:
  - Resolves original incident with solution
  - Creates new incident for discovered blocker
  - Links new incident as child of original
  - Returns newly created blocker incident
  - **Use Case**: When solving one issue reveals another problem

- `createFollowUpIncident(Long parentId, String newIssueDescription, String parentSolutionApplied)`:
  - Creates follow-up incident after parent resolution
  - Updates parent with applied solution
  - Links follow-up as child incident
  - Returns new follow-up incident
  - **Use Case**: Sequential problem discovery workflow

##### Search and Discovery:
- `findSolutions(String description)`:
  - Filters incidents by description (case-insensitive)
  - Returns all incidents if no filter provided
  - Used for solution knowledge base

- `handleIncident(String description)`:
  - Finds existing incidents with same description
  - Returns incident with solution if exists
  - Creates new incident if not found
  - Core method for incident processing

##### Hierarchy Management:
- `getChildIncidents(Long parentId)`: Returns all direct child incidents
- `getIncidentHierarchy(Long rootId)`: Returns complete incident tree
- `addChildrenToHierarchy(Long parentId, List&lt;Incident&gt; hierarchy)`: Recursive helper for hierarchy building

##### Utility Methods:
- `generateIncidentNumber()`: Creates unique INC-XXXX format numbers
- `getAllIncidents()`: Returns all incidents in system

---

### 5. ChatService.java (Conversation Management)
**Purpose**: Manages intelligent chat conversations with button-driven responses

#### Inner Classes:
- **ChatResponse**: Represents bot response with message, state, and buttons
- **ChatButton**: Represents clickable action buttons with text, action, and style
- **ChatSession**: Manages conversation state and history
- **ChatMessage**: Individual chat message with sender, content, and timestamp
- **ChatState**: Enum defining conversation states

#### Core Conversation Methods:

##### Message Processing:
- `processMessage(String sessionId, String message)`:
  - Processes user text input
  - Routes to appropriate handler based on session state
  - Stores message in conversation history
  - Returns ChatResponse with next steps

- `processButtonAction(String sessionId, String action)`:
  - Handles button click actions
  - Routes to specific action handlers
  - Maintains conversation flow state
  - Returns appropriate response

##### Conversation Flow Handlers:
- `handleInitialMessage(ChatSession session, String message)`:
  - Processes first user message describing issue
  - Searches for existing similar solutions
  - Returns existing solutions or offers built-in suggestion
  - **Returns**: Solutions with Yes/No buttons OR suggestion confirmation prompt

- `handleSuggestionConfirmation(ChatSession session, String message)`:
  - Processes user response to built-in suggestion offer
  - Calls built-in suggestion service if user agrees
  - Offers ticket creation if user declines
  - **Returns**: Built-in suggestion with helpful buttons OR ticket confirmation

- `handleSuggestionHelpfulResponse(ChatSession session, String message)`:
  - Processes feedback on built-in suggestion quality
  - Saves solution if helpful
  - Offers alternatives if not helpful
  - **Returns**: Success message OR alternative options

##### Yes/No Response Handlers:
- `handleHelpYes(ChatSession session)`:
  - User confirms solution was helpful
  - Asks about other issues
  - **Returns**: Other issues prompt with Yes/No buttons

- `handleHelpNo(ChatSession session)`:
  - User indicates solution wasn't helpful
  - Offers three options: new AI suggestion, create ticket, close session
  - **Returns**: Option selection prompt

- `handleOtherIssuesYes(ChatSession session)`:
  - User has additional issues
  - Resets session for new issue
  - **Returns**: Prompt for new issue description

- `handleOtherIssuesNo(ChatSession session)`:
  - User has no more issues
  - Closes session with goodbye message
  - **Returns**: Session closure confirmation

##### Advanced Flow Handlers:
- `handleOptionSelection(ChatSession session, String message)`:
  - Processes numbered option choices (1, 2, 3)
  - Option 1: New AI suggestion
  - Option 2: Create support ticket
  - Option 3: Close session
  - **Returns**: Appropriate next step based on selection

- `handleTicketConfirmation(ChatSession session, String message)`:
  - Processes ticket creation confirmation
  - Creates incident via IncidentService
  - **Returns**: Ticket creation confirmation OR decline handling

##### Session Management:
- `clearConversationHistory(String sessionId)`: Clears chat history
- `clearSession(String sessionId)`: Removes entire session
- `getConversationHistory(String sessionId)`: Returns message history

##### Intelligent Matching and Search:
- `findSimilarSolutions(String description)`:
  - Uses fuzzy matching algorithm
  - Extracts meaningful keywords
  - Scores solutions by relevance
  - Filters by solution availability
  - **Returns**: Top 3 most relevant incidents

- `calculateContextualSimilarity(String query, String incident)`:
  - Advanced similarity calculation
  - Domain-specific keyword matching
  - Phrase matching bonuses
  - **Returns**: Similarity score (0.0 to 1.0)

- `extractMeaningfulKeywords(String description)`:
  - Removes stop words
  - Identifies IT domain terms
  - Groups related concepts
  - **Returns**: List of relevant keywords

##### Response Recognition:
- `isPositiveResponse(String message)`: Detects yes/positive responses
- `isNegativeResponse(String message)`: Detects no/negative responses
- **Recognized positive terms**: yes, y, yeah, yep, sure, ok, okay, fine, alright, good, positive, yup, correct, right, true
- **Recognized negative terms**: no, n, nope, not, negative, nah, never, false

---

### 6. ChatController.java (Chat API Layer)

---

### 6. ChatController.java (Chat API Layer)
**Purpose**: REST endpoints for chat interface

#### Endpoints:
- `GET /`: Returns chat.html template
- `POST /api/chat/message`: Processes text messages
- `POST /api/chat/button`: Handles button actions
- `GET /api/chat/history`: Returns conversation history
- `POST /api/chat/clear`: Clears conversation history
- `POST /api/chat/close`: Closes chat session

#### Methods:
- `sendMessage(@RequestBody Map&lt;String, String&gt; payload, HttpSession session)`:
  - Validates message content
  - Uses session ID for conversation continuity
  - Delegates to ChatService
  - **Returns**: ChatResponse with next conversation step

- `handleButtonResponse(@RequestBody Map&lt;String, String&gt; payload, HttpSession session)`:
  - Processes button click actions
  - Maintains session state
  - **Returns**: ChatResponse for button action result

---

### 7. IncidentController.java (Incident Management API)
**Purpose**: REST endpoints for incident CRUD operations and UI

#### Core API Endpoints:

##### Incident Management:
- `POST /api/incidents`: Creates new incident
- `GET /api/incidents`: Returns all incidents
- `GET /api/incidents/solutions`: Searches solutions by description
- `POST /api/incidents/chat`: Handles incident via chat flow

##### Solution Management:
- `PUT /api/incidents/{id}/solution`: Updates incident solution
- `POST /api/incidents/{id}/solution-update`: Adds solution update
- `PUT /api/incidents/{id}/close`: Closes incident

##### Advanced Workflows:
- `POST /api/incidents/{id}/resolve-and-create-blocker`:
  - Resolves incident and creates blocker incident
  - Handles discovered issues workflow
  - **Returns**: Newly created blocker incident

- `POST /api/incidents/{parentId}/create-followup`:
  - Creates follow-up incident after resolution
  - Links to parent incident
  - **Returns**: New follow-up incident

##### Hierarchy Management:
- `GET /api/incidents/{id}/children`: Returns child incidents
- `GET /api/incidents/{id}/hierarchy`: Returns complete incident tree

##### UI Endpoints:
- `GET /api/incidents/table`: Dashboard with filtering and statistics
- `GET /api/incidents/solutions-ui`: Solution management interface
- `POST /api/incidents/solution-by-description`: Form-based solution updates

#### Advanced Features:

##### Dashboard Filtering:
- **Search Filter**: Text search in descriptions and solutions
- **Status Filter**: Filter by OPEN, IN_PROGRESS, CLOSED
- **Type Filter**: Filter by parent/sub-incident type
- **Statistics Calculation**: Real-time counters for all categories

##### Table Management Methods:
- `tablePage(@RequestParam filters, Model model)`:
  - Applies multiple filters simultaneously
  - Calculates dashboard statistics
  - Provides filtered incident lists
  - **Returns**: table.html with filtered data and statistics

---

## Functionality Breakdown

### 1. Intelligent Chat System

#### Conversation States:
- **INITIAL**: Ready for new incident description
- **WAITING_FOR_SUGGESTION_CONFIRMATION**: Asking if user wants built-in suggestion
- **WAITING_FOR_SUGGESTION_HELPFUL_RESPONSE**: Getting feedback on built-in suggestion
- **WAITING_FOR_OPTION_SELECTION**: User choosing numbered options
- **WAITING_FOR_TICKET_CONFIRMATION**: Confirming ticket creation
- **EXISTING_SOLUTION_FOUND**: Showing existing solutions
- **WAITING_FOR_OTHER_ISSUES**: Asking about additional issues
- **SESSION_CLOSED**: Conversation ended

#### Button Types and Actions:
- **help_yes/help_no**: Solution helpfulness feedback
- **other_issues_yes/other_issues_no**: Additional issues inquiry
- **suggestion_yes/suggestion_no**: Built-in suggestion acceptance
- **suggestion_helpful_yes/suggestion_helpful_no**: Built-in suggestion quality feedback
- **ticket_yes/ticket_no**: Ticket creation confirmation
- **option_1/option_2/option_3**: Numbered choice selection

### 2. Fuzzy Matching System

#### Keyword Extraction:
- **Stop Words Filtering**: Removes common words (the, a, is, etc.)
- **Domain Term Recognition**: IT-specific vocabulary mapping
- **Synonym Grouping**: Related terms mapped to categories
- **Length Filtering**: Excludes very short meaningless words

#### Similarity Scoring:
- **Keyword Overlap**: Jaccard similarity coefficient
- **Phrase Matching**: Bonus for exact multi-word phrases
- **Domain Context**: Higher weight for IT domain terms
- **Solution Quality**: Preference for incidents with solutions

#### Domain Term Categories:
- **Printer**: printer, printing, print, ink, toner, paper, cartridge
- **Network**: network, internet, wifi, connection, router, ethernet
- **Computer**: computer, pc, laptop, desktop, hardware, cpu
- **Software**: software, application, app, program, install, update
- **Login**: login, password, authentication, sso, access, credential
- **Email**: email, mail, outlook, exchange, mailbox, message

### 3. Built-in Suggestion Features

#### Rule-Based Suggestion System:
- **Keyword Recognition**: Problem categorization by keywords
- **Progressive Alternatives**: Multiple suggestion attempts with different approaches
- **Category-Specific Solutions**: Specialized responses for different problem types

#### Fallback System Categories:
- **Password/Login Issues**: Reset procedures, cache clearing, account checks
- **Network Problems**: Cable checks, router resets, DNS troubleshooting
- **Performance Issues**: Resource management, cleanup, malware scans
- **Email Problems**: Connection checks, profile repairs, server settings
- **Printer Issues**: Driver updates, queue management, hardware checks
- **Software Errors**: Reinstallation, compatibility mode, system repairs

#### Progressive Alternatives:
- **Attempt 1**: Basic troubleshooting steps
- **Attempt 2**: Intermediate solutions
- **Attempt 3**: Advanced technical procedures

### 4. Incident Lifecycle Management

#### Status Progression:
1. **OPEN**: Newly created incident
2. **IN_PROGRESS**: Solution being worked on
3. **CLOSED**: Incident resolved

#### Solution Logging:
- **Creation Log**: Timestamped incident creation
- **Solution Updates**: All solution attempts tracked
- **Status Changes**: State transitions recorded
- **Closure Log**: Final resolution timestamp

#### Hierarchical Structure:
- **Root Incidents**: parentIncidentId = null
- **Child Incidents**: parentIncidentId = parent.id
- **Blocker Incidents**: Created when solution reveals new issues
- **Follow-up Incidents**: Sequential problem resolution

### 5. Dashboard and Reporting

#### Statistics Tracking:
- **Total Incidents**: All incidents in system
- **Closed Incidents**: Successfully resolved tickets
- **In Progress**: Currently being worked
- **Open Incidents**: Awaiting attention
- **Sub-incidents**: Child tickets count

#### Filtering Capabilities:
- **Text Search**: Description and solution content
- **Status Filtering**: By current incident status
- **Type Filtering**: Parent vs. child incidents
- **Combined Filters**: Multiple criteria simultaneously

#### Visual Indicators:
- **Status Badges**: Color-coded status indicators
- **Hierarchy Indentation**: Visual parent-child relationships
- **Last Updated**: Timestamp display for tracking activity
- **Solution Previews**: Truncated solution text in listings

---

## API Endpoints Summary

### Chat API:
```
GET /                              → Chat interface
POST /api/chat/message            → Process text message
POST /api/chat/button             → Handle button action
GET /api/chat/history             → Get conversation history
POST /api/chat/clear              → Clear chat history
POST /api/chat/close              → Close session
```

### Incident Management API:
```
POST /api/incidents                       → Create incident
GET /api/incidents                        → Get all incidents
GET /api/incidents/solutions              → Search solutions
POST /api/incidents/chat                  → Process via chat
PUT /api/incidents/{id}/solution          → Update solution
POST /api/incidents/{id}/solution-update  → Add solution update
PUT /api/incidents/{id}/close             → Close incident
POST /api/incidents/{id}/resolve-and-create-blocker → Blocker workflow
POST /api/incidents/{parentId}/create-followup      → Follow-up workflow
GET /api/incidents/{id}/children          → Get child incidents
GET /api/incidents/{id}/hierarchy         → Get incident tree
```

### UI Endpoints:
```
GET /api/incidents/table                  → Dashboard interface
GET /api/incidents/solutions-ui           → Solution management
POST /api/incidents/solution-by-description → Form-based updates
```

---

## Database Schema

### Incident Table:
```sql
CREATE TABLE incident (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    incident_number VARCHAR(255),
    description TEXT,
    solution TEXT,
    parent_incident_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    closed_at TIMESTAMP,
    status VARCHAR(50),
    FOREIGN KEY (parent_incident_id) REFERENCES incident(id)
);

CREATE TABLE incident_solution_log (
    incident_id BIGINT,
    solution_log VARCHAR(1000),
    FOREIGN KEY (incident_id) REFERENCES incident(id)
);
```

---

## User Interface Components

### 1. Chat Interface (chat.html):
- **Message Display**: Chat bubbles with sender identification
- **Button Interface**: Dynamic button generation from ChatResponse
- **Input Management**: Conditional text input enabling/disabling
- **Typing Indicators**: Visual feedback during processing
- **History Display**: Persistent conversation viewing
- **Auto-scroll**: Automatic scroll to latest messages

### 2. Dashboard (table.html):
- **Statistics Cards**: Real-time incident counters
- **Search Interface**: Text search with filtering
- **Status Filters**: Dropdown for status selection
- **Type Filters**: Parent/sub-incident toggle
- **Incident Table**: Sortable, searchable data grid
- **Action Buttons**: Quick access to common operations

### 3. Solution Management (solutions.html):
- **Solution Display**: Formatted incident and solution viewing
- **Search Filtering**: Description-based solution search
- **Direct Links**: Navigation to full incident details
- **Update Forms**: In-line solution editing capabilities

---

## Integration Features

### 1. Built-in Suggestion System:
- **Rule-Based Configuration**: Keyword-based problem categorization
- **Progressive Fallback**: Graceful escalation through suggestion levels
- **Context Awareness**: Previous suggestion consideration for alternatives
- **Error Handling**: Robust error recovery and fallback mechanisms

### 2. Session Management:
- **HTTP Sessions**: Persistent conversation state
- **Multi-user Support**: Concurrent session handling
- **Session Cleanup**: Automatic and manual session clearing
- **State Persistence**: Conversation history maintenance

### 3. Database Integration:
- **JPA/Hibernate**: ORM with automatic schema generation
- **H2 Database**: In-memory database for development
- **Transaction Management**: Atomic operations for data consistency
- **Query Optimization**: Efficient searching and filtering

### 4. Template Engine:
- **Thymeleaf Integration**: Server-side template rendering
- **Bootstrap Styling**: Professional responsive design
- **JavaScript Enhancement**: Client-side interactivity
- **AJAX Communication**: Seamless API integration

---

## Conclusion

The Incident Tracker system provides a comprehensive IT support solution with intelligent conversation management, rule-based suggestions, and robust incident lifecycle tracking. The modular architecture allows for easy extension and maintenance, while the user-friendly interface ensures efficient problem resolution workflows.

Key strengths of the system:
- **Intelligent Automation**: Reduces manual intervention through smart matching
- **Comprehensive Tracking**: Complete audit trail of all incident activities  
- **Scalable Architecture**: Clean separation of concerns and extensible design
- **User Experience**: Intuitive chat interface with guided interactions
- **Flexibility**: Built-in rule-based suggestions with robust fallback mechanisms

This documentation covers all classes, methods, and functionalities implemented in the Incident Tracker project, providing a complete reference for developers and system administrators.
