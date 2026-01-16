package com.example.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class WeatherMcpServer {

        private static final Logger logger = LoggerFactory.getLogger(WeatherMcpServer.class);
        private static final HttpClient httpClient = HttpClient.newHttpClient();
        private static final ObjectMapper objectMapper = new ObjectMapper();

        public static void main(String[] args) {
                System.setProperty("org.slf4j.simpleLogger.logFile", "System.err");
                logger.info("Starting Weather MCP Server...");

                try {
                        JacksonMcpJsonMapper mapper = new JacksonMcpJsonMapper(objectMapper);
                        StdioServerTransportProvider transport = new StdioServerTransportProvider(mapper);

                        // Define Tool Input Schema
                        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
                                        "object",
                                        Map.of(
                                                        "city",
                                                        Map.of("type", "string", "description",
                                                                        "The name of the city")),
                                        List.of("city"),
                                        false,
                                        null,
                                        null);

                        // Define Tool
                        McpSchema.Tool tool = new McpSchema.Tool(
                                        "get-weather",
                                        "Get weather",
                                        "Get the current weather for a specific city",
                                        inputSchema,
                                        null,
                                        null,
                                        null);

                        var server = McpServer.sync(transport)
                                        .serverInfo("weather-mcp", "1.0.0")
                                        .capabilities(McpSchema.ServerCapabilities.builder()
                                                        .tools(false)
                                                        .build())
                                        .tool(tool, (exchange, arguments) -> {
                                                String city = "Unknown";
                                                if (arguments.containsKey("city")) {
                                                        city = String.valueOf(arguments.get("city"));
                                                }

                                                logger.info("Getting weather for city: {}", city);

                                                try {
                                                        // 1. Geocoding
                                                        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                                                                        +
                                                                        java.net.URLEncoder.encode(city,
                                                                                        java.nio.charset.StandardCharsets.UTF_8)
                                                                        +
                                                                        "&count=1&language=en&format=json";

                                                        HttpRequest geoRequest = HttpRequest.newBuilder()
                                                                        .uri(URI.create(geoUrl)).build();
                                                        HttpResponse<String> geoResponse = httpClient.send(geoRequest,
                                                                        HttpResponse.BodyHandlers.ofString());

                                                        Map<String, Object> geoData = objectMapper
                                                                        .readValue(geoResponse.body(), Map.class);
                                                        ListResults results = objectMapper.convertValue(
                                                                        geoData.get("results"), ListResults.class);

                                                        if (results == null || results.isEmpty()) {
                                                                return new McpSchema.CallToolResult(
                                                                                List.of(new McpSchema.TextContent(
                                                                                                "City not found: "
                                                                                                                + city)),
                                                                                true);
                                                        }

                                                        Map<String, Object> firstResult = (Map<String, Object>) results
                                                                        .get(0);
                                                        double lat = ((Number) firstResult.get("latitude"))
                                                                        .doubleValue();
                                                        double lon = ((Number) firstResult.get("longitude"))
                                                                        .doubleValue();
                                                        String name = (String) firstResult.get("name");
                                                        String country = (String) firstResult.get("country");

                                                        // 2. Weather
                                                        String weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude="
                                                                        + lat +
                                                                        "&longitude=" + lon + "&current_weather=true";

                                                        HttpRequest weatherRequest = HttpRequest.newBuilder()
                                                                        .uri(URI.create(weatherUrl)).build();
                                                        HttpResponse<String> weatherResponse = httpClient.send(
                                                                        weatherRequest,
                                                                        HttpResponse.BodyHandlers.ofString());

                                                        Map<String, Object> weatherData = objectMapper
                                                                        .readValue(weatherResponse.body(), Map.class);
                                                        Map<String, Object> current = (Map<String, Object>) weatherData
                                                                        .get("current_weather");

                                                        double temperature = ((Number) current.get("temperature"))
                                                                        .doubleValue();
                                                        double windSpeed = ((Number) current.get("windspeed"))
                                                                        .doubleValue();

                                                        String content = String.format(
                                                                        "Current weather in %s, %s: %.1f°C, Wind speed: %.1f km/h",
                                                                        name, country, temperature, windSpeed);

                                                        return new McpSchema.CallToolResult(
                                                                        List.of(new McpSchema.TextContent(content)),
                                                                        false);

                                                } catch (Exception e) {
                                                        logger.error("Error fetching weather", e);
                                                        return new McpSchema.CallToolResult(
                                                                        List.of(new McpSchema.TextContent(
                                                                                        "Error fetching weather: " + e
                                                                                                        .getMessage())),
                                                                        true);
                                                }
                                        })
                                        .build();

                        logger.info("Server initialized. Listening on stdio.");

                        // Keep main thread alive
                        new CountDownLatch(1).await();

                } catch (Exception e) {
                        logger.error("Error starting server", e);
                        System.exit(1);
                }
        }

        // Helper interface for Jackson conversion
        private static class ListResults extends java.util.ArrayList<Object> {
        }
}
