package com.work.repository;

import com.work.model.Installment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {
    List<Installment> findByLoanIdOrderByDueDateAsc(Long loanId);

    List<Installment> findByLoanIdAndIsPaidOrderByDueDateAsc(Long loanId, boolean isPaid);
}
