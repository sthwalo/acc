package fin.dto;

/**
 * Data class matching frontend expectations
 */
public class PayrollProcessingData {
    private final int processedEmployees;
    private final double totalGrossPay;
    private final double totalDeductions;
    private final double totalNetPay;
    private final int payslipsGenerated;

    public PayrollProcessingData(int processedEmployees, double totalGrossPay,
                               double totalDeductions, double totalNetPay, int payslipsGenerated) {
        this.processedEmployees = processedEmployees;
        this.totalGrossPay = totalGrossPay;
        this.totalDeductions = totalDeductions;
        this.totalNetPay = totalNetPay;
        this.payslipsGenerated = payslipsGenerated;
    }

    public int getProcessedEmployees() { return processedEmployees; }
    public double getTotalGrossPay() { return totalGrossPay; }
    public double getTotalDeductions() { return totalDeductions; }
    public double getTotalNetPay() { return totalNetPay; }
    public int getPayslipsGenerated() { return payslipsGenerated; }
}