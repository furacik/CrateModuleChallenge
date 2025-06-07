package com.work.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private BigDecimal creditLimit;
    private BigDecimal usedCreditLimit = BigDecimal.ZERO;
    private String username;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;  // ADMIN, CUSTOMER gibi

    public Customer( String username,String name, String password, Role role, BigDecimal creditLimit, BigDecimal usedCreditLimit) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.role = role;
        this.creditLimit = creditLimit;
        this.usedCreditLimit = usedCreditLimit;
    }
}
