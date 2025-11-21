package com.egfs.bio_new.service;

import com.egfs.bio_new.entity.Employee;
import com.egfs.bio_new.exception.EmployeeNotFoundException;
import com.egfs.bio_new.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {
    
    private final EmployeeRepository employeeRepository;
    
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }
    
    public List<Employee> getActiveEmployees() {
        return employeeRepository.findByActive(true);
    }
    
    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }
    
    public Optional<Employee> getEmployeeByEmployeeId(String employeeId) {
        return employeeRepository.findByEmployeeId(employeeId);
    }
    
    @Transactional
    public Employee saveEmployee(Employee employee) {
        log.info("Saving employee: {}", employee.getEmployeeId());
        return employeeRepository.save(employee);
    }
    
    @Transactional
    public Employee createEmployee(Employee employee) {
        log.info("Creating new employee: {}", employee.getEmployeeId());
        return employeeRepository.save(employee);
    }
    
    @Transactional
    public Employee updateEmployee(Long id, Employee updatedEmployee) {
        return employeeRepository.findById(id)
                .map(employee -> {
                    employee.setName(updatedEmployee.getName());
                    employee.setDepartment(updatedEmployee.getDepartment());
                    employee.setDesignation(updatedEmployee.getDesignation());
                    employee.setCardNumber(updatedEmployee.getCardNumber());
                    employee.setActive(updatedEmployee.getActive());
                    log.info("Updated employee: {}", employee.getEmployeeId());
                    return employeeRepository.save(employee);
                })
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }
    
    @Transactional
    public void deleteEmployee(Long id) {
        employeeRepository.deleteById(id);
        log.info("Deleted employee with id: {}", id);
    }
}
