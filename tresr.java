package org.example;

import org.w3c.dom.*;

import javax.json.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CharlesChlsToWireMock {
    public static void main(String[] args) throws Exception {
        // Load and parse the XML file
        File xmlFile = new File("src/main/resources/xcUntitled.chlsx"); // Update with the correct path
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        // Get the transaction node
        NodeList transactionList = doc.getElementsByTagName("transaction");

        List<JsonObjectBuilder> stubs = new ArrayList<>();

        for (int i = 0; i < transactionList.getLength(); i++) {
            Element transaction = (Element) transactionList.item(i);

            // Get request details
            String method = transaction.getAttribute("method");
            String path = transaction.getAttribute("path");
            String requestBody = getRequestBody(transaction);
            JsonArrayBuilder bodyPatterns = Json.createArrayBuilder();

            // Extract JSON keys for request body matching
            if (requestBody != null) {
                // Dynamically generate body patterns from the request JSON
                bodyPatterns.add(createJsonPathObject("$.clienttype_id"));
                bodyPatterns.add(createJsonPathObject("$.dev_name"));
                bodyPatterns.add(createJsonPathObject("$.device_id"));
                bodyPatterns.add(createJsonPathObject("$.latitude"));
                bodyPatterns.add(createJsonPathObject("$.longitude"));
                bodyPatterns.add(createJsonPathObject("$.cd16"));
                bodyPatterns.add(createJsonPathObject("$.os"));
                bodyPatterns.add(createJsonPathObject("$.os_version"));
                bodyPatterns.add(createJsonPathObject("$.platform_id"));
                bodyPatterns.add(createJsonPathObject("$.release_id"));
                bodyPatterns.add(createJsonPathObject("$.encrypted"));
                bodyPatterns.add(createJsonPathObject("$.app_version"));
                bodyPatterns.add(createJsonPathObject("$.sypi_version"));
                bodyPatterns.add(createJsonPathObject("$.visit_id"));
                bodyPatterns.add(createJsonPathObject("$.gps_syf_profileid"));
                bodyPatterns.add(createJsonPathObject("$.ls"));
                bodyPatterns.add(createJsonPathObject("$.state.tag"));
                bodyPatterns.add(createJsonPathObject("$.state.gps_syf_profileid"));
                bodyPatterns.add(createJsonPathObject("$.state.request_type"));
            }

            // Get response details
            JsonObjectBuilder responseBuilder = Json.createObjectBuilder();
            responseBuilder.add("status", Integer.parseInt(transaction.getElementsByTagName("response").item(0).getAttributes().getNamedItem("status").getNodeValue()));

            // Set response headers
            JsonObjectBuilder headersBuilder = Json.createObjectBuilder();
            Element responseElement = (Element) transaction.getElementsByTagName("response").item(0); // Cast the 'response' node to an Element

            NodeList headers = responseElement.getElementsByTagName("header"); // Now use getElementsByTagName on the Element

            for (int j = 0; j < headers.getLength(); j++) {
                Node header = headers.item(j);
                if (header.getNodeType() == Node.ELEMENT_NODE) {
                    Element headerElement = (Element) header;
                    String name = headerElement.getAttribute("name"); // Get header name from 'name' attribute
                    String value = headerElement.getAttribute("value"); // Get header value from 'value' attribute

                    if (name != null && !name.isEmpty()) {
                        headersBuilder.add(name, value); // Only add if the name is valid
                    }
                }
            }
            responseBuilder.add("headers", headersBuilder);


            // Set response body
            String responseBody = getResponseBody(transaction);
            JsonObjectBuilder jsonBodyBuilder = extractBodyFromResponse(responseBody);

            responseBuilder.add("jsonBody", jsonBodyBuilder);

            // Construct the final WireMock stub
            JsonObjectBuilder stubBuilder = Json.createObjectBuilder();
            stubBuilder.add("request", Json.createObjectBuilder()
                    .add("method", method)
                    .add("url", path)
                    .add("bodyPatterns", bodyPatterns));
            stubBuilder.add("response", responseBuilder);

            stubs.add(stubBuilder);
        }

        // Write the output to a JSON file
        try (JsonWriter jsonWriter = Json.createWriter(new FileWriter("wiremock-stus.json"))) {
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for (JsonObjectBuilder stub : stubs) {
                arrayBuilder.add(stub.build());
            }
            jsonWriter.writeArray(arrayBuilder.build());
        }
    }

    private static String getRequestBody(Element transaction) {
        return transaction.getElementsByTagName("request").item(0).getChildNodes().item(1).getTextContent();
    }

    private static String getResponseBody(Element transaction) {
        return transaction.getElementsByTagName("response").item(0).getChildNodes().item(3).getTextContent();
    }

    private static JsonObjectBuilder extractBodyFromResponse(String responseBody) {
        JsonObjectBuilder responseBuilder = Json.createObjectBuilder();

        try {
            // Parse the response body as JSON directly
            JsonReader reader = Json.createReader(new StringReader(responseBody.trim()));
            JsonValue jsonValue = reader.read(); // Read the top-level JSON value

            if (jsonValue.getValueType() == JsonValue.ValueType.ARRAY) {
                JsonArray responseArray = (JsonArray) jsonValue;

                // Assuming the relevant data is in the first element of the array
                if (!responseArray.isEmpty()) {
                    JsonObject responseJson = responseArray.getJsonObject(0);
                    addJsonObjectFieldsToBuilder(responseJson, responseBuilder);
                }
            } else if (jsonValue.getValueType() == JsonValue.ValueType.OBJECT) {
                // If the response body is an object, handle it directly
                JsonObject responseJson = (JsonObject) jsonValue;
                addJsonObjectFieldsToBuilder(responseJson, responseBuilder);
            }

        } catch (Exception e) {
            // Handle parsing errors
            e.printStackTrace();
        }

        return responseBuilder;
    }

    // Helper method to add all fields of a JsonObject to a JsonObjectBuilder
    private static void addJsonObjectFieldsToBuilder(JsonObject jsonObject, JsonObjectBuilder jsonObjectBuilder) {
        for (String key : jsonObject.keySet()) {
            jsonObjectBuilder.add(key, jsonObject.get(key));
        }
    }

    private static JsonObjectBuilder createJsonPathObject (String jsonPath){
                return Json.createObjectBuilder().add("matchesJsonPath", jsonPath);
            }
        }
