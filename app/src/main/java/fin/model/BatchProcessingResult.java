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

    public BatchProcessingResult(int processedCount, int classifiedCount, int failedCount, boolean success) {
        this.processedCount = processedCount;
        this.classifiedCount = classifiedCount;
        this.failedCount = failedCount;
        this.success = success;
    }

    // Getters and setters
    public int getProcessedCount() { return processedCount; }
    public void setProcessedCount(int processedCount) { this.processedCount = processedCount; }

    public int getClassifiedCount() { return classifiedCount; }
    public void setClassifiedCount(int classifiedCount) { this.classifiedCount = classifiedCount; }

    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}