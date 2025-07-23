package com.example.incidenttracker.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAISuggestionService {

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openAiApiUrl;

    private final RestTemplate restTemplate;

    public OpenAISuggestionService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Get AI-powered suggestion for an incident description
     * @param description The incident description
     * @return AI-generated suggestion or fallback message
     */
    public String getSuggestion(String description) {
        // If OpenAI API key is not configured, return a fallback suggestion
        if (openAiApiKey == null || openAiApiKey.trim().isEmpty()) {
            return generateFallbackSuggestion(description);
        }

        try {
            return callOpenAiApi(description);
        } catch (Exception e) {
            // Log the error and return fallback suggestion
            System.err.println("Error calling OpenAI API: " + e.getMessage());
            return generateFallbackSuggestion(description);
        }
    }

    /**
     * Call OpenAI API to get suggestion
     */
    private String callOpenAiApi(String description) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + openAiApiKey);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");
        requestBody.put("max_tokens", 150);
        requestBody.put("temperature", 0.7);

        // Create messages array
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are an IT support expert. Provide concise, practical solutions for technical incidents.");

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", "Provide a solution for this incident: " + description);

        requestBody.put("messages", List.of(systemMessage, userMessage));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                openAiApiUrl, 
                HttpMethod.POST, 
                entity, 
                Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    return (String) message.get("content");
                }
            }
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to call OpenAI API", e);
        }

        return generateFallbackSuggestion(description);
    }

    /**
     * Generate a fallback suggestion when OpenAI is not available
     */
    private String generateFallbackSuggestion(String description) {
        String lowerDescription = description.toLowerCase();
        
        // Simple rule-based suggestions based on keywords
        if (lowerDescription.contains("password") || lowerDescription.contains("login")) {
            return "Password/Login Issue: 1) Try resetting your password 2) Clear browser cache and cookies 3) Check if Caps Lock is on 4) Contact IT support if issue persists";
        }
        
        if (lowerDescription.contains("network") || lowerDescription.contains("internet") || lowerDescription.contains("connection")) {
            return "Network Issue: 1) Check network cables 2) Restart your router/modem 3) Check Wi-Fi connection 4) Run network diagnostics 5) Contact network administrator";
        }
        
        if (lowerDescription.contains("slow") || lowerDescription.contains("performance")) {
            return "Performance Issue: 1) Close unnecessary applications 2) Restart your computer 3) Check for software updates 4) Run disk cleanup 5) Check available storage space";
        }
        
        if (lowerDescription.contains("email") || lowerDescription.contains("outlook")) {
            return "Email Issue: 1) Check internet connection 2) Verify email settings 3) Clear email cache 4) Check spam/junk folder 5) Contact email administrator";
        }
        
        if (lowerDescription.contains("printer") || lowerDescription.contains("print")) {
            return "Printer Issue: 1) Check printer power and connections 2) Verify printer queue 3) Update printer drivers 4) Check paper and ink levels 5) Restart print spooler service";
        }
        
        if (lowerDescription.contains("software") || lowerDescription.contains("application") || lowerDescription.contains("app")) {
            return "Software Issue: 1) Restart the application 2) Check for software updates 3) Run as administrator 4) Reinstall the software 5) Check system requirements";
        }
        
        if (lowerDescription.contains("error") || lowerDescription.contains("crash")) {
            return "System Error: 1) Note down error message/code 2) Restart the system 3) Check event logs 4) Update system drivers 5) Run system diagnostics";
        }
        
        // Generic fallback
        return "General Troubleshooting: 1) Document the exact issue and error messages 2) Try restarting the affected system/application 3) Check for recent changes or updates 4) Verify user permissions 5) Contact IT support with detailed information if issue persists";
    }

    /**
     * Get multiple suggestions for an incident
     */
    public List<String> getMultipleSuggestions(String description) {
        String suggestion = getSuggestion(description);
        return List.of(suggestion);
    }

    /**
     * Check if OpenAI service is configured and available
     */
    public boolean isOpenAiConfigured() {
        return openAiApiKey != null && !openAiApiKey.trim().isEmpty();
    }
}
