package com.hrms.repository;

import com.hrms.entity.Channel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChannelRepository extends MongoRepository<Channel, String> {
    List<Channel> findByType(String type);
    java.util.Optional<Channel> findByName(String name);
}
