package com.smarthr.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smarthr.backend.model.Payroll;
import com.smarthr.backend.repository.PayrollRepository;

@RestController
@RequestMapping("/api/payroll")
@CrossOrigin(origins = "*")
public class PayrollController {

    @Autowired
    private PayrollRepository payrollRepository;

    @GetMapping("/all")
    public List<Payroll> getAllPayrolls() {
        return payrollRepository.findAll();
    }

    // STRICT FIX: Id-wise filter endpoint jo employee portal me sirf usi ka data
    // dikhayega
    @GetMapping("/employee/{employeeId}")
    public List<Payroll> getPayrollByEmployeeId(@PathVariable Long employeeId) {
        return payrollRepository.findByEmployeeId(employeeId);
    }

    @PostMapping("/create")
    public Payroll createPayroll(@RequestBody Payroll payroll) {
        return payrollRepository.save(payroll);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/delete/{id}")
    public void deletePayroll(@org.springframework.web.bind.annotation.PathVariable Long id) {
        payrollRepository.deleteById(id);
    }
}