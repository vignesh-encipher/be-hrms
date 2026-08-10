package com.hrms.repository;

import com.hrms.entity.WorkCalendarAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkCalendarAuditLogRepository extends MongoRepository<WorkCalendarAuditLog, String> {
    List<WorkCalendarAuditLog> findByEntityTypeAndEntityIdOrderByChangedAtDesc(String entityType, String entityId);
    List<WorkCalendarAuditLog> findAllByOrderByChangedAtDesc();
}
