import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HarToWireMockStub {

    public static void main(String[] args) {
        String harFilePath = "path/to/your.har";
        String stubDirectory = "path/to/output/stubs";
        
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode harFile = objectMapper.readTree(new File(harFilePath));
            JsonNode entries = harFile.get("log").get("entries");

            for (JsonNode entry : entries) {
                JsonNode request = entry.get("request");
                JsonNode response = entry.get("response");

                String method = request.get("method").asText();
                String url = request.get("url").asText();
                int statusCode = response.get("status").asInt();
                String body = response.get("content").get("text").asText();

                StubMapping stubMapping = WireMock.stubFor(WireMock.request(method, WireMock.urlEqualTo(url))
                        .willReturn(WireMock.aResponse()
                                .withStatus(statusCode)
                                .withBody(body)));

                // Convert the stub mapping to JSON and save it
                String stubJson = stubMapping.toString();
                String outputFileName = stubDirectory + "/stub_" + method + "_" + url.hashCode() + ".json";
                Files.write(Paths.get(outputFileName), stubJson.getBytes());
                System.out.println("Stub created for: " + url);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;

public class CharlesToWireMockStubJson {

    public static void main(String[] args) throws IOException {
        // Load and parse the .chlsj file (Charles JSON file)
        ObjectMapper mapper = new ObjectMapper();
        File chlsjFile = new File("path/to/your/file.chlsj"); // Update with the correct path to your .chlsj file
        JsonNode charlesData = mapper.readTree(chlsjFile);

        // Prepare the root object for the JSON stub
        ObjectNode wiremockStub = mapper.createObjectNode();
        ObjectNode requestNode = mapper.createObjectNode();
        ObjectNode responseNode = mapper.createObjectNode();

        // Process each entry in the .chlsj log file
        for (JsonNode entry : charlesData) {
            // Extract request data
            JsonNode request = entry.get("request");
            String method = request.get("method").asText();
            String url = request.get("path").asText();
            JsonNode requestBody = request.get("body");

            // Set up request
            requestNode.put("method", method);
            requestNode.put("url", url);

            // Set up bodyPatterns using matchesJsonPath
            if (requestBody != null) {
                ObjectNode bodyPatternsNode = mapper.createObjectNode();
                bodyPatternsNode.put("matchesJsonPath", "$[?(@.clienttype_id == " + requestBody.get("clienttype_id").asInt() + ")]");
                requestNode.putArray("bodyPatterns").add(bodyPatternsNode);
            }

            // Extract response data
            JsonNode response = entry.get("response");
            int status = response.get("status").asInt();
            JsonNode responseBody = response.get("body");
            JsonNode responseHeaders = response.get("header").get("headers");

            // Set up response
            responseNode.put("status", status);
            ObjectNode headersNode = mapper.createObjectNode();
            for (JsonNode header : responseHeaders) {
                headersNode.put(header.get("name").asText(), header.get("value").asText());
            }
            responseNode.set("headers", headersNode);

            // Add the response body
            if (responseBody != null) {
                responseNode.set("jsonBody", responseBody.get("text"));
            }

            // Combine request and response into the stub
            wiremockStub.set("request", requestNode);
            wiremockStub.set("response", responseNode);

            // Output the JSON to a file
            try (FileWriter file = new FileWriter("wiremock-stub.json")) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, wiremockStub);
            }
        }

        System.out.println("WireMock stub generated as 'wiremock-stub.json'");
    }
}
