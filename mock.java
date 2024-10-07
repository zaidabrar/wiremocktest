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


package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CharlesChlsToWireMock {

    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();
        File chlsjFile = new File("src/main/resources/ebilljsonsession.chlsj"); // Update with the correct path to your .chlsj file

        try {
            // Load and parse the .chlsj file (Charles JSON file)
            JsonNode charlesData = mapper.readTree(chlsjFile);
            ArrayNode wiremockStubs = mapper.createArrayNode(); // To hold multiple stubs

            // Process each entry in the .chlsj log file
            for (JsonNode entry : charlesData) {
                // Prepare the root object for the JSON stub
                ObjectNode wiremockStub = mapper.createObjectNode();
                ObjectNode requestNode = mapper.createObjectNode();
                ObjectNode responseNode = mapper.createObjectNode();

                // Extract request data
                JsonNode request = entry.get("request");
                if (request != null) {
                    String method = request.path("method").asText();
                    String url = request.path("path").asText();

                    // Set up request
                    requestNode.put("method", method);
                    requestNode.put("urlPath", url);

                    // Set up bodyPatterns if there is a body
                    JsonNode requestBody = request.path("body");
                    if (requestBody != null && requestBody.has("text")) {
                        String bodyText = requestBody.path("text").asText();
                        ObjectNode bodyPatternsNode = mapper.createObjectNode();
                        bodyPatternsNode.put("matchesJsonPath", "$[?(@.clienttype_id == " + bodyText + ")]");
                        requestNode.putArray("bodyPatterns").add(bodyPatternsNode);
                    }
                }

                // Extract response data
                JsonNode response = entry.get("response");
                if (response != null) {
                    int status = response.path("status").asInt();
                    JsonNode responseBody = response.path("body");

                    // Set up response
                    responseNode.put("status", status);
                    responseNode.put("statusText", response.path("header").path("headers").get(0).path("value").asText());

                    // Add the response body
                    if (responseBody != null && responseBody.has("text")) {
                        responseNode.set("jsonBody", responseBody.path("text"));
                    }

                    // Set up response headers
                    ObjectNode headersNode = mapper.createObjectNode();
                    JsonNode responseHeaders = response.path("header").path("headers");
                    if (responseHeaders != null) {
                        for (JsonNode header : responseHeaders) {
                            headersNode.put(header.path("name").asText(), header.path("value").asText());
                        }
                    }

                    responseNode.set("headers", headersNode);
                }

                // Combine request and response into the stub
                wiremockStub.set("request", requestNode);
                wiremockStub.set("response", responseNode);
                wiremockStubs.add(wiremockStub); // Add each stub to the array
            }

            // Output the JSON to a file
            try (FileWriter file = new FileWriter("wiremock-stubs.json")) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, wiremockStubs);
            }

            System.out.println("WireMock stubs generated as 'wiremock-stubs.json'");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
