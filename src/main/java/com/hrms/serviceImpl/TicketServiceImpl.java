package com.hrms.serviceImpl;

import com.hrms.entity.Employee;
import com.hrms.entity.Ticket;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.TicketRepository;
import com.hrms.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TicketServiceImpl implements TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Employee getCurrentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        String username = authentication.getName();
        return employeeRepository.findByEmail(username)
                .or(() -> employeeRepository.findByEmployeeId(username))
                .orElse(null);
    }

    private int resolveSlaHours(String priority) {
        if (priority == null) {
            return 24;
        }
        switch (priority) {
            case "High":
                return 8;
            case "Medium":
                return 24;
            case "Low":
                return 48;
            default:
                return 24;
        }
    }

    @Override
    public Ticket raiseTicket(Ticket ticket) {
        if (ticket.getSubject() == null || ticket.getSubject().isEmpty()) {
            throw new BadRequestException("Subject is required");
        }
        if (ticket.getCategory() == null || ticket.getCategory().isEmpty()) {
            throw new BadRequestException("Category is required");
        }

        Employee currentEmployee = getCurrentEmployee();
        if (currentEmployee != null) {
            ticket.setRaisedByEmployeeId(currentEmployee.getEmployeeId());
            ticket.setRaisedByName(currentEmployee.getFirstName() + " " + currentEmployee.getLastName());
        }

        if (ticket.getPriority() == null || ticket.getPriority().isEmpty()) {
            ticket.setPriority("Medium");
        }
        ticket.setSlaHours(resolveSlaHours(ticket.getPriority()));
        ticket.setStatus("Open");
        ticket.setCreatedAt(LocalDateTime.now());
        if (ticket.getReplies() == null) {
            ticket.setReplies(new ArrayList<>());
        }
        return ticketRepository.save(ticket);
    }

    @Override
    public List<Ticket> getAllTickets() {
        Employee currentEmployee = getCurrentEmployee();
        List<Ticket> all = ticketRepository.findAll();
        all.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        if (currentEmployee == null || currentEmployee.getRoles() == null) {
            return Collections.emptyList();
        }

        boolean isSuperAdmin = currentEmployee.getRoles().contains(com.hrms.entity.ERole.ROLE_SUPER_ADMIN);
        if (isSuperAdmin) {
            return all;
        }

        boolean isHR = currentEmployee.getRoles().contains(com.hrms.entity.ERole.ROLE_HR);
        boolean isFinance = currentEmployee.getRoles().contains(com.hrms.entity.ERole.ROLE_FINANCE);
        boolean isItAdmin = currentEmployee.getRoles().contains(com.hrms.entity.ERole.ROLE_IT_ADMIN);

        String empId = currentEmployee.getEmployeeId();

        if (isHR || isFinance || isItAdmin) {
            String category = isHR ? "HR" : isFinance ? "Finance" : "IT";
            return all.stream()
                    .filter(t -> category.equalsIgnoreCase(t.getCategory()) || empId.equals(t.getAssignedToEmployeeId()))
                    .toList();
        }

        // Employee / Manager - only see their own raised tickets
        return all.stream()
                .filter(t -> empId.equals(t.getRaisedByEmployeeId()))
                .toList();
    }

    @Override
    public Ticket getTicketById(String id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
    }

    @Override
    public Ticket assignTicket(String id) {
        Ticket ticket = getTicketById(id);
        Employee currentEmployee = getCurrentEmployee();
        if (currentEmployee == null) {
            throw new BadRequestException("Unable to determine current user for assignment");
        }
        ticket.setAssignedToEmployeeId(currentEmployee.getEmployeeId());
        ticket.setAssignedToName(currentEmployee.getFirstName() + " " + currentEmployee.getLastName());
        if (!"Resolved".equalsIgnoreCase(ticket.getStatus())) {
            ticket.setStatus("In progress");
        }
        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket replyToTicket(String id, String message) {
        if (message == null || message.isEmpty()) {
            throw new BadRequestException("Reply message cannot be empty");
        }
        Ticket ticket = getTicketById(id);
        Employee currentEmployee = getCurrentEmployee();

        Ticket.TicketReply reply = Ticket.TicketReply.builder()
                .authorId(currentEmployee != null ? currentEmployee.getEmployeeId() : "Unknown")
                .authorName(currentEmployee != null ? currentEmployee.getFirstName() + " " + currentEmployee.getLastName() : "Unknown")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        if (ticket.getReplies() == null) {
            ticket.setReplies(new ArrayList<>());
        }
        ticket.getReplies().add(reply);

        if ("Open".equalsIgnoreCase(ticket.getStatus())) {
            ticket.setStatus("In progress");
        }

        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket resolveTicket(String id) {
        Ticket ticket = getTicketById(id);
        ticket.setStatus("Resolved");
        ticket.setResolvedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }
}
