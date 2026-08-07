package com.hrms.controller;

import com.hrms.entity.Asset;
import com.hrms.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/assets")
public class AssetController {

    @Autowired
    private AssetService assetService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('IT_ADMIN')")
    public ResponseEntity<Asset> createAsset(@RequestBody Asset asset) {
        return ResponseEntity.ok(assetService.createAsset(asset));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('IT_ADMIN') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<List<Asset>> getAllAssets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String employeeId) {
        return ResponseEntity.ok(assetService.getAllAssets(status, kind, employeeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('IT_ADMIN') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<Asset> getAssetById(@PathVariable String id) {
        return ResponseEntity.ok(assetService.getAssetById(id));
    }

    @GetMapping("/tag/{assetTag}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('IT_ADMIN') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<Asset> getAssetByTag(@PathVariable String assetTag) {
        return ResponseEntity.ok(assetService.getAssetByTag(assetTag));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('IT_ADMIN')")
    public ResponseEntity<Asset> assignAsset(
            @PathVariable String id,
            @RequestParam String employeeId,
            @RequestParam(required = false) String conditionNotes) {
        return ResponseEntity.ok(assetService.assignAsset(id, employeeId, conditionNotes));
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('IT_ADMIN')")
    public ResponseEntity<Asset> returnAsset(
            @PathVariable String id,
            @RequestParam(required = false) String conditionNotes) {
        return ResponseEntity.ok(assetService.returnAsset(id, conditionNotes));
    }

    @PostMapping("/{id}/recovery-due")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('IT_ADMIN')")
    public ResponseEntity<Asset> markRecoveryDue(@PathVariable String id) {
        return ResponseEntity.ok(assetService.markRecoveryDue(id));
    }
}
