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

    public BatchProcessingStatistics(long initialTotalTransactions, long initialClassifiedTransactions, long initialUnclassifiedTransactions) {
        this.totalTransactions = initialTotalTransactions;
        this.classifiedTransactions = initialClassifiedTransactions;
        this.unclassifiedTransactions = initialUnclassifiedTransactions;
        this.classificationRate = initialTotalTransactions > 0 ? (double) initialClassifiedTransactions / initialTotalTransactions * PERCENTAGE_MULTIPLIER : 0.0;
    }

    // Getters and setters
    public long getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(long newTotalTransactions) { this.totalTransactions = newTotalTransactions; }

    public long getClassifiedTransactions() { return classifiedTransactions; }
    public void setClassifiedTransactions(long newClassifiedTransactions) { this.classifiedTransactions = newClassifiedTransactions; }

    public long getUnclassifiedTransactions() { return unclassifiedTransactions; }
    public void setUnclassifiedTransactions(long newUnclassifiedTransactions) { this.unclassifiedTransactions = newUnclassifiedTransactions; }

    public double getClassificationRate() { return classificationRate; }
    public void setClassificationRate(double newClassificationRate) { this.classificationRate = newClassificationRate; }
}