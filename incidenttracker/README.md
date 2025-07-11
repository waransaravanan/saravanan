# Incident Tracker Web Chat Tool

## Overview
The Incident Tracker is a web chat tool designed to help users report incidents and receive solutions based on previous incident resolutions. If an incident is new, the tool will create a new record and track the solution for future reference.

## Features
- Accepts incident descriptions as input from users.
- Provides solutions based on previous incident resolutions.
- Creates new records for new incidents and tracks their solutions.

## Project Structure
```
incidenttracker
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── example
│   │   │           └── incidenttracker
│   │   │               ├── IncidentTrackerApplication.java
│   │   │               ├── controller
│   │   │               │   └── IncidentController.java
│   │   │               ├── model
│   │   │               │   └── Incident.java
│   │   │               ├── repository
│   │   │               │   └── IncidentRepository.java
│   │   │               └── service
│   │   │                   └── IncidentService.java
│   │   └── resources
│   │       ├── application.properties
│   │       └── templates
│   │           └── chat.html
│   └── test
│       └── java
│           └── com
│               └── example
│                   └── incidenttracker
│                       └── IncidentTrackerApplicationTests.java
├── pom.xml
└── README.md
```

## Setup Instructions
1. **Clone the Repository**
   ```
   git clone <repository-url>
   cd incidenttracker
   ```

2. **Build the Project**
   Ensure you have Maven installed and run:
   ```
   mvn clean install
   ```

3. **Run the Application**
   You can run the application using:
   ```
   mvn spring-boot:run
   ```

4. **Access the Web Interface**
   Open your web browser and navigate to `http://localhost:8080/chat` to access the incident tracker chat interface.

## Usage Guidelines
- Users can enter incident descriptions in the chat interface.
- The tool will respond with solutions if available or prompt to create a new incident record.
- All incidents and solutions are stored for future reference.

## Contributing
Contributions are welcome! Please submit a pull request or open an issue for any enhancements or bug fixes. 

## License
This project is licensed under the MIT License.