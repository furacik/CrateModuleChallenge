package com.work.controller;

import com.work.dto.*;
import com.work.model.Installment;
import com.work.service.LoanService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@AllArgsConstructor
public class LoanController {

    private final LoanService loanService;
    @PostMapping("/create")
    public String createLoan(@RequestBody LoanRequestDTO dto) {
        return loanService.createLoan(dto);
    }
    @GetMapping("/list")
    public List<LoanResponseDTO> listLoans(
            @RequestParam Long customerId,
            @RequestParam(required = false) Boolean isPaid){
        return loanService.getLoansByCustomerId(customerId, isPaid);
    }
    @GetMapping("/installments")
    public List<InstallmentResponseDTO> getInstallments(
            @RequestParam Long loanId,
            @RequestParam(required = false) Boolean isPaid
    ) {
        return loanService.getInstallments(loanId, isPaid);
    }
    @PostMapping("/pay")
    public PaymentResultDTO payLoan(@RequestBody LoanPaymentRequestDTO dto) {
        return loanService.payInstallments(dto);
    }
}