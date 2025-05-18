package com.work.dto;

import lombok.Data;

import java.time.LocalDate;
@Data
public class LoanResponseDTO {
    private Long loanId;
    private Double loanAmount;
    private Integer numberOfInstallment;
    private LocalDate createDate;
    private Boolean isPaid;
}
