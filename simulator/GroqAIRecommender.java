package simulator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.logging.Level;

public class GroqAIRecommender {
    private static final Logger LOGGER = Logger.getLogger(GroqAIRecommender.class.getName());
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final int TIMEOUT_SECONDS = 30;
    private final String apiKey;
    private final HttpClient httpClient;

    public GroqAIRecommender() {
        String key = System.getenv("GROQ_API_KEY");
        if (key == null || key.trim().isEmpty()) {
            key = readApiKeyFromFile();
        }
        if (key == null || key.trim().isEmpty()) {
            System.err.println("ERROR: Groq API key not found. Set GROQ_API_KEY env variable or create groq_api_key.txt in project root or simulator/ directory.");
        }
        this.apiKey = key;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    private String readApiKeyFromFile() {
        String[] paths = {"groq_api_key.txt", "simulator/groq_api_key.txt"};
        for (String path : paths) {
            try (Scanner scanner = new Scanner(new File(path))) {
                if (scanner.hasNextLine()) {
                    String key = scanner.nextLine().trim();
                    if (!key.isEmpty()) {
                        LOGGER.info("Loaded Groq API key from file: " + path);
                        return key;
                    }
                }
            } catch (FileNotFoundException e) {
                // Ignore and try next
            }
        }
        return null;
    }

    public CompletableFuture<AIRecommendation> recommendAlgorithm(String scenario) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = buildPrompt(scenario);
                String response = callGroqAPI(prompt);
                return parseResponse(response);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error getting AI recommendation", e);
                return new AIRecommendation("Greedy", "Error occurred while getting AI recommendation. Using default algorithm.", false);
            }
        });
    }

    private String buildPrompt(String scenario) {
        return String.format("""
You are an expert in queueing and scheduling for banks.

Available algorithms:
1. Greedy (Least Finish Time): Assigns to the teller who will finish earliest. Can cause workload imbalance if transaction times vary.
2. Round Robin: Assigns tellers in cyclic order.
3. Least Work Left: Assigns to the teller with the minimum total workload (current + queued). This is best when transaction times are random or bursty.

Examples:
Scenario: 5 tellers, random transaction times, average-length queues.
Correct answer: Least Work Left

Scenario: 6 tellers, bursty arrivals, transaction times 2-25 units.
Correct answer: Least Work Left

Scenario: 3 tellers, all transactions are exactly 5 units.
Correct answer: Greedy

Scenario: 4 tellers, each customer always takes exactly 10 units, arrivals are steady.
Correct answer: Greedy

Scenario: 8 tellers, queue is almost always empty, all jobs are short and similar.
Correct answer: Greedy

Scenario: 5 tellers, some customers take 2 units, some take 50 units, arrivals are random.
Correct answer: Least Work Left

Scenario: 7 tellers, arrivals are in bursts (e.g., lunch hour), transaction times are unpredictable.
Correct answer: Least Work Left

Do NOT choose Greedy if transaction times are random or bursty, or if fairness is important.

For each scenario, think step by step about why each algorithm would or would not work, then pick the best.

User scenario:
%s

Which algorithm is best and why? Respond ONLY with a JSON object:
{
  "algorithm": "Greedy|Round Robin|Least Work Left",
  "explanation": "..."
}
""", scenario);
    }

    private String callGroqAPI(String prompt) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("GROQ_API_KEY environment variable is not set and groq_api_key.txt not found");
        }
        String requestBody = "{" +
                "\"model\": \"llama3-70b-8192\"," +
                "\"messages\": [" +
                "  {\"role\": \"system\", \"content\": \"You are a helpful AI assistant.\"}," +
                "  {\"role\": \"user\", \"content\": " + escapeJsonString(prompt) + "}" +
                "]," +
                "\"max_tokens\": 512" +
                "}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        LOGGER.info("Sending request to Groq API...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("API request failed with status: " + response.statusCode() + ", body: " + response.body());
        }
        LOGGER.info("Received response from Groq API");
        return response.body();
    }

    private String escapeJsonString(String text) {
        return "\"" + text.replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r")
                            .replace("\t", "\\t") + "\"";
    }

    private AIRecommendation parseResponse(String responseBody) {
        try {
            LOGGER.info("Full Groq API response: " + responseBody);
            // Extract the full content field value (handles multiline and embedded quotes)
            String content = extractContentField(responseBody);
            LOGGER.info("Raw AI response content (escaped): " + content);
            // Unescape common JSON escapes
            content = content.replace("\\n", "\n").replace("\\\"", "\"");
            LOGGER.info("Raw AI response content (unescaped): " + content);
            // Remove code block markers if present
            if (content.trim().startsWith("```")) {
                int firstNewline = content.indexOf('\n');
                if (firstNewline != -1) {
                    content = content.substring(firstNewline + 1);
                }
                if (content.trim().endsWith("```")) {
                    content = content.substring(0, content.lastIndexOf("```"));
                }
            }
            // Remove leading/trailing backslashes and whitespace
            content = content.replaceAll("^\\+|\\+$", "").trim();
            // Try to extract JSON from the content
            String jsonText = extractJSONFromText(content);
            String algorithm = extractJsonValue(jsonText, "algorithm");
            String explanation = extractJsonValue(jsonText, "explanation");
            if (!isValidAlgorithm(algorithm)) {
                throw new RuntimeException("Invalid algorithm name: " + algorithm);
            }
            return new AIRecommendation(algorithm, explanation, true);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse AI response, using fallback", e);
            return new AIRecommendation("Greedy", "Failed to parse AI response. Using default algorithm.", false);
        }
    }

    // Extracts the full value of the "content" field from the Groq API response, handling multiline and embedded quotes
    private String extractContentField(String responseBody) {
        String marker = "\"content\":";
        int idx = responseBody.indexOf(marker);
        if (idx == -1) throw new RuntimeException("No content field in response");
        int start = responseBody.indexOf('"', idx + marker.length());
        if (start == -1) throw new RuntimeException("No opening quote for content field");
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = start + 1; i < responseBody.length(); i++) {
            char c = responseBody.charAt(i);
            if (escape) {
                sb.append(c);
                escape = false;
            } else if (c == '\\') {
                sb.append(c);
                escape = true;
            } else if (c == '"') {
                // End of string unless escaped
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String extractJSONFromText(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        LOGGER.warning("No JSON object found in AI response content: " + text);
        throw new RuntimeException("No JSON object found in response");
    }

    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int keyIndex = json.indexOf(pattern);
        if (keyIndex == -1) {
            throw new RuntimeException("Key '" + key + "' not found in JSON");
        }
        int startQuote = json.indexOf('"', keyIndex + pattern.length());
        int endQuote = json.indexOf('"', startQuote + 1);
        return json.substring(startQuote + 1, endQuote);
    }

    private boolean isValidAlgorithm(String algorithm) {
        return algorithm.equals("Greedy") || 
               algorithm.equals("Round Robin") || 
               algorithm.equals("Least Work Left");
    }

    public static String mapAlgorithmToDisplayName(String algorithm) {
        switch (algorithm) {
            case "Greedy":
                return "Greedy (Least Finish Time)";
            case "Round Robin":
                return "Round Robin";
            case "Least Work Left":
                return "Least Work Left";
            default:
                return algorithm;
        }
    }

    public static String mapDisplayNameToAlgorithm(String displayName) {
        switch (displayName) {
            case "Greedy (Least Finish Time)":
                return "Greedy";
            case "Round Robin":
                return "Round Robin";
            case "Least Work Left":
                return "Least Work Left";
            default:
                return displayName;
        }
    }

    public static class AIRecommendation {
        private final String algorithm;
        private final String explanation;
        private final boolean success;
        public AIRecommendation(String algorithm, String explanation, boolean success) {
            this.algorithm = algorithm;
            this.explanation = explanation;
            this.success = success;
        }
        public String getAlgorithm() { return algorithm; }
        public String getExplanation() { return explanation; }
        public boolean isSuccess() { return success; }
        @Override
        public String toString() {
            return String.format("AIRecommendation{algorithm='%s', explanation='%s', success=%s}", 
                               algorithm, explanation, success);
        }
    }
} 