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
@Document(collection = "channels")
public class Channel {
    @Id
    private String id;

    @Indexed
    private String name;
    private String description;
    
    private String type; // PUBLIC or PRIVATE
    private String createdBy; // userId
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private String avatar; // base64 or URL placeholder
    
    @org.springframework.data.annotation.Transient
    private java.util.List<String> initialMembers;
}
