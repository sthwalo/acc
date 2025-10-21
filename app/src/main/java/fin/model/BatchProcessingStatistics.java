package fin.model;

/**
 * Statistics for batch processing operations
 */
public class BatchProcessingStatistics {
    // Constants for percentage calculations
    private static final double PERCENTAGE_MULTIPLIER = 100.0;

    private long totalTransactions;
    private long classifiedTransactions;
    private long unclassifiedTransactions;
    private double classificationRate;

    public BatchProcessingStatistics() {}

    public BatchProcessingStatistics(long totalTransactions, long classifiedTransactions, long unclassifiedTransactions) {
        this.totalTransactions = totalTransactions;
        this.classifiedTransactions = classifiedTransactions;
        this.unclassifiedTransactions = unclassifiedTransactions;
        this.classificationRate = totalTransactions > 0 ? (double) classifiedTransactions / totalTransactions * PERCENTAGE_MULTIPLIER : 0.0;
    }

    // Getters and setters
    public long getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(long totalTransactions) { this.totalTransactions = totalTransactions; }

    public long getClassifiedTransactions() { return classifiedTransactions; }
    public void setClassifiedTransactions(long classifiedTransactions) { this.classifiedTransactions = classifiedTransactions; }

    public long getUnclassifiedTransactions() { return unclassifiedTransactions; }
    public void setUnclassifiedTransactions(long unclassifiedTransactions) { this.unclassifiedTransactions = unclassifiedTransactions; }

    public double getClassificationRate() { return classificationRate; }
    public void setClassificationRate(double classificationRate) { this.classificationRate = classificationRate; }
}