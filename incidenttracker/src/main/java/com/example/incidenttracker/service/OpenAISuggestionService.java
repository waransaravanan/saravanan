package com.example.incidenttracker.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
        if (openAiApiKey == null || openAiApiKey.strip().isEmpty()) {
            return generateFallbackSuggestion(description, null);
        }

        try {
            return callOpenAiApi(description, null);
        } catch (Exception e) {
            // Log the error and return fallback suggestion
            System.err.println("Error calling OpenAI API: " + e.getMessage());
            return generateFallbackSuggestion(description, null);
        }
    }

    /**
     * Get alternative AI-powered suggestion that's different from previous suggestions
     * @param description The incident description
     * @param previousSuggestions List of previously given suggestions to avoid
     * @return AI-generated alternative suggestion or fallback message
     */
    public String getAlternativeSuggestion(String description, List<String> previousSuggestions) {
        // If OpenAI API key is not configured, return a fallback suggestion
        if (openAiApiKey == null || openAiApiKey.strip().isEmpty()) {
            return generateFallbackSuggestion(description, previousSuggestions);
        }

        try {
            return callOpenAiApi(description, previousSuggestions);
        } catch (Exception e) {
            // Log the error and return fallback suggestion
            System.err.println("Error calling OpenAI API: " + e.getMessage());
            return generateFallbackSuggestion(description, previousSuggestions);
        }
    }

    /**
     * Call OpenAI API to get suggestion
     */
    private String callOpenAiApi(String description, List<String> previousSuggestions) {
        var headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + openAiApiKey);
        headers.set("Content-Type", "application/json");

        var requestBody = new HashMap<String, Object>();
        requestBody.put("model", "gpt-3.5-turbo");
        requestBody.put("max_tokens", 150);
        requestBody.put("temperature", 0.9); // Higher temperature for more variety

        // Create messages array
        var systemMessage = new HashMap<String, String>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are an IT support expert. Provide concise, practical solutions for technical incidents. Always provide different approaches and alternatives.");

        var userMessage = new HashMap<String, String>();
        var userContent = "Provide a solution for this incident: " + description;
        
        // If there are previous suggestions, ask for alternatives
        if (previousSuggestions != null && !previousSuggestions.isEmpty()) {
            userContent += "\n\nPrevious suggestions that didn't work:\n" + 
                String.join("\n", previousSuggestions) + 
                "\n\nPlease provide a completely different approach or alternative solution.";
        }
        
        userMessage.put("content", userContent);

        requestBody.put("messages", List.of(systemMessage, userMessage));

        var entity = new HttpEntity<>(requestBody, headers);

        try {
            var response = restTemplate.exchange(
                openAiApiUrl, 
                HttpMethod.POST, 
                entity, 
                Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                var choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    var firstChoice = choices.get(0);
                    var message = (Map<String, Object>) firstChoice.get("message");
                    return (String) message.get("content");
                }
            }
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to call OpenAI API", e);
        }

        return generateFallbackSuggestion(description, previousSuggestions);
    }

    /**
     * Generate a fallback suggestion when OpenAI is not available
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

    /**
     * Get multiple suggestions for an incident
     */
    public List<String> getMultipleSuggestions(String description) {
        var suggestion = getSuggestion(description);
        return List.of(suggestion);
    }

    /**
     * Check if OpenAI service is configured and available
     */
    public boolean isOpenAiConfigured() {
        return openAiApiKey != null && !openAiApiKey.strip().isEmpty();
    }
}
