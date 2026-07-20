package com.hrms.repository;

import com.hrms.entity.RefreshToken;
import com.hrms.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);
    int deleteByUserId(String userId);
}
