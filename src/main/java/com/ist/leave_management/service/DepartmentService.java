package com.ist.leave_management.service;

import com.ist.leave_management.model.Department;
import com.ist.leave_management.repository.DepartmentRepository;
import com.ist.user_management.model.User;
import com.ist.user_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Optional<Department> getDepartmentById(Long id) {
        return departmentRepository.findById(id);
    }

    public Department createDepartment(Department department) {
        if (departmentRepository.existsByName(department.getName())) {
            throw new IllegalArgumentException("Department with name " + department.getName() + " already exists");
        }

        // Save the department first
        Department savedDepartment = departmentRepository.save(department);

        // If there are users, update their department
        if (department.getUsers() != null && !department.getUsers().isEmpty()) {
            for (User user : department.getUsers()) {
                User existingUser = userRepository.findById(user.getId())
                        .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + user.getId()));
                existingUser.setDepartment(savedDepartment);
                userRepository.save(existingUser);
            }
        }

        return savedDepartment;
    }

    public Department updateDepartment(Long id, Department departmentDetails) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found with id: " + id));

        if (!department.getName().equals(departmentDetails.getName()) &&
                departmentRepository.existsByName(departmentDetails.getName())) {
            throw new IllegalArgumentException(
                    "Department with name " + departmentDetails.getName() + " already exists");
        }

        // Update basic department info
        department.setName(departmentDetails.getName());
        department.setDescription(departmentDetails.getDescription());

        // Handle user updates if provided
        if (departmentDetails.getUsers() != null) {
            // Remove department from users not in the new list
            List<User> currentUsers = userRepository.findByDepartment(department);
            for (User user : currentUsers) {
                if (!departmentDetails.getUsers().contains(user)) {
                    user.setDepartment(null);
                    userRepository.save(user);
                }
            }

            // Add department to new users
            for (User user : departmentDetails.getUsers()) {
                User existingUser = userRepository.findById(user.getId())
                        .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + user.getId()));
                existingUser.setDepartment(department);
                userRepository.save(existingUser);
            }
        }

        return departmentRepository.save(department);
    }

    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found with id: " + id));

        // Remove department from all users
        List<User> users = userRepository.findByDepartment(department);
        for (User user : users) {
            user.setDepartment(null);
            userRepository.save(user);
        }

        departmentRepository.delete(department);
    }
}