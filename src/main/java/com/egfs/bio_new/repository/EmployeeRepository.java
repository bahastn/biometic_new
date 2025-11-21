package com.egfs.bio_new.repository;

import com.egfs.bio_new.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    Optional<Employee> findByEmployeeId(String employeeId);
    
    List<Employee> findByActive(Boolean active);
    
    List<Employee> findByDepartment(String department);
}
