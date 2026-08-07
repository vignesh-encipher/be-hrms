package com.hrms.repository;

import com.hrms.entity.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends MongoRepository<Ticket, String> {
    List<Ticket> findByRaisedByEmployeeId(String raisedByEmployeeId);
    List<Ticket> findByCategory(String category);
    List<Ticket> findByAssignedToEmployeeId(String assignedToEmployeeId);
    List<Ticket> findByStatus(String status);
}
