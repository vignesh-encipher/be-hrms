package com.hrms.service;

import com.hrms.entity.Ticket;

import java.util.List;

public interface TicketService {
    Ticket raiseTicket(Ticket ticket);
    List<Ticket> getAllTickets();
    Ticket getTicketById(String id);
    Ticket assignTicket(String id);
    Ticket replyToTicket(String id, String message);
    Ticket resolveTicket(String id);
}
