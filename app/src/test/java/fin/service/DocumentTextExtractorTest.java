package fin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DocumentTextExtractorTest {
    private DocumentTextExtractor extractor;
    
    @BeforeEach
    void setUp() {
        extractor = new DocumentTextExtractor();
    }
    
    @Test
    void isTransaction_WithTransactionLine_ReturnsTrue() {
        String[] transactions = {
            "BALANCE BROUGHT FORWARD",
            "IB PAYMENT TO NTSAKO MAPHOSA",
            "FEE-ELECTRONIC ACCOUNT PAYMENT ## 8.90-",
            "SERVICE FEE  35.00-"
        };
        
        for (String transaction : transactions) {
            assertTrue(extractor.isTransaction(transaction),
                       "Should identify '" + transaction + "' as transaction");
        }
    }
    
    @Test
    void isTransaction_WithHeaderLine_ReturnsFalse() {
        String[] headers = {
            "BRAAMFONTEIN PO BOX 62325",
            "Statement No: 3",
            "VAT Reg. No: 4640268068",
            "Page 1 of 15",
            "## These fees include VAT"
        };
        
        for (String header : headers) {
            assertFalse(extractor.isTransaction(header), 
                      "Should not identify '" + header + "' as transaction");
        }
    }
}
