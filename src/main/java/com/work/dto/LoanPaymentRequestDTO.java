package com.work.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanPaymentRequestDTO {
    private Long loanId;
    private BigDecimal amount;
}
