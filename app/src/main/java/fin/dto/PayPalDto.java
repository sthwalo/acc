/*
 * FIN Financial Management System
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * Licensed under Apache License 2.0 - Commercial use requires separate licensing
 */
package fin.dto;

import java.math.BigDecimal;

/**
 * PayPal payment related DTOs
 */
public class PayPalDto {

    /**
     * Request DTO for creating PayPal orders
     */
    public static class CreateOrderRequest {
        private BigDecimal amount;
        private String currency;
        private String description;
        private Long planId; // Optional: link to pricing plan

        public CreateOrderRequest() {}

        public CreateOrderRequest(BigDecimal amount, String currency, String description, Long planId) {
            this.amount = amount;
            this.currency = currency;
            this.description = description;
            this.planId = planId;
        }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Long getPlanId() { return planId; }
        public void setPlanId(Long planId) { this.planId = planId; }
    }

    /**
     * Request DTO for capturing PayPal orders
     */
    public static class CaptureOrderRequest {
        private String orderId;
        private Long planId; // Optional: for linking to plan after payment

        public CaptureOrderRequest() {}

        public CaptureOrderRequest(String orderId, Long planId) {
            this.orderId = orderId;
            this.planId = planId;
        }

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public Long getPlanId() { return planId; }
        public void setPlanId(Long planId) { this.planId = planId; }
    }

    /**
     * Response DTO for order creation
     */
    public static class OrderResponse {
        private String orderId;
        private String status;
        private String approvalUrl;
        private boolean dummy;

        public OrderResponse() {}

        public OrderResponse(String orderId, String status, String approvalUrl) {
            this.orderId = orderId;
            this.status = status;
            this.approvalUrl = approvalUrl;
        }

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getApprovalUrl() { return approvalUrl; }
        public void setApprovalUrl(String approvalUrl) { this.approvalUrl = approvalUrl; }

        public boolean isDummy() { return dummy; }
        public void setDummy(boolean dummy) { this.dummy = dummy; }
    }

    /**
     * Response DTO for order capture
     */
    public static class CaptureResponse {
        private String orderId;
        private String status;
        private boolean completed;
        private String captureId;
        private BigDecimal amount;
        private String currency;

        public CaptureResponse() {}

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }

        public String getCaptureId() { return captureId; }
        public void setCaptureId(String captureId) { this.captureId = captureId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }
}