

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static io.restassured.RestAssured.given;
@ExtendWith(BaseTest.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RegulationAPITest {

    private static final String BASE_URL = "http://172.20.207.16";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static String createdRegulationId;
    private static String executionTypeId = "b874f167-65ff-491a-bf17-aa4130e36c0c";
    private static String calendarView;
    private static String accessToken;
    private static List<String> regulationIdsToCleanup = new ArrayList<>();

    @BeforeAll
    public void setup() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Получаем токен авторизации перед выполнением тестов
        AuthTokenTest authService = new AuthTokenTest();
        accessToken = authService.getAccessToken();

        Assertions.assertNotNull(accessToken, "Access token не должен быть null");
        Assertions.assertFalse(accessToken.isEmpty(), "Access token не должен быть пустым");
    }

    // ==================== ОБЩИЕ МЕТОДЫ ====================

    private RequestSpecification getCommonRequest() {
        return given()
                .header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", USER_AGENT)
                .header("Accept-Encoding", "gzip, deflate")
                .header("Connection", "keep-alive")
                .header("accept-language", "ru")
                .header("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"Windows\"")
                .header("Authorization", "Bearer " + accessToken);
    }

    // ==================== МЕТОДЫ ДЛЯ УДАЛЕНИЯ ====================

    /**
     * Метод для удаления регуляции по ID
     */
    private Response deleteRegulationById(String regulationId) {
        return getCommonRequest()
                .header("Referer", BASE_URL + "/regulation/jobs/jobs-detailed/" + regulationId)
                .header("Content-Type", "application/json")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .when()
                .delete("/api/regulation/regulation-full/" + regulationId);
    }

    /**
     * Метод для безопасного создания регуляции с автоматической очисткой
     */
    private String createRegulationForTest(String name) {
        String requestBody = String.format("{" +
                "\"name\": \"%s\"," +
                "\"startDate\": \"2027-01-01 00:00:00.000\"," +
                "\"stopDate\": \"2027-12-18 00:00:00.000\"," +
                "\"contract\": \"Договор управление\"," +
                "\"contractor\": \"―\"," +
                "\"calendarView\": \"%s\"," +
                "\"executionType\": {" +
                "    \"id\": \"%s\"," +
                "    \"name\": \"Собственными силами\"" +
                "}" +
                "}", name, calendarView, executionTypeId);

        Response response = getCommonRequest()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post("/api/regulation/regulation-full")
                .then()
                .statusCode(200)
                .extract()
                .response();

        String regulationId = response.jsonPath().getString("id");
        regulationIdsToCleanup.add(regulationId);
        return regulationId;
    }

    // ==================== ТЕСТОВЫЕ СЦЕНАРИИ ====================

    @Test
    @Order(1)
    @DisplayName("1. Получение данных для создания регуляции")
    public void testGetRegulationInputData() {
        Response response = getCommonRequest()
                .header("Referer", BASE_URL + "/regulation/jobs/create")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .when()
                .get("/api/regulation/input/regulation")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("executionTypeDtos", not(empty()))
                .body("calendarView", not(empty()))
                .extract()
                .response();


        List<String> calendarViews = response.jsonPath().getList("calendarView");
        calendarView = calendarViews.contains("По неделям") ? "По неделям" : calendarViews.get(0);

        Assertions.assertNotNull(executionTypeId, "ID типа исполнения не найден");
        Assertions.assertNotNull(calendarView, "Календарный вид не найден");
    }

    @Test
    @Order(2)
    @DisplayName("2. Создание новой регуляции")
    public void testCreateRegulation() {
        String requestBody = String.format("{" +
                "\"name\": \"тест\"," +
                "\"startDate\": \"2027-01-01 00:00:00.000\"," +
                "\"stopDate\": \"2027-12-18 00:00:00.000\"," +
                "\"contract\": \"Договор управление\"," +
                "\"contractor\": \"―\"," +
                "\"calendarView\": \"%s\"," +
                "\"executionType\": {" +
                "    \"id\": \"%s\"," +
                "    \"name\": \"Собственными силами\"" +
                "}" +
                "}", calendarView, executionTypeId);

        Response response = RestAssured
                .given()
                .body(requestBody)
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .post("api/regulation/regulation-full")
                .andReturn();

        createdRegulationId = response.jsonPath().getString("id");
        regulationIdsToCleanup.add(createdRegulationId);
        Assertions.assertNotNull(createdRegulationId, "ID созданной регуляции не получен");
    }

    @Test
    @Order(3)
    @DisplayName("3. Получение детальной информации о созданной регуляции")
    public void testGetCreatedRegulation() {
        getCommonRequest()
                .header("Referer", BASE_URL + "/regulation/jobs/jobs-detailed/" + createdRegulationId)
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .when()
                .get("/api/regulation/regulation-full/" + createdRegulationId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(createdRegulationId))
                .body("name", equalTo("тест"))
                .body("startDate", equalTo("2027-01-01 00:00:00.000"))
                .body("stopDate", equalTo("2027-12-18 00:00:00.000"))
                .body("calendarView", equalTo(calendarView))
                .body("executionType.name", equalTo("Собственными силами"))
                .body("contractor", equalTo("―"))
                .body("contract", equalTo("Договор управление"))
                .body("alreadyUnderContract", equalTo(false))
                .body("possibilityMoving", equalTo(false));
    }

    @Test
    @Order(4)
    @DisplayName("4. Получение списка работ по регуляции")
    public void testGetRoutineWorkList() {
        getCommonRequest()
                .header("Referer", BASE_URL + "/regulation/jobs/jobs-detailed/" + createdRegulationId)
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .queryParam("sort", "number,desc")
                .queryParam("page", 0)
                .queryParam("size", 10000)
                .when()
                .get("/api/regulation/routine-work/full-by-regulation/" + createdRegulationId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("workWithTimeSlots", empty())
                .body("cells", empty());
    }

    @Test
    @Order(5)
    @DisplayName("5. Получение данных для диаграммы работ")
    public void testGetChartInputs() {
        Response response = getCommonRequest()
                .header("Referer", BASE_URL + "/regulation/jobs/jobs-detailed/" + createdRegulationId)
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .when()
                .get("/api/regulation/input/flow-chart")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("kindWorkDtos", not(empty()))
                .extract()
                .response();

        List<Map<String, Object>> kindWorks = response.jsonPath().getList("kindWorkDtos");
        Assertions.assertTrue(kindWorks.size() > 0, "Список видов работ должен содержать элементы");

        boolean hasCleaning = kindWorks.stream()
                .anyMatch(work -> "Уборка".equals(work.get("name")));
        boolean hasMaintenance = kindWorks.stream()
                .anyMatch(work -> "Техобслуживание".equals(work.get("name")));

        Assertions.assertTrue(hasCleaning, "Должен присутствовать вид работ 'Уборка'");
        Assertions.assertTrue(hasMaintenance, "Должен присутствовать вид работ 'Техобслуживание'");
    }

    @Test
    @Order(6)
    @DisplayName("6. Успешное удаление регуляции")
    public void testDeleteRegulationSuccess() {
        // Создаем временную регуляцию для теста удаления
        String tempRegulationId = createRegulationForTest("тест_для_удаления");

        // Удаляем регуляцию
        Response deleteResponse = deleteRegulationById(tempRegulationId);

        // Проверяем успешное удаление (может возвращать 200 OK или 204 No Content)
        int statusCode = deleteResponse.getStatusCode();
        Assertions.assertTrue(statusCode == 201 || statusCode == 204,
                "Ожидался статус 200 или 204, но получен: " + statusCode);

        // Проверяем, что регуляция больше не доступна
        getCommonRequest()
                .when()
                .get("/api/regulation/regulation-full/" + tempRegulationId)
                .then()
                .statusCode(500); // Ожидаем 500 после удаления

        // Удаляем ID из списка для очистки (так как уже удалено)
        regulationIdsToCleanup.remove(tempRegulationId);
    }

    @Test
    @Order(7)
    @DisplayName("7. Негативный тест: Удаление несуществующей регуляции")
    public void testDeleteNonExistentRegulation() {
        String nonExistentId = UUID.randomUUID().toString();

        deleteRegulationById(nonExistentId)
                .then()
                .statusCode(201);
    }

    @Test
    @Order(8)
    @DisplayName("8. Негативный тест: Удаление без токена авторизации")
    public void testDeleteWithoutAuthToken() {
        String tempRegulationId = createRegulationForTest("тест_удаление_без_токена");

        given()
                .header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json")
                .when()
                .delete("/api/regulation/regulation-full/" + tempRegulationId)
                .then()
                .statusCode(401); // Ожидаем 401 без токена
    }

    @Test
    @Order(9)
    @DisplayName("9. Негативный тест: Удаление с невалидным токеном")
    public void testDeleteWithInvalidToken() {
        String tempRegulationId = createRegulationForTest("тест_удаление_невалидный_токен");

        given()
                .header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", USER_AGENT)
                .header("Authorization", "Bearer invalid_token_12345")
                .header("Content-Type", "application/json")
                .when()
                .delete("/api/regulation/regulation-full/" + tempRegulationId)
                .then()
                .statusCode(401); // Ожидаем 401 с невалидным токеном
    }

    @Test
    @Order(10)
    @DisplayName("10. Негативный тест: Получение несуществующей регуляции")
    public void testGetNonExistentRegulation() {
        String nonExistentId = UUID.randomUUID().toString();

        getCommonRequest()
                .header("Referer", BASE_URL + "/regulation/jobs/jobs-detailed/" + nonExistentId)
                .when()
                .get("/api/regulation/regulation-full/" + nonExistentId)
                .then()
                .statusCode(500);
    }

    @Test
    @Order(11)
    @DisplayName("11. Негативный тест: Создание регуляции с некорректными данными")
    public void testCreateRegulationWithInvalidData() {
        String invalidRequestBody = "{" +
                "\"name\": \"\"," +
                "\"startDate\": \"invalid-date\"," +
                "\"stopDate\": \"2027-12-18 00:00:00.000\"," +
                "\"contract\": \"Договор управление\"," +
                "\"calendarView\": \"InvalidView\"" +
                "}";

        getCommonRequest()
                .header("Content-Type", "application/json")
                .body(invalidRequestBody)
                .when()
                .post("/api/regulation/regulation-full")
                .then()
                .statusCode(500);
    }

    @Test
    @Order(12)
    @DisplayName("12. Негативный тест: Запрос без токена авторизации")
    public void testRequestWithoutAuthToken() {
        given()
                .header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", USER_AGENT)
                .header("Referer", BASE_URL + "/regulation/jobs/create")
                .when()
                .get("/api/regulation/input/regulation")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(13)
    @DisplayName("13. Негативный тест: Запрос с невалидным токеном")
    public void testRequestWithInvalidToken() {
        given()
                .header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", USER_AGENT)
                .header("Authorization", "Bearer invalid_token_12345")
                .header("Referer", BASE_URL + "/regulation/jobs/create")
                .when()
                .get("/api/regulation/input/regulation")
                .then()
                .statusCode(401);
    }


    @Test
    @Order(15)
    @DisplayName("15. Проверка заголовков безопасности")
    public void testSecurityHeaders() {
        Response response = getCommonRequest()
                .when()
                .get("/api/regulation/input/regulation")
                .then()
                .statusCode(200)
                .extract()
                .response();

        Assertions.assertEquals("no-cache, no-store, max-age=0, must-revalidate",
                response.header("Cache-Control"));
        Assertions.assertEquals("nosniff", response.header("X-Content-Type-Options"));
        Assertions.assertEquals("DENY", response.header("X-Frame-Options"));
        Assertions.assertNotNull(response.header("X-XSS-Protection"));
    }

    @Test
    @Order(16)
    @DisplayName("16. Проверка корректности структуры JSON ответов")
    public void testResponseStructure() {
        Response response = getCommonRequest()
                .when()
                .get("/api/regulation/regulation-full/" + createdRegulationId)
                .then()
                .statusCode(200)
                .extract()
                .response();

        Map<String, Object> regulation = response.jsonPath().getMap("");
        Assertions.assertTrue(regulation.containsKey("id"));
        Assertions.assertTrue(regulation.containsKey("name"));
        Assertions.assertTrue(regulation.containsKey("startDate"));
        Assertions.assertTrue(regulation.containsKey("stopDate"));
        Assertions.assertTrue(regulation.containsKey("createdBy"));
        Assertions.assertTrue(regulation.containsKey("createdDate"));
        Assertions.assertTrue(regulation.containsKey("lastModifiedBy"));
        Assertions.assertTrue(regulation.containsKey("lastModifiedDate"));

        Map<String, Object> executionType = response.jsonPath().getMap("executionType");
        Assertions.assertTrue(executionType.containsKey("id"));
        Assertions.assertTrue(executionType.containsKey("name"));
    }

    @Test
    @Order(17)
    @DisplayName("17. Проверка временных меток создания/обновления")
    public void testTimestamps() {
        Response response = getCommonRequest()
                .when()
                .get("/api/regulation/regulation-full/" + createdRegulationId)
                .then()
                .statusCode(200)
                .extract()
                .response();

        String createdDate = response.jsonPath().getString("createdDate");
        String modifiedDate = response.jsonPath().getString("lastModifiedDate");

        Assertions.assertNotNull(createdDate);
        Assertions.assertNotNull(modifiedDate);
        Assertions.assertTrue(createdDate.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
        Assertions.assertTrue(modifiedDate.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
    }


    @AfterAll
    public void cleanup() {
        System.out.println("Начинается очистка тестовых данных...");

        // Удаляем все регуляции, которые были созданы в тестах
        for (String regulationId : regulationIdsToCleanup) {
            try {
                System.out.println("Попытка удаления регуляции ID: " + regulationId);

                Response deleteResponse = deleteRegulationById(regulationId);
                int statusCode = deleteResponse.getStatusCode();

                if (statusCode == 201 || statusCode == 200) {
                    System.out.println("Регуляция " + regulationId + " успешно удалена");
                } else {
                    System.out.println("Не удалось удалить регуляцию " + regulationId +
                            ", статус: " + statusCode);
                }

                // Небольшая пауза между запросами
                Thread.sleep(100);

            } catch (Exception e) {
                System.err.println("Ошибка при удалении регуляции " + regulationId + ": " + e.getMessage());
            }
        }

        System.out.println("Очистка тестовых данных завершена.");
        System.out.println("Всего создано регуляций для тестов: " + regulationIdsToCleanup.size());

        // Проверяем, что основная тестовая регуляция удалена
        if (createdRegulationId != null) {
            try {
                getCommonRequest()
                        .when()
                        .get("/api/regulation/regulation-full/" + createdRegulationId)
                        .then()
                        .statusCode(500);
                System.out.println("Основная тестовая регуляция успешно удалена: " + createdRegulationId);
            } catch (AssertionError e) {
                System.err.println("Основная тестовая регуляция все еще существует: " + createdRegulationId);
            }
        }
    }
}