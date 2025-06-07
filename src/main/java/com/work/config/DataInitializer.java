package com.work.config;

import com.work.model.Customer;
import com.work.model.Role;
import com.work.repository.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {
    @Bean
    public CommandLineRunner init(CustomerRepository repository, PasswordEncoder encoder) {
        return args -> {
            repository.save(new Customer( "admin","furat", encoder.encode("admin"), Role.ADMIN, new BigDecimal("50000"), BigDecimal.ZERO));
            repository.save(new Customer( "customer1","ahmet", encoder.encode("custpass1"), Role.CUSTOMER, new BigDecimal("50000"), BigDecimal.ZERO));
        };
    }
}
