package com.work.service;

import com.work.dto.*;
import com.work.model.Customer;
import com.work.model.Installment;
import com.work.model.Loan;
import com.work.repository.CustomerRepository;
import com.work.repository.InstallmentRepository;
import com.work.repository.LoanRepository;
import com.work.utils.ErrorConstants;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final CustomerRepository customerRepository;
    private final LoanRepository loanRepository;
    private final InstallmentRepository installmentRepository;

    @PreAuthorize("hasRole('ADMIN') or @loanSecurity.isLoanOwner(#dto.loanId, authentication.name)")
    @Transactional
    public String createLoan(LoanRequestDTO dto) {
        List<Integer> allowedInstallments = List.of(6, 9, 12, 24);

        if (!allowedInstallments.contains(dto.getNumberOfInstallment())) {
            throw new IllegalArgumentException(ErrorConstants.INSTALLMENT_COUNT_ERROR);
        }

        if (dto.getInterestRate().compareTo(new BigDecimal("0.1")) < 0 ||
                dto.getInterestRate().compareTo(new BigDecimal("0.5")) > 0) {
            throw new IllegalArgumentException(ErrorConstants.INTEREST_RATE_ERROR);
        }

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new RuntimeException(ErrorConstants.CUSTOMER_NOT_FOUND_ERROR));

        BigDecimal interestRate = dto.getInterestRate();
        BigDecimal loanAmount = dto.getLoanAmount();
        BigDecimal totalRepayment = loanAmount.multiply(BigDecimal.ONE.add(interestRate));

        if (customer.getUsedCreditLimit().add(totalRepayment).compareTo(customer.getCreditLimit()) > 0) {
            throw new IllegalArgumentException(ErrorConstants.CUSTOMER_LIMIT_EXCEEDED_ERROR);
        }

        Loan loan = new Loan();
        loan.setCustomer(customer);
        loan.setLoanAmount(totalRepayment);
        loan.setNumberOfInstallments(dto.getNumberOfInstallment());
        loan.setCreateDate(LocalDate.now());
        loan.setIsPaid(false);

        loanRepository.save(loan);

        int installmentCount = dto.getNumberOfInstallment();
        BigDecimal baseInstallment = totalRepayment.divide(BigDecimal.valueOf(installmentCount), 2, RoundingMode.HALF_UP);
        List<Installment> installments = new ArrayList<>();

        LocalDate dueDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);

        for (int i = 0; i < installmentCount; i++) {
            Installment ins = new Installment();
            ins.setLoan(loan);
            if (i == installmentCount - 1) {
                BigDecimal totalBase = baseInstallment.multiply(BigDecimal.valueOf(installmentCount - 1));
                BigDecimal lastInstallment = totalRepayment.subtract(totalBase);
                ins.setAmount(lastInstallment);
            } else {
                ins.setAmount(baseInstallment);
            }
            ins.setDueDate(dueDate.plusMonths(i));
            ins.setIsPaid(false);
            ins.setPaidAmount(BigDecimal.ZERO);
            installments.add(ins);
        }

        installmentRepository.saveAll(installments);

        customer.setUsedCreditLimit(customer.getUsedCreditLimit().add(totalRepayment));
        customerRepository.save(customer);

        return ErrorConstants.LOAN_CREATE_SUCCESS;
    }


    @PreAuthorize("hasRole('ADMIN') or @loanSecurity.isLoanOwner(#dto.loanId, authentication.name)")
    public List<LoanResponseDTO> getLoansByCustomerId(Long customerId, Boolean isPaid) {
        List<Loan> loans;

        if (isPaid != null) {
            loans = loanRepository.findByCustomerIdAndIsPaid(customerId, isPaid);
        } else {
            loans = loanRepository.findByCustomerId(customerId);
        }

        return loans.stream().map(loan -> {
            LoanResponseDTO dto = new LoanResponseDTO();
            dto.setLoanId(loan.getId());
            dto.setLoanAmount(loan.getLoanAmount());
            dto.setNumberOfInstallment(loan.getNumberOfInstallments());
            dto.setCreateDate(loan.getCreateDate());
            dto.setIsPaid(loan.getIsPaid());
            return dto;
        }).collect(Collectors.toList());
    }
    @PreAuthorize("hasRole('ADMIN') or @loanSecurity.isLoanOwner(#dto.loanId, authentication.name)")
    public List<InstallmentResponseDTO> getInstallments(Long loanId, Boolean isPaid) {
        List<Installment> installments;

        if (isPaid != null) {
            installments = installmentRepository.findByLoanIdAndIsPaidOrderByDueDateAsc(loanId, isPaid);
        } else {
            installments = installmentRepository.findByLoanIdOrderByDueDateAsc(loanId);
        }

        return installments.stream().map(ins -> {
            InstallmentResponseDTO dto = new InstallmentResponseDTO();
            dto.setId(ins.getId());
            dto.setAmount(ins.getAmount());
            dto.setPaidAmount(ins.getPaidAmount());
            dto.setDueDate(ins.getDueDate());
            dto.setPaymentDate(ins.getPaymentDate());
            dto.setIsPaid(ins.getIsPaid());
            return dto;
        }).toList();
    }
    @PreAuthorize("hasRole('ADMIN') or @loanSecurity.isLoanOwner(#dto.loanId, authentication.name)")
    public PaymentResultDTO payInstallments(LoanPaymentRequestDTO dto) {
        Loan loan = loanRepository.findById(dto.getLoanId())
                .orElseThrow(() -> new RuntimeException("Kredi bulunamadı."));

        Customer customer = loan.getCustomer();

        LocalDate today = LocalDate.now();
        LocalDate maxDueDate = today.plusMonths(3);

        List<Installment> installments = installmentRepository
                .findByLoanIdOrderByDueDateAsc(loan.getId());

        BigDecimal remainingAmount = dto.getAmount();
        int paidCount = 0;
        BigDecimal totalPaid = BigDecimal.ZERO;

        for (Installment ins : installments) {
            if (ins.getIsPaid()) continue;
            if (ins.getDueDate().isAfter(maxDueDate)) continue;

            BigDecimal baseAmount = ins.getAmount();
            BigDecimal adjustedAmount = baseAmount;

            long daysBetween = ChronoUnit.DAYS.between(today, ins.getDueDate());

            if (daysBetween > 0) {
                // Erken ödeme indirimi
                BigDecimal discountRate = new BigDecimal("0.001");
                BigDecimal discount = discountRate.multiply(BigDecimal.valueOf(daysBetween)).multiply(baseAmount);
                adjustedAmount = baseAmount.subtract(discount);
            } else if (daysBetween < 0) {
                // Gecikme cezası
                long lateDays = -daysBetween;
                BigDecimal penaltyRate = new BigDecimal("0.001");
                BigDecimal penalty = penaltyRate.multiply(BigDecimal.valueOf(lateDays)).multiply(baseAmount);
                adjustedAmount = baseAmount.add(penalty);
            }

            // 2 ondalık basamağa yuvarla
            adjustedAmount = adjustedAmount.setScale(2, RoundingMode.HALF_UP);
            if (adjustedAmount.compareTo(BigDecimal.ZERO) < 0) {
                adjustedAmount = BigDecimal.ZERO;
            }
            if (remainingAmount.compareTo(adjustedAmount) >= 0) {
                ins.setIsPaid(true);
                ins.setPaidAmount(adjustedAmount);
                ins.setPaymentDate(today);
                installmentRepository.save(ins);

                remainingAmount = remainingAmount.subtract(adjustedAmount);
                totalPaid = totalPaid.add(adjustedAmount);
                paidCount++;
            } else {
                break; // Taksit ödenemiyorsa dur
            }
        }

        // check if all loan paid
        boolean allPaid = installmentRepository
                .findByLoanIdOrderByDueDateAsc(loan.getId())
                .stream().allMatch(Installment::getIsPaid);

        loan.setIsPaid(allPaid);
        loanRepository.save(loan);

        // customer usedCreditLimit update
        customer.setUsedCreditLimit(customer.getUsedCreditLimit().subtract(totalPaid));
        customerRepository.save(customer);

        PaymentResultDTO result = new PaymentResultDTO();
        result.installmentsPaid = paidCount;
        result.totalPaid = totalPaid;
        result.loanFullyPaid = allPaid;

        return result;
    }
}
