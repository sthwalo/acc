package fin.model;

/**
 * Result of batch processing operations
 */
public class BatchProcessingResult {
    private int processedCount;
    private int classifiedCount;
    private int failedCount;
    private boolean success;

    public BatchProcessingResult() {}

    public BatchProcessingResult(int initialProcessedCount, int initialClassifiedCount, int initialFailedCount, boolean initialSuccess) {
        this.processedCount = initialProcessedCount;
        this.classifiedCount = initialClassifiedCount;
        this.failedCount = initialFailedCount;
        this.success = initialSuccess;
    }

    // Getters and setters
    public int getProcessedCount() { return processedCount; }
    public void setProcessedCount(int newProcessedCount) { this.processedCount = newProcessedCount; }

    public int getClassifiedCount() { return classifiedCount; }
    public void setClassifiedCount(int newClassifiedCount) { this.classifiedCount = newClassifiedCount; }

    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int newFailedCount) { this.failedCount = newFailedCount; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean newSuccess) { this.success = newSuccess; }
}