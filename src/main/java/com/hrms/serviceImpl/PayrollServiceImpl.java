package com.hrms.serviceImpl;

import com.hrms.entity.Employee;
import com.hrms.entity.Payroll;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.PayrollRepository;
import com.hrms.service.PayrollService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Service
public class PayrollServiceImpl implements PayrollService {

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public Payroll generateSalary(Payroll payroll) {
        double basic = payroll.getBasic() != null ? payroll.getBasic() : 0.0;
        double hra = payroll.getHra() != null ? payroll.getHra() : 0.0;
        double allowance = payroll.getAllowance() != null ? payroll.getAllowance() : 0.0;
        double bonus = payroll.getBonus() != null ? payroll.getBonus() : 0.0;
        double deductions = payroll.getDeductions() != null ? payroll.getDeductions() : 0.0;

        double netSalary = basic + hra + allowance + bonus - deductions;
        payroll.setNetSalary(netSalary);
        payroll.setStatus("Generated");
        payroll.setPaidDate(LocalDate.now());

        return payrollRepository.save(payroll);
    }

    @Override
    public List<Payroll> getPayrollHistory(String employeeId) {
        return payrollRepository.findByEmployeeId(employeeId);
    }

    @Override
    public List<Payroll> getAllPayrolls() {
        return payrollRepository.findAll();
    }

    @Override
    public byte[] generatePayslipPdf(String payrollId) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found with id: " + payrollId));

        Employee employee = employeeRepository.findByEmployeeId(payroll.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + payroll.getEmployeeId()));

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // Font configurations
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            // Title block
            Paragraph title = new Paragraph("HRMS CORP - PAYSLIP", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Payslip for the Period: " + payroll.getMonth() + " " + payroll.getYear(), subTitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // Employee Details Table
            PdfPTable empTable = new PdfPTable(2);
            empTable.setWidthPercentage(100);
            empTable.setSpacingAfter(15);

            empTable.addCell(new PdfPCell(new Phrase("Employee ID: " + employee.getEmployeeId(), regularFont)));
            empTable.addCell(new PdfPCell(new Phrase("Employee Name: " + employee.getFirstName() + " " + employee.getLastName(), regularFont)));
            empTable.addCell(new PdfPCell(new Phrase("Email: " + employee.getEmail(), regularFont)));
            empTable.addCell(new PdfPCell(new Phrase("Status: " + employee.getStatus(), regularFont)));
            
            document.add(empTable);

            // Salary Breakdown Table
            PdfPTable salaryTable = new PdfPTable(2);
            salaryTable.setWidthPercentage(100);
            salaryTable.setSpacingAfter(20);

            // Header
            PdfPCell cell1 = new PdfPCell(new Phrase("Component", boldFont));
            PdfPCell cell2 = new PdfPCell(new Phrase("Amount ($)", boldFont));
            salaryTable.addCell(cell1);
            salaryTable.addCell(cell2);

            salaryTable.addCell(new PdfPCell(new Phrase("Basic Salary", regularFont)));
            salaryTable.addCell(new PdfPCell(new Phrase(String.format("%.2f", payroll.getBasic()), regularFont)));

            salaryTable.addCell(new PdfPCell(new Phrase("HRA", regularFont)));
            salaryTable.addCell(new PdfPCell(new Phrase(String.format("%.2f", payroll.getHra()), regularFont)));

            salaryTable.addCell(new PdfPCell(new Phrase("Allowance", regularFont)));
            salaryTable.addCell(new PdfPCell(new Phrase(String.format("%.2f", payroll.getAllowance()), regularFont)));

            salaryTable.addCell(new PdfPCell(new Phrase("Bonus", regularFont)));
            salaryTable.addCell(new PdfPCell(new Phrase(String.format("%.2f", payroll.getBonus()), regularFont)));

            salaryTable.addCell(new PdfPCell(new Phrase("Deductions", regularFont)));
            salaryTable.addCell(new PdfPCell(new Phrase(String.format("%.2f", payroll.getDeductions()), regularFont)));

            PdfPCell netCellLabel = new PdfPCell(new Phrase("Net Salary", boldFont));
            PdfPCell netCellVal = new PdfPCell(new Phrase(String.format("%.2f", payroll.getNetSalary()), boldFont));
            salaryTable.addCell(netCellLabel);
            salaryTable.addCell(netCellVal);

            document.add(salaryTable);

            // Signature placeholder
            Paragraph sig = new Paragraph("Authorized Signature\nHRMS Corp", regularFont);
            sig.setAlignment(Element.ALIGN_RIGHT);
            sig.setSpacingBefore(30);
            document.add(sig);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }
}
