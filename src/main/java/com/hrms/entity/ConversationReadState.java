package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Per-user read cursor for a conversation (DM or channel). Replaces scanning every
 * message's readBy set to determine the unread boundary - lastReadAt is all that's
 * needed to cheaply query "messages after this point are unread".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversation_read_state")
@CompoundIndex(name = "conversationId_userId", def = "{'conversationId': 1, 'userId': 1}", unique = true)
public class ConversationReadState {
    @Id
    private String id;

    private String conversationId;
    private String userId;
    private LocalDateTime lastReadAt;
}
