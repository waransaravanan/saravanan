# Incident Tracker - AI-Powered IT Support System

## Overview
The Incident Tracker is a comprehensive web-based IT support system featuring an intelligent chat interface, solution management, and ticket tracking. The system provides AI-powered suggestions, manages incident lifecycles, and maintains a searchable knowledge base of solutions.

## Key Features

### 🤖 **Intelligent Chat Interface**
- **Smart Conversation Flow**: Button-driven responses for yes/no questions and option selections
- **Built-in Suggestions**: Rule-based intelligent problem-solving recommendations
- **Fuzzy Matching**: Finds similar existing solutions using contextual keyword matching
- **Input Management**: Text input disabled when buttons are present, enabled only for incident descriptions
- **Conversation History**: Persistent chat history with session management

### 📊 **Comprehensive Dashboard**
- **Real-time Statistics**: Total incidents, closed, in-progress, open, and sub-incidents counters
- **Advanced Filtering**: Search by description, status, and incident type
- **Last Updated Tracking**: Displays date/time of last solution updates
- **Parent/Sub-incident Hierarchy**: Visual distinction with indentation and styling

### 🎫 **Solution Management System**
- **Solution History Logging**: Complete audit trail of all solution updates
- **Ticket Lifecycle Management**: Open → In Progress → Closed workflow
- **Manual Solution Updates**: Support team can add solution entries
- **Status Tracking**: Visual badges for different incident states

### 🔍 **Advanced Search & Discovery**
- **Contextual Similarity Matching**: Intelligent keyword extraction and matching
- **Solution Scoring**: Prioritizes results based on solution quality and relevance
- **Cross-reference Links**: Direct navigation between related incidents
- **Fuzzy Search**: Handles variations in problem descriptions

## Technical Architecture

### **Backend Components**
- **Spring Boot Framework**: RESTful API with MVC architecture
- **JPA/Hibernate**: Data persistence with H2 database
- **Session Management**: Stateful chat conversations
- **Built-in Suggestion Service**: Rule-based suggestion system

### **Frontend Features**
- **Responsive Design**: Bootstrap 5 with custom styling
- **Interactive Chat UI**: Real-time messaging with typing indicators
- **Button-driven UX**: Guided user interactions for better experience
- **Dynamic Content**: AJAX-based updates without page refreshes

## Project Structure
```
incidenttracker/
├── src/
│   ├── main/
│   │   ├── java/com/example/incidenttracker/
│   │   │   ├── IncidentTrackerApplication.java
│   │   │   ├── controller/
│   │   │   │   ├── ChatController.java          # Chat API endpoints
│   │   │   │   └── IncidentController.java      # Incident management
│   │   │   ├── model/
│   │   │   │   └── Incident.java               # Data model with solution logging
│   │   │   ├── repository/
│   │   │   │   └── IncidentRepository.java     # JPA repository
│   │   │   └── service/
│   │   │       ├── ChatService.java            # Conversation flow management
│   │   │       └── IncidentService.java        # Business logic
│   │   └── resources/
│   │       ├── application.properties
│   │       └── templates/
│   │           ├── chat.html                   # Interactive chat interface
│   │           ├── table.html                  # Incident dashboard
│   │           └── solutions.html              # Solution management
│   └── test/
├── pom.xml
└── README.md
```

## Setup Instructions

### **Prerequisites**
- Java 11 or higher
- Maven 3.6 or higher

### **Installation Steps**

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd incidenttracker
   ```

2. **Configure Application** (Optional)
   Edit `src/main/resources/application.properties` to set:
   ```properties
   # Database configuration (H2 in-memory by default)
   spring.datasource.url=jdbc:h2:mem:testdb
   spring.h2.console.enabled=true
   ```

3. **Build the Project**
   ```bash
   mvn clean install
   ```

4. **Run the Application**
   ```bash
   mvn spring-boot:run
   ```

5. **Access the Application**
   - **Chat Interface**: `http://localhost:8080/chat`
   - **Incident Dashboard**: `http://localhost:8080/api/incidents/table`
   - **Solutions Management**: `http://localhost:8080/api/incidents/solutions-ui`
   - **H2 Database Console**: `http://localhost:8080/h2-console`

## API Endpoints

### **Chat API**
- `POST /api/chat/message` - Send chat message
- `POST /api/chat/button` - Handle button responses
- `GET /api/chat/history` - Get conversation history
- `POST /api/chat/clear` - Clear chat history

### **Incident Management API**
- `GET /api/incidents/table` - Dashboard with filtering
- `GET /api/incidents/solutions-ui` - Solution management interface
- `POST /api/incidents/{id}/solution-update` - Add solution update
- `PUT /api/incidents/{id}/close` - Close incident

## Usage Guide

### **For End Users**
1. **Report an Issue**: Navigate to the chat interface and describe your IT problem
2. **Follow Guided Flow**: Use buttons to respond to yes/no questions
3. **Review Suggestions**: Get built-in rule-based solutions or existing knowledge base matches
4. **Track Progress**: Monitor ticket status through the dashboard

### **For Support Teams**
1. **Monitor Dashboard**: View all incidents with real-time statistics
2. **Manage Solutions**: Add solution updates and close tickets
3. **Search & Filter**: Find related incidents using advanced search
4. **Review History**: Access complete solution audit trails

## Conversation Flow

```
User describes issue → 
System searches existing solutions → 
If found: Present solutions → Ask if helpful
If not found: Offer built-in suggestion → 
If suggestion helpful: Save solution
If not helpful: Offer options (new suggestion, create ticket, close)
Create ticket → Support team resolves → Close ticket
```

## Features in Detail

### **Smart Input Management**
- Text input enabled only for incident descriptions
- Button-driven responses for all other interactions
- Visual feedback when input is disabled
- Auto-focus management for better UX

### **Rule-Based Suggestions**
- Contextual problem analysis
- Multiple suggestion attempts
- Learning from user feedback
- Fallback to manual ticket creation

### **Solution Knowledge Base**
- Fuzzy matching algorithms
- Contextual keyword extraction
- Solution quality scoring
- Cross-referencing related issues

## Contributing
We welcome contributions! Please follow these guidelines:

1. **Fork the repository** and create a feature branch
2. **Follow coding standards** and include tests
3. **Update documentation** for new features
4. **Submit a pull request** with detailed description

## License
This project is licensed under the MIT License - see the LICENSE file for details.

## Support
For support or questions, please:
- Open an issue on GitHub
- Contact the development team
- Check the documentation wiki