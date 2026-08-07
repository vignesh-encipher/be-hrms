package com.hrms.repository;

import com.hrms.entity.Asset;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends MongoRepository<Asset, String> {
    Optional<Asset> findByAssetTag(String assetTag);
    List<Asset> findByStatus(String status);
    List<Asset> findByKind(String kind);
    List<Asset> findByAssignedToEmployeeId(String employeeId);
}
