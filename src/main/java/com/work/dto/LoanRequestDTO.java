package com.work.dto;

import lombok.Data;

@Data
public class LoanRequestDTO {
    private Long customerId;
    private Double loanAmount;
    private Double interestRate;
    private Integer numberOfInstallment;
}
