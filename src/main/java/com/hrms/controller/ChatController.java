package com.hrms.controller;

import com.hrms.config.ChatWebSocketHandler;
import com.hrms.dto.MessagePageResponse;
import com.hrms.entity.Channel;
import com.hrms.entity.ChannelMember;
import com.hrms.entity.ConversationReadState;
import com.hrms.entity.Employee;
import com.hrms.entity.Message;
import com.hrms.repository.ChannelMemberRepository;
import com.hrms.repository.ChannelRepository;
import com.hrms.repository.ConversationReadStateRepository;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.MessageRepository;
import com.hrms.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ChannelMemberRepository channelMemberRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private ConversationReadStateRepository readStateRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final int INITIAL_READ_HISTORY_SIZE = 70;
    private static final int OLDER_PAGE_SIZE = 50;
    private static final int UNREAD_SAFETY_CAP = 500;
    private static final int READ_RECEIPT_BULK_LIMIT = 200;
    // Sentinel "beginning of time" for a conversation with no read history at all - not
    // LocalDateTime.MIN, which Spring Data Mongo's LocalDateTime->Date converter cannot
    // represent (year -999999999 overflows java.util.Date's range) and throws on.
    private static final LocalDateTime NEVER_READ = LocalDateTime.of(1970, 1, 1, 0, 0);

    private UserDetailsImpl getCurrentUser(Authentication authentication) {
        return (UserDetailsImpl) authentication.getPrincipal();
    }

    private Criteria notDeletedForUser(String userId) {
        return new Criteria().orOperator(
                Criteria.where("deletedForUsers").exists(false),
                Criteria.where("deletedForUsers").is(null),
                Criteria.where("deletedForUsers").ne(userId)
        );
    }

    private Message findLatestMessage(String conversationId) {
        Query query = Query.query(Criteria.where("conversationId").is(conversationId))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .limit(1);
        return mongoTemplate.findOne(query, Message.class);
    }

    /**
     * The per-user read cursor for a conversation. Falls back to a one-time derivation
     * from the legacy per-message readBy set when no cursor exists yet (pre-migration
     * conversations), so existing history doesn't suddenly appear unread; a real cursor
     * gets created the next time markConversationAsRead runs. A brand-new conversation
     * with no read history at all correctly resolves to "everything is unread".
     */
    private LocalDateTime resolveLastReadAt(String conversationId, String userId) {
        Optional<ConversationReadState> state = readStateRepository.findByConversationIdAndUserId(conversationId, userId);
        if (state.isPresent()) {
            return state.get().getLastReadAt();
        }
        Query legacyQuery = Query.query(Criteria.where("conversationId").is(conversationId).and("readBy").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .limit(1);
        Message lastRead = mongoTemplate.findOne(legacyQuery, Message.class);
        return lastRead != null ? lastRead.getCreatedAt() : NEVER_READ;
    }

    private long countUnread(String conversationId, LocalDateTime since, String userId) {
        Query query = Query.query(Criteria.where("conversationId").is(conversationId)
                .and("createdAt").gt(since)
                .andOperator(notDeletedForUser(userId)));
        return mongoTemplate.count(query, Message.class);
    }

    /** O(1) upsert of a user's read cursor - called both explicitly (mark-as-read) and
     * implicitly (sending a message means you've necessarily seen everything up to it). */
    private void advanceReadCursor(String conversationId, String userId) {
        ConversationReadState state = readStateRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseGet(() -> ConversationReadState.builder()
                        .conversationId(conversationId)
                        .userId(userId)
                        .build());
        state.setLastReadAt(LocalDateTime.now());
        readStateRepository.save(state);
    }

    // Channels CRUD
    @GetMapping("/channels")
    public ResponseEntity<List<Map<String, Object>>> getChannels(Authentication authentication) {
        String userId = getCurrentUser(authentication).getId();
        List<Channel> allChannels = channelRepository.findAll();
        List<ChannelMember> userMemberships = channelMemberRepository.findByUserId(userId);
        Set<String> memberChannelIds = userMemberships.stream()
                .map(ChannelMember::getChannelId)
                .collect(Collectors.toSet());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Channel channel : allChannels) {
            boolean isMember = memberChannelIds.contains(channel.getId());
            // User can see public channels or channels they are a member of
            if ("PUBLIC".equalsIgnoreCase(channel.getType()) || isMember) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", channel.getId());
                map.put("name", channel.getName());
                map.put("description", channel.getDescription());
                map.put("type", channel.getType());
                map.put("createdBy", channel.getCreatedBy());
                map.put("createdAt", channel.getCreatedAt());
                map.put("avatar", channel.getAvatar());
                map.put("isMember", isMember);
                
                long memberCount = channelMemberRepository.findByChannelId(channel.getId()).size();
                map.put("memberCount", memberCount);

                Message latestMsg = findLatestMessage(channel.getId());
                if (latestMsg != null) {
                    map.put("lastMessage", latestMsg.getMessage());
                    map.put("lastMessageTime", latestMsg.getCreatedAt());
                } else {
                    map.put("lastMessage", "");
                    map.put("lastMessageTime", channel.getCreatedAt());
                }

                LocalDateTime lastReadAt = resolveLastReadAt(channel.getId(), userId);
                long unreadCount = countUnread(channel.getId(), lastReadAt, userId);
                map.put("unreadCount", unreadCount);

                result.add(map);
            }
        }

        result.sort((c1, c2) -> {
            LocalDateTime t1 = (LocalDateTime) c1.get("lastMessageTime");
            LocalDateTime t2 = (LocalDateTime) c2.get("lastMessageTime");
            return t2.compareTo(t1);
        });

        return ResponseEntity.ok(result);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<Map<String, Object>>> getActiveConversations(Authentication authentication) {
        String currentUserId = getCurrentUser(authentication).getId();

        // Only the distinct conversation ids are needed here, not every message in them -
        // avoids loading every DM message this user has ever sent/received just to list threads.
        Query distinctQuery = Query.query(Criteria.where("conversationId").regex(Pattern.quote(currentUserId)));
        List<String> conversationIds = mongoTemplate.findDistinct(distinctQuery, "conversationId", Message.class, String.class)
                .stream()
                .filter(id -> id != null && id.contains("_"))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();

        for (String convId : conversationIds) {
            Message latestMsg = findLatestMessage(convId);
            if (latestMsg == null) continue;

            String[] parts = convId.split("_");
            if (parts.length < 2) continue;
            String otherUserId = parts[0].equals(currentUserId) ? parts[1] : parts[0];

            Optional<Employee> otherUserOpt = employeeRepository.findById(otherUserId);
            if (otherUserOpt.isPresent()) {
                LocalDateTime lastReadAt = resolveLastReadAt(convId, currentUserId);
                long unreadCount = countUnread(convId, lastReadAt, currentUserId);

                Employee otherUser = otherUserOpt.get();
                Map<String, Object> map = new HashMap<>();
                map.put("conversationId", convId);
                map.put("otherUserId", otherUserId);
                map.put("name", otherUser.getFirstName() + " " + otherUser.getLastName());
                map.put("avatar", otherUser.getPhoto());
                map.put("status", otherUser.getStatus());
                map.put("lastMessage", latestMsg.getMessage());
                map.put("lastMessageTime", latestMsg.getCreatedAt());
                map.put("unreadCount", unreadCount);
                result.add(map);
            }
        }

        result.sort((c1, c2) -> ((LocalDateTime) c2.get("lastMessageTime")).compareTo((LocalDateTime) c1.get("lastMessageTime")));

        return ResponseEntity.ok(result);
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<?> markConversationAsRead(@PathVariable String conversationId, Authentication authentication) {
        String currentUserId = getCurrentUser(authentication).getId();
        advanceReadCursor(conversationId, currentUserId);

        // Bounded bulk update for the per-message read-receipt (double-checkmark) UI only -
        // the unread badge itself now comes entirely from the cursor above, not this.
        Query recentUnread = Query.query(Criteria.where("conversationId").is(conversationId)
                .and("senderId").ne(currentUserId)
                .and("readBy").ne(currentUserId))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .limit(READ_RECEIPT_BULK_LIMIT);
        List<Message> toMark = mongoTemplate.find(recentUnread, Message.class);
        boolean updated = !toMark.isEmpty();
        if (updated) {
            List<String> ids = toMark.stream().map(Message::getId).collect(Collectors.toList());
            mongoTemplate.updateMulti(
                    Query.query(Criteria.where("id").in(ids)),
                    new Update().addToSet("readBy", currentUserId),
                    Message.class
            );
        }

        if (updated) {
            Map<String, Object> wsMsg = Map.of(
                "type", "CONVERSATION_READ",
                "conversationId", conversationId,
                "readerId", currentUserId
            );
            if (conversationId.contains("_")) {
                String[] parts = conversationId.split("_");
                String otherUserId = parts[0].equals(currentUserId) ? parts[1] : parts[0];
                chatWebSocketHandler.sendMessageToUser(otherUserId, wsMsg);
                chatWebSocketHandler.sendMessageToUser(currentUserId, wsMsg); // Echo to current user sessions/tabs
            } else {
                chatWebSocketHandler.broadcastToChannel(conversationId, wsMsg, null);
            }
        }
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/channels")
    public ResponseEntity<Channel> createChannel(@RequestBody Channel channel, Authentication authentication) {
        String userId = getCurrentUser(authentication).getId();
        channel.setCreatedBy(userId);
        channel.setCreatedAt(LocalDateTime.now());
        Channel saved = channelRepository.save(channel);

        // Creator automatically joins as ADMIN
        ChannelMember member = ChannelMember.builder()
                .channelId(saved.getId())
                .userId(userId)
                .role("ADMIN")
                .joinedAt(LocalDateTime.now())
                .build();
        channelMemberRepository.save(member);

        // Add initial members if provided
        if (channel.getInitialMembers() != null) {
            for (String memberId : channel.getInitialMembers()) {
                if (!memberId.equals(userId) && !channelMemberRepository.existsByChannelIdAndUserId(saved.getId(), memberId)) {
                    ChannelMember extraMember = ChannelMember.builder()
                            .channelId(saved.getId())
                            .userId(memberId)
                            .role("MEMBER")
                            .joinedAt(LocalDateTime.now())
                            .build();
                    channelMemberRepository.save(extraMember);
                }
            }
        }

        // Notify all channel members via WebSocket that a new channel was created
        Map<String, Object> wsMsg = Map.of(
            "type", "CHANNEL_CREATED",
            "channelId", saved.getId()
        );
        chatWebSocketHandler.broadcastToChannel(saved.getId(), wsMsg, null);
        chatWebSocketHandler.sendMessageToUser(userId, wsMsg);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/channels/{id}")
    public ResponseEntity<?> updateChannel(@PathVariable String id, @RequestBody Channel channelDetails, Authentication authentication) {
        String userId = getCurrentUser(authentication).getId();
        Optional<Channel> channelOpt = channelRepository.findById(id);
        if (channelOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Verify user is member/admin of this channel
        Optional<ChannelMember> membership = channelMemberRepository.findByChannelIdAndUserId(id, userId);
        if (membership.isEmpty() && !"PUBLIC".equalsIgnoreCase(channelOpt.get().getType())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        Channel channel = channelOpt.get();
        channel.setName(channelDetails.getName());
        channel.setDescription(channelDetails.getDescription());
        channel.setType(channelDetails.getType());
        if (channelDetails.getAvatar() != null) {
            channel.setAvatar(channelDetails.getAvatar());
        }

        Channel updated = channelRepository.save(channel);

        // Notify all channel members via WebSocket that the channel was updated
        Map<String, Object> wsMsg = Map.of(
            "type", "CHANNEL_UPDATED",
            "channelId", updated.getId(),
            "name", updated.getName(),
            "description", updated.getDescription() == null ? "" : updated.getDescription(),
            "channelType", updated.getType()
        );
        chatWebSocketHandler.broadcastToChannel(updated.getId(), wsMsg, null);
        chatWebSocketHandler.sendMessageToUser(userId, wsMsg);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/channels/{id}")
    public ResponseEntity<?> deleteChannel(@PathVariable String id, Authentication authentication) {
        String userId = getCurrentUser(authentication).getId();
        Optional<Channel> channelOpt = channelRepository.findById(id);
        if (channelOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Only ADMIN of the channel or SUPER_ADMIN/HR can delete
        Optional<ChannelMember> membership = channelMemberRepository.findByChannelIdAndUserId(id, userId);
        boolean isCreatorOrAdmin = membership.isPresent() && "ADMIN".equalsIgnoreCase(membership.get().getRole());
        boolean hasPrivilege = getCurrentUser(authentication).getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN") || a.getAuthority().equals("ROLE_HR"));

        if (!isCreatorOrAdmin && !hasPrivilege) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only channel admins can delete this channel");
        }

        channelRepository.deleteById(id);
        channelMemberRepository.deleteByChannelId(id);
        // Delete all messages associated with the channel
        Pageable unpaged = Pageable.unpaged();
        Page<Message> messages = messageRepository.findByConversationId(id, unpaged);
        messageRepository.deleteAll(messages.getContent());

        return ResponseEntity.noContent().build();
    }

    // Channel Member Management
    @GetMapping("/channels/{id}/members")
    public ResponseEntity<?> getChannelMembers(@PathVariable String id, Authentication authentication) {
        String userId = getCurrentUser(authentication).getId();
        Optional<ChannelMember> membership = channelMemberRepository.findByChannelIdAndUserId(id, userId);
        Optional<Channel> channelOpt = channelRepository.findById(id);
        
        if (channelOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (membership.isEmpty() && !"PUBLIC".equalsIgnoreCase(channelOpt.get().getType())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        List<ChannelMember> members = channelMemberRepository.findByChannelId(id);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChannelMember cm : members) {
            Optional<Employee> empOpt = employeeRepository.findById(cm.getUserId());
            if (empOpt.isPresent()) {
                Employee emp = empOpt.get();
                Map<String, Object> map = new HashMap<>();
                map.put("userId", emp.getId());
                map.put("name", emp.getFirstName() + " " + emp.getLastName());
                map.put("email", emp.getEmail());
                map.put("role", cm.getRole());
                map.put("joinedAt", cm.getJoinedAt());
                map.put("employeeId", emp.getEmployeeId());
                map.put("photo", emp.getPhoto());
                map.put("status", emp.getStatus());
                result.add(map);
            }
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/channels/{id}/members")
    public ResponseEntity<?> addMemberToChannel(@PathVariable String id, @RequestBody Map<String, String> payload, Authentication authentication) {
        String targetUserId = payload.get("userId");
        if (targetUserId == null) {
            return ResponseEntity.badRequest().body("userId is required");
        }

        if (channelMemberRepository.existsByChannelIdAndUserId(id, targetUserId)) {
            return ResponseEntity.badRequest().body("User is already a member of this channel");
        }

        ChannelMember member = ChannelMember.builder()
                .channelId(id)
                .userId(targetUserId)
                .role("MEMBER")
                .joinedAt(LocalDateTime.now())
                .build();
        channelMemberRepository.save(member);

        // Notify member
        Map<String, Object> wsMsg = Map.of(
            "type", "CHANNEL_INVITATION",
            "channelId", id,
            "inviterId", getCurrentUser(authentication).getId()
        );
        chatWebSocketHandler.sendMessageToUser(targetUserId, wsMsg);

        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @DeleteMapping("/channels/{id}/members/{userId}")
    public ResponseEntity<?> removeMemberFromChannel(@PathVariable String id, @PathVariable String userId, Authentication authentication) {
        String currentUserId = getCurrentUser(authentication).getId();
        
        // Allowed if removing self (leave channel), or if current user is ADMIN of the channel
        boolean isSelf = currentUserId.equals(userId);
        Optional<ChannelMember> currentMembership = channelMemberRepository.findByChannelIdAndUserId(id, currentUserId);
        boolean isAdmin = currentMembership.isPresent() && "ADMIN".equalsIgnoreCase(currentMembership.get().getRole());

        if (!isSelf && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only channel admins can remove members");
        }

        channelMemberRepository.deleteByChannelIdAndUserId(id, userId);

        // Notify user
        Map<String, Object> wsMsg = Map.of(
            "type", "REMOVED_FROM_CHANNEL",
            "channelId", id,
            "removedBy", currentUserId
        );
        chatWebSocketHandler.sendMessageToUser(userId, wsMsg);

        return ResponseEntity.noContent().build();
    }

    // Direct Messages User List
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getChatUsers(Authentication authentication) {
        String currentUserId = getCurrentUser(authentication).getId();
        List<Employee> allEmployees = employeeRepository.findAll();
        
        List<Map<String, Object>> result = allEmployees.stream()
                .filter(emp -> !emp.getId().equals(currentUserId))
                .map(emp -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", emp.getId());
                    map.put("employeeId", emp.getEmployeeId());
                    map.put("name", emp.getFirstName() + " " + emp.getLastName());
                    map.put("email", emp.getEmail());
                    map.put("photo", emp.getPhoto());
                    map.put("status", emp.getStatus());
                    map.put("departmentId", emp.getDepartmentId());
                    map.put("designationId", emp.getDesignationId());
                    map.put("phone", emp.getPhone());
                    return map;
                })
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(result);
    }

    // Messages API
    //
    // Two modes on the same endpoint, both cursor-based (no offset pagination, so
    // performance doesn't degrade as a conversation grows into the tens of thousands
    // of messages):
    //   - no `before`: initial load. Returns the last 70 already-read messages plus
    //     every unread message (capped defensively), so a conversation you haven't
    //     opened in a while doesn't force-load its entire history.
    //   - `before=<ISO timestamp>`: infinite-scroll "load older", a plain bounded
    //     page of messages strictly before that cursor.
    @GetMapping("/messages/{conversationId}")
    public ResponseEntity<?> getMessages(
            @PathVariable String conversationId,
            @RequestParam(required = false) String before,
            Authentication authentication) {

        String userId = getCurrentUser(authentication).getId();

        // Ensure user belongs to the conversation if it is a channel or a DM
        if (conversationId.contains("_")) {
            String[] parts = conversationId.split("_");
            if (!parts[0].equals(userId) && !parts[1].equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else {
            // Channel check
            Optional<Channel> channelOpt = channelRepository.findById(conversationId);
            if (channelOpt.isPresent() && "PRIVATE".equalsIgnoreCase(channelOpt.get().getType())) {
                if (!channelMemberRepository.existsByChannelIdAndUserId(conversationId, userId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
        }

        if (before != null) {
            LocalDateTime beforeTs = LocalDateTime.parse(before);
            Query olderQuery = Query.query(Criteria.where("conversationId").is(conversationId)
                    .and("createdAt").lt(beforeTs)
                    .andOperator(notDeletedForUser(userId)))
                    .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                    .limit(OLDER_PAGE_SIZE);
            List<Message> older = mongoTemplate.find(olderQuery, Message.class);
            Collections.reverse(older);

            boolean hasMoreOlder = older.size() == OLDER_PAGE_SIZE;
            String oldestCursor = older.isEmpty() ? before : older.get(0).getCreatedAt().toString();
            return ResponseEntity.ok(new MessagePageResponse(older, oldestCursor, hasMoreOlder, null));
        }

        LocalDateTime lastReadAt = resolveLastReadAt(conversationId, userId);

        Query unreadQuery = Query.query(Criteria.where("conversationId").is(conversationId)
                .and("createdAt").gt(lastReadAt)
                .andOperator(notDeletedForUser(userId)))
                .with(Sort.by(Sort.Direction.ASC, "createdAt"))
                .limit(UNREAD_SAFETY_CAP);
        List<Message> unread = mongoTemplate.find(unreadQuery, Message.class);

        Query readQuery = Query.query(Criteria.where("conversationId").is(conversationId)
                .and("createdAt").lte(lastReadAt)
                .andOperator(notDeletedForUser(userId)))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .limit(INITIAL_READ_HISTORY_SIZE);
        List<Message> readRecent = mongoTemplate.find(readQuery, Message.class);
        Collections.reverse(readRecent);

        List<Message> combined = new ArrayList<>(readRecent);
        combined.addAll(unread);

        String firstUnreadId = unread.isEmpty() ? null : unread.get(0).getId();
        String oldestCursor;
        if (!readRecent.isEmpty()) {
            oldestCursor = readRecent.get(0).getCreatedAt().toString();
        } else if (!unread.isEmpty()) {
            oldestCursor = unread.get(0).getCreatedAt().toString();
        } else {
            oldestCursor = null;
        }
        boolean hasMoreOlder = readRecent.size() == INITIAL_READ_HISTORY_SIZE;

        return ResponseEntity.ok(new MessagePageResponse(combined, oldestCursor, hasMoreOlder, firstUnreadId));
    }

    @PostMapping("/messages")
    public ResponseEntity<Message> createMessage(@RequestBody Message message, Authentication authentication) {
        String userId = getCurrentUser(authentication).getId();
        message.setSenderId(userId);
        message.setCreatedAt(LocalDateTime.now());
        Message saved = messageRepository.save(message);

        // Sending a message implies you've seen everything up to it - advances your own
        // cursor so your own just-sent messages never count toward your own unread badge.
        advanceReadCursor(saved.getConversationId(), userId);

        // Deliver message in real time
        Map<String, Object> wsMsg = Map.of(
            "type", "CHAT_MESSAGE",
            "message", saved
        );

        String conversationId = saved.getConversationId();
        if (conversationId.contains("_")) {
            // Direct chat
            String[] parts = conversationId.split("_");
            String receiverId = parts[0].equals(userId) ? parts[1] : parts[0];
            chatWebSocketHandler.sendMessageToUser(receiverId, wsMsg);
            chatWebSocketHandler.sendMessageToUser(userId, wsMsg); // echo back to sender
        } else {
            // Channel chat
            chatWebSocketHandler.broadcastToChannel(conversationId, wsMsg, userId);
            chatWebSocketHandler.sendMessageToUser(userId, wsMsg); // echo back to sender
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/messages/{id}")
    public ResponseEntity<?> editMessage(@PathVariable String id, @RequestBody Map<String, String> payload, Authentication authentication) {
        String userId = getCurrentUser(authentication).getId();
        Optional<Message> messageOpt = messageRepository.findById(id);
        if (messageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Message message = messageOpt.get();
        if (!message.getSenderId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot edit another user's message");
        }

        message.setMessage(payload.get("message"));
        message.setEdited(true);
        message.setUpdatedAt(LocalDateTime.now());
        Message saved = messageRepository.save(message);

        Map<String, Object> wsMsg = Map.of(
            "type", "MESSAGE_EDITED",
            "message", saved
        );

        String conversationId = saved.getConversationId();
        if (conversationId.contains("_")) {
            String[] parts = conversationId.split("_");
            String receiverId = parts[0].equals(userId) ? parts[1] : parts[0];
            chatWebSocketHandler.sendMessageToUser(receiverId, wsMsg);
            chatWebSocketHandler.sendMessageToUser(userId, wsMsg);
        } else {
            chatWebSocketHandler.broadcastToChannel(conversationId, wsMsg, null);
        }

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable String id,
            @RequestParam(defaultValue = "me") String type,
            Authentication authentication) {
        String userId = getCurrentUser(authentication).getId();
        Optional<Message> messageOpt = messageRepository.findById(id);
        if (messageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Message message = messageOpt.get();

        if ("everyone".equalsIgnoreCase(type)) {
            if (!message.getSenderId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot delete for everyone");
            }
            message.setDeleted(true);
            message.setMessage("This message was deleted");
            message.setAttachmentUrl(null);
            message.setAttachmentName(null);
            message.setAttachmentSize(null);
            Message saved = messageRepository.save(message);

            Map<String, Object> wsMsg = Map.of(
                "type", "MESSAGE_DELETED",
                "messageId", id,
                "conversationId", message.getConversationId()
            );

            String conversationId = message.getConversationId();
            if (conversationId.contains("_")) {
                String[] parts = conversationId.split("_");
                String receiverId = parts[0].equals(userId) ? parts[1] : parts[0];
                chatWebSocketHandler.sendMessageToUser(receiverId, wsMsg);
                chatWebSocketHandler.sendMessageToUser(userId, wsMsg);
            } else {
                chatWebSocketHandler.broadcastToChannel(conversationId, wsMsg, null);
            }
        } else {
            // Delete for me
            message.getDeletedForUsers().add(userId);
            messageRepository.save(message);
        }

        return ResponseEntity.noContent().build();
    }

    // Media and Attachments API
    @GetMapping("/attachments/{conversationId}")
    public ResponseEntity<List<Message>> getAttachments(@PathVariable String conversationId) {
        return ResponseEntity.ok(messageRepository.findAttachmentsByConversationId(conversationId));
    }

    // Star / Favorite / Pinned Message Actions
    @PostMapping("/messages/{id}/star")
    public ResponseEntity<Message> toggleStarMessage(@PathVariable String id, Authentication authentication) {
        String userId = getCurrentUser(authentication).getId();
        Optional<Message> messageOpt = messageRepository.findById(id);
        if (messageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Message message = messageOpt.get();
        if (message.getStarredBy().contains(userId)) {
            message.getStarredBy().remove(userId);
        } else {
            message.getStarredBy().add(userId);
        }

        return ResponseEntity.ok(messageRepository.save(message));
    }

    @PostMapping("/messages/{id}/pin")
    public ResponseEntity<Message> togglePinMessage(@PathVariable String id) {
        Optional<Message> messageOpt = messageRepository.findById(id);
        if (messageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Message message = messageOpt.get();
        message.setPinned(!message.isPinned());
        Message saved = messageRepository.save(message);

        Map<String, Object> wsMsg = Map.of(
            "type", "MESSAGE_PINNED_TOGGLE",
            "message", saved
        );
        String conversationId = saved.getConversationId();
        if (conversationId.contains("_")) {
            String[] parts = conversationId.split("_");
            chatWebSocketHandler.sendMessageToUser(parts[0], wsMsg);
            chatWebSocketHandler.sendMessageToUser(parts[1], wsMsg);
        } else {
            chatWebSocketHandler.broadcastToChannel(conversationId, wsMsg, null);
        }

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/messages/{id}/react")
    public ResponseEntity<Message> addReaction(@PathVariable String id, @RequestBody Map<String, String> payload, Authentication authentication) {
        String userId = getCurrentUser(authentication).getId();
        String emoji = payload.get("emoji");
        Optional<Message> messageOpt = messageRepository.findById(id);
        if (messageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Message message = messageOpt.get();
        // Remove existing reaction by same user
        message.getReactions().removeIf(r -> r.getUserId().equals(userId));
        
        if (emoji != null && !emoji.trim().isEmpty()) {
            message.getReactions().add(new Message.Reaction(userId, emoji));
        }

        Message saved = messageRepository.save(message);

        Map<String, Object> wsMsg = Map.of(
            "type", "MESSAGE_REACTION",
            "message", saved
        );
        String conversationId = saved.getConversationId();
        if (conversationId.contains("_")) {
            String[] parts = conversationId.split("_");
            chatWebSocketHandler.sendMessageToUser(parts[0], wsMsg);
            chatWebSocketHandler.sendMessageToUser(parts[1], wsMsg);
        } else {
            chatWebSocketHandler.broadcastToChannel(conversationId, wsMsg, null);
        }

        return ResponseEntity.ok(saved);
    }

    // File Upload API
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            Path uploadDir = Paths.get("uploads");
            String uploadPath = uploadDir.toFile().getAbsolutePath();
            File dir = new File(uploadPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String newFilename = UUID.randomUUID().toString() + extension;
            File targetFile = new File(uploadPath + File.separator + newFilename);
            file.transferTo(targetFile);

            String fileUrl = "/chat/files/" + newFilename;
            String contentType = file.getContentType();
            String messageType = "DOCUMENT";
            if (contentType != null) {
                if (contentType.startsWith("image/")) {
                    messageType = "IMAGE";
                } else if (contentType.startsWith("video/")) {
                    messageType = "VIDEO";
                } else if (contentType.startsWith("audio/")) {
                    messageType = "AUDIO";
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("attachmentUrl", fileUrl);
            response.put("attachmentName", originalFilename);
            response.put("attachmentSize", file.getSize());
            response.put("messageType", messageType);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file: " + e.getMessage());
        }
    }
}
