package com.hrms.repository;

import com.hrms.entity.ConversationReadState;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationReadStateRepository extends MongoRepository<ConversationReadState, String> {
    Optional<ConversationReadState> findByConversationIdAndUserId(String conversationId, String userId);
}
