package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "channel_members")
public class ChannelMember {
    @Id
    private String id;

    @Indexed
    private String channelId;

    @Indexed
    private String userId;

    private String role; // ADMIN or MEMBER

    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();
}
