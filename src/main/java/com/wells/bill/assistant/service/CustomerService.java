package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.Customer;
import com.wells.bill.assistant.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepo;

    public CustomerService(CustomerRepository customerRepo) {
        this.customerRepo = customerRepo;
    }

    public Customer createCustomer(String name, String email, UUID merchantId) {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setName(name);
        c.setEmail(email);
        c.setMerchantId(merchantId);
        c.setCreatedAt(Instant.now());
        return customerRepo.save(c);
    }
}
