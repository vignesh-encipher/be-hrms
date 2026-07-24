package com.hrms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "messages")
@CompoundIndex(name = "conversationId_createdAt", def = "{'conversationId': 1, 'createdAt': -1}")
public class Message {
    @Id
    private String id;

    @Indexed
    private String conversationId; // Channel ID or userId1_userId2 (sorted)

    @Indexed
    private String senderId;

    private String message;
    private String messageType; // TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT
    
    private String attachmentUrl;
    private String attachmentName;
    private Long attachmentSize;
    
    private String replyTo; // message ID
    
    @Builder.Default
    private boolean edited = false;
    
    @Builder.Default
    private boolean deleted = false;
    
    @Builder.Default
    private boolean pinned = false;

    @Builder.Default
    private List<Reaction> reactions = new ArrayList<>();

    @Builder.Default
    private Set<String> starredBy = new HashSet<>();

    @Builder.Default
    private Set<String> readBy = new HashSet<>();

    @Builder.Default
    private Set<String> deliveredTo = new HashSet<>();
    
    @JsonIgnore
    @Builder.Default
    private Set<String> deletedForUsers = new HashSet<>(); // User IDs who deleted this message "for me" - server-side filter only, never needed by clients

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Reaction {
        private String userId;
        private String emoji;
    }
}
