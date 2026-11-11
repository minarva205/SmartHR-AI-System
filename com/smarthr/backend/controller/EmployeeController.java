package com.smarthr.backend.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smarthr.backend.model.Employee;
import com.smarthr.backend.repository.EmployeeRepository;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "*")
public class EmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    // 1. Get all employees (Dono roles ke liye)
    @GetMapping("/all")
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    // 1.5 Get current employee based on logged in user's email
    @GetMapping("/me")
    public ResponseEntity<Employee> getCurrentEmployee(jakarta.servlet.http.HttpSession session) {
        com.smarthr.backend.model.User user = (com.smarthr.backend.model.User) session.getAttribute("loggedInUser");
        if (user != null) {
            Employee emp = employeeRepository.findByEmail(user.getEmail()).orElse(null);
            if (emp != null) {
                return ResponseEntity.ok(emp);
            }
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(401).build();
    }

    // 2. Create Employee (Sirf Admin ke liye)
    @PostMapping("/create")
    public Employee createEmployee(@RequestBody Employee employee) {
        return employeeRepository.save(employee);
    }

    // 3. Get Employee by ID
    @GetMapping("/{id}")
    public Employee getEmployeeById(@PathVariable Long id) {
        return employeeRepository.findById(id).orElse(null);
    }

    // 4. Update Employee Details (Sirf Admin ke liye)
    @PutMapping("/update/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id, @RequestBody Employee updatedDetails) {
        return employeeRepository.findById(id).map(employee -> {
            employee.setFullName(updatedDetails.getFullName());
            employee.setEmail(updatedDetails.getEmail());
            employee.setDesignation(updatedDetails.getDesignation());
            employee.setDepartment(updatedDetails.getDepartment());
            if (updatedDetails.getBaseSalary() != null) {
                employee.setBaseSalary(updatedDetails.getBaseSalary());
            }
            Employee saved = employeeRepository.save(employee);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    // 5. Delete Employee Profile (Sirf Admin ke liye)
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        if (employeeRepository.existsById(id)) {
            employeeRepository.deleteById(id);
            return ResponseEntity.ok().body("Employee profile deleted successfully.");
        }
        return ResponseEntity.notFound().build();
    }

    // 6. Profile Pic Upload
    @PostMapping("/upload-profile-pic")
    public String uploadProfilePic(
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (!file.isEmpty()) {
            try {
                String uploadDir = "src/main/resources/static/uploads/";
                java.io.File dir = new java.io.File(uploadDir);
                if (!dir.exists())
                    dir.mkdirs();
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                file.transferTo(new java.io.File(dir.getAbsolutePath() + "/" + fileName));
                return "/uploads/" + fileName;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}