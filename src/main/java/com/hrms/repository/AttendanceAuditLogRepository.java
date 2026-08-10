package com.hrms.repository;

import com.hrms.entity.AttendanceAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceAuditLogRepository extends MongoRepository<AttendanceAuditLog, String> {
    List<AttendanceAuditLog> findByEmployeeId(String employeeId);
}
