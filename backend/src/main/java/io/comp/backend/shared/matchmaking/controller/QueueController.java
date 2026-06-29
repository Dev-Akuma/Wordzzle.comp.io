package io.comp.backend.shared.matchmaking.controller;

import io.comp.backend.shared.matchmaking.service.MatchmakerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/queue")
public class QueueController {
    @Autowired
    private MatchmakerService matchmakerService;

    @PostMapping("/join")
    public ResponseEntity<?> joinQueue() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        matchmakerService.joinQueue(username);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "QUEUED");
        response.put("message", "Joined matchmaking queue");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/leave")
    public ResponseEntity<?> leaveQueue() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        matchmakerService.leaveQueue(username);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "IDLE");
        response.put("message", "Left matchmaking queue");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<?> getQueueStatus() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean queued = matchmakerService.isInQueue(username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("queued", queued);
        response.put("status", queued ? "QUEUED" : "IDLE");
        return ResponseEntity.ok(response);
    }
}
