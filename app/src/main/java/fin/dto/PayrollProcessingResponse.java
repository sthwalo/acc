package fin.dto;

/**
 * Response class matching frontend PayrollProcessingResult interface
 */
public class PayrollProcessingResponse {
    private final boolean success;
    private final String message;
    private final PayrollProcessingData data;

    public PayrollProcessingResponse(boolean success, String message, PayrollProcessingData data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public PayrollProcessingData getData() { return data; }
}