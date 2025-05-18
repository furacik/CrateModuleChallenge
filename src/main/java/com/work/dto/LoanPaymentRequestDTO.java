package com.work.dto;

import lombok.Data;

@Data
public class LoanPaymentRequestDTO {
    private Long loanId;
    private Double amount;
}
