import java.net.http.*;
import java.net.URI;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class WebhookApp {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        String regNo = args.length > 0 ? args[0] : "REG12347";
        System.out.println("Starting with regNo: " + regNo);
        
        // Generate webhook
        String[] response = generateWebhook(regNo);
        String webhookUrl = response[0];
        String accessToken = response[1];
        
        // Get SQL query
        String sqlQuery = getSqlQuery(regNo);
        System.out.println("SQL Query: " + sqlQuery);
        
        // Submit solution
        submitSolution(webhookUrl, accessToken, sqlQuery);
        System.out.println("Done!");
    }

    private static String[] generateWebhook(String regNo) throws Exception {
        String json = String.format(
            "{\"name\":\"John Doe\",\"regNo\":\"%s\",\"email\":\"john@example.com\"}", 
            regNo);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode node = mapper.readTree(response.body());
        
        return new String[]{
            node.get("webhook").asText(),
            node.get("accessToken").asText()
        };
    }

    private static String getSqlQuery(String regNo) {
        String digits = regNo.replaceAll("\\D", "");
        int lastTwo = Integer.parseInt(digits.substring(digits.length() - 2));
        
        if (lastTwo % 2 == 1) {
            return "SELECT p.AMOUNT as SALARY, CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) as NAME, TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) as AGE, d.DEPARTMENT_NAME FROM PAYMENTS p JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID WHERE DAY(p.PAYMENT_TIME) != 1 ORDER BY p.AMOUNT DESC LIMIT 1";
        } else {
            return "SELECT e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME, COUNT(e2.EMP_ID) as YOUNGER_EMPLOYEES_COUNT FROM EMPLOYEE e1 JOIN DEPARTMENT d ON e1.DEPARTMENT = d.DEPARTMENT_ID LEFT JOIN EMPLOYEE e2 ON e1.DEPARTMENT = e2.DEPARTMENT AND e2.DOB > e1.DOB GROUP BY e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME ORDER BY e1.EMP_ID DESC";
        }
    }

    private static void submitSolution(String webhookUrl, String accessToken, String sqlQuery) throws Exception {
        String json = String.format("{\"finalQuery\":\"%s\"}", 
            sqlQuery.replace("\"", "\\\""));
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(webhookUrl))
            .header("Content-Type", "application/json")
            .header("Authorization", accessToken)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Response: " + response.body());
    }
}
