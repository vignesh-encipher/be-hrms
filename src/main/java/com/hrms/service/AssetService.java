package com.hrms.service;

import com.hrms.entity.Asset;

import java.util.List;

public interface AssetService {
    Asset createAsset(Asset asset);
    List<Asset> getAllAssets(String status, String kind, String employeeId);
    Asset getAssetById(String id);
    Asset getAssetByTag(String assetTag);
    Asset assignAsset(String id, String employeeId, String conditionNotes);
    Asset returnAsset(String id, String conditionNotes);
    Asset markRecoveryDue(String id);
}
