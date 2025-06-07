package com.work.config;

import com.work.repository.LoanRepository;
import org.springframework.stereotype.Component;

@Component("loanSecurity")
public class LoanSecurity {
    private final LoanRepository loanRepository;

    public LoanSecurity(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    public boolean isLoanOwner(Long loanId, String username) {
        return loanRepository.findById(loanId)
                .map(loan -> loan.getCustomer().getUsername().equals(username)) // Customer entity’de username olmalı
                .orElse(false);
    }


    public boolean isCustomerSelf(Long customerId, String username) {
        return loanRepository.findByCustomerUsername(username)  // ya da customerRepository'den çek
                .stream().anyMatch(loan -> loan.getCustomer().getId().equals(customerId));
    }
}
