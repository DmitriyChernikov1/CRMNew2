import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BaseTest.class)
public class CalendarTest {

    private static String accessToken;
    // Календарь на другом порту чем авторизация (8083)!
    private static final String BASE_URL = "http://172.20.207.16";
    private static final String JSON_PATH = "src/test/java/JsonFiles/";

    @BeforeAll
    static void setUp() {
        // Токен получается ОДИН раз для всего класса
        // ВАЖНО: используем BASE_URL календаря, а не BASE_URL авторизации (8083)
        RestAssured.baseURI = BASE_URL;
        accessToken = new AuthTokenTest().getAccessToken();
    }

    // Базовый запрос с авторизацией — baseUri берётся из RestAssured.baseURI выше
    private RequestSpecification authRequest() {
        return RestAssured.given()
                .baseUri(BASE_URL)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .header("Accept-Language", "ru,ru-RU;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Authorization", "Bearer " + accessToken);
    }

    // ─────────────────────────────────────────────
    // События (Events)
    // ─────────────────────────────────────────────

    @Test
    @Description("get Event")
    @DisplayName("Получение ивентов")
    public void getEvent() {
        authRequest()
                .get("/api/calendar/input/event")
                .then()
                .statusCode(200);
    }

    @Test
    @Description("get calendar params")
    @DisplayName("Получение параметров календаря")
    public void getCalendarParams() {
        Response response = authRequest()
                .get("/api/calendar/input/event")
                .andReturn();

        assertEquals(200, response.getStatusCode());
        assertNotNull(response.jsonPath().getString("typeEventDtos"), "поле не должно быть null");
        assertNotNull(response.jsonPath().getString("kindEventDtos"), "поле не должно быть null");
    }

    // ─────────────────────────────────────────────
    // Типы событий (Type Events)
    // ─────────────────────────────────────────────

    @Test
    @Description("Get Type Events with custom pagination parameters")
    @DisplayName("Получение типов событий")
    public void getTypeEventsWithCustomPagination() {
        authRequest()
                .queryParam("page", 1)
                .queryParam("size", 10)
                .queryParam("sort", "name,desc")
                .get("/api/calendar/type-event")
                .then()
                .statusCode(200);
    }

    // ─────────────────────────────────────────────
    // Виды событий (Kind Events)
    // ─────────────────────────────────────────────

    @Test
    @Description("Get Kind Events with default pagination")
    @DisplayName("Получение видов событий с пагинацией по умолчанию")
    public void getKindEventsWithDefaultPagination() {
        Response response = authRequest()
                .get("/api/calendar/kind-event")
                .andReturn();

        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody().asString(), "Тело ответа не должно быть null");
    }

    @Test
    @Description("Get Kind Events with custom pagination parameters")
    @DisplayName("Получение видов событий с кастомными параметрами пагинации")
    public void getKindEventsWithCustomPagination() {
        authRequest()
                .queryParam("page", 1)
                .queryParam("size", 10)
                .queryParam("sort", "name,desc")
                .get("/api/calendar/kind-event")
                .then()
                .statusCode(200);
    }

    @Test
    @Description("Get Kind Events with sorting by name ascending")
    @DisplayName("Получение видов событий с сортировкой по имени по возрастанию")
    public void getKindEventsWithSortingAsc() {
        authRequest()
                .queryParam("sort", "name")
                .get("/api/calendar/kind-event")
                .then()
                .statusCode(200);
    }

    @Test
    @Description("Get Kind Events with sorting by name descending")
    @DisplayName("Получение видов событий с сортировкой по имени по убыванию")
    public void getKindEventsWithSortingDesc() {
        authRequest()
                .queryParam("sort", "name,desc")
                .get("/api/calendar/kind-event")
                .then()
                .statusCode(200);
    }

    @Test
    @Description("Get Kind Events without authentication")
    @DisplayName("Получение видов событий без авторизации")
    public void getKindEventsWithoutAuth() {
        new AuthTokenTest().baseRequest()   // без токена Authorization
                .get("/api/calendar/kind-event")
                .then()
                .statusCode(401);
    }

    @Test
    @Description("Get Kind Events and validate response structure")
    @DisplayName("Получение видов событий с проверкой структуры ответа")
    public void getKindEventsAndValidateStructure() {
        authRequest()
                .get("/api/calendar/kind-event")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("totalPages", greaterThanOrEqualTo(0))
                .body("totalElements", greaterThanOrEqualTo(0))
                .body("size", equalTo(20))   // default size
                .body("number", equalTo(0)); // default page
    }

    // ─────────────────────────────────────────────
    // Шиномонтаж (Tire Fitting)
    // ─────────────────────────────────────────────

    @Test
    @Description("Get tire fitting events with default pagination")
    @DisplayName("Получение ивентов шиномонтажа с пагинацией по умолчанию")
    public void getTireFittingEventsWithDefaultPagination() {
        authRequest()
                .body(TestDataJson.jsonTime())
                .post("/api/calendar/event/tire-fitting")
                .then()
                .statusCode(200);
    }

    @Test
    @Description("Get tire fitting events with custom pagination")
    @DisplayName("Получение ивентов шиномонтажа с кастомной пагинацией")
    public void getTireFittingEventsWithCustomPagination() {
        authRequest()
                .queryParam("page", 1)
                .queryParam("size", 10)
                .queryParam("sort", "name,desc")
                .body(TestDataJson.jsonTime())
                .post("/api/calendar/event/tire-fitting")
                .then()
                .statusCode(200);
    }

    @Test
    @Description("Get tire fitting events without authentication")
    @DisplayName("Получение ивентов шиномонтажа без авторизации")
    public void getTireFittingEventsWithoutAuth() {
        new AuthTokenTest().baseRequest()   // без токена Authorization
                .body(TestDataJson.jsonTime())
                .post("/api/calendar/event/tire-fitting")
                .then()
                .statusCode(401);
    }

    // ─────────────────────────────────────────────
    // Фильтры (Filters)
    // ─────────────────────────────────────────────

    @Test
    @Description("get filter")
    @DisplayName("Получение списка календаря")
    public void getFilterForCalendar() {
        authRequest()
                .body(new File(JSON_PATH + "GetFiltersCalendar.json"))
                .post("/api/calendar/event/filtering-event-map")
                .then()
                .statusCode(200);
    }

    @Test
    @Description("get filter")
    @DisplayName("Получение списка графика дежурств")
    public void getFilterDutySchedule() {
        authRequest()
                .body(new File(JSON_PATH + "dutySchedule.json"))
                .post("/api/calendar/event/filtering-event-map")
                .then()
                .statusCode(200);
    }

    // ─────────────────────────────────────────────
    // График дежурств (Schedule)
    // ─────────────────────────────────────────────

    @Test
    @Description("create schedule")
    @DisplayName("Создание графика дежурств")
    public void createSchedule() {
        authRequest()
                .body(new File(JSON_PATH + "createSchedule.json"))
                .post("/api/calendar/event/generate-schedule")
                .then()
                .statusCode(200);
    }

    @Test
    @Description("delete schedule")
    @DisplayName("Удаление графика дежурств")
    public void deleteSchedule() {
        authRequest()
                .body(new File(JSON_PATH + "deleteSchedule.json"))
                .post("/api/calendar/event/delete-schedule")
                .then()
                .statusCode(201);
    }

    // ─────────────────────────────────────────────
    // Создание события (Create Event) — e2e цепочка
    // ─────────────────────────────────────────────

    @Test
    @Description("creating a simple task")
    @DisplayName("Создание встречи")
    public void createSimpleEvent() {
        // ШАГ 1 — создаём событие
        String body = TestDataJson.CreateEvent;
        Response responseCreateEvent = authRequest()
                .body(body)
                .post("/api/calendar/event")
                .andReturn();

        assertEquals(200, responseCreateEvent.getStatusCode());
        String eventId = responseCreateEvent.jsonPath().getString("id");
        assertNotNull(eventId, "id события не должен быть null");

        // ШАГ 2 — назначаем сотрудника на событие
        String employeeId = "b69f0af0-43bd-4a37-b3c1-f68c123fde0c";
        String taskBody = String.format(
                "{\"eventId\": \"%s\", \"employees\": [{\"employeeId\": \"%s\", \"isMain\": true, \"partyStatus\": \"Ожидает решения\"}]}",
                eventId, employeeId
        );

        authRequest()
                .body(taskBody)
                .post("/api/calendar/party-employee/create")
                .then()
                .statusCode(200);
    }
}