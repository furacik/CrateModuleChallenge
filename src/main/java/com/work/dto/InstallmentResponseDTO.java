package com.work.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InstallmentResponseDTO {
    private Long id;
    private Double amount;
    private Double paidAmount;
    private LocalDate dueDate;
    private LocalDate paymentDate;
    private Boolean isPaid;
}
