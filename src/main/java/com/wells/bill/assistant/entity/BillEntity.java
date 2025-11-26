package com.wells.bill.assistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "bills")
@NoArgsConstructor
@AllArgsConstructor
public class BillEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private double amount;
    private LocalDate dueDate;
    // e.g., PENDING, PAID, OVERDUE
    private String status;
    // e.g., "Acme Utilities"
    private String vendor;
    // e.g., "electricity", "water"
    private String category;
}