package fin.codequality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

public class ResourceLeakCheckTest {

    private static final Pattern RESULTSET_ASSIGN_PATTERN = Pattern.compile("\\bResultSet\\s+\\w+\\s*=\\s*.*executeQuery\\(");

    @Test
    void detectRawResultSetAssignmentsNotInTryWithResources() throws IOException {
        List<String> violations = new ArrayList<>();
        Path src = Paths.get("app/src/main/java");
        try (Stream<Path> files = Files.walk(src)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
                try {
                    List<String> lines = Files.readAllLines(path);
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        if (RESULTSET_ASSIGN_PATTERN.matcher(line).find()) {
                            boolean ok = false;
                            // check current and previous 2 lines for try-with-resources start
                            for (int j = Math.max(0, i - 2); j <= i; j++) {
                                if (lines.get(j).contains("try (")) {
                                    ok = true;
                                    break;
                                }
                            }
                            if (!ok) {
                                violations.add(path + ":" + (i + 1) + " -> " + line.trim());
                            }
                        }
                    }
                } catch (IOException e) {
                    // ignore
                }
            });
        }

        if (!violations.isEmpty()) {
            StringBuilder msg = new StringBuilder("Found potential ResultSet usages not protected by try-with-resources:\n");
            violations.forEach(v -> msg.append(v).append('\n'));
            fail(msg.toString());
        }
    }
}
