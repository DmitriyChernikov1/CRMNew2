import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
@ExtendWith(BaseTest.class)
public class StatusTasksTest {
    @Test
    @Description("Получение статуса задачи по UUID")
    @DisplayName("Успешное получение статуса задачи")
    public void getStatusTask_Success() {
        AuthTokenTest authService = new AuthTokenTest();
        String accessToken = authService.getAccessToken();

        String validUuid = "fd86640c-a261-4edf-b7e7-827215eca67d"; // Заменить на реальный UUID

        Response getStatusTask = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.14:9000/client-relations/tasks/status-task/" + validUuid)
                .andReturn();

        int statusCode = getStatusTask.getStatusCode();
        assertEquals(200, statusCode);

        String responseBody = getStatusTask.getBody().asString();
        assertNotNull(responseBody);
}
    @Test
    @Description("Получение статуса задачи - неавторизованный доступ")
    @DisplayName("Ошибка авторизации при получении статуса задачи")
    public void getStatusTask_Unauthorized() {
        String validUuid = "123e4567-e89b-12d3-a456-426614174000";

        Response getStatusTask = RestAssured
                .given()
                .headers("Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.14:9000/client-relations/tasks/status-task/" + validUuid)
                .andReturn();

        int statusCode = getStatusTask.getStatusCode();
        assertEquals(401, statusCode);
    }
    @Test
    @Description("Получение статуса задачи с неверным UUID")
    @DisplayName("Ошибка при неверном формате UUID")
    public void getStatusTask_InvalidUuid() {
        AuthTokenTest authService = new AuthTokenTest();
        String accessToken = authService.getAccessToken();

        String invalidUuid = "invalid-uuid-format";

        Response getStatusTask = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.14:9000/client-relations/tasks/status-task/" + invalidUuid)
                .andReturn();

        int statusCode = getStatusTask.getStatusCode();
        assertEquals(500, statusCode);
    }
    @Test
    @Description("Получение несуществующего статуса задачи")
    @DisplayName("Ошибка при запросе несуществующего статуса")
    public void getStatusTask_NotFound() {
        AuthTokenTest authService = new AuthTokenTest();
        String accessToken = authService.getAccessToken();

        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";

        Response getStatusTask = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.14:9000/client-relations/tasks/status-task/" + nonExistentUuid)
                .andReturn();

        int statusCode = getStatusTask.getStatusCode();
        assertEquals(500, statusCode);
    }
}
