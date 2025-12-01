package com.wells.bill.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BillAndPaymentAssistant {

	public static void main(String[] args) {
		SpringApplication.run(BillAndPaymentAssistant.class, args);
	}
}
