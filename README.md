# weather-mcp-demo
Implementation and verification of a Weather MCP Server using java mcp SDKs


# Implementation Details
The server is implemented in Java using the io.modelcontextprotocol.sdk version 0.17.1. It exposes a single tool get-weather which accepts a city name and returns real-time weather data using the Open-Meteo API.

# Key Files
pom.xml : Maven configuration with MCP SDK and Jackson dependencies.
WeatherMcpServer.java : The main server application. It uses java.net.http.HttpClient to fetch weather data from api.open-meteo.com.
VerifyClient.java : A Java verification client that uses StdioClientTransport to test the server.

# Verification
The server was verified using a custom Java client that spawns the server process and communicates via Standard I/O using the MCP compliance/client SDK.

## Steps Performed
Build: The project was packaged into an executable JAR.

mvn clean package

## Verification Run: 
The VerifyClient executed the following steps:

Started the server JAR as a subprocess.
Initialized the MCP connection.
Listed available tools (Found get-weather).
Called get-weather with argument city="Paris".
Received live weather data.

# Claude Desktop verification 

Test with an MCP Client (REAL AI) in Claude Desktop
- Configure MCP in ~/Library/Application Support/Claude/claude_desktop_config.json 
- Add MCP Server : 
{
  "mcpServers": {
    "weather-mcp": {
      "command": "java",
      "args": [
        "-jar",
        "/ABSOLUTE_PATH/weather-mcp/target/weather-mcp-1.0-SNAPSHOT.jar"
      ]
    }
  }
}
- Restart Claude Desktop
- Test in Claude Chat
Ask: What is the weather in Singapore
Claude will:

Discover your MCP tool and provide the weather of Singapore.