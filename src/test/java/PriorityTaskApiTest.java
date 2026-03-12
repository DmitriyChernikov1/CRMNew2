import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(BaseTest.class)
public class PriorityTaskApiTest {
    AuthTokenTest authService = new AuthTokenTest();
    String accessToken = authService.getAccessToken();

    @Test
    @Description("Получение приоритета задачи по UUID")
    @DisplayName("Успешное получение приоритета задачи")
    public void getPriorityTask_Success() {


        String validUuid = "146eb1fc-877e-466b-984b-bd1cd6351dab"; // Заменить на реальный UUID

        Response getPriorityTask = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.16:9000/client-relations/tasks/priority-task/" + validUuid)
                .andReturn();

        int statusCode = getPriorityTask.getStatusCode();
        assertEquals(200, statusCode);

        String responseBody = getPriorityTask.getBody().asString();
        assertNotNull(responseBody);
    }

    @Test
    @Description("Получение приоритета задачи - неавторизованный доступ")
    @DisplayName("Ошибка авторизации при получении приоритета")
    public void getPriorityTask_Unauthorized() {
        String validUuid = "123e4567-e89b-12d3-a456-426614174000";

        Response getPriorityTask = RestAssured
                .given()
                .headers("Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.16:9000/client-relations/tasks/priority-task/" + validUuid)
                .andReturn();

        int statusCode = getPriorityTask.getStatusCode();
        assertEquals(401, statusCode);
    }

    @Test
    @Description("Получение приоритета задачи с неверным UUID")
    @DisplayName("Ошибка при неверном формате UUID")
    public void getPriorityTask_InvalidUuid() {


        String invalidUuid = "invalid-uuid-format";

        Response getPriorityTask = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.16:9000/client-relations/tasks/priority-task/" + invalidUuid)
                .andReturn();

        int statusCode = getPriorityTask.getStatusCode();
        assertEquals(500, statusCode);
    }

    @Test
    @Description("Получение несуществующего приоритета задачи")
    @DisplayName("Ошибка при запросе несуществующего приоритета")
    public void getPriorityTask_NotFound() {

        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";

        Response getPriorityTask = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.16:9000/client-relations/tasks/priority-task/" + nonExistentUuid)
                .andReturn();

        int statusCode = getPriorityTask.getStatusCode();
        assertEquals(500, statusCode);
    }



    @Test
    @Description("Получение приоритета задачи с использованием path параметра")
    @DisplayName("Успешное получение приоритета (path param)")
    public void getPriorityTask_WithPathParam() {


        String validUuid = "146eb1fc-877e-466b-984b-bd1cd6351dab";

        Response getPriorityTask = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .pathParam("uuid", validUuid)
                .get("http://172.20.207.16:9000/client-relations/tasks/priority-task/{uuid}")
                .andReturn();

        int statusCode = getPriorityTask.getStatusCode();
        assertEquals(200, statusCode);

        String responseBody = getPriorityTask.getBody().asString();
        assertNotNull(responseBody);

        // Проверка что возвращен корректный UUID
        String responseUuid = getPriorityTask.jsonPath().getString("id");
        assertEquals(validUuid, responseUuid);
    }



    @Test
    @Description("Получение приоритета задачи с проверкой заголовков ответа")
    @DisplayName("Проверка заголовков ответа")
    public void getPriorityTask_ResponseHeaders() {


        String validUuid = "146eb1fc-877e-466b-984b-bd1cd6351dab";

        Response getPriorityTask = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.16:9000/client-relations/tasks/priority-task/" + validUuid)
                .andReturn();

        int statusCode = getPriorityTask.getStatusCode();
        assertEquals(200, statusCode);

        // Проверка заголовков ответа
        String contentType = getPriorityTask.getHeader("Content-Type");
        assertNotNull(contentType);
        assertTrue(contentType.contains("application/json"));
    }
    @Test
    @Description("Получение списка приоритетов задач с пагинацией")
    @DisplayName("Успешное получение списка приоритетов задач")
    public void getPriorityTaskList_Success() {


        Response getPriorityTaskList = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.16:9000/client-relations/tasks/priority-task")
                .andReturn();

        int statusCode = getPriorityTaskList.getStatusCode();
        assertEquals(200, statusCode);

        String responseBody = getPriorityTaskList.getBody().asString();
        assertNotNull(responseBody);
    }
    @Test
    @Description("Получение списка приоритетов задач с указанием страницы")
    @DisplayName("Успешное получение списка с пагинацией")
    public void getPriorityTaskList_WithPagination() {

        Response getPriorityTaskList = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .get("http://172.20.207.16:9000/client-relations/tasks/priority-task")
                .andReturn();

        int statusCode = getPriorityTaskList.getStatusCode();
        assertEquals(200, statusCode);

        String responseBody = getPriorityTaskList.getBody().asString();
        assertNotNull(responseBody);
    }
    @Test
    @Description("Получение списка приоритетов задач с сортировкой по имени")
    @DisplayName("Успешное получение списка с сортировкой")
    public void getPriorityTaskList_WithSorting() {

        Response getPriorityTaskList = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .queryParam("sort", "name")
                .get("http://172.20.207.16:9000/client-relations/tasks/priority-task")
                .andReturn();

        int statusCode = getPriorityTaskList.getStatusCode();
        assertEquals(200, statusCode);

        String responseBody = getPriorityTaskList.getBody().asString();
        assertNotNull(responseBody);
    }
    @Test
    @Description("Получение списка приоритетов задач с сортировкой по убыванию")
    @DisplayName("Успешное получение списка с сортировкой по убыванию")
    public void getPriorityTaskList_WithDescSorting() {

        Response getPriorityTaskList = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .queryParam("sort", "name,desc")
                .get("http://172.20.207.16:9000/client-relations/tasks/priority-task")
                .andReturn();

        int statusCode = getPriorityTaskList.getStatusCode();
        assertEquals(200, statusCode);

        String responseBody = getPriorityTaskList.getBody().asString();
        assertNotNull(responseBody);
    }
}