package com.hrms.service;

import com.hrms.entity.SeparationRequest;

import java.util.List;

public interface SeparationService {
    SeparationRequest submitResignation(SeparationRequest request);
    SeparationRequest getById(String id);
    List<SeparationRequest> getAll();
    SeparationRequest toggleClearance(String id, String department, boolean done, String notes);
    SeparationRequest toggleAssetReturn(String id, String assetTag, boolean returned);
    SeparationRequest generateFullAndFinal(String id, SeparationRequest.FullAndFinal fullAndFinal);
    SeparationRequest approve(String id, String role, String remarks);
    SeparationRequest reject(String id, String role, String remarks);
}
