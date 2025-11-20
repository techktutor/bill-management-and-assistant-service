package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.entity.Customer;
import com.wells.bill.assistant.service.CustomerService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public Customer create(@RequestBody Customer c) {
        return customerService.createCustomer(c.getName(), c.getEmail(), c.getMerchantId());
    }
}
