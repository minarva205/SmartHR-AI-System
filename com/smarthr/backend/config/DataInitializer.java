package com.smarthr.backend.config;

import com.smarthr.backend.model.Course;
import com.smarthr.backend.model.Policy;
import com.smarthr.backend.repository.CourseRepository;
import com.smarthr.backend.repository.PolicyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initCourses(CourseRepository courseRepository) {
        return args -> {
            if (courseRepository.count() == 0) {
                List<Course> defaultCourses = List.of(
                        createCourse("Core Java Masterclass",
                                "Learn Core Java from scratch, including OOPs, Collections, and Threads.",
                                "Programming", "40 hours"),
                        createCourse("Advanced Python Programming",
                                "Master Python, Django, Data Science and Machine Learning basics.", "Programming",
                                "35 hours"),
                        createCourse("Hibernate & JPA",
                                "Deep dive into Hibernate ORM, mapping, caching, and performance.", "Database/ORM",
                                "20 hours"),
                        createCourse("Spring Boot for Beginners",
                                "Build modern microservices and REST APIs with Spring Boot.", "Framework", "25 hours"),
                        createCourse("React JS Front to Back", "Modern React, Hooks, Context API, and Redux.",
                                "Frontend", "30 hours"),
                        createCourse("Angular & TypeScript", "Complete Angular developer course with TypeScript.",
                                "Frontend", "35 hours"),
                        createCourse("Node.js, Express & MongoDB", "Fullstack backend development with JavaScript.",
                                "Backend", "28 hours"),
                        createCourse("AWS Certified Solutions Architect",
                                "Prepare for the AWS cert with real world examples.", "Cloud", "45 hours"),
                        createCourse("Docker & Kubernetes", "Master containerization and orchestration.", "DevOps",
                                "25 hours"),
                        createCourse("Machine Learning A-Z",
                                "Learn algorithms, Pandas, Scikit-Learn, and Neural Networks.", "AI/ML", "50 hours"),
                        createCourse("C++ for Competitive Programming", "Master C++ concepts and data structures.",
                                "Programming", "30 hours"),
                        createCourse("C# & .NET Core", "Build enterprise applications with C# and .NET.", "Backend",
                                "40 hours"),
                        createCourse("SQL Bootcamp", "Master SQL database design, queries, and optimization.",
                                "Database", "15 hours"),
                        createCourse("Go Programming (Golang)", "Learn Google's Go language for fast backend services.",
                                "Programming", "20 hours"));
                courseRepository.saveAll(defaultCourses);
            }
        };
    }

    @Bean
    CommandLineRunner initPolicies(PolicyRepository policyRepository) {
        return args -> {
            if (policyRepository.count() == 0) {
                List<Policy> defaultPolicies = List.of(
                        createPolicy("Employee Leave Guidelines",
                                "Guidelines regarding annual, medical, casual and parental leave allowances and submission procedures.",
                                "Leave", "2026-01-01", "https://example.com/docs/leave-policy.pdf"),
                        createPolicy("Information Security & 2FA Policy",
                                "Rules regarding data privacy, password configuration, clean desk practices, and mandatory 2FA compliance.",
                                "Security", "2026-01-01", "https://example.com/docs/security-policy.pdf"),
                        createPolicy("Professional Code of Conduct",
                                "Expected standards of behavior, anti-harassment guidelines, and professional integrity criteria in the office.",
                                "Conduct", "2026-01-01", "https://example.com/docs/conduct-policy.pdf"),
                        createPolicy("Travel & Expense Reimbursement Policy",
                                "Process and policies for logging travel, meals, lodging allowances, and claiming business expenses.",
                                "Finance", "2026-01-01", "https://example.com/docs/travel-policy.pdf")
                );
                policyRepository.saveAll(defaultPolicies);
            }
        };
    }

    private Course createCourse(String name, String description, String category, String duration) {
        Course course = new Course();
        course.setCourseName(name);
        course.setDescription(description);
        course.setCategory(category);
        course.setDuration(duration);
        return course;
    }

    private Policy createPolicy(String title, String description, String category, String effectiveDate, String documentUrl) {
        Policy policy = new Policy();
        policy.setTitle(title);
        policy.setDescription(description);
        policy.setCategory(category);
        policy.setEffectiveDate(effectiveDate);
        policy.setDocumentUrl(documentUrl);
        return policy;
    }
}
