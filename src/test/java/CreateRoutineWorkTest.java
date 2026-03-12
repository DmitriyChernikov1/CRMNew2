import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
@ExtendWith(BaseTest.class)
/**
 * E2E тест: «Создание регламентной работы»
 *
 * Сценарий
 *  1.  POST /api/users/auth/login                                  — аутентификация
 *  2.  GET  /api/users/auth/info                                   — проверка токена
 *  3.  POST /api/regulation/kind-work                              — создание вида работы
 *  4.  GET  /api/regulation/kind-work/{id}                         — проверка вида работы
 *  5.  POST /api/regulation/flow-chart-full                        — создание технологической карты
 *  6.  GET  /api/regulation/flow-chart-full/{id}                   — проверка карты
 *  7.  POST /api/regulation/regulation-full                        — создание регламента
 *  8.  GET  /api/regulation/regulation-full/{id}                   — проверка регламента
 *  9.  GET  /api/regulation/routine-work/full-by-regulation/{id}   — список пуст до создания
 * 10.  POST /api/regulation/routine-work/create-with-time-slots    — создание регламентной работы
 * 11.  POST /api/regulation/time-slot/scheduled                    — планирование тайм-слота
 * 12.  POST /api/regulation/time-slot/create-task                  — создание задачи по тайм-слоту
 * 13.  GET  /api/regulation/routine-work/full-by-regulation/{id}   — финальная проверка
 * 14.  Cleanup @AfterAll                                           — удаление всех созданных данных
 */
@Epic("Регламентные работы")
@Feature("Создание регламентной работы")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CreateRoutineWorkTest {

    private static final String BASE_URL  = "http://172.20.207.16";
    private static final String LOGIN     = "DChernikov@sbercity.ru";
    private static final String PASSWORD  = "G$h8pY}%ci~ZD%H1";

    // Справочные ID
    private static final String CLASSIFIER_EVENT_TYPE_ID   = "ea51b5e2-04cf-4e11-ab4a-bdbadd31780e";
    private static final String CLASSIFIER_EVENT_TYPE_NAME = "Благодарность";
    private static final String CLASSIFIER_MESTO1_ID       = "654b09d3-0fac-410d-b69d-2a475241b535";
    private static final String CLASSIFIER_MESTO1_NAME     = "Алина/Дима. sla 2 мин";
    private static final String RESPONSIBLE_ID             = "b69f0af0-43bd-4a37-b3c1-f68c123fde0c";
    private static final String RESPONSIBLE_NAME           = "Черников Дмитрий Витальевич";
    private static final String OBJECT_UK_ID               = "6d47b5b8-1fc4-4b94-addd-c5de4fefd1cf";
    private static final String OBJECT_UK_NAME             = "д.53, эт. 14, кв. 250";
    private static final String EXECUTION_TYPE_ID          = "b874f167-65ff-491a-bf17-aa4130e36c0c";
    private static final String EXECUTION_TYPE_NAME        = "Собственными силами";

    // ══════════════════════════════════════════════════════════
    //  Тестовые данные (статика без дат)
    // ══════════════════════════════════════════════════════════

    private static final String KIND_WORK_NAME        = "1234";

    private static final String FLOW_CHART_NAME       = "черников";
    private static final String FLOW_CHART_NOTE       = "<p>Описание</p>";
    private static final String FLOW_CHART_DESC_OPS   = "<p>Описание операций</p>";
    private static final String FLOW_CHART_DESC_RES   = "<p>Материалы</p>";

    private static final String REGULATION_NAME       = "тест";
    private static final String REGULATION_CONTRACT   = "Договор управление";
    private static final String REGULATION_CONTRACTOR = "―";
    private static final String REGULATION_CALENDAR   = "По неделям";

    private static final String ROUTINE_WORK_OBJ_NAME = "123";

    // ══════════════════════════════════════════════════════════
    //  Динамические даты — вычисляются один раз при старте класса
    //
    //  flowChart:   сегодня → сегодня + 23 дня
    //  regulation:  сегодня → сегодня + 17 дней
    // ══════════════════════════════════════════════════════════

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static final String FLOW_CHART_START;
    private static final String FLOW_CHART_STOP;
    private static final String REGULATION_START;
    private static final String REGULATION_STOP;

    static {
        Date today = new Date();

        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        FLOW_CHART_START = DATE_FORMAT.format(cal.getTime());

        cal.setTime(today);
        cal.add(Calendar.DAY_OF_MONTH, 23);
        FLOW_CHART_STOP = DATE_FORMAT.format(cal.getTime());

        cal.setTime(today);
        REGULATION_START = DATE_FORMAT.format(cal.getTime());

        cal.setTime(today);
        cal.add(Calendar.DAY_OF_MONTH, 17);
        REGULATION_STOP = DATE_FORMAT.format(cal.getTime());
    }

    // ══════════════════════════════════════════════════════════
    //  Состояние между шагами (static — живёт весь класс)
    // ══════════════════════════════════════════════════════════

    private static RequestSpecification authSpec;
    private static String kindWorkId;
    private static String flowChartId;
    private static String regulationId;
    private static String routineWorkId;
    private static String scheduledTimeSlotId;

    // ══════════════════════════════════════════════════════════
    //  Вспомогательный метод для создания HashMap
    // ══════════════════════════════════════════════════════════

    private static Map<String, Object> map(Object... keysAndValues) {
        Map<String, Object> m = new HashMap<String, Object>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            m.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return m;
    }

    // ══════════════════════════════════════════════════════════
    //  Инициализация RestAssured
    // ══════════════════════════════════════════════════════════

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // ══════════════════════════════════════════════════════════
    //  Очистка данных после всех тестов
    // ══════════════════════════════════════════════════════════

    @AfterAll
    static void cleanup() {
        if (authSpec == null) return;

        // Мягкое удаление через PUT с isDelete=true в теле запроса.
        // Порядок важен: сначала дочерние сущности, потом родительские.
        // Сначала получаем актуальный объект через GET, затем отправляем его обратно с isDelete=true.

        // 1. Удаляем регламентную работу
        if (routineWorkId != null) {
            softDelete("/api/regulation/routine-work/{id}", routineWorkId);
        }

        // 2. Удаляем регламент
        if (regulationId != null) {
            softDelete("/api/regulation/regulation-full/{id}", regulationId);
        }

        // 3. Удаляем технологическую карту
        if (flowChartId != null) {
            softDelete("/api/regulation/flow-chart-full/{id}", flowChartId);
        }

        // 4. Удаляем вид работы
        if (kindWorkId != null) {
            softDelete("/api/regulation/kind-work/{id}", kindWorkId);
        }
    }

    /**
     * Получает актуальный объект по GET, добавляет isDelete=true и отправляет PUT.
     * Ошибки не бросают исключение — выводятся в stderr, чтобы не скрывать результаты тестов.
     */
    @SuppressWarnings("unchecked")
    private static void softDelete(String path, String id) {
        try {
            // 1. Получаем текущее состояние объекта
            Response getResp = given().spec(authSpec)
                    .when()
                    .get(path, id);

            if (getResp.statusCode() != 200) {
                System.err.println("[cleanup] GET " + path + " id=" + id
                        + " вернул " + getResp.statusCode() + ", пропускаем удаление");
                return;
            }

            // 2. Десериализуем в Map и проставляем isDelete=true
            Map<String, Object> body = getResp.as(Map.class);
            body.put("isDelete", true);

            // 3. Отправляем PUT с обновлённым телом
            Response putResp = given().spec(authSpec)
                    .body(body)
                    .when()
                    .put(path, id);

            if (putResp.statusCode() != 200 && putResp.statusCode() != 204) {
                System.err.println("[cleanup] PUT " + path + " id=" + id
                        + " вернул " + putResp.statusCode()
                        + ": " + putResp.body().asString());
            }
        } catch (Exception e) {
            System.err.println("[cleanup] Ошибка при удалении " + path + " id=" + id + ": " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  ШАГ 1: Аутентификация
    // ══════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @Story("Аутентификация")
    @DisplayName("Шаг 1: POST /api/users/auth/login — аутентификация")
    void step01_login() {
        String accessToken = given()
                .contentType(ContentType.JSON)
                .body(map("login", LOGIN, "password", PASSWORD))
                .when()
                .post("/api/users/auth/login")
                .then()
                .statusCode(200)
                .body("type", equalTo("Bearer"))
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .extract()
                .jsonPath().getString("accessToken");

        assertThat(accessToken)
                .as("accessToken не должен быть пустым")
                .isNotBlank();

        authSpec = given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept-Language", "ru");
    }

    // ══════════════════════════════════════════════════════════
    //  ШАГ 2: Проверка текущего пользователя
    // ══════════════════════════════════════════════════════════

    @Test
    @Order(2)
    @Story("Аутентификация")
    @DisplayName("Шаг 2: GET /api/users/auth/info — проверка токена")
    void step02_getAuthInfo() {
        given()
                .spec(authSpec)
                .when()
                .get("/api/users/auth/info")
                .then()
                .statusCode(200)
                .body("login", equalTo(LOGIN))
                .body("id", notNullValue());
    }

    // ══════════════════════════════════════════════════════════
    //  ШАГ 3: Создание вида работы
    // ══════════════════════════════════════════════════════════

    @Test
    @Order(3)
    @Story("Вид работы")
    @DisplayName("Шаг 3: POST /api/regulation/kind-work — создание вида работы")
    void step03_createKindWork() {
        kindWorkId = given()
                .spec(authSpec)
                .body(map("name", KIND_WORK_NAME))
                .when()
                .post("/api/regulation/kind-work")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo(KIND_WORK_NAME))
                .extract()
                .jsonPath().getString("id");

        assertThat(kindWorkId)
                .as("id вида работы не должен быть пустым")
                .isNotBlank();
    }

    // ══════════════════════════════════════════════════════════
    //  ШАГ 4: Проверка вида работы по id
    // ══════════════════════════════════════════════════════════

    @Test
    @Order(4)
    @Story("Вид работы")
    @DisplayName("Шаг 4: GET /api/regulation/kind-work/{id} — проверка вида работы")
    void step04_getKindWork() {
        given()
                .spec(authSpec)
                .when()
                .get("/api/regulation/kind-work/{id}", kindWorkId)
                .then()
                .statusCode(200)
                .body("id", equalTo(kindWorkId))
                .body("name", equalTo(KIND_WORK_NAME));
    }

    // ══════════════════════════════════════════════════════════
    //  ШАГ 5: Создание технологической карты
    // ══════════════════════════════════════════════════════════

    @Test
    @Order(5)
    @Story("Технологическая карта")
    @DisplayName("Шаг 5: POST /api/regulation/flow-chart-full — создание технологической карты")
    void step05_createFlowChartFull() {
        Map<String, Object> body = map(
                "name",                   FLOW_CHART_NAME,
                "note",                   FLOW_CHART_NOTE,
                "descriptionOperations",  FLOW_CHART_DESC_OPS,
                "descriptionResource",    FLOW_CHART_DESC_RES,
                "startDate",              FLOW_CHART_START,
                "stopDate",               FLOW_CHART_STOP,
                "classifierEventType",    map("id", CLASSIFIER_EVENT_TYPE_ID, "name", CLASSIFIER_EVENT_TYPE_NAME),
                "classifierEventMesto1",  map("id", CLASSIFIER_MESTO1_ID,     "name", CLASSIFIER_MESTO1_NAME),
                "responsible",            map("id", RESPONSIBLE_ID,           "name", RESPONSIBLE_NAME),
                "kindWork",               map("id", kindWorkId,                "name", KIND_WORK_NAME)
        );

        flowChartId = given()
                .spec(authSpec)
                .body(body)
                .when()
                .post("/api/regulation/flow-chart-full")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo(FLOW_CHART_NAME))
                .body("kindWork.id", equalTo(kindWorkId))
                .body("responsible.id", equalTo(RESPONSIBLE_ID))
                .body("classifierEventType.id", equalTo(CLASSIFIER_EVENT_TYPE_ID))
                .body("classifierEventMesto1.id", equalTo(CLASSIFIER_MESTO1_ID))
                .extract()
                .jsonPath().getString("id");

        assertThat(flowChartId)
                .as("id технологической карты не должен быть пустым")
                .isNotBlank();
    }

    // ══════════════════════════════════════════════════════════
    //  ШАГ 6: Проверка технологической карты по id
    // ══════════════════════════════════════════════════════════

    @Test
    @Order(6)
    @Story("Технологическая карта")
    @DisplayName("Шаг 6: GET /api/regulation/flow-chart-full/{id} — проверка карты")
    void step06_getFlowChartFull() {
        given()
                .spec(authSpec)
                .when()
                .get("/api/regulation/flow-chart-full/{id}", flowChartId)
                .then()
                .statusCode(200)
                .body("id", equalTo(flowChartId))
                .body("name", equalTo(FLOW_CHART_NAME))
                .body("kindWork.id", equalTo(kindWorkId));
    }

    // ══════════════════════════════════════════════════════════
    //  ШАГ 7: Создание регламента
    // ══════════════════════════════════════════════════════════

    @Test
    @Order(7)
    @Story("Регламент")
    @DisplayName("Шаг 7: POST /api/regulation/regulation-full — создание регламента")
    void step07_createRegulationFull() {
        Map<String, Object> body = map(
                "name",          REGULATION_NAME,
                "contract",      REGULATION_CONTRACT,
                "contractor",    REGULATION_CONTRACTOR,
                "calendarView",  REGULATION_CALENDAR,
                "startDate",     REGULATION_START,
                "stopDate",      REGULATION_STOP,
                "executionType", map("id", EXECUTION_TYPE_ID, "name", EXECUTION_TYPE_NAME)
        );

        regulationId = given()
                .spec(authSpec)
                .body(body)
                .when()
                .post("/api/regulation/regulation-full")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo(REGULATION_NAME))
                .body("contract", equalTo(REGULATION_CONTRACT))
                .body("calendarView", equalTo(REGULATION_CALENDAR))
                .body("executionType.id", equalTo(EXECUTION_TYPE_ID))
                .extract()
                .jsonPath().getString("id");

        assertThat(regulationId)
                .as("id регламента не должен быть пустым")
                .isNotBlank();
    }

    // ══════════════════════════════════════════════════════════
    //  ШАГ 8: Проверка регламента по id
    // ══════════════════════════════════════════════════════════

    @Test
    @Order(8)
    @Story("Регламент")
    @DisplayName("Шаг 8: GET /api/regulation/regulation-full/{id} — проверка регламента")
    void step08_getRegulationFull() {
        given()
                .spec(authSpec)
                .when()
                .get("/api/regulation/regulation-full/{id}", regulationId)
                .then()
                .statusCode(200)
                .body("id", equalTo(regulationId))
                .body("name", equalTo(REGULATION_NAME))
                .body("calendarView", equalTo(REGULATION_CALENDAR));
    }

    // ══════════════════════════════════════════════════════════
    //  ШАГ 9: Убеждаемся, что работ пока нет
    // ══════════════════════════════════════════════════════════

    @Test
    @Order(9)
    @Story("Регламентная работа")
    @DisplayName("Шаг 9: GET .../routine-work/full-by-regulation/{id} — список пуст до создания")
    void step09_checkNoRoutineWorksBefore() {
        List<?> works = given()
                .spec(authSpec)
                .queryParam("sort", "number,desc")
                .queryParam("page", 0)
                .queryParam("size", 10000)
                .when()
                .get("/api/regulation/routine-work/full-by-regulation/{id}", regulationId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath().getList("workWithTimeSlots");

        assertThat(works)
                .as("Список регламентных работ должен быть пустым до создания")
                .isEmpty();
    }

    // ══════════════════════════════════════════════════════════
    //  ШАГ 10: Создание регламентной работы с тайм-слотами
    // ══════════════════════════════════════════════════════════

    @Test
    @Order(10)
    @Story("Регламентная работа")
    @DisplayName("Шаг 10: POST .../routine-work/create-with-time-slots — создание регламентной работы")
    void step10_createRoutineWorkWithTimeSlots() {
        Map<String, Object> body = map(
                "nameObject",  ROUTINE_WORK_OBJ_NAME,
                "acceptance",  false,
                "regulation",  map(
                        "id",           regulationId,
                        "calendarView", REGULATION_CALENDAR,
                        "startDate",    REGULATION_START,
                        "stopDate",     REGULATION_STOP
                ),
                "objectUk",    map("id", OBJECT_UK_ID,    "name", OBJECT_UK_NAME),
                "flowChart",   map("id", flowChartId,     "name", FLOW_CHART_NAME),
                "responsible", map("id", RESPONSIBLE_ID,  "name", RESPONSIBLE_NAME),
                "kindWork",    map("id", kindWorkId,       "name", KIND_WORK_NAME)
        );

        Response resp = given()
                .spec(authSpec)
                .body(body)
                .when()
                .post("/api/regulation/routine-work/create-with-time-slots")
                .then()
                .statusCode(200)
                .body("routineWork.id", notNullValue())
                .body("routineWork.nameObject", equalTo(ROUTINE_WORK_OBJ_NAME))
                .body("routineWork.regulation.id", equalTo(regulationId))
                .body("routineWork.flowChart.id", equalTo(flowChartId))
                .body("routineWork.objectUk.id", equalTo(OBJECT_UK_ID))
                .body("timeSlots", not(empty()))
                .extract().response();

        routineWorkId = resp.jsonPath().getString("routineWork.id");

        // Берём второй тайм-слот (индекс 1) — соответствует следующей неделе (как в HAR)
        List<String> timeSlotIds = resp.jsonPath().getList("timeSlots.id");
        assertThat(timeSlotIds)
                .as("Должно быть создано больше одного тайм-слота")
                .hasSizeGreaterThan(1);

        scheduledTimeSlotId = timeSlotIds.get(1);
    }

    // ══════════════════════════════════════════════════════════
    //  ШАГ 11: Планирование тайм-слота
    // ══════════════════════════════════════════════════════════

    @Test
    @Order(11)
    @Story("Тайм-слот")
    @DisplayName("Шаг 11: POST /api/regulation/time-slot/scheduled — планирование тайм-слота")
    void step11_scheduleTimeSlot() {
        given()
                .spec(authSpec)
                .body(Arrays.asList(scheduledTimeSlotId))
                .when()
                .post("/api/regulation/time-slot/scheduled")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("[0].id", equalTo(scheduledTimeSlotId))
                .body("[0].statusTimeSlot.name", equalTo("ЗАПЛАНИРОВАНО"));
    }

    // ══════════════════════════════════════════════════════════
    //  ШАГ 12: Создание задачи по тайм-слоту
    // ══════════════════════════════════════════════════════════

    @Test
    @Order(12)
    @Story("Тайм-слот")
    @DisplayName("Шаг 12: POST /api/regulation/time-slot/create-task — создание задачи")
    void step12_createTaskFromTimeSlot() {
        Response resp = given()
                .spec(authSpec)
                .body(Arrays.asList(scheduledTimeSlotId))
                .when()
                .post("/api/regulation/time-slot/create-task")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("[0].id", equalTo(scheduledTimeSlotId))
                .body("[0].statusTimeSlot.name", equalTo("НОВОЕ"))
                .body("[0].task", notNullValue())
                .body("[0].taskName", notNullValue())
                .extract().response();

        assertThat(resp.jsonPath().getString("[0].taskName"))
                .as("taskName не должен быть пустым")
                .isNotBlank();
    }

    // ══════════════════════════════════════════════════════════
    //  ШАГ 13: Финальная проверка — работа появилась в регламенте
    // ══════════════════════════════════════════════════════════

    @Test
    @Order(13)
    @Story("Регламентная работа")
    @DisplayName("Шаг 13: GET .../routine-work/full-by-regulation/{id} — работа присутствует в регламенте")
    void step13_verifyRoutineWorkInRegulation() {
        List<String> workIds = given()
                .spec(authSpec)
                .queryParam("sort", "number,desc")
                .queryParam("page", 0)
                .queryParam("size", 10000)
                .when()
                .get("/api/regulation/routine-work/full-by-regulation/{id}", regulationId)
                .then()
                .statusCode(200)
                .body("workWithTimeSlots", not(empty()))
                .extract()
                .jsonPath().getList("workWithTimeSlots.routineWork.id");

        assertThat(workIds)
                .as("Созданная регламентная работа id=%s должна присутствовать в регламенте", routineWorkId)
                .contains(routineWorkId);
    }
}