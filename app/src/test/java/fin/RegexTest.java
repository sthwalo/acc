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