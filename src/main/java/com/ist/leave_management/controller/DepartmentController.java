package com.ist.leave_management.controller;

import com.ist.leave_management.dto.DepartmentRequestDto;
import com.ist.leave_management.dto.DepartmentResponseDto;
import com.ist.leave_management.model.Department;
import com.ist.leave_management.service.DepartmentService;
import com.ist.user_management.model.User;
import com.ist.user_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DepartmentResponseDto>> getAllDepartments() {
        List<Department> departments = departmentService.getAllDepartments();
        List<DepartmentResponseDto> response = departments.stream()
                .map(DepartmentResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DepartmentResponseDto> getDepartmentById(@PathVariable Long id) {
        return departmentService.getDepartmentById(id)
                .map(DepartmentResponseDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DepartmentResponseDto> createDepartment(@RequestBody DepartmentRequestDto departmentDto) {
        Department department = new Department();
        department.setName(departmentDto.getName());
        department.setDescription(departmentDto.getDescription());

        // Set users if provided
        if (departmentDto.getUserIds() != null && !departmentDto.getUserIds().isEmpty()) {
            List<User> users = userRepository.findAllById(departmentDto.getUserIds());
            department.setUsers(users);
        }

        Department createdDepartment = departmentService.createDepartment(department);
        return ResponseEntity.ok(DepartmentResponseDto.fromEntity(createdDepartment));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DepartmentResponseDto> updateDepartment(@PathVariable Long id,
            @RequestBody DepartmentRequestDto departmentDto) {
        Department department = new Department();
        department.setName(departmentDto.getName());
        department.setDescription(departmentDto.getDescription());

        // Set users if provided
        if (departmentDto.getUserIds() != null) {
            List<User> users = userRepository.findAllById(departmentDto.getUserIds());
            department.setUsers(users);
        }

        Department updatedDepartment = departmentService.updateDepartment(id, department);
        return ResponseEntity.ok(DepartmentResponseDto.fromEntity(updatedDepartment));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok().build();
    }
}