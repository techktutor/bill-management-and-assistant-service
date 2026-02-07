package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.CustomerEntity;
import com.wells.bill.assistant.repository.CustomerRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public String getCustomerEmailByUserId(UUID userId) {
        var customer = customerRepository.findByUserId(userId);
        String email = customer != null ? customer.getEmail() : null;

        log.info("Retrieved customer email: {}", email);
        return email;
    }

    public UUID createCustomerIfNotExists(UUID userId) {
        var existingCustomer = customerRepository.findByUserId(userId);
        if (existingCustomer != null) {
            return existingCustomer.getId();
        }
        var newCustomer = new CustomerEntity();
        newCustomer.setCustomerType("INDIVIDUAL");
        newCustomer.setUserId(userId);
        newCustomer.setEmail("techktutor@zohomail.in");
        newCustomer.setFullName("Angel Patel");
        return customerRepository.save(newCustomer).getId();
    }
}
