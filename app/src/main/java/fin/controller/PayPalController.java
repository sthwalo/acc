/*
 * FIN Financial Management System
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * Licensed under Apache License 2.0 - Commercial use requires separate licensing
 */
package fin.controller;

import com.paypal.orders.Order;
import fin.dto.ApiResponse;
import fin.dto.PayPalDto;
import fin.exception.ErrorCode;
import fin.service.PayPalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * PayPal payment processing controller
 * Handles order creation and capture for user registration payments
 */
@RestController
@RequestMapping("/api/v1/paypal")
@CrossOrigin(origins = "${FIN_CORS_ALLOWED_ORIGINS:http://localhost:3000}")
public class PayPalController {

    private static final Logger logger = LoggerFactory.getLogger(PayPalController.class);

    private final PayPalService payPalService;

    @Autowired
    public PayPalController(PayPalService payPalService) {
        this.payPalService = payPalService;
    }

    /**
     * Create a PayPal order for payment
     * Called by frontend to initiate payment process
     *
     * @param request Payment request containing amount, currency, and description
     * @return PayPal order details including order ID and approval URL
     */
    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<PayPalDto.OrderResponse>> createOrder(@RequestBody PayPalDto.CreateOrderRequest request) {
        try {
            logger.info("Creating PayPal order for amount: {} {}", request.getAmount(), request.getCurrency());

            // Check for dummy mode
            String dummyMode = System.getenv("PAYPAL_DUMMY_MODE");
            if ("true".equalsIgnoreCase(dummyMode)) {
                logger.info("DUMMY MODE: Creating mock PayPal order");

                // Return mock response for testing
                PayPalDto.OrderResponse mockResponse = new PayPalDto.OrderResponse();
                mockResponse.setOrderId("DUMMY_ORDER_" + System.currentTimeMillis());
                mockResponse.setStatus("CREATED");
                mockResponse.setApprovalUrl("dummy://approve");

                return ResponseEntity.ok(
                    ApiResponse.success("Mock PayPal order created successfully", mockResponse)
                );
            }

            // Validate request
            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Invalid payment amount", ErrorCode.VALIDATION_ERROR.getCode())
                );
            }

            if (request.getCurrency() == null || request.getCurrency().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Currency is required", ErrorCode.VALIDATION_ERROR.getCode())
                );
            }

            // Create the PayPal order
            Order order = payPalService.createOrder(
                request.getAmount(),
                request.getCurrency(),
                request.getDescription() != null ? request.getDescription() : "FIN Registration Payment"
            );

            // Extract approval URL from order links
            String approvalUrl = order.links().stream()
                .filter(link -> "approve".equals(link.rel()))
                .findFirst()
                .map(link -> link.href())
                .orElse(null);

            // Prepare response
            PayPalDto.OrderResponse responseData = new PayPalDto.OrderResponse();
            responseData.setOrderId(order.id());
            responseData.setStatus(order.status());
            responseData.setApprovalUrl(approvalUrl);

            logger.info("PayPal order created successfully: {}", order.id());

            return ResponseEntity.ok(
                ApiResponse.success("PayPal order created successfully", responseData)
            );

        } catch (IOException e) {
            logger.error("Failed to create PayPal order", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to create payment order: " + e.getMessage(),
                    ErrorCode.INTERNAL_ERROR.getCode())
            );
        } catch (Exception e) {
            logger.error("Unexpected error creating PayPal order", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Unexpected error occurred",
                    ErrorCode.INTERNAL_ERROR.getCode())
            );
        }
    }

    /**
     * Capture a PayPal order after user approval
     * Called by frontend after user approves payment on PayPal
     *
     * @param request Capture request containing order ID
     * @return Payment capture details
     */
    @PostMapping("/orders/capture")
    public ResponseEntity<ApiResponse<PayPalDto.CaptureResponse>> captureOrder(@RequestBody PayPalDto.CaptureOrderRequest request) {
        try {
            logger.info("Capturing PayPal order: {}", request.getOrderId());

            // Check for dummy mode
            String dummyMode = System.getenv("PAYPAL_DUMMY_MODE");
            if ("true".equalsIgnoreCase(dummyMode)) {
                logger.info("DUMMY MODE: Mock capturing PayPal order: {}", request.getOrderId());

                // Return mock capture response for testing
                PayPalDto.CaptureResponse mockResponse = new PayPalDto.CaptureResponse();
                mockResponse.setOrderId(request.getOrderId());
                mockResponse.setStatus("COMPLETED");
                mockResponse.setCompleted(true);
                mockResponse.setCaptureId("DUMMY_CAPTURE_" + System.currentTimeMillis());
                mockResponse.setAmount(new BigDecimal("299.00"));
                mockResponse.setCurrency("USD");

                return ResponseEntity.ok(
                    ApiResponse.success("Mock payment captured successfully", mockResponse)
                );
            }

            // Validate request
            if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Order ID is required", ErrorCode.VALIDATION_ERROR.getCode())
                );
            }

            // Capture the PayPal order
            Order order = payPalService.captureOrder(request.getOrderId());

            // Check if capture was successful
            boolean isCompleted = "COMPLETED".equals(order.status());

            // Prepare response
            PayPalDto.CaptureResponse responseData = new PayPalDto.CaptureResponse();
            responseData.setOrderId(order.id());
            responseData.setStatus(order.status());
            responseData.setCompleted(isCompleted);

            // Extract payment details if available
            if (order.purchaseUnits() != null && !order.purchaseUnits().isEmpty()) {
                var purchaseUnit = order.purchaseUnits().get(0);
                if (purchaseUnit.payments() != null && purchaseUnit.payments().captures() != null
                    && !purchaseUnit.payments().captures().isEmpty()) {
                    var capture = purchaseUnit.payments().captures().get(0);
                    responseData.setCaptureId(capture.id());
                    if (capture.amount() != null) {
                        responseData.setAmount(new BigDecimal(capture.amount().value()));
                        responseData.setCurrency(capture.amount().currencyCode());
                    }
                }
            }

            logger.info("PayPal order capture result - Order: {}, Status: {}", order.id(), order.status());

            if (isCompleted) {
                return ResponseEntity.ok(
                    ApiResponse.success("Payment captured successfully", responseData)
                );
            } else {
                return ResponseEntity.ok(
                    ApiResponse.success("Payment processing", responseData)
                );
            }

        } catch (IOException e) {
            logger.error("Failed to capture PayPal order", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to capture payment: " + e.getMessage(),
                    ErrorCode.INTERNAL_ERROR.getCode())
            );
        } catch (Exception e) {
            logger.error("Unexpected error capturing PayPal order", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Unexpected error occurred",
                    ErrorCode.INTERNAL_ERROR.getCode())
            );
        }
    }
}