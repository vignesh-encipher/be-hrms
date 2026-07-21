package com.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgTreeUpdateRequest {
    private String employeeId;
    private String newManagerId;
    private String newHrApproverId;
}
