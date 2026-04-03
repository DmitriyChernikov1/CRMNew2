import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(BaseTest.class)
public class CreateMeetTest {

    private static String accessToken;
    private static String createdEventId;

    private static final String BASE_URL        = "http://172.20.207.16";
    private static final String CALENDAR_API    = BASE_URL + "/api/calendar";

    // Справочные ID из HAR (статичные данные системы)
    private static final String STATUS_NEW_ID       = "b8ff7136-821a-46f1-8e79-68d82860b781";
    private static final String TYPE_EVENT_ID       = "b92313c7-bee6-4854-b649-d5990d979247"; // "Событие"
    private static final String KIND_MEETING_ID     = "8f3dc94e-1c71-4590-a4ae-70d4245c31cc"; // "Встреча"

    // Сотрудники из HAR
    private static final String EMPLOYEE_MAIN_ID    = "b69f0af0-43bd-4a37-b3c1-f68c123fde0c"; // Черников
    private static final String EMPLOYEE_SECOND_ID  = "e8f0d81b-6e7a-4f5e-a5d7-8ff504b8e8c9"; // Сопин

    @BeforeAll
    static void setUp() {
        accessToken = new AuthTokenTest().getAccessToken();
        RestAssured.baseURI = BASE_URL;
        System.setProperty("java.net.useSystemProxies", "false");
    }

    private RequestSpecification authRequest() {
        return RestAssured.given()
                .baseUri(BASE_URL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/plain, */*")
                .header("Authorization", "Bearer " + accessToken);
    }

    // ─── Вспомогательные методы ──────────────────────────────────────────────

    private Map<String, Object> buildStatusNew() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("createdBy", "user");
        status.put("createdDate", "2024-03-22 13:50:00.000");
        status.put("lastModifiedBy", "user");
        status.put("lastModifiedDate", "2024-03-22 13:50:00.000");
        status.put("deletedDate", null);
        status.put("deletedBy", "");
        status.put("isDelete", false);
        status.put("id", STATUS_NEW_ID);
        status.put("name", "Новое");
        return status;
    }

    private Map<String, Object> buildTypeEvent() {
        Map<String, Object> type = new LinkedHashMap<>();
        type.put("createdBy", "user");
        type.put("createdDate", "2023-01-09 12:28:56.767");
        type.put("lastModifiedBy", "user");
        type.put("lastModifiedDate", "2023-01-09 12:28:56.767");
        type.put("deletedDate", null);
        type.put("deletedBy", null);
        type.put("isDelete", false);
        type.put("id", TYPE_EVENT_ID);
        type.put("name", "Событие");
        type.put("color", "");
        type.put("backgroundColor", null);
        type.put("icon", null);
        return type;
    }

    private Map<String, Object> buildKindMeeting() {
        Map<String, Object> kind = new LinkedHashMap<>();
        kind.put("createdBy", "user");
        kind.put("createdDate", "2023-02-21 14:00:00.000");
        kind.put("lastModifiedBy", "user");
        kind.put("lastModifiedDate", "2025-02-06 16:30:00.000");
        kind.put("deletedDate", null);
        kind.put("deletedBy", null);
        kind.put("isDelete", false);
        kind.put("id", KIND_MEETING_ID);
        kind.put("name", "Встреча");
        kind.put("color", "#469AA4");
        kind.put("backgroundColor", "#DBECEE");
        kind.put("icon", null);
        return kind;
    }

    private String todayDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00.000";
    }

    private String todayEndDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 20:00:00.000";
    }

    // ─── Тесты ───────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @Description("Получение справочных данных для создания события")
    @DisplayName("GET /api/calendar/input/event — справочник типов событий")
    public void getEventInputData() {
        Response response = authRequest()
                .get(CALENDAR_API + "/input/event")
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> types = response.jsonPath().getList("typeEventDtos");
        assertNotNull(types, "Список типов событий не должен быть null");
        assertFalse(types.isEmpty(), "Список типов событий не должен быть пустым");
    }

    @Test
    @Order(2)
    @Description("Поиск сотрудников для добавления на встречу")
    @DisplayName("GET /api/calendar/autocomplete/employee — автодополнение по имени")
    public void autocompleteEmployee() {
        Response response = authRequest()
                .queryParam("page", 0)
                .queryParam("size", 20)
                .queryParam("name", "соп")
                .get(CALENDAR_API + "/autocomplete/employee")
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertNotNull(content, "Список сотрудников не должен быть null");

        boolean found = content.stream()
                .anyMatch(e -> EMPLOYEE_SECOND_ID.equals(e.get("id")));
        assertTrue(found, "Сотрудник Сопин должен присутствовать в результатах поиска по 'соп'");
    }

    @Test
    @Order(3)
    @Description("Поиск клиентов для добавления на встречу")
    @DisplayName("GET /api/calendar/autocomplete/client — автодополнение клиентов")
    public void autocompleteClient() {
        Response response = authRequest()
                .queryParam("page", 0)
                .queryParam("size", 20)
                .queryParam("name", "")
                .get(CALENDAR_API + "/autocomplete/client")
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertNotNull(content, "Список клиентов не должен быть null");
    }

    @Test
    @Order(4)
    @Description("Фильтрация событий на карте по периоду и статусам")
    @DisplayName("POST /api/calendar/event/filtering-event-map — фильтр по дате и статусу")
    public void filterEventMapByDateAndStatus() {
        Map<String, Object> body = new LinkedHashMap<>();

        List<Map<String, Object>> dtos = new ArrayList<>();
        dtos.add(buildFilter("", "typeEvent", "not eq", "График"));
        dtos.add(buildFilter("AND", "startPlanDate", "greater", "2026-03-18 00:00:00.000"));
        dtos.add(buildFilter("AND", "stopPlanDate", "less", "2026-05-14 23:59:59.999"));
        dtos.add(buildFilter("AND", "statusEvent", "in", "Новое,В работе,Ожидает выполнения,Выполнено"));

        body.put("dtos", dtos);
        body.put("employeeIds", List.of(EMPLOYEE_MAIN_ID));

        Response response = authRequest()
                .queryParam("showArchive", false)
                .body(body)
                .post(CALENDAR_API + "/event/filtering-event-map")
                .then()
                .statusCode(200)
                .extract().response();

        Map<String, Object> result = response.jsonPath().getMap("$");
        assertNotNull(result, "Результат фильтрации не должен быть null");
        assertTrue(result.containsKey(EMPLOYEE_MAIN_ID),
                "Ответ должен содержать события для сотрудника " + EMPLOYEE_MAIN_ID);
    }

    @Test
    @Order(5)
    @Description("Фильтрация событий на карте по нескольким сотрудникам")
    @DisplayName("POST /api/calendar/event/filtering-event-map — фильтр по двум сотрудникам")
    public void filterEventMapByMultipleEmployees() {
        Map<String, Object> body = new LinkedHashMap<>();

        List<Map<String, Object>> dtos = new ArrayList<>();
        dtos.add(buildFilter("", "typeEvent", "not eq", "График"));
        dtos.add(buildFilter("AND", "startPlanDate", "greater", "2026-03-18 00:00:00.000"));
        dtos.add(buildFilter("AND", "stopPlanDate", "less", "2026-05-14 23:59:59.999"));
        dtos.add(buildFilter("AND", "statusEvent", "in", "Новое,В работе,Ожидает выполнения"));

        body.put("dtos", dtos);
        body.put("employeeIds", List.of(EMPLOYEE_MAIN_ID, EMPLOYEE_SECOND_ID));

        Response response = authRequest()
                .queryParam("showArchive", false)
                .body(body)
                .post(CALENDAR_API + "/event/filtering-event-map")
                .then()
                .statusCode(200)
                .extract().response();

        Map<String, Object> result = response.jsonPath().getMap("$");
        assertNotNull(result);
        assertTrue(result.containsKey(EMPLOYEE_MAIN_ID), "Ответ должен содержать события первого сотрудника");
        assertTrue(result.containsKey(EMPLOYEE_SECOND_ID), "Ответ должен содержать события второго сотрудника");
    }

    @Test
    @Order(6)
    @Description("Фильтрация событий по приоритету «Гарантийная»")
    @DisplayName("POST /api/calendar/event/filtering-event-map — фильтр по приоритету")
    public void filterEventMapByPriority() {
        Map<String, Object> body = new LinkedHashMap<>();

        List<Map<String, Object>> dtos = new ArrayList<>();
        dtos.add(buildFilter("", "typeEvent", "not eq", "График"));
        dtos.add(buildFilter("AND", "priorityEvent", "in", "Гарантийная"));
        dtos.add(buildFilter("AND", "statusEvent", "in", "Новое,В работе,Ожидает выполнения"));

        body.put("dtos", dtos);
        body.put("employeeIds", List.of(EMPLOYEE_MAIN_ID));

        authRequest()
                .queryParam("showArchive", false)
                .body(body)
                .post(CALENDAR_API + "/event/filtering-event-map")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(7)
    @Description("Получение метрик сотрудника — круговая диаграмма задач")
    @DisplayName("GET /api/calendar/employee/gaugeChart/{employeeId}")
    public void getEmployeeGaugeChart() {
        Response response = authRequest()
                .get(CALENDAR_API + "/employee/gaugeChart/" + EMPLOYEE_MAIN_ID)
                .then()
                .statusCode(200)
                .extract().response();

        assertNotNull(response.jsonPath().get("completionPerc"), "completionPerc не должен быть null");
        assertNotNull(response.jsonPath().get("totalTasks"), "totalTasks не должен быть null");
        assertNotNull(response.jsonPath().get("statuses"), "statuses не должен быть null");
    }

    @Test
    @Order(8)
    @Description("Получение просроченных задач сотрудника")
    @DisplayName("GET /api/calendar/employee/overdueTasks/{employeeId}")
    public void getEmployeeOverdueTasks() {
        Response response = authRequest()
                .get(CALENDAR_API + "/employee/overdueTasks/" + EMPLOYEE_MAIN_ID)
                .then()
                .statusCode(200)
                .extract().response();

        // Список может быть пустым, но должен быть массивом
        assertNotNull(response.body(), "Тело ответа не должно быть null");
    }

    @Test
    @Order(9)
    @Description("Создание встречи — шаг 1: POST /api/calendar/event")
    @DisplayName("Создание события типа «Встреча»")
    public void createMeetingEvent() {
        Map<String, Object> partyMain = new LinkedHashMap<>();
        partyMain.put("id", EMPLOYEE_MAIN_ID);
        partyMain.put("isMain", true);

        Map<String, Object> partySecond = new LinkedHashMap<>();
        partySecond.put("id", EMPLOYEE_SECOND_ID);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("lastModifiedDate", null);
        body.put("deletedDate", null);
        body.put("stopFactDate", null);
        body.put("startFactDate", null);
        body.put("createdDate", null);
        body.put("startPlanDate", todayDate());
        body.put("stopPlanDate", todayEndDate());
        body.put("comment", "автотест встреча");
        body.put("name", "автотест встреча");
        body.put("meetingLink", "https://meet.example.com/autotest");
        body.put("statusEvent", buildStatusNew());
        body.put("typeEvent", buildTypeEvent());
        body.put("kindEvent", buildKindMeeting());
        body.put("responsibleIdName", "Я");
        body.put("responsibleId", EMPLOYEE_MAIN_ID);
        body.put("partyEmployeeDtos", List.of(partyMain, partySecond));

        Response response = authRequest()
                .body(body)
                .post(CALENDAR_API + "/event")
                .then()
                .statusCode(200)
                .extract().response();

        createdEventId = response.jsonPath().getString("id");
        String name    = response.jsonPath().getString("name");
        Integer number = response.jsonPath().getInt("number");

        assertNotNull(createdEventId, "id созданного события не должен быть null");
        assertEquals("автотест встреча", name, "Имя события должно совпадать с отправленным");
        assertNotNull(number, "Номер события должен быть присвоен");
    }

    @Test
    @Order(10)
    @Description("Создание встречи — шаг 2: POST /api/calendar/party-employee/create")
    @DisplayName("Добавление участников к созданному событию")
    public void addParticipantsToEvent() {
        assertNotNull(createdEventId, "Для этого теста требуется createdEventId из предыдущего шага");

        Map<String, Object> mainParticipant = new LinkedHashMap<>();
        mainParticipant.put("employeeId", EMPLOYEE_MAIN_ID);
        mainParticipant.put("isMain", true);
        mainParticipant.put("partyStatus", "Ожидает решения");

        Map<String, Object> secondParticipant = new LinkedHashMap<>();
        secondParticipant.put("employeeId", EMPLOYEE_SECOND_ID);
        secondParticipant.put("isMain", false);
        secondParticipant.put("partyStatus", "Ожидает решения");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("employees", List.of(mainParticipant, secondParticipant));
        body.put("eventId", createdEventId);

        Response response = authRequest()
                .body(body)
                .post(CALENDAR_API + "/party-employee/create")
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> parties = response.jsonPath().getList("$");
        assertNotNull(parties, "Список участников не должен быть null");
        assertEquals(2, parties.size(), "Должно быть добавлено 2 участника");

        boolean hasMain = parties.stream()
                .anyMatch(p -> Boolean.TRUE.equals(p.get("isMain")) &&
                        EMPLOYEE_MAIN_ID.equals(p.get("employee")));
        assertTrue(hasMain, "Главный участник (isMain=true) должен присутствовать в ответе");
    }

    @Test
    @Order(11)
    @Description("Получение события по ID после создания")
    @DisplayName("GET /api/calendar/event/{id} — проверка созданного события")
    public void getCreatedEventById() {
        assertNotNull(createdEventId, "Для этого теста требуется createdEventId");

        Response response = authRequest()
                .get(CALENDAR_API + "/event/" + createdEventId)
                .then()
                .statusCode(200)
                .extract().response();

        String id        = response.jsonPath().getString("id");
        String name      = response.jsonPath().getString("name");
        String kindName  = response.jsonPath().getString("kindEvent.name");

        assertEquals(createdEventId, id, "ID события в ответе должен совпадать");
        assertEquals("автотест встреча", name, "Имя события должно совпадать");
        assertEquals("Встреча", kindName, "Тип вида события должен быть «Встреча»");
    }

    @Test
    @Order(12)
    @Description("Создание встречи — шаг 3: PUT /api/calendar/event/{id}")
    @DisplayName("Финальное сохранение (PUT) созданного события")
    public void updateCreatedEvent() {
        assertNotNull(createdEventId, "Для этого теста требуется createdEventId");

        // Получаем актуальное состояние события перед PUT
        Response getResponse = authRequest()
                .get(CALENDAR_API + "/event/" + createdEventId)
                .then()
                .statusCode(200)
                .extract().response();

        // Берём тело как Map и обновляем нужные поля
        Map<String, Object> body = getResponse.jsonPath().getMap("$");
        body.put("comment", "автотест встреча — обновлено");
        body.put("meetingLink", "https://meet.example.com/autotest-updated");

        Response putResponse = authRequest()
                .body(body)
                .put(CALENDAR_API + "/event/" + createdEventId)
                .then()
                .statusCode(200)
                .extract().response();

        String updatedId   = putResponse.jsonPath().getString("id");
        assertEquals(createdEventId, updatedId, "ID события не должен измениться после PUT");
    }

    @Test
    @Order(13)
    @Description("Проверка созданной встречи в фильтрации событий на карте")
    @DisplayName("Созданная встреча присутствует в результатах фильтрации")
    public void createdEventAppearsInFilterMap() {
        assertNotNull(createdEventId, "Для этого теста требуется createdEventId");

        Map<String, Object> body = new LinkedHashMap<>();

        List<Map<String, Object>> dtos = new ArrayList<>();
        dtos.add(buildFilter("", "typeEvent", "not eq", "График"));
        dtos.add(buildFilter("AND", "statusEvent", "in", "Новое,В работе,Ожидает выполнения,Выполнено"));

        body.put("dtos", dtos);
        body.put("employeeIds", List.of(EMPLOYEE_MAIN_ID));

        Response response = authRequest()
                .queryParam("showArchive", false)
                .body(body)
                .post(CALENDAR_API + "/event/filtering-event-map")
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> events = response.jsonPath().getList(EMPLOYEE_MAIN_ID);
        assertNotNull(events, "Список событий сотрудника не должен быть null");

        boolean found = events.stream()
                .anyMatch(e -> createdEventId.equals(e.get("id")));
        assertTrue(found, "Созданная встреча должна присутствовать в результатах фильтрации");
    }

    // ─── Утилита построения DTO фильтра ──────────────────────────────────────

    private Map<String, Object> buildFilter(String logicalOperator, String field,
                                            String operator, String value) {
        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put("logicalOperator", logicalOperator);
        filter.put("field", field);
        filter.put("operator", operator);
        filter.put("value", value);
        return filter;
    }
}