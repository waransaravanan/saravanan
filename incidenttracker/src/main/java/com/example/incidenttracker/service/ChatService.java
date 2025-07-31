package com.example.incidenttracker.service;

import com.example.incidenttracker.model.Incident;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {
    
    @Autowired
    private IncidentService incidentService;
    
    // Session state management
    private Map<String, ChatSession> sessions = new HashMap<>();
    
    public ChatResponse processMessage(String sessionId, String message) {
        ChatSession session = sessions.computeIfAbsent(sessionId, k -> new ChatSession());
        
        // Store user message in conversation history
        session.addMessage("user", message);
        
        ChatResponse response = handleConversation(session, message.trim());
        
        // Store bot response in conversation history
        session.addMessage("bot", response.getMessage());
        
        return response;
    }
    
    public ChatResponse processButtonAction(String sessionId, String action) {
        ChatSession session = sessions.computeIfAbsent(sessionId, k -> new ChatSession());
        
        // Store user action in conversation history
        session.addMessage("user", "[Button: " + action + "]");
        
        ChatResponse response = handleButtonAction(session, action);
        
        // Store bot response in conversation history
        session.addMessage("bot", response.getMessage());
        
        return response;
    }
    
    public List<ChatMessage> getConversationHistory(String sessionId) {
        ChatSession session = sessions.get(sessionId);
        return session != null ? session.getConversationHistory() : new ArrayList<>();
    }
    
    public void clearConversationHistory(String sessionId) {
        ChatSession session = sessions.get(sessionId);
        if (session != null) {
            session.clearHistory();
        }
    }
    
    private ChatResponse handleConversation(ChatSession session, String message) {
        var lowerMessage = message.toLowerCase();
        
        switch (session.getState()) {
            case INITIAL:
                return handleInitialMessage(session, message);
                
            case WAITING_FOR_AI_CONFIRMATION:
                return handleAiConfirmation(session, lowerMessage);
                
            case WAITING_FOR_AI_HELPFUL_RESPONSE:
                return handleAiHelpfulResponse(session, lowerMessage);
                
            case WAITING_FOR_OPTION_SELECTION:
                return handleOptionSelection(session, message.trim());
                
            case WAITING_FOR_TICKET_CONFIRMATION:
                return handleTicketConfirmation(session, lowerMessage);
                
            case WAITING_FOR_NEW_ISSUE:
                return handleWaitingForNewIssue(session, message);
                
            case EXISTING_SOLUTION_FOUND:
                return handleExistingSolutionResponse(session, lowerMessage);
                
            case WAITING_FOR_OTHER_ISSUES:
                return handleOtherIssuesResponse(session, lowerMessage);
                
            case WAITING_FOR_CHILD_INCIDENT_CONFIRMATION:
                return handleChildIncidentConfirmation(session, lowerMessage);
                
            case WAITING_FOR_CHILD_INCIDENT_DESCRIPTION:
                return handleChildIncidentDescription(session, message);
                
            case WAITING_FOR_NEW_INCIDENT_DESCRIPTION:
                return handleNewIncidentDescription(session, message);
                
            case SESSION_CLOSED:
                return handleNewQuery(session, message);
                
            default:
                // Instead of generic error, try to understand the context better
                return handleUnexpectedMessage(session, message);
        }
    }
    
    private ChatResponse handleButtonAction(ChatSession session, String action) {
        switch (action) {
            case "help_yes":
                return handleHelpYes(session);
            case "help_no":
                return handleHelpNo(session);
            case "other_issues_yes":
                return handleOtherIssuesYes(session);
            case "other_issues_no":
                return handleOtherIssuesNo(session);
            case "ai_suggestion_yes":
                return handleAiConfirmation(session, "yes");
            case "ai_suggestion_no":
                return handleAiConfirmation(session, "no");
            case "ai_helpful_yes":
                return handleAiHelpfulResponse(session, "yes");
            case "ai_helpful_no":
                return handleAiHelpfulResponse(session, "no");
            case "option_1":
                return handleOptionSelection(session, "1");
            case "option_2":
                return handleOptionSelection(session, "2");
            case "option_3":
                return handleOptionSelection(session, "3");
            case "ticket_yes":
                return handleTicketConfirmation(session, "yes");
            case "ticket_no":
                return handleTicketConfirmation(session, "no");
            case "child_incident_yes":
                return handleChildIncidentConfirmation(session, "yes");
            case "new_ticket_yes":
                return handleNewTicketRequest(session);
            default:
                return new ChatResponse("I didn't understand that action. Please try again.", session.getState());
        }
    }
    
    private ChatResponse handleHelpYes(ChatSession session) {
        session.setState(ChatState.WAITING_FOR_OTHER_ISSUES);
        var buttons = List.of(
            new ChatButton("Yes", "other_issues_yes", "primary"),
            new ChatButton("No", "other_issues_no", "secondary")
        );
        return new ChatResponse("Great! I'm glad I could help you resolve the issue. 🎉\n\nDo you have any other IT problems I can help you with?", ChatState.WAITING_FOR_OTHER_ISSUES, buttons);
    }
    
    private ChatResponse handleHelpNo(ChatSession session) {
        // User said existing solutions didn't help
        if (session.getParentIncident() != null) {
            // Ask if they want to create a child incident linked to the parent
            session.setState(ChatState.WAITING_FOR_CHILD_INCIDENT_CONFIRMATION);
            var buttons = List.of(
                new ChatButton("Yes, create child incident", "child_incident_yes", "primary"),
                new ChatButton("No, create new ticket", "new_ticket_yes", "secondary")
            );
            return new ChatResponse(
                "Since the existing solutions didn't resolve your issue, would you like to create a related " +
                "sub-incident linked to incident " + 
                (session.getParentIncident().getIncidentNumber() != null ? 
                    session.getParentIncident().getIncidentNumber() : 
                    "ID: " + session.getParentIncident().getId()) + 
                "? This will help track related issues together.",
                ChatState.WAITING_FOR_CHILD_INCIDENT_CONFIRMATION,
                buttons
            );
        } else {
            // Fallback to regular ticket creation
            session.setState(ChatState.WAITING_FOR_TICKET_CONFIRMATION);
            var buttons = List.of(
                new ChatButton("Yes", "ticket_yes", "primary"),
                new ChatButton("No", "ticket_no", "secondary")
            );
            return new ChatResponse(
                "I understand the solution wasn't quite right. Would you like me to create a support ticket for manual resolution?", 
                ChatState.WAITING_FOR_TICKET_CONFIRMATION,
                buttons
            );
        }
    }
    
    private ChatResponse handleOtherIssuesYes(ChatSession session) {
        session.reset();
        session.setState(ChatState.INITIAL);
        return new ChatResponse("Perfect! Please describe your new IT issue and I'll help you find a solution.", ChatState.INITIAL);
    }
    
    private ChatResponse handleOtherIssuesNo(ChatSession session) {
        session.setState(ChatState.SESSION_CLOSED);
        return new ChatResponse("Thank you for using the IT Support Chat! Have a great day! 👋\n\nFeel free to start a new conversation anytime you need help.", ChatState.SESSION_CLOSED);
    }
    
    private ChatResponse handleInitialMessage(ChatSession session, String message) {
        session.setCurrentIncidentDescription(message);
        session.setState(ChatState.INITIAL);
        
        // Check for existing solutions using fuzzy matching
        List<Incident> existingSolutions = findSimilarSolutions(message);
        
        if (!existingSolutions.isEmpty()) {
            // Found existing solutions - store the first one as potential parent for child incident
            session.setParentIncident(existingSolutions.get(0));
            
            var solutionsBuilder = new StringBuilder();
            
            for (var inc : existingSolutions) {
                var incidentNumber = inc.getIncidentNumber() != null ? inc.getIncidentNumber() : "ID: " + inc.getId();
                var encodedDescription = inc.getDescription().replace(" ", "%20");
                var solutionsUrl = "https://bug-free-broccoli-w9v9xj7w55q2pq4-8080.app.github.dev/api/incidents/solutions-ui?description=" + encodedDescription;
                
                solutionsBuilder.append("🎫 **[").append(incidentNumber).append("](").append(solutionsUrl).append(")** - ")
                    .append(inc.getStatus() != null ? inc.getStatus() : "UNKNOWN").append("\n");
                solutionsBuilder.append("📝 Issue: ").append(inc.getDescription()).append("\n");
                
                // Show main solution if available
                if (inc.getSolution() != null && !inc.getSolution().trim().isEmpty()) {
                    solutionsBuilder.append("💡 Solution: ").append(inc.getSolution()).append("\n");
                }
                
                // Show recent solution log entries (limited to first 3 with ellipsis if more)
                if (inc.getSolutionLog() != null && !inc.getSolutionLog().isEmpty()) {
                    solutionsBuilder.append("📋 Recent updates:\n");
                    var relevantLogs = inc.getSolutionLog().stream()
                        .filter(log -> log.toLowerCase().contains("solution") || log.toLowerCase().contains("update"))
                        .collect(Collectors.toList());
                    
                    var logsToShow = Math.min(3, relevantLogs.size());
                    for (int i = 0; i < logsToShow; i++) {
                        solutionsBuilder.append("   • ").append(relevantLogs.get(i)).append("\n");
                    }
                    
                    if (relevantLogs.size() > 3) {
                        solutionsBuilder.append("   • ... and ").append(relevantLogs.size() - 3).append(" more updates\n");
                    }
                }
                
                solutionsBuilder.append("🔗 [Click here to view full details](").append(solutionsUrl).append(")")
                    .append("\n\n");
            }
            
            session.setState(ChatState.EXISTING_SOLUTION_FOUND);
            var buttons = List.of(
                new ChatButton("Yes", "help_yes", "success"),
                new ChatButton("No", "help_no", "danger")
            );
            return new ChatResponse(
                "Great news! I found existing tickets for similar issues:\n\n" + solutionsBuilder.toString() + 
                "Did this help resolve your issue?", 
                ChatState.EXISTING_SOLUTION_FOUND,
                buttons
            );
        } else {
            // No existing solutions found - go directly to ticket creation
            session.setState(ChatState.WAITING_FOR_TICKET_CONFIRMATION);
            var buttons = List.of(
                new ChatButton("Yes", "ticket_yes", "primary"),
                new ChatButton("No", "ticket_no", "secondary")
            );
            return new ChatResponse(
                "I don't have any existing solutions for issues like '" + message + 
                "' in our system. Would you like me to create a support ticket for this issue?", 
                ChatState.WAITING_FOR_TICKET_CONFIRMATION,
                buttons
            );
        }
    }
    
    private ChatResponse handleAiConfirmation(ChatSession session, String message) {
        if (isPositiveResponse(message)) {
            // User wants AI suggestion
            String aiSuggestion = generateFallbackSuggestion(session.getCurrentIncidentDescription(), null);
            session.setCurrentAiSuggestion(aiSuggestion);
            session.addPreviousAiSuggestion(aiSuggestion);
            session.setState(ChatState.WAITING_FOR_AI_HELPFUL_RESPONSE);
            
            var buttons = List.of(
                new ChatButton("Yes", "ai_helpful_yes", "success"),
                new ChatButton("No", "ai_helpful_no", "danger")
            );
            return new ChatResponse(
                "Here's a suggestion for your issue:\n\n" + aiSuggestion + 
                "\n\nIs this helpful?", 
                ChatState.WAITING_FOR_AI_HELPFUL_RESPONSE,
                buttons
            );
        } else if (isNegativeResponse(message)) {
            // User doesn't want AI suggestion
            session.setState(ChatState.WAITING_FOR_TICKET_CONFIRMATION);
            var buttons = List.of(
                new ChatButton("Yes", "ticket_yes", "primary"),
                new ChatButton("No", "ticket_no", "secondary")
            );
            return new ChatResponse(
                "No problem! Since we don't have a matching solution, would you like me to create a support ticket for this issue?", 
                ChatState.WAITING_FOR_TICKET_CONFIRMATION,
                buttons
            );
        } else {
            // Unclear response - ask again with buttons
            var buttons = List.of(
                new ChatButton("Yes", "ai_suggestion_yes", "primary"),
                new ChatButton("No", "ai_suggestion_no", "secondary")
            );
            return new ChatResponse(
                "I didn't quite understand your response. Would you like me to get a suggestion for '" + 
                session.getCurrentIncidentDescription() + "'?", 
                ChatState.WAITING_FOR_AI_CONFIRMATION,
                buttons
            );
        }
    }
    
    private ChatResponse handleAiHelpfulResponse(ChatSession session, String message) {
        if (isPositiveResponse(message)) {
            // AI suggestion was helpful - save it
            Incident incident = incidentService.createIncidentWithoutSolution(session.getCurrentIncidentDescription());
            incidentService.updateIncidentSolution(incident.getId(), session.getCurrentAiSuggestion());
            session.setState(ChatState.SESSION_CLOSED);
            
            var buttons = List.of(
                new ChatButton("Yes", "other_issues_yes", "primary"),
                new ChatButton("No", "other_issues_no", "secondary")
            );
            return new ChatResponse(
                "Excellent! I've saved this solution for future reference as incident " + incident.getIncidentNumber() + 
                ". This will help other users with similar issues.\n\nDo you have any other IT issues I can help you with?", 
                ChatState.SESSION_CLOSED,
                buttons
            );
        } else if (isNegativeResponse(message)) {
            // AI suggestion wasn't helpful - provide options
            session.setState(ChatState.WAITING_FOR_OPTION_SELECTION);
            var buttons = List.of(
                new ChatButton("1. Try another suggestion", "option_1", "primary"),
                new ChatButton("2. Create support ticket", "option_2", "warning"),
                new ChatButton("3. Close session", "option_3", "secondary")
            );
            return new ChatResponse(
                "I understand the suggestion wasn't quite right. I have a few options:", 
                ChatState.WAITING_FOR_OPTION_SELECTION,
                buttons
            );
        } else {
            // Unclear response - ask again with buttons
            var buttons = List.of(
                new ChatButton("Yes", "ai_helpful_yes", "success"),
                new ChatButton("No", "ai_helpful_no", "danger")
            );
            return new ChatResponse(
                "I didn't quite understand your response. Was the suggestion helpful for your issue '" + 
                session.getCurrentIncidentDescription() + "'?", 
                ChatState.WAITING_FOR_AI_HELPFUL_RESPONSE,
                buttons
            );
        }
    }
    
    private ChatResponse handleOptionSelection(ChatSession session, String message) {
        switch (message) {
            case "1":
                // Try another suggestion
                String aiSuggestion = generateFallbackSuggestion(
                    session.getCurrentIncidentDescription(), 
                    session.getPreviousAiSuggestions()
                );
                session.setCurrentAiSuggestion(aiSuggestion);
                session.addPreviousAiSuggestion(aiSuggestion);
                session.setState(ChatState.WAITING_FOR_AI_HELPFUL_RESPONSE);
                
                var buttons1 = List.of(
                    new ChatButton("Yes", "ai_helpful_yes", "success"),
                    new ChatButton("No", "ai_helpful_no", "danger")
                );
                return new ChatResponse(
                    "Let me try a different suggestion for your issue:\n\n" + aiSuggestion + 
                    "\n\nIs this helpful?", 
                    ChatState.WAITING_FOR_AI_HELPFUL_RESPONSE,
                    buttons1
                );
                
            case "2":
                // Create support ticket
                session.setState(ChatState.WAITING_FOR_TICKET_CONFIRMATION);
                var buttons2 = List.of(
                    new ChatButton("Yes", "ticket_yes", "primary"),
                    new ChatButton("No", "ticket_no", "secondary")
                );
                return new ChatResponse(
                    "I'll create a support ticket for manual resolution. Would you like me to proceed?", 
                    ChatState.WAITING_FOR_TICKET_CONFIRMATION,
                    buttons2
                );
                
            case "3":
                // Close session
                session.setState(ChatState.SESSION_CLOSED);
                var buttons3 = List.of(
                    new ChatButton("Yes", "other_issues_yes", "primary"),
                    new ChatButton("No", "other_issues_no", "secondary")
                );
                return new ChatResponse(
                    "No problem! If you need help with anything else, just let me know.\n\nDo you have any other IT issues I can help you with?", 
                    ChatState.SESSION_CLOSED,
                    buttons3
                );
                
            default:
                // Invalid option - show buttons again
                var buttonsDefault = List.of(
                    new ChatButton("1. Try another suggestion", "option_1", "primary"),
                    new ChatButton("2. Create support ticket", "option_2", "warning"),
                    new ChatButton("3. Close session", "option_3", "secondary")
                );
                return new ChatResponse(
                    "I didn't understand that option. Please choose one of the following:", 
                    ChatState.WAITING_FOR_OPTION_SELECTION,
                    buttonsDefault
                );
        }
    }
    
    private ChatResponse handleTicketConfirmation(ChatSession session, String message) {
        if (isPositiveResponse(message)) {
            // Create ticket
            var incident = incidentService.createIncidentWithoutSolution(session.getCurrentIncidentDescription());
            session.setState(ChatState.SESSION_CLOSED);
            
            var buttons = List.of(
                new ChatButton("Yes", "other_issues_yes", "primary"),
                new ChatButton("No", "other_issues_no", "secondary")
            );
            return new ChatResponse(
                "Perfect! I've created support ticket " + incident.getIncidentNumber() + 
                " for your issue: '" + session.getCurrentIncidentDescription() + 
                "'. Our support team will work on this and update the ticket with a solution.\n\n" +
                "You can check the status anytime in our solutions page.\n\n" +
                "Do you have any other IT issues I can help you with?",
                ChatState.SESSION_CLOSED,
                buttons
            );
        } else if (isNegativeResponse(message)) {
            // User doesn't want ticket
            session.setState(ChatState.SESSION_CLOSED);
            var buttons = List.of(
                new ChatButton("Yes", "other_issues_yes", "primary"),
                new ChatButton("No", "other_issues_no", "secondary")
            );
            return new ChatResponse(
                "No worries! If you change your mind, just let me know. " +
                "You can also try describing your issue differently - sometimes I can find better matches.\n\n" +
                "Do you have any other IT issues I can help you with?",
                ChatState.SESSION_CLOSED,
                buttons
            );
        } else {
            // Unclear response - ask again with buttons
            var buttons = List.of(
                new ChatButton("Yes", "ticket_yes", "primary"),
                new ChatButton("No", "ticket_no", "secondary")
            );
            return new ChatResponse(
                "I didn't quite understand your response. Would you like me to create a support ticket for your issue '" + 
                session.getCurrentIncidentDescription() + "'?", 
                ChatState.WAITING_FOR_TICKET_CONFIRMATION,
                buttons
            );
        }
    }
    
    private ChatResponse handleChildIncidentConfirmation(ChatSession session, String message) {
        if (isPositiveResponse(message)) {
            // User wants to create a child incident
            session.setState(ChatState.WAITING_FOR_CHILD_INCIDENT_DESCRIPTION);
            return new ChatResponse(
                "Great! Please provide a more specific description of your issue that will be linked to the parent incident " +
                (session.getParentIncident().getIncidentNumber() != null ? 
                    session.getParentIncident().getIncidentNumber() : 
                    "ID: " + session.getParentIncident().getId()) + ":",
                ChatState.WAITING_FOR_CHILD_INCIDENT_DESCRIPTION
            );
        } else {
            // User wants to create a new independent ticket - ask for new description
            session.setState(ChatState.WAITING_FOR_NEW_INCIDENT_DESCRIPTION);
            return new ChatResponse(
                "Sure! Please provide a description for your new incident ticket:",
                ChatState.WAITING_FOR_NEW_INCIDENT_DESCRIPTION
            );
        }
    }
    
    private ChatResponse handleNewTicketRequest(ChatSession session) {
        // User wants to create a new independent ticket - ask for new description
        session.setState(ChatState.WAITING_FOR_NEW_INCIDENT_DESCRIPTION);
        return new ChatResponse(
            "Sure! Please provide a description for your new incident ticket:",
            ChatState.WAITING_FOR_NEW_INCIDENT_DESCRIPTION
        );
    }
    
    private ChatResponse handleChildIncidentDescription(ChatSession session, String message) {
        if (message.trim().isEmpty()) {
            return new ChatResponse(
                "Please provide a description for the child incident:",
                ChatState.WAITING_FOR_CHILD_INCIDENT_DESCRIPTION
            );
        }
        
        // Create child incident linked to parent
        var parentIncident = session.getParentIncident();
        var childIncident = incidentService.createSubIncident(parentIncident.getId(), message);
        
        session.setState(ChatState.SESSION_CLOSED);
        var buttons = List.of(
            new ChatButton("Yes", "other_issues_yes", "primary"),
            new ChatButton("No", "other_issues_no", "secondary")
        );
        
        return new ChatResponse(
            "Perfect! I've created child incident " + childIncident.getIncidentNumber() + 
            " linked to parent incident " + 
            (parentIncident.getIncidentNumber() != null ? 
                parentIncident.getIncidentNumber() : 
                "ID: " + parentIncident.getId()) + 
            ".\n\n" +
            "Child Issue: '" + message + "'\n" +
            "Parent Issue: '" + parentIncident.getDescription() + "'\n\n" +
            "Our support team will work on this child incident and update it with a solution. " +
            "This helps us track related issues together for better resolution.\n\n" +
            "Do you have any other IT issues I can help you with?",
            ChatState.SESSION_CLOSED,
            buttons
        );
    }
    
    private ChatResponse handleNewIncidentDescription(ChatSession session, String message) {
        if (message.trim().isEmpty()) {
            return new ChatResponse(
                "Please provide a description for the new incident:",
                ChatState.WAITING_FOR_NEW_INCIDENT_DESCRIPTION
            );
        }
        
        // Create new independent incident with the new description
        var newIncident = incidentService.createIncidentWithoutSolution(message);
        
        session.setState(ChatState.SESSION_CLOSED);
        var buttons = List.of(
            new ChatButton("Yes", "other_issues_yes", "primary"),
            new ChatButton("No", "other_issues_no", "secondary")
        );
        
        return new ChatResponse(
            "Perfect! I've created new incident ticket " + newIncident.getIncidentNumber() + 
            " for your issue: '" + message + 
            "'. Our support team will work on this and update the ticket with a solution.\n\n" +
            "You can check the status anytime in our solutions page.\n\n" +
            "Do you have any other IT issues I can help you with?",
            ChatState.SESSION_CLOSED,
            buttons
        );
    }
    
    private ChatResponse handleNewQuery(ChatSession session, String message) {
        // Check if user is responding to "Is there anything else I can help you with?"
        if (isPositiveResponse(message)) {
            // User wants to continue with a new issue
            session.setState(ChatState.WAITING_FOR_NEW_ISSUE);
            return new ChatResponse(
                "Great! Please describe your new IT issue and I'll help you find a solution.",
                ChatState.WAITING_FOR_NEW_ISSUE
            );
        } else if (isNegativeResponse(message)) {
            // User doesn't need more help
            return new ChatResponse(
                "Thank you for using our IT support chat! Feel free to return anytime you need assistance. Have a great day!",
                ChatState.SESSION_CLOSED
            );
        } else {
            // Treat as new incident description
            session.reset();
            return handleInitialMessage(session, message);
        }
    }
    
    private ChatResponse handleWaitingForNewIssue(ChatSession session, String message) {
        // User is providing a new issue description
        session.reset();
        return handleInitialMessage(session, message);
    }
    
    private ChatResponse handleExistingSolutionResponse(ChatSession session, String message) {
        // Handle response when existing solutions were found
        if (isPositiveResponse(message)) {
            return handleHelpYes(session);
        } else if (isNegativeResponse(message)) {
            return handleHelpNo(session);
        } else {
            // Unclear response - ask again with buttons
            var buttons = List.of(
                new ChatButton("Yes", "help_yes", "success"),
                new ChatButton("No", "help_no", "danger")
            );
            return new ChatResponse(
                "I didn't quite understand your response. Did any of the existing solutions help resolve your issue?", 
                ChatState.EXISTING_SOLUTION_FOUND,
                buttons
            );
        }
    }
    
    private ChatResponse handleOtherIssuesResponse(ChatSession session, String message) {
        // Handle response to "Do you have other issues?"
        if (isPositiveResponse(message)) {
            return handleOtherIssuesYes(session);
        } else if (isNegativeResponse(message)) {
            return handleOtherIssuesNo(session);
        } else {
            // Unclear response - ask again with buttons
            var buttons = List.of(
                new ChatButton("Yes", "other_issues_yes", "primary"),
                new ChatButton("No", "other_issues_no", "secondary")
            );
            return new ChatResponse(
                "I didn't quite understand. Do you have any other IT issues I can help you with?", 
                ChatState.WAITING_FOR_OTHER_ISSUES,
                buttons
            );
        }
    }
    
    private ChatResponse handleUnexpectedMessage(ChatSession session, String message) {
        // Try to understand what the user wants based on context and conversation history
        
        // Check if it looks like a new IT issue description
        var meaningfulKeywords = extractMeaningfulKeywords(message);
        if (!meaningfulKeywords.isEmpty()) {
            // Looks like a new issue - reset and handle as initial message
            session.reset();
            return handleInitialMessage(session, message);
        }
        
        // Check if it's a yes/no response
        if (isPositiveResponse(message) || isNegativeResponse(message)) {
            // Provide options based on current state
            var buttons = List.of(
                new ChatButton("Start new issue", "other_issues_yes", "primary"),
                new ChatButton("End conversation", "other_issues_no", "secondary")
            );
            return new ChatResponse(
                "I understand you're trying to respond, but I'm not sure what you're responding to. Would you like to start with a new IT issue or end our conversation?", 
                ChatState.WAITING_FOR_OTHER_ISSUES,
                buttons
            );
        }
        
        // Default fallback - but maintain context
        var buttons = List.of(
            new ChatButton("Start new issue", "other_issues_yes", "primary"),
            new ChatButton("End conversation", "other_issues_no", "secondary")
        );
        return new ChatResponse(
            "I'm having trouble understanding what you need. Could you please describe your IT issue clearly, or would you like to end our conversation?", 
            ChatState.WAITING_FOR_OTHER_ISSUES,
            buttons
        );
    }
    
    private List<Incident> findSimilarSolutions(String description) {
        // Enhanced contextual similarity matching
        var allIncidents = incidentService.getAllIncidents();
        var meaningfulKeywords = extractMeaningfulKeywords(description);
        
        // Debug logging - you can enable this to see what's happening
        // System.out.println("User query: " + description);
        // System.out.println("Extracted keywords: " + meaningfulKeywords);
        
        // If no meaningful keywords found, return empty list
        if (meaningfulKeywords.isEmpty()) {
            return new ArrayList<>();
        }
        
        return allIncidents.stream()
            .filter(incident -> {
                // Include incidents that have either:
                // 1. A solution in the main solution field, OR
                // 2. Solution updates in the log (solutionLog), OR  
                // 3. Are closed tickets (likely have solutions)
                var hasSolution = incident.getSolution() != null && !incident.getSolution().trim().isEmpty();
                var hasSolutionLog = incident.getSolutionLog() != null && !incident.getSolutionLog().isEmpty() && 
                    incident.getSolutionLog().stream().anyMatch(log -> log.toLowerCase().contains("solution") || log.toLowerCase().contains("update"));
                var isClosedWithActivity = "CLOSED".equals(incident.getStatus()) || "IN_PROGRESS".equals(incident.getStatus());
                
                return hasSolution || hasSolutionLog || isClosedWithActivity;
            })
            .filter(incident -> {
                // Context-aware similarity matching - lowered threshold for better matching
                double similarity = calculateContextualSimilarity(description, incident.getDescription());
                
                // Debug logging - you can enable this to see similarity scores
                // System.out.println("Comparing with: " + incident.getDescription());
                // System.out.println("Similarity score: " + similarity);
                
                return similarity > 0.15;
            })
            .sorted((i1, i2) -> {
                // Sort by similarity score combined with solution score
                var similarity1 = calculateContextualSimilarity(description, i1.getDescription());
                var similarity2 = calculateContextualSimilarity(description, i2.getDescription());
                var solutionScore1 = calculateSolutionScore(i1);
                var solutionScore2 = calculateSolutionScore(i2);
                
                var totalScore1 = similarity1 * 10 + solutionScore1;
                var totalScore2 = similarity2 * 10 + solutionScore2;
                
                return Double.compare(totalScore2, totalScore1); // Higher score first
            })
            .limit(3) // Return top 3 most relevant matches
            .collect(Collectors.toList());
    }
    
    private double calculateContextualSimilarity(String query, String incidentDescription) {
        // Normalize both strings
        var normalizedQuery = normalizeText(query);
        var normalizedIncident = normalizeText(incidentDescription);
        
        // Extract domain-specific keywords
        var queryKeywords = extractMeaningfulKeywords(normalizedQuery);
        var incidentKeywords = extractMeaningfulKeywords(normalizedIncident);
        
        if (queryKeywords.isEmpty() || incidentKeywords.isEmpty()) {
            return 0.0;
        }
        
        // Calculate keyword overlap with domain context
        var keywordScore = calculateKeywordOverlap(queryKeywords, incidentKeywords);
        
        // Bonus for exact phrase matches
        var phraseScore = calculatePhraseMatches(normalizedQuery, normalizedIncident);
        
        // Additional check for core domain terms (more lenient matching)
        var domainMatchScore = calculateDomainTermMatch(normalizedQuery, normalizedIncident);
        
        // Combine scores - give more weight to domain matches
        return Math.min(1.0, keywordScore * 0.5 + phraseScore * 0.2 + domainMatchScore * 0.3);
    }
    
    private double calculateDomainTermMatch(String query, String incident) {
        // Define core domain terms and their variations
        var domainTermGroups = Map.of(
            "printer", List.of("printer", "printing", "print", "ink", "paper", "toner", "cartridge", "inkjet", "laser"),
            "monitor", List.of("monitor", "display", "screen", "resolution", "brightness"),
            "network", List.of("network", "internet", "wifi", "connection", "router"),
            "computer", List.of("computer", "pc", "laptop", "desktop", "hardware"),
            "software", List.of("software", "application", "app", "program"),
            "login", List.of("login", "password", "authentication", "sso", "access"),
            "email", List.of("email", "mail", "outlook", "exchange"),
            "server", List.of("server", "database", "service")
        );
        
        double score = 0.0;
        
        for (var entry : domainTermGroups.entrySet()) {
            var variations = entry.getValue();
            
            // Check if query mentions any variation of this domain term
            boolean queryHasTerm = variations.stream().anyMatch(term -> query.contains(term));
            // Check if incident mentions any variation of this domain term  
            boolean incidentHasTerm = variations.stream().anyMatch(term -> incident.contains(term));
            
            if (queryHasTerm && incidentHasTerm) {
                score += 1.0; // Full match for same domain
                
                // Extra bonus if both contain the exact same term
                for (String term : variations) {
                    if (query.contains(term) && incident.contains(term)) {
                        score += 0.5; // Bonus for exact term match
                        break;
                    }
                }
            }
        }
        
        return Math.min(1.0, score);
    }
    
    private String normalizeText(String text) {
        return text.toLowerCase()
            .replaceAll("[^a-zA-Z0-9\\s]", " ") // Remove punctuation
            .replaceAll("\\s+", " ") // Normalize whitespace
            .trim();
    }
    
    private double calculateKeywordOverlap(List<String> queryKeywords, List<String> incidentKeywords) {
        var querySet = new HashSet<>(queryKeywords);
        var incidentSet = new HashSet<>(incidentKeywords);
        
        // Find intersection
        var intersection = new HashSet<>(querySet);
        intersection.retainAll(incidentSet);
        
        // Calculate Jaccard similarity
        var union = new HashSet<>(querySet);
        union.addAll(incidentSet);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    private double calculatePhraseMatches(String query, String incident) {
        var queryWords = query.split("\\s+");
        var score = 0.0;
        
        // Check for exact phrase matches (2+ words)
        for (int i = 0; i < queryWords.length - 1; i++) {
            var phrase = queryWords[i] + " " + queryWords[i + 1];
            if (incident.contains(phrase)) {
                score += 0.5; // Bonus for each phrase match
            }
        }
        
        return Math.min(1.0, score);
    }
    
    private List<String> extractMeaningfulKeywords(String description) {
        // Extract only meaningful IT-related keywords, excluding generic words
        var words = description.toLowerCase().split("\\s+");
        var keywords = new ArrayList<String>();
        
        // Define stop words to exclude
        var stopWords = Set.of(
            "issue", "problem", "not", "working", "error", "trouble", "help", 
            "the", "a", "an", "and", "or", "but", "is", "are", "was", "were",
            "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
            "can", "cant", "cannot", "my", "me", "i", "you", "your", "we", "us", "our",
            "with", "from", "to", "in", "on", "at", "by", "for", "of", "as", "up", "down"
        );
        
        // Domain-specific meaningful terms with expanded synonyms
        var domainTerms = Map.of(
            "printer", List.of("printer", "printing", "print", "inkjet", "laser", "ink", "toner", "paper", "cartridge"),
            "monitor", List.of("monitor", "display", "screen", "lcd", "led", "resolution", "brightness"),
            "network", List.of("network", "internet", "wifi", "ethernet", "connection", "router", "lan", "wan"),
            "computer", List.of("computer", "pc", "laptop", "desktop", "cpu", "hardware", "machine"),
            "software", List.of("software", "application", "app", "program", "install", "uninstall", "update"),
            "login", List.of("login", "password", "authentication", "sso", "signin", "access", "credential", "account"),
            "email", List.of("email", "mail", "outlook", "gmail", "exchange", "mailbox", "message"),
            "file", List.of("file", "document", "folder", "share", "storage", "drive", "directory"),
            "server", List.of("server", "database", "web", "api", "service", "host", "domain"),
            "security", List.of("security", "virus", "malware", "firewall", "antivirus", "scan", "threat")
        );
        
        // Extract meaningful terms
        for (var word : words) {
            // Skip stop words and very short words
            if (!stopWords.contains(word) && word.length() > 2) {
                // Check if it's a domain-specific term
                var isDomainTerm = false;
                for (var entry : domainTerms.entrySet()) {
                    if (entry.getValue().contains(word)) {
                        keywords.add(entry.getKey()); // Add the main category
                        keywords.add(word); // Add the specific term
                        isDomainTerm = true;
                        break;
                    }
                }
                
                // If not a domain term but longer than 3 chars, consider it
                if (!isDomainTerm && word.length() > 3) {
                    keywords.add(word);
                }
            }
        }
        
        return keywords.stream().distinct().collect(Collectors.toList());
    }
    
    private int calculateSolutionScore(Incident incident) {
        int score = 0;
        
        // Higher score for closed tickets
        if ("CLOSED".equals(incident.getStatus())) {
            score += 10;
        } else if ("IN_PROGRESS".equals(incident.getStatus())) {
            score += 5;
        }
        
        // Score based on solution content
        if (incident.getSolution() != null && !incident.getSolution().trim().isEmpty()) {
            score += 3;
        }
        
        // Score based on solution log entries
        if (incident.getSolutionLog() != null) {
            score += incident.getSolutionLog().size();
        }
        
        return score;
    }
    
    /**
     * Generate a fallback suggestion when AI service is not available
     */
    private String generateFallbackSuggestion(String description, List<String> previousSuggestions) {
        var lowerDescription = description.toLowerCase();
        
        // Count how many previous suggestions we have to provide different alternatives
        var attemptNumber = (previousSuggestions != null) ? previousSuggestions.size() : 0;
        
        // Simple rule-based suggestions based on keywords with alternatives
        if (lowerDescription.contains("password") || lowerDescription.contains("login")) {
            switch (attemptNumber) {
                case 0:
                    return "Password/Login Issue: 1) Try resetting your password 2) Clear browser cache and cookies 3) Check if Caps Lock is on 4) Contact IT support if issue persists";
                case 1:
                    return "Alternative Password Solution: 1) Use password recovery via security questions 2) Try logging in from a different browser 3) Check account lockout status 4) Verify correct username/domain";
                default:
                    return "Advanced Password Troubleshooting: 1) Check Active Directory account status 2) Verify network connectivity to domain controller 3) Try safe mode login 4) Contact system administrator for manual unlock";
            }
        }
        
        if (lowerDescription.contains("network") || lowerDescription.contains("internet") || lowerDescription.contains("connection")) {
            switch (attemptNumber) {
                case 0:
                    return "Network Issue: 1) Check network cables 2) Restart your router/modem 3) Check Wi-Fi connection 4) Run network diagnostics 5) Contact network administrator";
                case 1:
                    return "Alternative Network Solution: 1) Flush DNS cache (ipconfig /flushdns) 2) Reset network adapter 3) Check firewall settings 4) Try different DNS servers (8.8.8.8, 1.1.1.1)";
                default:
                    return "Advanced Network Troubleshooting: 1) Check network adapter drivers 2) Disable/enable network adapter 3) Run network reset commands 4) Check proxy settings 5) Perform network stack reset";
            }
        }
        
        if (lowerDescription.contains("slow") || lowerDescription.contains("performance")) {
            switch (attemptNumber) {
                case 0:
                    return "Performance Issue: 1) Close unnecessary applications 2) Restart your computer 3) Check for software updates 4) Run disk cleanup 5) Check available storage space";
                case 1:
                    return "Alternative Performance Solution: 1) Check Task Manager for high CPU/memory usage 2) Disable startup programs 3) Run antivirus scan 4) Check for malware 5) Defragment hard drive";
                default:
                    return "Advanced Performance Troubleshooting: 1) Check system temperature 2) Update device drivers 3) Check for failing hardware 4) Run memory diagnostic 5) Consider hardware upgrade";
            }
        }
        
        if (lowerDescription.contains("email") || lowerDescription.contains("outlook")) {
            switch (attemptNumber) {
                case 0:
                    return "Email Issue: 1) Check internet connection 2) Verify email settings 3) Clear email cache 4) Check spam/junk folder 5) Contact email administrator";
                case 1:
                    return "Alternative Email Solution: 1) Try webmail access 2) Create new Outlook profile 3) Check email server settings 4) Disable antivirus email scanning temporarily";
                default:
                    return "Advanced Email Troubleshooting: 1) Run Outlook in safe mode 2) Repair PST/OST files 3) Check Exchange server connectivity 4) Reset Outlook configuration 5) Reinstall email client";
            }
        }
        
        if (lowerDescription.contains("printer") || lowerDescription.contains("print")) {
            switch (attemptNumber) {
                case 0:
                    return "Printer Issue: 1) Check printer power and connections 2) Verify printer queue 3) Update printer drivers 4) Check paper and ink levels 5) Restart print spooler service";
                case 1:
                    return "Alternative Printer Solution: 1) Remove and re-add printer 2) Clear print queue completely 3) Check for printer firmware updates 4) Try printing from different application 5) Use generic printer driver";
                default:
                    return "Advanced Printer Troubleshooting: 1) Check printer port settings 2) Test with different USB/network cable 3) Reset printer to factory defaults 4) Check for Windows updates 5) Try different print processor";
            }
        }
        
        if (lowerDescription.contains("software") || lowerDescription.contains("application") || lowerDescription.contains("app")) {
            switch (attemptNumber) {
                case 0:
                    return "Software Issue: 1) Restart the application 2) Check for software updates 3) Run as administrator 4) Reinstall the software 5) Check system requirements";
                case 1:
                    return "Alternative Software Solution: 1) Check Windows Event Viewer for errors 2) Run application in compatibility mode 3) Disable antivirus temporarily 4) Check application logs 5) Clear application cache/settings";
                default:
                    return "Advanced Software Troubleshooting: 1) Check for conflicting software 2) Run system file checker (sfc /scannow) 3) Check .NET Framework/Visual C++ redistributables 4) Try clean boot environment 5) Contact software vendor support";
            }
        }
        
        if (lowerDescription.contains("error") || lowerDescription.contains("crash")) {
            switch (attemptNumber) {
                case 0:
                    return "System Error: 1) Note down error message/code 2) Restart the system 3) Check event logs 4) Update system drivers 5) Run system diagnostics";
                case 1:
                    return "Alternative Error Solution: 1) Boot in safe mode 2) Run memory test 3) Check hard drive for errors 4) Disable recently installed software 5) Restore from system restore point";
                default:
                    return "Advanced Error Troubleshooting: 1) Check dump files for crash analysis 2) Run hardware diagnostics 3) Check system temperature 4) Perform clean Windows installation 5) Test with minimal hardware configuration";
            }
        }
        
        // Generic fallback with alternatives
        switch (attemptNumber) {
            case 0:
                return "General Troubleshooting: 1) Document the exact issue and error messages 2) Try restarting the affected system/application 3) Check for recent changes or updates 4) Verify user permissions 5) Contact IT support with detailed information if issue persists";
            case 1:
                return "Alternative General Solution: 1) Check system logs and event viewer 2) Try the issue in safe mode 3) Create new user profile 4) Check for malware/virus 5) Verify hardware connections";
            default:
                return "Advanced General Troubleshooting: 1) Perform system restore to previous working state 2) Check hardware diagnostics 3) Review recent system changes 4) Test with minimal system configuration 5) Escalate to senior technical support";
        }
    }
    
    private boolean isPositiveResponse(String message) {
        var lower = message.toLowerCase().strip();
        return lower.matches(".*\\b(yes|y|yeah|yep|sure|ok|okay|fine|alright|good|sounds good|positive|yup|correct|right|true)\\b.*") ||
               lower.equals("yes") || lower.equals("y") || lower.equals("ok") || lower.equals("sure");
    }
    
    private boolean isNegativeResponse(String message) {
        var lower = message.toLowerCase().strip();
        return lower.matches(".*\\b(no|n|nope|not|negative|nah|never|false)\\b.*") ||
               lower.equals("no") || lower.equals("n") || lower.equals("nope");
    }
    
    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }
    
    // Inner classes
    public static class ChatResponse {
        private String message;
        private ChatState state;
        private boolean sessionActive;
        private List<ChatButton> buttons;
        
        public ChatResponse(String message, ChatState state) {
            this.message = message;
            this.state = state;
            this.sessionActive = state != ChatState.SESSION_CLOSED;
            this.buttons = new ArrayList<>();
        }
        
        public ChatResponse(String message, ChatState state, List<ChatButton> buttons) {
            this.message = message;
            this.state = state;
            this.sessionActive = state != ChatState.SESSION_CLOSED;
            this.buttons = buttons != null ? buttons : new ArrayList<>();
        }
        
        // Getters
        public String getMessage() { return message; }
        public ChatState getState() { return state; }
        public boolean isSessionActive() { return sessionActive; }
        public List<ChatButton> getButtons() { return buttons; }
    }
    
    public static class ChatButton {
        private String text;
        private String action;
        private String style;
        
        public ChatButton(String text, String action) {
            this.text = text;
            this.action = action;
            this.style = "primary";
        }
        
        public ChatButton(String text, String action, String style) {
            this.text = text;
            this.action = action;
            this.style = style;
        }
        
        // Getters
        public String getText() { return text; }
        public String getAction() { return action; }
        public String getStyle() { return style; }
    }
    
    public static class ChatSession {
        private ChatState state = ChatState.INITIAL;
        private String currentIncidentDescription;
        private String currentAiSuggestion;
        private List<String> previousAiSuggestions = new ArrayList<>();
        private List<ChatMessage> conversationHistory = new ArrayList<>();
        private Incident parentIncident;
        
        public void reset() {
            this.state = ChatState.INITIAL;
            this.currentIncidentDescription = null;
            this.currentAiSuggestion = null;
            this.previousAiSuggestions.clear();
            this.parentIncident = null;
            // Don't clear conversation history on reset - only on new session
        }
        
        public void addMessage(String sender, String message) {
            conversationHistory.add(new ChatMessage(sender, message, System.currentTimeMillis()));
        }
        
        public void clearHistory() {
            this.conversationHistory.clear();
            this.state = ChatState.INITIAL;
            this.currentIncidentDescription = null;
            this.currentAiSuggestion = null;
            this.previousAiSuggestions.clear();
            this.parentIncident = null;
        }
        
        public void addPreviousAiSuggestion(String suggestion) {
            if (suggestion != null && !suggestion.trim().isEmpty()) {
                this.previousAiSuggestions.add(suggestion);
            }
        }
        
        // Getters and setters
        public ChatState getState() { return state; }
        public void setState(ChatState state) { this.state = state; }
        public String getCurrentIncidentDescription() { return currentIncidentDescription; }
        public void setCurrentIncidentDescription(String description) { this.currentIncidentDescription = description; }
        public String getCurrentAiSuggestion() { return currentAiSuggestion; }
        public void setCurrentAiSuggestion(String suggestion) { this.currentAiSuggestion = suggestion; }
        public List<ChatMessage> getConversationHistory() { return conversationHistory; }
        public List<String> getPreviousAiSuggestions() { return previousAiSuggestions; }
        public Incident getParentIncident() { return parentIncident; }
        public void setParentIncident(Incident parentIncident) { this.parentIncident = parentIncident; }
    }
    
    public enum ChatState {
        INITIAL,
        WAITING_FOR_AI_CONFIRMATION,
        WAITING_FOR_AI_HELPFUL_RESPONSE,
        WAITING_FOR_OPTION_SELECTION,
        WAITING_FOR_TICKET_CONFIRMATION,
        WAITING_FOR_NEW_ISSUE,
        EXISTING_SOLUTION_FOUND,
        WAITING_FOR_HELP_RESPONSE,
        WAITING_FOR_OTHER_ISSUES,
        WAITING_FOR_CHILD_INCIDENT_CONFIRMATION,
        WAITING_FOR_CHILD_INCIDENT_DESCRIPTION,
        WAITING_FOR_NEW_INCIDENT_DESCRIPTION,
        SESSION_CLOSED
    }
    
    public static class ChatMessage {
        private String sender;
        private String message;
        private long timestamp;
        
        public ChatMessage(String sender, String message, long timestamp) {
            this.sender = sender;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getSender() { return sender; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
}
