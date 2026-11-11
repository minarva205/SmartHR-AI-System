package com.smarthr.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String showDashboard() {
        return "redirect:/index.html";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/api/health")
    @ResponseBody
    public String health() {
        return "{\"status\": \"UP\", \"message\": \"SmartHR Backend is running!\"}";
    }
}