package com.ist.leave_management.dto;

import com.ist.leave_management.model.LeaveWorkflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LeaveWorkflowMapper {
    @Autowired
    private LeaveApplicationMapper leaveApplicationMapper;

    public LeaveWorkflowDto toDto(LeaveWorkflow workflow) {
        LeaveWorkflowDto dto = new LeaveWorkflowDto();
        dto.setId(workflow.getId());
        dto.setLeaveApplication(leaveApplicationMapper.toDto(workflow.getLeaveApplication()));

        if (workflow.getApprovedBy() != null) {
            dto.setApprovedById(workflow.getApprovedBy().getId());
            dto.setApprovedByName(
                    workflow.getApprovedBy().getFirstName() + " " + workflow.getApprovedBy().getLastName());
        }

        if (workflow.getRejectedBy() != null) {
            dto.setRejectedById(workflow.getRejectedBy().getId());
            dto.setRejectedByName(
                    workflow.getRejectedBy().getFirstName() + " " + workflow.getRejectedBy().getLastName());
        }

        dto.setApprovedDate(workflow.getApprovedDate());
        dto.setRejectedDate(workflow.getRejectedDate());
        dto.setComments(workflow.getComments());
        dto.setStatus(workflow.getStatus());
        dto.setCreatedAt(workflow.getCreatedAt());
        dto.setUpdatedAt(workflow.getUpdatedAt());

        return dto;
    }
}