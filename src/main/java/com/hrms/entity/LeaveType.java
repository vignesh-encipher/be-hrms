package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "leave_types")
public class LeaveType {
    @Id
    private String id;
    
    private String name;               // e.g. Casual Leave (CL)
    private String code;               // e.g. CL, SL, EL, MAT, PAT, MAR, BER, OPH, WFH, LOP
    private Double totalDays;          // e.g. 12.0
    private Boolean monthlyAccrual;    // true / false
    private Boolean carryForwardAllowed; // true / false
    private Double maxCarryForwardDays;// e.g. 5.0
    private Boolean encashmentAllowed; // true / false
    private Double maxPerRequest;      // e.g. 3.0
    private Integer validityDays;      // e.g. 365
    private Boolean active;            // true / false
    private Boolean readOnly;          // true for LOP
}
