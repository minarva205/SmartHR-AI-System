package com.smarthr.backend.controller;

import com.smarthr.backend.model.Task;
import com.smarthr.backend.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    // Direct system environment variable injection from application.properties
    @Value("${api.openai.secret-key:dummy-key}")
    private String openAiKey;

    @GetMapping("/all")
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @GetMapping("/employee/{employeeId}")
    public List<Task> getTasksByEmployeeId(@PathVariable Long employeeId) {
        return taskRepository.findByEmployeeId(employeeId);
    }

    @PostMapping("/assign")
    public Task assignTask(@RequestBody Task task) {
        if (task.getStatus() == null) {
            task.setStatus("Pending");
        }
        return taskRepository.save(task);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Task> updateTaskStatus(@PathVariable Long id, @RequestParam String status) {
        return taskRepository.findById(id).map(task -> {
            task.setStatus(status);
            Task saved = taskRepository.save(task);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // ==========================================
    // NEW FEATURE 1: AI ROADMAP GENERATOR FOR EMPLOYEES
    // ==========================================
    @GetMapping("/{id}/ai-roadmap")
    public ResponseEntity<?> getAiTaskRoadmap(@PathVariable Long id) {
        return taskRepository.findById(id).map(task -> {
            String prompt = "You are an HR AI expert. Provide a concise, professional 3-step actionable strategy for an employee to complete this corporate task. Title: "
                    + task.getTitle() + ", Description: " + task.getDescription() + ". Keep it under 100 words.";
            String aiResponse = callOpenAiGpt(prompt);
            Map<String, String> result = new HashMap<>();
            result.put("roadmap", aiResponse);
            return ResponseEntity.ok(result);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // NEW FEATURE 2: AI TASK PRIORITY ANALYZER FOR ADMIN
    // ==========================================
    @PostMapping("/ai-analyze")
    public ResponseEntity<?> analyzeTaskUrgency(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        String description = request.get("description");
        String dueDate = request.get("dueDate");

        String prompt = "Analyze this task for an HR dashboard. Give a 1-sentence urgent risk matrix assessment and suggest if the current operational priority aligns. Task: "
                + title + ", Detail: " + description + ", Due: " + dueDate;

        String aiResponse = callOpenAiGpt(prompt);
        Map<String, String> result = new HashMap<>();
        result.put("analysis", aiResponse);
        return ResponseEntity.ok(result);
    }

    // Reusable core network adapter method to interact with OpenAI API
    private String callOpenAiGpt(String prompt) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openAiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");

            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            requestBody.put("messages", new java.util.ArrayList<>(List.of(message)));
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity("https://api.openai.com/v1/chat/completions",
                    entity, (Class<Map<String, Object>>) (Class<?>) Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<?> choices = (List<?>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> msg = (Map<?, ?>) firstChoice.get("message");
                    return (String) msg.get("content");
                }
            }
            return "AI services are currently updating operational data models.";
        } catch (Exception e) {
            return "AI Insight temporarily offline. Please verify API Key activation parameters. Error: "
                    + e.getMessage();
        }
    }
}