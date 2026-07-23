package com.hrms.repository;

import com.hrms.entity.ChannelMember;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelMemberRepository extends MongoRepository<ChannelMember, String> {
    List<ChannelMember> findByChannelId(String channelId);
    List<ChannelMember> findByUserId(String userId);
    Optional<ChannelMember> findByChannelIdAndUserId(String channelId, String userId);
    void deleteByChannelIdAndUserId(String channelId, String userId);
    void deleteByChannelId(String channelId);
    boolean existsByChannelIdAndUserId(String channelId, String userId);
}
