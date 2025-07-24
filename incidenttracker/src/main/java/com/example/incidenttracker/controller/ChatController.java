package com.example.incidenttracker.controller;

import com.example.incidenttracker.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
public class ChatController {
    
    @Autowired
    private ChatService chatService;
    
    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }
    
    @PostMapping("/api/chat/message")
    @ResponseBody
    public ResponseEntity<ChatService.ChatResponse> sendMessage(
            @RequestBody Map<String, String> payload,
            HttpSession httpSession) {
        
        var message = payload.get("message");
        if (message == null || message.strip().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        var sessionId = httpSession.getId();
        var response = chatService.processMessage(sessionId, message);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/api/chat/history")
    @ResponseBody
    public ResponseEntity<List<ChatService.ChatMessage>> getHistory(HttpSession httpSession) {
        var sessionId = httpSession.getId();
        var history = chatService.getConversationHistory(sessionId);
        return ResponseEntity.ok(history);
    }
    
    @PostMapping("/api/chat/clear")
    @ResponseBody
    public ResponseEntity<String> clearHistory(HttpSession httpSession) {
        var sessionId = httpSession.getId();
        chatService.clearConversationHistory(sessionId);
        return ResponseEntity.ok("History cleared");
    }
    
    @PostMapping("/api/chat/close")
    @ResponseBody
    public ResponseEntity<String> closeSession(HttpSession httpSession) {
        var sessionId = httpSession.getId();
        chatService.clearSession(sessionId);
        return ResponseEntity.ok("Session closed");
    }
    
    @PostMapping("/api/chat/button")
    @ResponseBody
    public ResponseEntity<ChatService.ChatResponse> handleButtonResponse(
            @RequestBody Map<String, String> payload,
            HttpSession httpSession) {
        
        var action = payload.get("action");
        if (action == null || action.strip().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        var sessionId = httpSession.getId();
        var response = chatService.processButtonAction(sessionId, action);
        
        return ResponseEntity.ok(response);
    }
}
