package com.hrms.repository;

import com.hrms.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    @Query("{ 'conversationId': ?0, $or: [ { 'deletedForUsers': { $exists: false } }, { 'deletedForUsers': null }, { 'deletedForUsers': { $ne: ?1 } } ] }")
    Page<Message> findByConversationIdAndDeletedForUsersNotContains(String conversationId, String userId, Pageable pageable);
    
    // Fallback if deletedForUsers list is empty or for direct queries
    Page<Message> findByConversationId(String conversationId, Pageable pageable);

    @Query("{ 'conversationId': ?0, 'attachmentUrl': { $ne: null } }")
    List<Message> findAttachmentsByConversationId(String conversationId);

    List<Message> findByConversationIdAndPinnedTrue(String conversationId);
}
