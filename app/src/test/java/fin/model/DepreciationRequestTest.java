package fin.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;

/**
 * Comprehensive test suite for DepreciationRequest model
 * Tests builder pattern, validation, and all functionality
 */
class DepreciationRequestTest {

    private DepreciationRequest.Builder builder;

    @BeforeEach
    void setUp() {
        builder = new DepreciationRequest.Builder();
    }

    @Test
    void testBuilderCreation() {
        assertNotNull(builder);
    }

    @Test
    void testBuildWithRequiredFields() {
        DepreciationRequest request = builder
                .cost(new BigDecimal("10000.00"))
                .salvageValue(new BigDecimal("1000.00"))
                .usefulLife(5)
                .method(DepreciationMethod.STRAIGHT_LINE)
                .build();

        assertNotNull(request);
        assertEquals(new BigDecimal("10000.00"), request.getCost());
        assertEquals(new BigDecimal("1000.00"), request.getSalvageValue());
        assertEquals(5, request.getUsefulLife());
        assertEquals(DepreciationMethod.STRAIGHT_LINE, request.getMethod());
    }

    @Test
    void testBuildWithAllFields() {
        DepreciationRequest request = builder
                .cost(new BigDecimal("50000.00"))
                .salvageValue(new BigDecimal("5000.00"))
                .usefulLife(7)
                .method(DepreciationMethod.DECLINING_BALANCE)
                .dbFactor(new BigDecimal("2.0"))
                .convention("half-year")
                .build();

        assertNotNull(request);
        assertEquals(new BigDecimal("50000.00"), request.getCost());
        assertEquals(new BigDecimal("5000.00"), request.getSalvageValue());
        assertEquals(7, request.getUsefulLife());
        assertEquals(DepreciationMethod.DECLINING_BALANCE, request.getMethod());
        assertEquals(new BigDecimal("2.0"), request.getDbFactor());
        assertEquals("half-year", request.getConvention());
    }

    @Test
    void testDefaultValues() {
        DepreciationRequest request = builder
                .cost(new BigDecimal("10000.00"))
                .salvageValue(new BigDecimal("1000.00"))
                .usefulLife(5)
                .method(DepreciationMethod.STRAIGHT_LINE)
                .build();

        // Test default values
        assertEquals(new BigDecimal("1.0"), request.getDbFactor()); // Default DB factor
        assertNull(request.getConvention()); // Default convention is null
    }

    @Test
    void testValidationCostRequired() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            builder
                    .salvageValue(new BigDecimal("1000.00"))
                    .usefulLife(5)
                    .method(DepreciationMethod.STRAIGHT_LINE)
                    .build();
        });
        assertEquals("Cost is required", exception.getMessage());
    }

    @Test
    void testValidationSalvageValueRequired() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            builder
                    .cost(new BigDecimal("10000.00"))
                    .usefulLife(5)
                    .method(DepreciationMethod.STRAIGHT_LINE)
                    .build();
        });
        assertEquals("Salvage value is required", exception.getMessage());
    }

    @Test
    void testValidationUsefulLifeRequired() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            builder
                    .cost(new BigDecimal("10000.00"))
                    .salvageValue(new BigDecimal("1000.00"))
                    .method(DepreciationMethod.STRAIGHT_LINE)
                    .build();
        });
        assertEquals("Useful life is required", exception.getMessage());
    }

    @Test
    void testValidationMethodRequired() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            builder
                    .cost(new BigDecimal("10000.00"))
                    .salvageValue(new BigDecimal("1000.00"))
                    .usefulLife(5)
                    .build();
        });
        assertEquals("Depreciation method is required", exception.getMessage());
    }

    @Test
    void testValidationCostPositive() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            builder
                    .cost(new BigDecimal("-1000.00"))
                    .salvageValue(new BigDecimal("1000.00"))
                    .usefulLife(5)
                    .method(DepreciationMethod.STRAIGHT_LINE)
                    .build();
        });
        assertEquals("Cost must be positive", exception.getMessage());
    }

    @Test
    void testValidationSalvageValueNonNegative() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            builder
                    .cost(new BigDecimal("10000.00"))
                    .salvageValue(new BigDecimal("-1000.00"))
                    .usefulLife(5)
                    .method(DepreciationMethod.STRAIGHT_LINE)
                    .build();
        });
        assertEquals("Salvage value cannot be negative", exception.getMessage());
    }

    @Test
    void testValidationSalvageValueLessThanCost() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            builder
                    .cost(new BigDecimal("10000.00"))
                    .salvageValue(new BigDecimal("15000.00"))
                    .usefulLife(5)
                    .method(DepreciationMethod.STRAIGHT_LINE)
                    .build();
        });
        assertEquals("Salvage value cannot be greater than cost", exception.getMessage());
    }

    @Test
    void testValidationUsefulLifePositive() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            builder
                    .cost(new BigDecimal("10000.00"))
                    .salvageValue(new BigDecimal("1000.00"))
                    .usefulLife(0)
                    .method(DepreciationMethod.STRAIGHT_LINE)
                    .build();
        });
        assertEquals("Useful life must be positive", exception.getMessage());
    }

    @Test
    void testValidationUsefulLifeReasonable() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            builder
                    .cost(new BigDecimal("10000.00"))
                    .salvageValue(new BigDecimal("1000.00"))
                    .usefulLife(100)
                    .method(DepreciationMethod.STRAIGHT_LINE)
                    .build();
        });
        assertEquals("Useful life seems unreasonably long (max 50 years)", exception.getMessage());
    }

    @Test
    void testValidationDbFactorForDecliningBalance() {
        // Should work with DB method
        DepreciationRequest request = builder
                .cost(new BigDecimal("10000.00"))
                .salvageValue(new BigDecimal("1000.00"))
                .usefulLife(5)
                .method(DepreciationMethod.DECLINING_BALANCE)
                .dbFactor(new BigDecimal("1.5"))
                .build();

        assertEquals(new BigDecimal("1.5"), request.getDbFactor());
    }

    @Test
    void testValidationDbFactorPositive() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            builder
                    .cost(new BigDecimal("10000.00"))
                    .salvageValue(new BigDecimal("1000.00"))
                    .usefulLife(5)
                    .method(DepreciationMethod.DECLINING_BALANCE)
                    .dbFactor(new BigDecimal("0.0"))
                    .build();
        });
        assertEquals("Declining balance factor must be positive", exception.getMessage());
    }

    // @Test
    // void testToString() {
    //     DepreciationRequest request = builder
    //             .cost(new BigDecimal("10000.00"))
    //             .salvageValue(new BigDecimal("1000.00"))
    //             .usefulLife(5)
    //             .method(DepreciationMethod.STRAIGHT_LINE)
    //             .build();

    //     String toString = request.toString();
    //     assertNotNull(toString);
    //     assertTrue(toString.startsWith("DepreciationRequest{"));
    //     assertTrue(toString.contains("cost=10000"));
    //     assertTrue(toString.contains("method=STRAIGHT_LINE"));
    // }

    @Test
    void testEqualsAndHashCode() {
        DepreciationRequest request1 = builder
                .cost(new BigDecimal("10000.00"))
                .salvageValue(new BigDecimal("1000.00"))
                .usefulLife(5)
                .method(DepreciationMethod.STRAIGHT_LINE)
                .build();

        DepreciationRequest request2 = builder
                .cost(new BigDecimal("10000.00"))
                .salvageValue(new BigDecimal("1000.00"))
                .usefulLife(5)
                .method(DepreciationMethod.STRAIGHT_LINE)
                .build();

        DepreciationRequest request3 = builder
                .cost(new BigDecimal("20000.00"))
                .salvageValue(new BigDecimal("1000.00"))
                .usefulLife(5)
                .method(DepreciationMethod.STRAIGHT_LINE)
                .build();

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotEquals(request1, request3);
    }
}