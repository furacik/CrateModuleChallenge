package com.work.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private String surname;
    private Double creditLimit;
    private Double usedCreditLimit = 0.0;
    private String username;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;  // ADMIN, CUSTOMER gibi
}
