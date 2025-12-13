/*
 * FIN Financial Management System
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * Licensed under Apache License 2.0 - Commercial use requires separate licensing
 */
package fin.service;

import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * PayPal payment processing service
 * Handles order creation and capture operations
 */
@Service
public class PayPalService {

    private static final Logger logger = LoggerFactory.getLogger(PayPalService.class);

    private final PayPalHttpClient payPalHttpClient;

    @Autowired
    public PayPalService(PayPalHttpClient payPalHttpClient) {
        this.payPalHttpClient = payPalHttpClient;
    }

    /**
     * Create a PayPal order for payment
     *
     * @param amount The payment amount
     * @param currency The currency code (e.g., "ZAR", "USD")
     * @param description Description of the purchase
     * @return Order object with PayPal order details
     * @throws IOException if the API call fails
     */
    public Order createOrder(BigDecimal amount, String currency, String description) throws IOException {
        logger.info("Creating PayPal order for amount: {} {}", amount, currency);

        OrdersCreateRequest request = new OrdersCreateRequest();
        request.prefer("return=representation");

        request.requestBody(buildOrderRequest(amount, currency, description));

        try {
            HttpResponse<Order> response = payPalHttpClient.execute(request);
            Order order = response.result();

            logger.info("PayPal order created successfully. Order ID: {}", order.id());
            return order;

        } catch (Exception e) {
            logger.error("Failed to create PayPal order", e);
            throw new IOException("Failed to create PayPal order: " + e.getMessage(), e);
        }
    }

    /**
     * Capture a PayPal order after user approval
     *
     * @param orderId The PayPal order ID to capture
     * @return Capture object with payment details
     * @throws IOException if the API call fails
     */
    public Order captureOrder(String orderId) throws IOException {
        logger.info("Capturing PayPal order: {}", orderId);

        OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
        request.prefer("return=representation");

        try {
            HttpResponse<Order> response = payPalHttpClient.execute(request);
            Order order = response.result();

            logger.info("PayPal order captured successfully. Order ID: {}", order.id());
            return order;

        } catch (Exception e) {
            logger.error("Failed to capture PayPal order: {}", orderId, e);
            throw new IOException("Failed to capture PayPal order: " + e.getMessage(), e);
        }
    }

    /**
     * Build the order request body
     */
    private OrderRequest buildOrderRequest(BigDecimal amount, String currency, String description) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");

        List<PurchaseUnitRequest> purchaseUnits = new ArrayList<>();
        PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest();

        // Set amount
        AmountWithBreakdown amountWithBreakdown = new AmountWithBreakdown();
        amountWithBreakdown.currencyCode(currency);
        amountWithBreakdown.value(amount.toString());
        purchaseUnit.amountWithBreakdown(amountWithBreakdown);

        // Set description
        purchaseUnit.description(description);

        purchaseUnits.add(purchaseUnit);
        orderRequest.purchaseUnits(purchaseUnits);

        return orderRequest;
    }

    /**
     * Get order details by ID
     *
     * @param orderId The PayPal order ID
     * @return Order object with details
     * @throws IOException if the API call fails
     */
    public Order getOrder(String orderId) throws IOException {
        logger.info("Getting PayPal order details: {}", orderId);

        OrdersGetRequest request = new OrdersGetRequest(orderId);

        try {
            HttpResponse<Order> response = payPalHttpClient.execute(request);
            return response.result();

        } catch (Exception e) {
            logger.error("Failed to get PayPal order: {}", orderId, e);
            throw new IOException("Failed to get PayPal order: " + e.getMessage(), e);
        }
    }
}