package fin.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates required environment variables at startup and fails fast with clear message if missing.
 */
@Component
public class RequiredEnvValidator {

    @Value("${JWT_SECRET:}")
    private String jwtSecret;

    @PostConstruct
    public void validate() {
        List<String> missing = new ArrayList<>();
        if (jwtSecret == null || jwtSecret.isBlank()) {
            missing.add("JWT_SECRET");
        }

        if (!missing.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Missing required environment variables: ");
            sb.append(String.join(", ", missing));
            sb.append(".\nPlease set them in your environment or in .env and export before running Gradle/bootRun.\n");
            sb.append("Example: export JWT_SECRET=some-secret && ./gradlew :app:bootRun\n");
            sb.append("Or: set -a && source .env && set +a && ./gradlew :app:bootRun (for bash/zsh)\n");
            throw new IllegalStateException(sb.toString());
        }
    }
}
