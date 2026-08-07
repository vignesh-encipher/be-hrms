package com.hrms.controller;

import com.hrms.entity.Ticket;
import com.hrms.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping("/raise")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE') or hasRole('FINANCE') or hasRole('IT_ADMIN')")
    public ResponseEntity<Ticket> raiseTicket(@RequestBody Ticket ticket) {
        return ResponseEntity.ok(ticketService.raiseTicket(ticket));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE') or hasRole('FINANCE') or hasRole('IT_ADMIN')")
    public ResponseEntity<List<Ticket>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE') or hasRole('FINANCE') or hasRole('IT_ADMIN')")
    public ResponseEntity<Ticket> getTicketById(@PathVariable String id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @PostMapping("/assign/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE') or hasRole('IT_ADMIN')")
    public ResponseEntity<Ticket> assignTicket(@PathVariable String id) {
        return ResponseEntity.ok(ticketService.assignTicket(id));
    }

    @PostMapping("/reply/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE') or hasRole('FINANCE') or hasRole('IT_ADMIN')")
    public ResponseEntity<Ticket> replyToTicket(@PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ticketService.replyToTicket(id, body.get("message")));
    }

    @PostMapping("/resolve/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE') or hasRole('IT_ADMIN')")
    public ResponseEntity<Ticket> resolveTicket(@PathVariable String id) {
        return ResponseEntity.ok(ticketService.resolveTicket(id));
    }
}
