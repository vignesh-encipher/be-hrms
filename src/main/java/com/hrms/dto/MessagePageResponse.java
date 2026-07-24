package com.hrms.dto;

import com.hrms.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessagePageResponse {
    private List<Message> messages;
    private String oldestCursor;
    private boolean hasMoreOlder;
    private String firstUnreadMessageId;
}
