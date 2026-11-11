package com.smarthr.backend.controller;

import com.smarthr.backend.model.*;
import com.smarthr.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/lms")
@CrossOrigin(origins = "*")
public class LmsController {

    @Autowired
    private CourseRepository courseRepo;
    @Autowired
    private TrainingRepository trainingRepo;

    @GetMapping("/courses")
    public List<Course> getCourses() {
        return courseRepo.findAll();
    }

    @GetMapping("/progress")
    public List<TrainingProgress> getProgress() {
        return trainingRepo.findAll();
    }

    @GetMapping("/progress/employee/{employeeId}")
    public List<TrainingProgress> getProgressByEmployeeId(@PathVariable Long employeeId) {
        return trainingRepo.findByEmployeeId(employeeId);
    }

    @PostMapping("/assign")
    public TrainingProgress assign(@RequestBody TrainingProgress tp) {
        tp.setAssignedDate(LocalDate.now());
        tp.setStatus("Assigned");
        tp.setCompletionPercentage(0);
        return trainingRepo.save(tp);
    }

    @PostMapping("/courses")
    public Course createCourse(@RequestBody Course course) {
        return courseRepo.save(course);
    }

    @PutMapping("/courses/{id}")
    public Course updateCourse(@PathVariable Long id, @RequestBody Course courseDetails) {
        Course course = courseRepo.findById(id).orElse(null);
        if (course != null) {
            course.setCourseName(courseDetails.getCourseName());
            course.setDescription(courseDetails.getDescription());
            course.setCategory(courseDetails.getCategory());
            course.setDuration(courseDetails.getDuration());
            course.setChapters(courseDetails.getChapters());
            return courseRepo.save(course);
        }
        return null;
    }

    @DeleteMapping("/courses/{id}")
    public void deleteCourse(@PathVariable Long id) {
        courseRepo.deleteById(id);
    }

    @PutMapping("/progress/{id}")
    public TrainingProgress updateProgress(@PathVariable Long id, @RequestBody TrainingProgress progressDetails) {
        TrainingProgress progress = trainingRepo.findById(id).orElse(null);
        if (progress != null) {
            progress.setCompletionPercentage(progressDetails.getCompletionPercentage());
            progress.setStatus(progressDetails.getStatus());
            if (progressDetails.getAiCertified() != null) {
                progress.setAiCertified(progressDetails.getAiCertified());
            }
            if (progressDetails.getQuizScore() != null) {
                progress.setQuizScore(progressDetails.getQuizScore());
            }
            return trainingRepo.save(progress);
        }
        return null;
    }

    @DeleteMapping("/progress/{id}")
    public void deleteProgress(@PathVariable Long id) {
        trainingRepo.deleteById(id);
    }

    @PostMapping("/ai/generate-course")
    public Map<String, Object> aiGenerateCourse(@RequestBody Map<String, String> payload) {
        String topic = payload.getOrDefault("topic", "").trim();
        String cleanTopic = topic.toLowerCase();
        
        Map<String, Object> result = new HashMap<>();
        String name;
        String desc;
        String cat;
        String dur;
        String chapters;
        
        if (cleanTopic.contains("react") || cleanTopic.contains("frontend") || cleanTopic.contains("vue") || cleanTopic.contains("angular") || cleanTopic.contains("web")) {
            name = "React & Modern Frontend Architecture";
            desc = "Learn component-based development, state management hooks, and scalable client performance.";
            cat = "Frontend Engineering";
            dur = "12 hours";
            chapters = "Chapter 1: Intro to Component Thinking\nChapter 2: Managing State with React Hooks\nChapter 3: Context API vs Redux Toolkit\nChapter 4: Handling Asynchronous API Side-Effects\nChapter 5: Client-Side Routing and Dynamic Imports\nChapter 6: Styling with Tailwind & Styled System\nChapter 7: Optimizing Rendering and Virtualization\nChapter 8: Writing Robust Unit Tests with Jest\nChapter 9: Deployment Strategies & CDN Caching\nChapter 10: AI Co-piloting for Frontend Productivity";
        } else if (cleanTopic.contains("java") || cleanTopic.contains("springboot") || cleanTopic.contains("backend") || cleanTopic.contains("spring")) {
            name = "Spring Boot & Cloud-Native Backends";
            desc = "Master enterprise Java development, REST architectures, Spring Data JPA, and security frameworks.";
            cat = "Backend Engineering";
            dur = "16 hours";
            chapters = "Chapter 1: Dynamic Java and Maven Configurations\nChapter 2: Building Core REST API Architectures\nChapter 3: Spring Dependency Injection and Beans\nChapter 4: Persistent Databases with Hibernate & JPA\nChapter 5: Spring Security and JWT Integrations\nChapter 6: Caching Strategies with Redis\nChapter 7: Message Brokers using RabbitMQ/Kafka\nChapter 8: Writing Spring Boot Controller Tests\nChapter 9: Dockerizing Enterprise Java Microservices\nChapter 10: Production Logging and Cloud Deployments";
        } else if (cleanTopic.contains("kotlin") || cleanTopic.contains("android") || cleanTopic.contains("mobile")) {
            name = "Kotlin Fundamentals & Android Jetpack";
            desc = "Design premium mobile applications using native Android SDK, coroutines, and declarative compose UI.";
            cat = "Mobile Development";
            dur = "14 hours";
            chapters = "Chapter 1: Introduction to Kotlin Syntax & Types\nChapter 2: Functional Programming & Lambda Basics\nChapter 3: Native Jetpack Compose Declarative UI\nChapter 4: Managing App Navigation Structures\nChapter 5: Asynchronous Coroutines and Flow Architectures\nChapter 6: Local Databases using Room persistence\nChapter 7: REST Integrations with Retrofit & OkHttp\nChapter 8: State Holders and ViewModel Implementation\nChapter 9: Google Play Store Release Checklists\nChapter 10: Performance Audits & Memory Profiling";
        } else {
            String capTopic = topic.isEmpty() ? "General Topic" : topic.substring(0, 1).toUpperCase() + topic.substring(1);
            name = capTopic + " Mastery & Practices";
            desc = "A comprehensive curriculum focused on standard methodologies, tooling, and real-world architectures for " + capTopic + ".";
            cat = "Professional Development";
            dur = "8 hours";
            chapters = "Chapter 1: Foundations of " + capTopic + "\n" +
                       "Chapter 2: Essential tools & architecture for " + capTopic + "\n" +
                       "Chapter 3: Intermediate concepts and best practices\n" +
                       "Chapter 4: Advanced structures in " + capTopic + "\n" +
                       "Chapter 5: Case studies and patterns\n" +
                       "Chapter 6: Overcoming common challenges\n" +
                       "Chapter 7: Integrating AI tools with " + capTopic + "\n" +
                       "Chapter 8: Testing, validation, and benchmarking\n" +
                       "Chapter 9: Real-world production deployment\n" +
                       "Chapter 10: Future trends & lifelong mastery";
        }
        
        result.put("courseName", name);
        result.put("description", desc);
        result.put("category", cat);
        result.put("duration", dur);
        result.put("chapters", chapters);
        return result;
    }

    @PostMapping("/ai/chat")
    public Map<String, String> aiChat(@RequestBody Map<String, Object> payload) {
        Long courseId = Long.valueOf(payload.getOrDefault("courseId", "0").toString());
        String userMessage = payload.getOrDefault("message", "").toString().trim().toLowerCase();
        
        Course course = courseRepo.findById(courseId).orElse(null);
        String courseName = course != null ? course.getCourseName() : "General Studies";
        
        Map<String, String> response = new HashMap<>();
        String aiResponse;
        
        if (userMessage.contains("hi") || userMessage.contains("hello") || userMessage.contains("hey")) {
            aiResponse = "### Hello there! 🌟\n\nI am your personalized **AI Study Co-pilot** for the **" + courseName + "** course. " +
                         "I'm here to clarify complex topics, write code snippets, and review core components with you. " +
                         "Ask me anything about this training module!";
        } else if (userMessage.contains("code") || userMessage.contains("example") || userMessage.contains("snippet")) {
            if (courseName.contains("React") || courseName.contains("Frontend") || courseName.contains("web")) {
                aiResponse = "### 💻 React Code Example\n\nHere is a complete example demonstrating the `useEffect` hook to fetch data in a functional component:\n\n" +
                             "```javascript\n" +
                             "import React, { useState, useEffect } from 'react';\n\n" +
                             "function DataFetchComponent() {\n" +
                             "  const [data, setData] = useState([]);\n" +
                             "  const [loading, setLoading] = useState(true);\n\n" +
                             "  useEffect(() => {\n" +
                             "    fetch('https://api.example.com/items')\n" +
                             "      .then(res => res.json())\n" +
                             "      .then(json => {\n" +
                             "        setData(json);\n" +
                             "        setLoading(false);\n" +
                             "      });\n" +
                             "  }, []); // Run once on mount\n\n" +
                             "  if (loading) return <p>Loading data...</p>;\n\n" +
                             "  return (\n" +
                             "    <ul>\n" +
                             "      {data.map(item => (\n" +
                             "        <li key={item.id}>{item.name}</li>\n" +
                             "      ))}\n" +
                             "    </ul>\n" +
                             "  );\n" +
                             "}\n" +
                             "```\n\n- **Rule of Thumb**: Always pass the dependency array `[]` so the effect doesn't run on every single re-render!";
            } else if (courseName.contains("Spring") || courseName.contains("Java") || courseName.contains("Backend")) {
                aiResponse = "### 💻 Spring Boot Rest Controller Example\n\nHere is how you can write a secure Spring REST Controller with request parameters:\n\n" +
                             "```java\n" +
                             "package com.smarthr.backend.controller;\n\n" +
                             "import org.springframework.web.bind.annotation.*;\n" +
                             "import java.util.*;\n\n" +
                             "@RestController\n" +
                             "@RequestMapping(\"/api/v1/users\")\n" +
                             "public class UserController {\n\n" +
                             "    @GetMapping(\"/{id}\")\n" +
                             "    public Map<String, String> getUser(@PathVariable Long id) {\n" +
                             "        Map<String, String> user = new HashMap<>();\n" +
                             "        user.put(\"id\", String.valueOf(id));\n" +
                             "        user.put(\"fullName\", \"Alex Mercer\");\n" +
                             "        user.put(\"status\", \"Active\");\n" +
                             "        return user;\n" +
                             "    }\n" +
                             "}\n" +
                             "```\n\n- **Tip**: Spring automatic marshalling uses Jackson to convert the returned Map into high-performance JSON format.";
            } else if (courseName.contains("Kotlin") || courseName.contains("Android") || courseName.contains("Mobile")) {
                aiResponse = "### 💻 Kotlin Coroutines Example\n\nBelow is a highly reactive asynchronous function structure in Kotlin using Coroutines and Flows:\n\n" +
                             "```kotlin\n" +
                             "import kotlinx.coroutines.flow.*\n" +
                             "import kotlinx.coroutines.*\n\n" +
                             "class MainViewModel : ViewModel() {\n" +
                             "    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)\n" +
                             "    val uiState: StateFlow<UiState> = _uiState\n\n" +
                             "    fun fetchContent() {\n" +
                             "        viewModelScope.launch {\n" +
                             "            try {\n" +
                             "                val data = repository.getData()\n" +
                             "                _uiState.value = UiState.Success(data)\n" +
                             "            } catch (e: Exception) {\n" +
                             "                _uiState.value = UiState.Error(e.message)\n" +
                             "            }\n" +
                             "        }\n" +
                             "    }\n" +
                             "}\n" +
                             "```\n\n- **Safety**: Ensure you're triggering launch configurations inside appropriate lifecycle scopes to bypass dangerous memory leaks.";
            } else {
                aiResponse = "### 💻 Generic Code Sample\n\nHere is a modern ES6 javascript pattern for handling async responses cleanly:\n\n" +
                             "```javascript\n" +
                             "async function executeOperation(payload) {\n" +
                             "  try {\n" +
                             "    const response = await fetch('/api/lms/endpoint', {\n" +
                             "      method: 'POST',\n" +
                             "      headers: { 'Content-Type': 'application/json' },\n" +
                             "      body: JSON.stringify(payload)\n" +
                             "    });\n" +
                             "    if (!response.ok) throw new Error('Operational failure');\n" +
                             "    return await response.json();\n" +
                             "  } catch (err) {\n" +
                             "    console.error('AI Log: Pipeline failed', err);\n" +
                             "    return null;\n" +
                             "  }\n" +
                             "}\n" +
                             "```";
            }
        } else if (userMessage.contains("explain") || userMessage.contains("what is") || userMessage.contains("how to")) {
            aiResponse = "### 💡 Topic Decoded: " + courseName + "\n\nLet's break down your question about this domain. For optimal learning, focus on these critical operational pillars:\n\n" +
                         "1. **Core Philosophy**: Building scalable, robust implementations that bypass runtime errors.\n" +
                         "2. **State Management**: Keeping source of truth isolated and mutable only via formal actions.\n" +
                         "3. **Testing Patterns**: Using comprehensive test cases (unit + integration) to cover edge cases.\n\n" +
                         "> **AI Tip**: When writing software or mastering operations, always trace raw values through your logs to diagnose state leakage early!";
        } else {
            aiResponse = "### 🎓 Study Assistant Insight\n\nThat's a very insightful point regarding the **" + courseName + "** course! Here is a recommended study pattern to master this topic:\n\n" +
                         "- **Implement & Test**: Write out simple tests matching your coursework.\n" +
                         "- **Refactor**: Simplify complex nested expressions into clean modules.\n" +
                         "- **Optimize**: Review runtime performance using CPU logs.\n\n" +
                         "Feel free to ask for specific code templates or explanations on any chapter in the curriculum!";
        }
        
        response.put("response", aiResponse);
        return response;
    }

    @GetMapping("/ai/generate-quiz/{courseId}")
    public List<Map<String, Object>> aiGenerateQuiz(@PathVariable Long courseId) {
        Course course = courseRepo.findById(courseId).orElse(null);
        String courseName = course != null ? course.getCourseName() : "";
        
        List<Map<String, Object>> quiz = new ArrayList<>();
        
        if (courseName.contains("React") || courseName.contains("Frontend") || courseName.contains("web")) {
            quiz.add(createQuestion("Which React hook is used to perform side effects in functional components?", 
                Arrays.asList("useContext", "useMemo", "useEffect", "useReducer"), 2));
            quiz.add(createQuestion("What is the Virtual DOM in React?", 
                Arrays.asList("A direct copy of the HTML DOM for styling", 
                              "A lightweight, in-memory representation of the real DOM", 
                              "A browser tool for inspections", 
                              "An automated responsive layout system"), 1));
            quiz.add(createQuestion("How do you pass data from a parent component to a child component in React?", 
                Arrays.asList("Using localStorage", "Using props", "Using database records", "Using route states"), 1));
        } else if (courseName.contains("Spring") || courseName.contains("Java") || courseName.contains("backend")) {
            quiz.add(createQuestion("Which Spring Boot annotation is used to declare a REST controller?", 
                Arrays.asList("@Controller", "@RestController", "@Service", "@Repository"), 1));
            quiz.add(createQuestion("What is the default port of a standard Spring Boot application?", 
                Arrays.asList("8080", "9090", "3000", "5000"), 0));
            quiz.add(createQuestion("What dependency injection scope is default for a standard Spring Bean?", 
                Arrays.asList("Prototype", "Session", "Singleton", "Request"), 2));
        } else if (courseName.contains("Kotlin") || courseName.contains("Android") || courseName.contains("mobile")) {
            quiz.add(createQuestion("Which keyword is used in Kotlin to declare a read-only variable?", 
                Arrays.asList("var", "val", "const val", "let"), 1));
            quiz.add(createQuestion("What is a Coroutine in Kotlin?", 
                Arrays.asList("A heavy system thread for running background work", 
                              "A lightweight, cooperative design for async work", 
                              "A class for handling Activity lifecycles", 
                              "A reactive compiler optimization"), 1));
            quiz.add(createQuestion("What is the primary layout paradigm of Jetpack Compose?", 
                Arrays.asList("XML Constraints", "Nested Relative Layouts", "Declarative Composable functions", "CSS Flexbox grids"), 2));
        } else {
            quiz.add(createQuestion("What is the primary objective of studying this " + courseName + " module?", 
                Arrays.asList("To ignore best practices and write hacks", 
                              "To acquire standardized skills, industry benchmarks, and operational mastery", 
                              "To increase browser page speeds automatically", 
                              "To replace all human employees with static algorithms"), 1));
            quiz.add(createQuestion("Which phase of learning " + courseName + " focuses on application integration?", 
                Arrays.asList("Initial theoretical overview", 
                              "Practical deployment and error-handling verification", 
                              "Deleting all configurations and files", 
                              "Skipping modules to gain leaderboards"), 1));
            quiz.add(createQuestion("How does AI integration impact modern masteries like " + courseName + "?", 
                Arrays.asList("It has no effect on workflows", 
                              "It acts as a high-powered co-pilot to accelerate efficiency and validation", 
                              "It makes learning completely redundant", 
                              "It deletes database tables automatically"), 1));
        }
        
        return quiz;
    }

    @PostMapping("/ai/submit-quiz/{progressId}")
    public TrainingProgress aiSubmitQuiz(@PathVariable Long progressId, @RequestBody Map<String, Object> payload) {
        TrainingProgress progress = trainingRepo.findById(progressId).orElse(null);
        if (progress != null) {
            Integer score = Integer.valueOf(payload.getOrDefault("score", "0").toString());
            progress.setQuizScore(score);
            if (score >= 100) {
                progress.setAiCertified(true);
                progress.setStatus("Completed");
                progress.setCompletionPercentage(100);
            } else {
                progress.setAiCertified(false);
            }
            return trainingRepo.save(progress);
        }
        return null;
    }

    private Map<String, Object> createQuestion(String question, List<String> options, int correctIdx) {
        Map<String, Object> q = new HashMap<>();
        q.put("question", question);
        q.put("options", options);
        q.put("correctAnswer", correctIdx);
        return q;
    }
}