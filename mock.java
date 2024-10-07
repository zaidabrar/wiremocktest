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
