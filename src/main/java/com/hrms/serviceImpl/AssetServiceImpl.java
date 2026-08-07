package com.hrms.serviceImpl;

import com.hrms.entity.Asset;
import com.hrms.entity.AssetHistoryEntry;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.AssetRepository;
import com.hrms.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AssetServiceImpl implements AssetService {

    @Autowired
    private AssetRepository assetRepository;

    @Override
    public Asset createAsset(Asset asset) {
        if (asset.getAssetTag() == null || asset.getAssetTag().isEmpty()) {
            throw new BadRequestException("Asset tag is required");
        }
        assetRepository.findByAssetTag(asset.getAssetTag()).ifPresent(existing -> {
            throw new BadRequestException("Asset tag already exists: " + asset.getAssetTag());
        });

        if (asset.getStatus() == null || asset.getStatus().isEmpty()) {
            asset.setStatus(asset.getAssignedToEmployeeId() != null ? "Assigned" : "In stock");
        }
        if (asset.getHistory() == null) {
            asset.setHistory(new ArrayList<>());
        }
        if ("Assigned".equalsIgnoreCase(asset.getStatus()) && asset.getAssignedToEmployeeId() != null) {
            LocalDate since = asset.getAssignedSince() != null ? asset.getAssignedSince() : LocalDate.now();
            asset.setAssignedSince(since);
            asset.getHistory().add(AssetHistoryEntry.builder()
                    .employeeId(asset.getAssignedToEmployeeId())
                    .assignedOn(since)
                    .build());
        }
        return assetRepository.save(asset);
    }

    @Override
    public List<Asset> getAllAssets(String status, String kind, String employeeId) {
        List<Asset> assets = assetRepository.findAll();
        return assets.stream()
                .filter(a -> status == null || status.isEmpty() || status.equalsIgnoreCase(a.getStatus()))
                .filter(a -> kind == null || kind.isEmpty() || kind.equalsIgnoreCase(a.getKind()))
                .filter(a -> employeeId == null || employeeId.isEmpty() || employeeId.equals(a.getAssignedToEmployeeId()))
                .toList();
    }

    @Override
    public Asset getAssetById(String id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));
    }

    @Override
    public Asset getAssetByTag(String assetTag) {
        return assetRepository.findByAssetTag(assetTag)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with tag: " + assetTag));
    }

    @Override
    public Asset assignAsset(String id, String employeeId, String conditionNotes) {
        if (employeeId == null || employeeId.isEmpty()) {
            throw new BadRequestException("employeeId is required to assign an asset");
        }
        Asset asset = getAssetById(id);
        if ("Assigned".equalsIgnoreCase(asset.getStatus())) {
            throw new BadRequestException("Asset is already assigned. Return it before reassigning.");
        }
        LocalDate now = LocalDate.now();
        asset.setAssignedToEmployeeId(employeeId);
        asset.setAssignedSince(now);
        asset.setStatus("Assigned");

        if (asset.getHistory() == null) {
            asset.setHistory(new ArrayList<>());
        }
        asset.getHistory().add(AssetHistoryEntry.builder()
                .employeeId(employeeId)
                .assignedOn(now)
                .conditionNotes(conditionNotes)
                .build());

        return assetRepository.save(asset);
    }

    @Override
    public Asset returnAsset(String id, String conditionNotes) {
        Asset asset = getAssetById(id);
        if (!"Assigned".equalsIgnoreCase(asset.getStatus()) && !"Recovery due".equalsIgnoreCase(asset.getStatus())) {
            throw new BadRequestException("Asset is not currently assigned");
        }

        if (asset.getHistory() != null) {
            for (int i = asset.getHistory().size() - 1; i >= 0; i--) {
                AssetHistoryEntry entry = asset.getHistory().get(i);
                if (entry.getReturnedOn() == null) {
                    entry.setReturnedOn(LocalDate.now());
                    entry.setConditionNotes(conditionNotes);
                    break;
                }
            }
        }

        asset.setAssignedToEmployeeId(null);
        asset.setAssignedSince(null);
        asset.setStatus("In stock");

        return assetRepository.save(asset);
    }

    @Override
    public Asset markRecoveryDue(String id) {
        Asset asset = getAssetById(id);
        if (!"Assigned".equalsIgnoreCase(asset.getStatus())) {
            throw new BadRequestException("Only assigned assets can be marked recovery due");
        }
        asset.setStatus("Recovery due");
        return assetRepository.save(asset);
    }
}
