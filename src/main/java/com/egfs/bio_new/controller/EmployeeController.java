package com.egfs.bio_new.controller;

import com.egfs.bio_new.entity.Employee;
import com.egfs.bio_new.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {
    
    private final EmployeeService employeeService;
    
    @GetMapping
    public String listEmployees(Model model) {
        List<Employee> employees = employeeService.getAllEmployees();
        model.addAttribute("employees", employees);
        model.addAttribute("pageTitle", "Employee Management");
        return "employees/list";
    }
    
    @GetMapping("/new")
    public String showNewEmployeeForm(Model model) {
        model.addAttribute("employee", new Employee());
        model.addAttribute("pageTitle", "Add New Employee");
        return "employees/form";
    }
    
    @PostMapping("/save")
    public String saveEmployee(@ModelAttribute Employee employee, RedirectAttributes redirectAttributes) {
        try {
            employeeService.createEmployee(employee);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Employee added successfully: " + employee.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Error adding employee: " + e.getMessage());
        }
        return "redirect:/employees";
    }
    
    @GetMapping("/edit/{id}")
    public String showEditEmployeeForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return employeeService.getEmployeeById(id)
                .map(employee -> {
                    model.addAttribute("employee", employee);
                    model.addAttribute("pageTitle", "Edit Employee");
                    return "employees/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Employee not found");
                    return "redirect:/employees";
                });
    }
    
    @PostMapping("/update/{id}")
    public String updateEmployee(@PathVariable Long id, @ModelAttribute Employee employee, 
                                 RedirectAttributes redirectAttributes) {
        try {
            employeeService.updateEmployee(id, employee);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Employee updated successfully: " + employee.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Error updating employee: " + e.getMessage());
        }
        return "redirect:/employees";
    }
    
    @GetMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            employeeService.deleteEmployee(id);
            redirectAttributes.addFlashAttribute("successMessage", "Employee deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Error deleting employee: " + e.getMessage());
        }
        return "redirect:/employees";
    }
}
