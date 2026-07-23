package com.hrms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.entity.Employee;
import com.hrms.entity.Message;
import com.hrms.repository.ChannelMemberRepository;
import com.hrms.repository.ChannelRepository;
import com.hrms.repository.EmployeeRepository;
import com.hrms.security.jwt.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    @Autowired
    private ObjectMapper objectMapper;

    // Mapping: userId -> Set of WebSocketSessions
    private final Map<String, java.util.Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    
    // Mapping: sessionId -> userId
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ChannelMemberRepository channelMemberRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        String query = uri.getQuery();
        String token = null;
        if (query != null && query.contains("token=")) {
            token = query.split("token=")[1].split("&")[0];
        }

        if (token == null || !jwtUtils.validateJwtToken(token)) {
            logger.warn("WebSocket connection rejected: invalid JWT token");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        String username = jwtUtils.getUsernameFromJwtToken(token);
        Optional<Employee> employeeOpt = employeeRepository.findByEmail(username);
        if (employeeOpt.isEmpty()) {
            employeeOpt = employeeRepository.findByEmployeeId(username);
        }

        if (employeeOpt.isEmpty()) {
            logger.warn("WebSocket connection rejected: employee not found for username {}", username);
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Employee employee = employeeOpt.get();
        String userId = employee.getId();

        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionUserMap.put(session.getId(), userId);

        logger.info("WebSocket connected: user {} (ID: {})", employee.getFirstName() + " " + employee.getLastName(), userId);

        // Broadcast presence online to all users
        broadcastStatusChange(userId, "online");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String senderId = sessionUserMap.get(session.getId());
        if (senderId == null) return;

        try {
            Map<String, Object> event = objectMapper.readValue(payload, Map.class);
            String type = (String) event.get("type");

            if ("TYPING".equalsIgnoreCase(type)) {
                String conversationId = (String) event.get("conversationId");
                Boolean isTyping = (Boolean) event.get("isTyping");
                
                // Broadcast typing event to receiver or channel members
                broadcastTyping(senderId, conversationId, isTyping);
            } else if ("READ_RECEIPT".equalsIgnoreCase(type)) {
                String conversationId = (String) event.get("conversationId");
                String messageId = (String) event.get("messageId");
                
                broadcastReadReceipt(senderId, conversationId, messageId);
            }
        } catch (Exception e) {
            logger.error("Error handling text message: {}", e.getMessage(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = sessionUserMap.remove(session.getId());
        if (userId != null) {
            java.util.Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                    logger.info("WebSocket disconnected (all sessions closed): user ID {}", userId);
                    // Broadcast presence offline
                    broadcastStatusChange(userId, "offline");
                } else {
                    logger.info("WebSocket closed 1 session for user ID {} ({} remaining active)", userId, sessions.size());
                }
            }
        }
    }

    // Public method to broadcast messaging to a user or channel members
    public void sendMessageToUser(String userId, Object payload) {
        java.util.Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null && !sessions.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(payload);
                TextMessage textMessage = new TextMessage(json);
                sessions.forEach(session -> {
                    if (session.isOpen()) {
                        try {
                            session.sendMessage(textMessage);
                        } catch (IOException e) {
                            logger.error("Error sending message in session {} for user {}: {}", session.getId(), userId, e.getMessage());
                        }
                    }
                });
            } catch (IOException e) {
                logger.error("Serialization error for payload: {}", e.getMessage());
            }
        }
    }

    public void broadcastToChannel(String channelId, Object payload, String excludeUserId) {
        Optional<com.hrms.entity.Channel> channelOpt = channelRepository.findById(channelId);
        if (channelOpt.isPresent() && "PUBLIC".equalsIgnoreCase(channelOpt.get().getType())) {
            logger.info("[WS-BROADCAST] Channel {} is PUBLIC. Broadcasting to all connected users.", channelId);
            userSessions.keySet().forEach(userId -> {
                if (!userId.equals(excludeUserId)) {
                    logger.info("[WS-BROADCAST] Dispatching message payload to user: {}", userId);
                    sendMessageToUser(userId, payload);
                }
            });
        } else {
            java.util.List<com.hrms.entity.ChannelMember> members = channelMemberRepository.findByChannelId(channelId);
            logger.info("[WS-BROADCAST] Channel {} is PRIVATE. Found {} members. Exclude: {}", channelId, members.size(), excludeUserId);
            members.forEach(member -> {
                logger.info("[WS-BROADCAST] Checking member: {} (exclude match: {})", member.getUserId(), member.getUserId().equals(excludeUserId));
                if (!member.getUserId().equals(excludeUserId)) {
                    logger.info("[WS-BROADCAST] Dispatching message payload to user: {}", member.getUserId());
                    sendMessageToUser(member.getUserId(), payload);
                }
            });
        }
    }

    private void broadcastStatusChange(String userId, String status) {
        Map<String, Object> msg = Map.of(
            "type", "PRESENCE",
            "userId", userId,
            "status", status
        );
        userSessions.values().forEach(sessions -> {
            sessions.forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            });
        });
    }

    private void broadcastTyping(String senderId, String conversationId, Boolean isTyping) {
        Map<String, Object> msg = Map.of(
            "type", "TYPING",
            "conversationId", conversationId,
            "senderId", senderId,
            "isTyping", isTyping
        );

        if (conversationId.contains("_")) {
            // Direct message: conversationId is userId1_userId2
            String[] parts = conversationId.split("_");
            String receiverId = parts[0].equals(senderId) ? parts[1] : parts[0];
            sendMessageToUser(receiverId, msg);
        } else {
            // Group message: broadcast to all channel members
            broadcastToChannel(conversationId, msg, senderId);
        }
    }

    private void broadcastReadReceipt(String readerId, String conversationId, String messageId) {
        Map<String, Object> msg = Map.of(
            "type", "READ_RECEIPT",
            "conversationId", conversationId,
            "messageId", messageId,
            "readerId", readerId
        );

        if (conversationId.contains("_")) {
            String[] parts = conversationId.split("_");
            String receiverId = parts[0].equals(readerId) ? parts[1] : parts[0];
            sendMessageToUser(receiverId, msg);
        } else {
            broadcastToChannel(conversationId, msg, readerId);
        }
    }
}
