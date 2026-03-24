import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BaseTest.class)
public class StatusTasksTest {

    private static String accessToken;
    private static final String BASE_URL = "http://172.20.207.14:9000";
    private static final String STATUS_TASK_PATH = "/client-relations/tasks/status-task/";

    private static final String VALID_UUID = "fd86640c-a261-4edf-b7e7-827215eca67d";
    private static final String NON_EXISTENT_UUID = "00000000-0000-0000-0000-000000000000";
    private static final String INVALID_UUID = "invalid-uuid-format";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        accessToken = new AuthTokenTest().getAccessToken();
    }

    private RequestSpecification authRequest() {
        return RestAssured.given()
                .baseUri(BASE_URL)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .header("Accept-Language", "ru,ru-RU;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Authorization", "Bearer " + accessToken);
    }

    // ─────────────────────────────────────────────
    // Статус задачи (Status Task)
    // ─────────────────────────────────────────────

    @Test
    @Description("Получение статуса задачи по UUID")
    @DisplayName("Успешное получение статуса задачи")
    public void getStatusTask_Success() {
        Response response = authRequest()
                .get(STATUS_TASK_PATH + VALID_UUID)
                .andReturn();

        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody().asString(), "Тело ответа не должно быть null");
    }

    @Test
    @Description("Получение статуса задачи - неавторизованный доступ")
    @DisplayName("Ошибка авторизации при получении статуса задачи")
    public void getStatusTask_Unauthorized() {
        new AuthTokenTest().baseRequest()
                .get(BASE_URL + STATUS_TASK_PATH + VALID_UUID)
                .then()
                .statusCode(401);
    }

    @Test
    @Description("Получение статуса задачи с неверным UUID")
    @DisplayName("Ошибка при неверном формате UUID")
    public void getStatusTask_InvalidUuid() {
        authRequest()
                .get(STATUS_TASK_PATH + INVALID_UUID)
                .then()
                .statusCode(500);
    }

    @Test
    @Description("Получение несуществующего статуса задачи")
    @DisplayName("Ошибка при запросе несуществующего статуса")
    public void getStatusTask_NotFound() {
        authRequest()
                .get(STATUS_TASK_PATH + NON_EXISTENT_UUID)
                .then()
                .statusCode(500);
    }
}