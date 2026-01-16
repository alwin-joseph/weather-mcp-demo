package com.example.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Duration;
import java.util.Map;
import java.util.List;

public class VerifyClient {
    public static void main(String[] args) {
        try {
            System.out.println("Starting Verification Client...");

            // Path to the jar
            String jarPath = "/Users/alwinjoseph/Code/weather-mcp/target/weather-mcp-1.0-SNAPSHOT.jar";

            ServerParameters params = ServerParameters.builder("java")
                    .args(List.of("-jar", jarPath))
                    .build();

            JacksonMcpJsonMapper mapper = new JacksonMcpJsonMapper(new ObjectMapper());
            StdioClientTransport transport = new StdioClientTransport(params, mapper);

            McpSyncClient client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(10))
                    .build();

            System.out.println("Initializing client...");
            client.initialize();

            System.out.println("Listing tools...");
            var tools = client.listTools();
            System.out.println("Tools found: " + tools.tools().size());
            tools.tools().forEach(t -> System.out.println("- " + t.name()));

            System.out.println("Calling get-weather for Paris...");
            var result = client.callTool(new McpSchema.CallToolRequest(
                    "get-weather",
                    Map.of("city", "Paris")));

            System.out.println("Result: " + result.content());

            client.close();
            System.out.println("Verification successful.");

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
