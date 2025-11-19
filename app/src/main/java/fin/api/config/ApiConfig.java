package fin.api.config;

import fin.api.util.ApiConstants;
import spark.Spark;

/**
 * API Configuration class for server setup.
 * Handles server port configuration and basic Spark setup.
 */
public class ApiConfig {

    /**
     * Configures the API server settings.
     * Sets the server port and basic Spark configuration.
     */
    public static void configure() {
        Spark.port(ApiConstants.SERVER_PORT);
    }
}