

package com.smarthr.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AboutController {

    @GetMapping("/about")
    public String showAbout() {
        // This directs Spring Boot to src/main/resources/templates/about.html
      return "about";
    }
}