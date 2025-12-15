package fin.codequality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

public class ErrorStatusMismatchTest {

    @Test
    void detectBadRequestWithInternalError() throws IOException {
        List<String> violations = new ArrayList<>();
        Path src = Paths.get("app/src/main/java");
        try (Stream<Path> files = Files.walk(src)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
                try {
                    List<String> lines = Files.readAllLines(path);
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        if (line.contains("badRequest().body(")) {
                            // inspect this line and the following three lines for INTERNAL_ERROR
                            boolean found = false;
                            for (int j = i; j < Math.min(lines.size(), i + 4); j++) {
                                if (lines.get(j).contains("INTERNAL_ERROR")) {
                                    found = true;
                                    break;
                                }
                            }
                            if (found) {
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
            StringBuilder msg = new StringBuilder("Found badRequest() responses paired with INTERNAL_ERROR (should be 500):\n");
            violations.forEach(v -> msg.append(v).append('\n'));
            fail(msg.toString());
        }
    }
}
