package com.work.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanRequestDTO {
    private Long customerId;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer numberOfInstallment;
}
