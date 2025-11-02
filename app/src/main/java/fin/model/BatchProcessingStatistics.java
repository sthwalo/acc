/*
 * FIN Financial Management System
 * 
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 * 
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
 * 
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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