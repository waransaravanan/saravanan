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
    
    @Autowired
    private OpenAISuggestionService openAISuggestionService;
    
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
        String lowerMessage = message.toLowerCase();
        
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
                
            case SESSION_CLOSED:
                return handleNewQuery(session, message);
                
            default:
                return new ChatResponse("I'm not sure what you mean. Could you please rephrase?", ChatState.INITIAL);
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
            default:
                return new ChatResponse("I didn't understand that action. Please try again.", session.getState());
        }
    }
    
    private ChatResponse handleHelpYes(ChatSession session) {
        session.setState(ChatState.WAITING_FOR_OTHER_ISSUES);
        List<ChatButton> buttons = Arrays.asList(
            new ChatButton("Yes", "other_issues_yes", "primary"),
            new ChatButton("No", "other_issues_no", "secondary")
        );
        return new ChatResponse("Great! I'm glad I could help you resolve the issue. 🎉\n\nDo you have any other IT problems I can help you with?", ChatState.WAITING_FOR_OTHER_ISSUES, buttons);
    }
    
    private ChatResponse handleHelpNo(ChatSession session) {
        session.setState(ChatState.WAITING_FOR_OPTION_SELECTION);
        return new ChatResponse(
            "I understand the solution wasn't quite right. I have a few options:\n" +
            "1. Try another AI suggestion\n" +
            "2. Create a support ticket for manual resolution\n" +
            "3. Close this session\n\n" +
            "Please type 1, 2, or 3 to choose an option.", 
            ChatState.WAITING_FOR_OPTION_SELECTION
        );
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
            // Found existing solutions
            StringBuilder solutionsBuilder = new StringBuilder();
            
            for (Incident inc : existingSolutions) {
                String incidentNumber = inc.getIncidentNumber() != null ? inc.getIncidentNumber() : "ID: " + inc.getId();
                String encodedDescription = inc.getDescription().replace(" ", "%20");
                String solutionsUrl = "https://bug-free-broccoli-w9v9xj7w55q2pq4-8080.app.github.dev/api/incidents/solutions-ui?description=" + encodedDescription;
                
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
                    List<String> relevantLogs = inc.getSolutionLog().stream()
                        .filter(log -> log.toLowerCase().contains("solution") || log.toLowerCase().contains("update"))
                        .collect(Collectors.toList());
                    
                    int logsToShow = Math.min(3, relevantLogs.size());
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
            List<ChatButton> buttons = Arrays.asList(
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
            // No existing solutions found
            session.setState(ChatState.WAITING_FOR_AI_CONFIRMATION);
            return new ChatResponse(
                "I don't have any existing solutions for issues like '" + message + 
                "' in our system. Would you like me to get an AI-powered suggestion? (yes/y/sure/ok)", 
                ChatState.WAITING_FOR_AI_CONFIRMATION
            );
        }
    }
    
    private ChatResponse handleAiConfirmation(ChatSession session, String message) {
        if (isPositiveResponse(message)) {
            // User wants AI suggestion
            String aiSuggestion = openAISuggestionService.getSuggestion(session.getCurrentIncidentDescription());
            session.setCurrentAiSuggestion(aiSuggestion);
            session.addPreviousAiSuggestion(aiSuggestion);
            session.setState(ChatState.WAITING_FOR_AI_HELPFUL_RESPONSE);
            
            return new ChatResponse(
                "Here's an AI-powered suggestion for your issue:\n\n" + aiSuggestion + 
                "\n\nIs this helpful? (yes/no)", 
                ChatState.WAITING_FOR_AI_HELPFUL_RESPONSE
            );
        } else if (isNegativeResponse(message)) {
            // User doesn't want AI suggestion
            session.setState(ChatState.WAITING_FOR_TICKET_CONFIRMATION);
            return new ChatResponse(
                "No problem! Since we don't have a matching solution, would you like me to create a support ticket for this issue? (yes/no)", 
                ChatState.WAITING_FOR_TICKET_CONFIRMATION
            );
        } else {
            // Unclear response - ask again
            return new ChatResponse(
                "I didn't quite understand your response. Would you like me to get an AI-powered suggestion for '" + 
                session.getCurrentIncidentDescription() + "'? Please answer yes or no.", 
                ChatState.WAITING_FOR_AI_CONFIRMATION
            );
        }
    }
    
    private ChatResponse handleAiHelpfulResponse(ChatSession session, String message) {
        if (isPositiveResponse(message)) {
            // AI suggestion was helpful - save it
            Incident incident = incidentService.createIncidentWithoutSolution(session.getCurrentIncidentDescription());
            incidentService.updateIncidentSolution(incident.getId(), session.getCurrentAiSuggestion());
            session.setState(ChatState.SESSION_CLOSED);
            
            return new ChatResponse(
                "Excellent! I've saved this solution for future reference as incident " + incident.getIncidentNumber() + 
                ". This will help other users with similar issues.\n\nDo you have any other IT issues I can help you with? (yes/no)", 
                ChatState.SESSION_CLOSED
            );
        } else if (isNegativeResponse(message)) {
            // AI suggestion wasn't helpful - provide options
            session.setState(ChatState.WAITING_FOR_OPTION_SELECTION);
            return new ChatResponse(
                "I understand the AI suggestion wasn't quite right. I have a few options:\n" +
                "1. Try another AI suggestion\n" +
                "2. Create a support ticket for manual resolution\n" +
                "3. Close this session\n\n" +
                "Please type 1, 2, or 3 to choose an option.", 
                ChatState.WAITING_FOR_OPTION_SELECTION
            );
        } else {
            // Unclear response - ask again
            return new ChatResponse(
                "I didn't quite understand your response. Was the AI suggestion helpful for your issue '" + 
                session.getCurrentIncidentDescription() + "'? Please answer yes or no.", 
                ChatState.WAITING_FOR_AI_HELPFUL_RESPONSE
            );
        }
    }
    
    private ChatResponse handleOptionSelection(ChatSession session, String message) {
        switch (message) {
            case "1":
                // Try another AI suggestion
                String aiSuggestion = openAISuggestionService.getAlternativeSuggestion(
                    session.getCurrentIncidentDescription(), 
                    session.getPreviousAiSuggestions()
                );
                session.setCurrentAiSuggestion(aiSuggestion);
                session.addPreviousAiSuggestion(aiSuggestion);
                session.setState(ChatState.WAITING_FOR_AI_HELPFUL_RESPONSE);
                
                return new ChatResponse(
                    "Let me try a different AI suggestion for your issue:\n\n" + aiSuggestion + 
                    "\n\nIs this helpful? (yes/no)", 
                    ChatState.WAITING_FOR_AI_HELPFUL_RESPONSE
                );
                
            case "2":
                // Create support ticket
                session.setState(ChatState.WAITING_FOR_TICKET_CONFIRMATION);
                return new ChatResponse(
                    "I'll create a support ticket for manual resolution. Would you like me to proceed? (yes/no)", 
                    ChatState.WAITING_FOR_TICKET_CONFIRMATION
                );
                
            case "3":
                // Close session
                session.setState(ChatState.SESSION_CLOSED);
                return new ChatResponse(
                    "No problem! If you need help with anything else, just let me know.\n\nDo you have any other IT issues I can help you with? (yes/no)", 
                    ChatState.SESSION_CLOSED
                );
                
            default:
                // Invalid option
                return new ChatResponse(
                    "I didn't understand that option. Please choose:\n" +
                    "1. Try another AI suggestion\n" +
                    "2. Create a support ticket for manual resolution\n" +
                    "3. Close this session\n\n" +
                    "Please type 1, 2, or 3.", 
                    ChatState.WAITING_FOR_OPTION_SELECTION
                );
        }
    }
    
    private ChatResponse handleTicketConfirmation(ChatSession session, String message) {
        if (isPositiveResponse(message)) {
            // Create ticket
            Incident incident = incidentService.createIncidentWithoutSolution(session.getCurrentIncidentDescription());
            session.setState(ChatState.SESSION_CLOSED);
            
            return new ChatResponse(
                "Perfect! I've created support ticket " + incident.getIncidentNumber() + 
                " for your issue: '" + session.getCurrentIncidentDescription() + 
                "'. Our support team will work on this and update the ticket with a solution.\n\n" +
                "You can check the status anytime in our solutions page.\n\n" +
                "Do you have any other IT issues I can help you with? (yes/no)",
                ChatState.SESSION_CLOSED
            );
        } else if (isNegativeResponse(message)) {
            // User doesn't want ticket
            session.setState(ChatState.SESSION_CLOSED);
            return new ChatResponse(
                "No worries! If you change your mind, just let me know. " +
                "You can also try describing your issue differently - sometimes I can find better matches.\n\n" +
                "Do you have any other IT issues I can help you with? (yes/no)",
                ChatState.SESSION_CLOSED
            );
        } else {
            // Unclear response - ask again
            return new ChatResponse(
                "I didn't quite understand your response. Would you like me to create a support ticket for your issue '" + 
                session.getCurrentIncidentDescription() + "'? Please answer yes or no.", 
                ChatState.WAITING_FOR_TICKET_CONFIRMATION
            );
        }
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
    
    private List<Incident> findSimilarSolutions(String description) {
        // Enhanced similarity matching
        List<Incident> allIncidents = incidentService.getAllIncidents();
        List<String> keywords = extractKeywords(description);
        
        return allIncidents.stream()
            .filter(incident -> {
                // Include incidents that have either:
                // 1. A solution in the main solution field, OR
                // 2. Solution updates in the log (solutionLog), OR  
                // 3. Are closed tickets (likely have solutions)
                boolean hasSolution = incident.getSolution() != null && !incident.getSolution().trim().isEmpty();
                boolean hasSolutionLog = incident.getSolutionLog() != null && !incident.getSolutionLog().isEmpty() && 
                    incident.getSolutionLog().stream().anyMatch(log -> log.toLowerCase().contains("solution") || log.toLowerCase().contains("update"));
                boolean isClosedWithActivity = "CLOSED".equals(incident.getStatus()) || "IN_PROGRESS".equals(incident.getStatus());
                
                return hasSolution || hasSolutionLog || isClosedWithActivity;
            })
            .filter(incident -> {
                String incidentDesc = incident.getDescription().toLowerCase();
                return keywords.stream().anyMatch(keyword -> 
                    incidentDesc.contains(keyword) || 
                    incidentDesc.matches(".*\\b" + keyword + "\\b.*")
                );
            })
            .sorted((i1, i2) -> {
                // Prioritize closed tickets and those with more solution updates
                int score1 = calculateSolutionScore(i1);
                int score2 = calculateSolutionScore(i2);
                return Integer.compare(score2, score1); // Higher score first
            })
            .limit(5) // Return top 5 matches
            .collect(Collectors.toList());
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
    
    private List<String> extractKeywords(String description) {
        // Extract meaningful keywords from description
        String[] words = description.toLowerCase().split("\\s+");
        List<String> keywords = new ArrayList<>();
        
        // Common IT keywords and their variations
        Map<String, List<String>> synonyms = Map.of(
            "printer", Arrays.asList("printer", "printing", "print"),
            "network", Arrays.asList("network", "internet", "connection", "wifi", "lan"),
            "computer", Arrays.asList("computer", "pc", "laptop", "desktop", "machine"),
            "software", Arrays.asList("software", "application", "app", "program"),
            "password", Arrays.asList("password", "login", "authentication", "access"),
            "email", Arrays.asList("email", "mail", "outlook", "gmail"),
            "file", Arrays.asList("file", "document", "folder", "directory")
        );
        
        for (String word : words) {
            // Add exact word
            if (word.length() > 3) {
                keywords.add(word);
            }
            
            // Add synonyms
            synonyms.forEach((key, values) -> {
                if (values.contains(word)) {
                    keywords.addAll(values);
                }
            });
        }
        
        return keywords.stream().distinct().collect(Collectors.toList());
    }
    
    private boolean isPositiveResponse(String message) {
        String lower = message.toLowerCase().trim();
        return lower.matches(".*\\b(yes|y|yeah|yep|sure|ok|okay|fine|alright|good|sounds good|positive|yup|correct|right|true)\\b.*") ||
               lower.equals("yes") || lower.equals("y") || lower.equals("ok") || lower.equals("sure");
    }
    
    private boolean isNegativeResponse(String message) {
        String lower = message.toLowerCase().trim();
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
        
        public void reset() {
            this.state = ChatState.INITIAL;
            this.currentIncidentDescription = null;
            this.currentAiSuggestion = null;
            this.previousAiSuggestions.clear();
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
