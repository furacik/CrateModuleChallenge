package com.work.service;

import com.work.dto.*;
import com.work.model.Customer;
import com.work.model.Installment;
import com.work.model.Loan;
import com.work.repository.CustomerRepository;
import com.work.repository.InstallmentRepository;
import com.work.repository.LoanRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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


    @Transactional
    public String createLoan(LoanRequestDTO dto) {
        //installment count control
        List<Integer> allowedInstallments = List.of(6, 9, 12, 24);
        if (!allowedInstallments.contains(dto.getNumberOfInstallment())) {
            return "Taksit sayısı sadece 6, 9, 12, 24 olabilir.";
        }

        // interest rate control
        if (dto.getInterestRate() < 0.1 || dto.getInterestRate() > 0.5) {
            return "Faiz oranı 0.1 ile 0.5 arasında olmalıdır.";
        }

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Müşteri bulunamadı."));

        double totalRepayment = dto.getLoanAmount() * (1 + dto.getInterestRate());

        // Limit control
        if (customer.getUsedCreditLimit() + totalRepayment > customer.getCreditLimit()) {
            return "Müşteri limiti yetersiz.";
        }

        // create loan
        Loan loan = new Loan();
        loan.setCustomer(customer);
        loan.setLoanAmount(totalRepayment);
        loan.setNumberOfInstallments(dto.getNumberOfInstallment());
        loan.setCreateDate(LocalDate.now());
        loan.setIsPaid(false);

        loanRepository.save(loan);

        // create installment
        int installmentCount = dto.getNumberOfInstallment();
        BigDecimal totalAmount = BigDecimal.valueOf(totalRepayment);
        BigDecimal baseInstallment = totalAmount
                .divide(BigDecimal.valueOf(installmentCount), 2, RoundingMode.HALF_UP);
        List<Installment> installments = new ArrayList<>();

        LocalDate dueDate = LocalDate.now().plusMonths(1).withDayOfMonth(1); // first day after other month

        for (int i = 0; i < dto.getNumberOfInstallment(); i++) {
            Installment ins = new Installment();
            ins.setLoan(loan);
            // added last penny last installment
            if (i == installmentCount - 1) {
                BigDecimal totalBase = baseInstallment.multiply(BigDecimal.valueOf(installmentCount - 1));
                BigDecimal lastInstallment = totalAmount.subtract(totalBase);
                ins.setAmount(lastInstallment.doubleValue());
            } else {
                ins.setAmount(baseInstallment.doubleValue());
            }
            ins.setDueDate(dueDate.plusMonths(i));
            ins.setIsPaid(false);
            ins.setPaidAmount(0.0);
            installments.add(ins);
        }

        installmentRepository.saveAll(installments);

        // update customer limit
        customer.setUsedCreditLimit(customer.getUsedCreditLimit() + totalRepayment);
        customerRepository.save(customer);

        return "Kredi başarıyla oluşturuldu.";
    }



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
    public PaymentResultDTO payInstallments(LoanPaymentRequestDTO dto) {
        Loan loan = loanRepository.findById(dto.getLoanId())
                .orElseThrow(() -> new RuntimeException("Kredi bulunamadı."));

        Customer customer = loan.getCustomer();

        LocalDate today = LocalDate.now();
        LocalDate maxDueDate = today.plusMonths(3);

        List<Installment> installments = installmentRepository
                .findByLoanIdOrderByDueDateAsc(loan.getId());

        double remainingAmount = dto.getAmount();
        int paidCount = 0;
        double totalPaid = 0.0;

        for (Installment ins : installments) {
            if (ins.getIsPaid()) continue;
            if (ins.getDueDate().isAfter(maxDueDate)) continue;

            double baseAmount = ins.getAmount();
            double adjustedAmount = baseAmount;

            long daysBetween = ChronoUnit.DAYS.between(today, ins.getDueDate());

            if (daysBetween > 0) {
                // if pay early, create a discount
                double discount = baseAmount * 0.001 * daysBetween;
                adjustedAmount = baseAmount - discount;
            } else if (daysBetween < 0) {
                // if pay late , do panishment
                long lateDays = -daysBetween;
                double penalty = baseAmount * 0.001 * lateDays;
                adjustedAmount = baseAmount + penalty;
            }
            adjustedAmount = BigDecimal.valueOf(adjustedAmount)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();

            if (remainingAmount >= adjustedAmount) {
                ins.setIsPaid(true);
                ins.setPaidAmount(adjustedAmount);
                ins.setPaymentDate(today);
                installmentRepository.save(ins);

                remainingAmount -= adjustedAmount;
                totalPaid += adjustedAmount;
                paidCount++;
            } else {
                break; // pay all instalment. dont pass if not enough
            }
        }

        // check if all loan paid
        boolean allPaid = installmentRepository
                .findByLoanIdOrderByDueDateAsc(loan.getId())
                .stream().allMatch(Installment::getIsPaid);

        loan.setIsPaid(allPaid);
        loanRepository.save(loan);

        // customer usedCreditLimit update
        customer.setUsedCreditLimit(customer.getUsedCreditLimit() - totalPaid);
        customerRepository.save(customer);

        PaymentResultDTO result = new PaymentResultDTO();
        result.installmentsPaid = paidCount;
        result.totalPaid = totalPaid;
        result.loanFullyPaid = allPaid;

        return result;
    }
}
