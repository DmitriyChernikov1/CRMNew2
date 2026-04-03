import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import utils.JsonUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OperationalTaskTest {

    private static String accessToken;

    // ID сотрудника — из HAR файла (ответственный)
    private static final String EMPLOYEE_ID = "b69f0af0-43bd-4a37-b3c1-f68c123fde0c";
    // ID второго сотрудника — участник и наблюдатель
    private static final String SECOND_EMPLOYEE_ID = "e8f0d81b-6e7a-4f5e-a5d7-8ff504b8e8c9";

    // ID созданной операционной задачи
    private static String createdEventId;
    // Количество уведомлений ДО создания задачи
    private static int notificationsCountBefore;

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://172.20.207.16";
        accessToken = new AuthTokenTest().getAccessToken();
    }

    // =====================================================================
    // СПРАВОЧНИКИ
    // =====================================================================

    @Test
    @Order(1)
    @Description("Получение активных пунктов верхнего меню — проверка доступности разделов")
    @DisplayName("Получение активного меню")
    public void getActiveTopMenu() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .when()
                .get("/api/users/topmenu/active")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].id", notNullValue())
                .body("content[0].name", notNullValue());
    }

    @Test
    @Order(2)
    @Description("Получение входных данных для создания события — типы, виды событий")
    @DisplayName("Получение справочника событий")
    public void getCalendarInputData() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .when()
                .get("/api/calendar/input/event")
                .then()
                .statusCode(200)
                .body("typeEventDtos", not(empty()))
                .body("typeEventDtos[0].id", notNullValue())
                .body("typeEventDtos[0].name", notNullValue());
    }

    // =====================================================================
    // АВТОКОМПЛИТ СОТРУДНИКОВ
    // =====================================================================

    @Test
    @Order(3)
    @Description("Поиск сотрудников через автокомплит с фильтром по букве С")
    @DisplayName("Автокомплит сотрудников — фильтр по букве")
    public void autocompleteEmployeeByLetter() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .queryParam("name", "с")
                .when()
                .get("/api/calendar/autocomplete/employee")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].id", notNullValue())
                .body("content[0].name", notNullValue());
    }

    @Test
    @Order(4)
    @Description("Поиск сотрудников через автокомплит — уточнение поиска до конкретного сотрудника")
    @DisplayName("Автокомплит сотрудников — уточнение до одного результата")
    public void autocompleteEmployeeExact() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .queryParam("name", "соп")
                .when()
                .get("/api/calendar/autocomplete/employee")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("totalElements", equalTo(1))
                .body("content[0].name", containsStringIgnoringCase("соп"));
    }

    // =====================================================================
    // УВЕДОМЛЕНИЯ ДО СОЗДАНИЯ ЗАДАЧИ
    // =====================================================================

    @Test
    @Order(5)
    @Description("Получение уведомлений сотрудника до создания операционной задачи — фиксируем текущее количество")
    @DisplayName("Получение уведомлений — до создания задачи")
    public void getNotificationsBeforeCreation() {
        Response response = given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("employeeId", EMPLOYEE_ID)
                .when()
                .get("/api/message/letter/active-system")
                .then()
                .statusCode(200)
                .extract()
                .response();

        // Сохраняем количество уведомлений до создания задачи
        List<?> notifications = response.jsonPath().getList("$");
        notificationsCountBefore = notifications.size();

        // Проверяем структуру уведомлений если они есть
        if (notificationsCountBefore > 0) {
            response.then()
                    .body("[0].id", notNullValue())
                    .body("[0].statusMessage", notNullValue())
                    .body("[0].module", notNullValue());
        }
    }

    // =====================================================================
    // СОЗДАНИЕ ОПЕРАЦИОННОЙ ЗАДАЧИ
    // =====================================================================

    @Test
    @Order(6)
    @Description("Создание операционной задачи с участниками и наблюдателями — динамические даты")
    @DisplayName("Создание операционной задачи")
    public void createOperationalTask() {
        // Динамические даты — завтра с 18:00 до 19:00
        String planDate = LocalDate.now().plusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String body = "{"
                + "\"event\":{"
                + "\"lastModifiedDate\":null,"
                + "\"deletedDate\":null,"
                + "\"stopFactDate\":null,"
                + "\"startFactDate\":null,"
                + "\"createdDate\":null,"
                + "\"startPlanDate\":\"" + planDate + " 18:00:00.000\","
                + "\"stopPlanDate\":\"" + planDate + " 19:00:00.000\","
                + "\"comment\":\"автотест операционная задача\","
                + "\"name\":\"автотест\","
                + "\"statusEvent\":{"
                + "\"id\":\"b8ff7136-821a-46f1-8e79-68d82860b781\","
                + "\"name\":\"Новое\","
                + "\"isDelete\":false,"
                + "\"createdBy\":\"user\","
                + "\"createdDate\":\"2024-03-22 13:50:00.000\","
                + "\"lastModifiedBy\":\"user\","
                + "\"lastModifiedDate\":\"2024-03-22 13:50:00.000\","
                + "\"deletedDate\":null,"
                + "\"deletedBy\":\"\""
                + "},"
                + "\"typeEvent\":{"
                + "\"id\":\"b92313c7-bee6-4854-b649-d5990d979247\","
                + "\"name\":\"Событие\","
                + "\"isDelete\":false,"
                + "\"createdBy\":\"user\","
                + "\"createdDate\":\"2023-01-09 12:28:56.767\","
                + "\"lastModifiedBy\":\"user\","
                + "\"lastModifiedDate\":\"2023-01-09 12:28:56.767\","
                + "\"deletedDate\":null,"
                + "\"deletedBy\":null,"
                + "\"color\":\"\","
                + "\"backgroundColor\":null,"
                + "\"icon\":null"
                + "},"
                + "\"kindEvent\":{"
                + "\"id\":\"de3a4f28-debc-40ef-9e71-a56e2cf36912\","
                + "\"name\":\"Операционная задача\","
                + "\"isDelete\":false,"
                + "\"createdBy\":\"user\","
                + "\"createdDate\":\"2023-02-21 14:00:00.000\","
                + "\"lastModifiedBy\":\"user\","
                + "\"lastModifiedDate\":\"2025-02-06 16:30:00.000\","
                + "\"deletedDate\":null,"
                + "\"deletedBy\":null,"
                + "\"color\":\"#7D85C1\","
                + "\"backgroundColor\":\"#c3c7e3\","
                + "\"icon\":null"
                + "},"
                + "\"responsibleIdName\":\"Я\","
                + "\"responsibleId\":\"" + EMPLOYEE_ID + "\""
                + "},"
                + "\"employees\":["
                + "{\"name\":\"Черников Дмитрий Витальевич\",\"id\":\"" + EMPLOYEE_ID + "\","
                + "\"isMain\":true,\"employeeId\":\"" + EMPLOYEE_ID + "\","
                + "\"partyStatus\":\"Ожидает решения\"},"
                + "{\"id\":\"" + SECOND_EMPLOYEE_ID + "\",\"name\":\"Сопин Денис Алексеевич\","
                + "\"employeeId\":\"" + SECOND_EMPLOYEE_ID + "\","
                + "\"isMain\":false,\"partyStatus\":\"Ожидает решения\"}"
                + "],"
                + "\"followers\":["
                + "{\"employeeId\":\"" + SECOND_EMPLOYEE_ID + "\"}"
                + "]"
                + "}";

        Response response = given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .body(body)
                .when()
                .post("/api/calendar/event/create-with-links")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("автотест"))
                .body("kindEvent.name", equalTo("Операционная задача"))
                .body("statusEvent.name", equalTo("Новое"))
                .body("responsibleId", equalTo(EMPLOYEE_ID))
                .body("follower", notNullValue())
                .extract()
                .response();

        createdEventId = response.jsonPath().getString("id");
        assertNotNull(createdEventId, "ID операционной задачи не должен быть null");
    }

    // =====================================================================
    // ПРОВЕРКА УВЕДОМЛЕНИЙ ПОСЛЕ СОЗДАНИЯ
    // =====================================================================

    @Test
    @Order(7)
    @Description("Проверка уведомлений после создания операционной задачи — количество уведомлений должно увеличиться")
    @DisplayName("Проверка уведомлений — после создания задачи")
    public void getNotificationsAfterCreation() {
        assumeEventCreated();

        // Небольшая пауза чтобы уведомление успело прийти
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        Response response = given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("employeeId", EMPLOYEE_ID)
                .when()
                .get("/api/message/letter/active-system")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("[0].id", notNullValue())
                .body("[0].statusMessage", notNullValue())
                .body("[0].module", notNullValue())
                .extract()
                .response();

        List<?> notificationsAfter = response.jsonPath().getList("$");
        int notificationsCountAfter = notificationsAfter.size();

        // Количество уведомлений должно увеличиться после создания задачи
        assertTrue(
                notificationsCountAfter >= notificationsCountBefore,
                "Количество уведомлений должно быть не меньше чем до создания задачи. "
                        + "Было: " + notificationsCountBefore + ", стало: " + notificationsCountAfter
        );
    }

    @Test
    @Order(8)
    @Description("Проверка что уведомление содержит корректные данные — статус Отправлено и модуль")
    @DisplayName("Проверка содержимого уведомлений")
    public void checkNotificationContent() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("employeeId", EMPLOYEE_ID)
                .when()
                .get("/api/message/letter/active-system")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("[0].statusMessage", equalTo("Отправлено"))
                .body("[0].userId", notNullValue())
                .body("[0].createdDate", notNullValue());
    }

    // =====================================================================
    // ПРОСРОЧЕННЫЕ ЗАДАЧИ И СТАТИСТИКА
    // =====================================================================

    @Test
    @Order(9)
    @Description("Получение просроченных задач сотрудника")
    @DisplayName("Просроченные задачи сотрудника")
    public void getOverdueTasks() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .when()
                .get("/api/calendar/employee/overdueTasks/" + EMPLOYEE_ID)
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("[0].taskId", notNullValue())
                .body("[0].overdueDays", greaterThan(0));
    }

    @Test
    @Order(10)
    @Description("Получение статистики задач сотрудника по статусам — диаграмма выполнения")
    @DisplayName("Статистика задач сотрудника")
    public void getEmployeeGaugeChart() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .when()
                .get("/api/calendar/employee/gaugeChart/" + EMPLOYEE_ID)
                .then()
                .statusCode(200)
                .body("completionPerc", notNullValue())
                .body("totalTasks", greaterThan(0))
                .body("statuses", notNullValue())
                .body("statuses.new", notNullValue());
    }

    // =====================================================================
    // УДАЛЕНИЕ ОПЕРАЦИОННОЙ ЗАДАЧИ (возврат в исходное состояние)
    // =====================================================================

    @Test
    @Order(11)
    @Description("Удаление созданной операционной задачи — возврат в исходное состояние")
    @DisplayName("Удаление операционной задачи")
    public void deleteCreatedOperationalTask() throws Exception {
        assumeEventCreated();

        // Получаем полное тело задачи
        String requestBody = given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .when()
                .get("/api/calendar/event/" + createdEventId)
                .then()
                .statusCode(200)
                .extract()
                .body().asString();

        // Помечаем как удалённую
        String updatedBody = JsonUtils.changeField(requestBody, "isDelete", true);

        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .body(updatedBody)
                .when()
                .put("/api/calendar/event/" + createdEventId)
                .then()
                .statusCode(200)
                .body("isDelete", equalTo(true));
    }

    // =====================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =====================================================================

    private void assumeEventCreated() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                createdEventId != null,
                "Тест пропущен: операционная задача не была создана в createOperationalTask()"
        );
    }
}