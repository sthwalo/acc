package fin;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class RegexTest {
    public static void main(String[] args) {
        // Test line with two brackets (two-column layout)
        String testLine = "R 25,079   -   R 25,179   R 301,548   R 3,517   R 2,730   R 2,468   R 30,129   -   R 30,229   R 362,148    R 4,830    R 4,043   R 3,781";

        // Regex for two-column layout: captures both brackets
        // Pattern: R lower1 - R upper1 R annual1 R tax1_under65 R tax1_65-74 R tax1_over75 R lower2 - R upper2 R annual2 R tax2_under65 R tax2_65-74 R tax2_over75
        Pattern pattern = Pattern.compile(
            "R\\s*(\\d+[,\\d]*)\\s*-\\s*R\\s*(\\d+[,\\d]*)\\s*R\\s*\\d+[,\\d]*\\s*R\\s*(\\d+[,\\d]*)\\s*R\\s*\\d+[,\\d]*\\s*R\\s*\\d+[,\\d]*\\s*" +
            "R\\s*(\\d+[,\\d]*)\\s*-\\s*R\\s*(\\d+[,\\d]*)\\s*R\\s*\\d+[,\\d]*\\s*R\\s*(\\d+[,\\d]*)"
        );

        System.out.println("Testing two-column line:");
        Matcher matcher = pattern.matcher(testLine);
        if (matcher.find()) {
            System.out.println("First bracket:");
            System.out.println("  Group 1 (lower1): " + matcher.group(1));
            System.out.println("  Group 2 (upper1): " + matcher.group(2));
            System.out.println("  Group 3 (tax1): " + matcher.group(3));

            System.out.println("Second bracket:");
            System.out.println("  Group 4 (lower2): " + matcher.group(4));
            System.out.println("  Group 5 (upper2): " + matcher.group(5));
            System.out.println("  Group 6 (tax2): " + matcher.group(6));
        } else {
            System.out.println("No match!");
        }
    }
}