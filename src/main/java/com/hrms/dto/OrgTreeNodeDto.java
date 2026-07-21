package com.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgTreeNodeDto {
    private String id;
    private String key;
    private String title;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String name;
    private String email;
    private String phone;
    private String designation;
    private String department;
    private String managerId;
    private String managerName;
    private String hrApproverId;
    private String hrApproverName;
    private String status;
    private String photo;
    private String joiningDate;
    private Double salary;
    private int directReportsCount;

    @Builder.Default
    private List<OrgTreeNodeDto> children = new ArrayList<>();
}
