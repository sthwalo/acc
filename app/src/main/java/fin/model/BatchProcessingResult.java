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