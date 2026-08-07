package com.hrms.service;

import com.hrms.entity.SalaryRevision;

import java.util.List;

public interface SalaryRevisionService {
    SalaryRevision createRevision(SalaryRevision revision);
    List<SalaryRevision> getAllRevisions();
    SalaryRevision getRevisionById(String id);
    SalaryRevision approveRevision(String id);
}
