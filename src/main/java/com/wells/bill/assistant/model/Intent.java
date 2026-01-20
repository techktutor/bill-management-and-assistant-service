package com.wells.bill.assistant.model;

public sealed interface Intent
        permits ConfirmPaymentIntent, InitiatePaymentIntent, QueryBillsIntent, SchedulePaymentIntent, UnknownIntent {
}
