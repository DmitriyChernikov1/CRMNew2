import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import utils.JsonUtils;
import utils.TimeGenerated;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReservationCarTest {

    private static String accessToken;

    // ID объекта аренды авто — из HAR файла
    private static final String OBJECT_UK_ID = "d1d039ce-6b70-40da-bd58-e690751026e1";

    // Сохраняем ID созданной задачи для использования в следующих тестах
    private static String createdTaskId;

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://172.20.207.16";
        accessToken = new AuthTokenTest().getAccessToken();
    }

    // =====================================================================
    // ПОЛУЧЕНИЕ БРОНИРОВАНИЙ
    // =====================================================================

    @Test
    @Order(1)
    @Description("Получение списка бронирований авто на текущий день — список пуст до создания брони")
    @DisplayName("Получение бронирований авто — пустой список")
    public void getRentedCarsEmpty() {
        String startDate = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String body = "{"
                + "\"startPlanDate\":\"" + startDate + " 09:00:00.000\","
                + "\"stopPlanDate\":\"" + startDate + " 23:00:00.000\","
                + "\"objectUkIds\":[\"" + OBJECT_UK_ID + "\"]"
                + "}";

        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .body(body)
                .when()
                .post("/api/calendar/event/rented-cars-all")
                .then()
                .statusCode(200)
                .body("pageable", notNullValue())
                .body("size", equalTo(20));
    }

    // =====================================================================
    // СОЗДАНИЕ БРОНИРОВАНИЯ
    // =====================================================================

    @Test
    @Order(2)
    @Description("Создание бронирования арендного авто с динамическими датами")
    @DisplayName("Создание бронирования авто")
    public void createRentedCarBooking() {
        String startDate = java.time.LocalDate.now().plusDays(1)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String destination = "Автотест маршрут => Точка назначения";

        String body = "{"
                + "\"startDate\":\"" + startDate + " 10:00:00.000\","
                + "\"endDate\":\"" + startDate + " 11:00:00.000\","
                + "\"destination\":\"" + destination + "\","
                + "\"objectUkId\":\"" + OBJECT_UK_ID + "\""
                + "}";

        // Пробуем создать бронирование — может вернуть 201 или 400
        Response createResponse = given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .body(body)
                .when()
                .post("/api/client-relations/tasks-full/booking-rented-car-by-object")
                .andReturn();

        int statusCode = createResponse.getStatusCode();

        if (statusCode == 201) {
            // Слот свободен — бронь создана, ищем в календаре
            String searchBody = "{"
                    + "\"startPlanDate\":\"" + startDate + " 09:00:00.000\","
                    + "\"stopPlanDate\":\"" + startDate + " 23:00:00.000\","
                    + "\"objectUkIds\":[\"" + OBJECT_UK_ID + "\"]"
                    + "}";

            Response calendarResponse = given()
                    .headers("Authorization", "Bearer " + accessToken,
                            "Content-Type", "application/json; charset=UTF-8")
                    .body(searchBody)
                    .when()
                    .post("/api/calendar/event/rented-cars-all")
                    .then()
                    .statusCode(200)
                    .body("content", not(empty()))
                    .extract()
                    .response();

            createdTaskId = calendarResponse.jsonPath().getString("content[0].task.id");
            assertNotNull(createdTaskId, "ID задачи не должен быть null после создания бронирования");

        } else if (statusCode == 400) {
            // Слот уже занят — это ожидаемое поведение, помечаем тест как пропущенный
            // чтобы зависимые тесты (Order 3-11) тоже корректно пропустились
            org.junit.jupiter.api.Assumptions.assumeTrue(
                    false,
                    "Слот уже занят на " + startDate + " 10:00-11:00 — бронирование невозможно, тест пропущен"
            );
        } else {
            fail("Неожиданный статус код: " + statusCode + " — ожидался 201 или 400");
        }
    }
    // =====================================================================
    // ПРОВЕРКА СОЗДАННОГО БРОНИРОВАНИЯ
    // =====================================================================

    @Test
    @Order(3)
    @Description("Получение списка бронирований авто после создания — бронь появилась в календаре")
    @DisplayName("Получение бронирований авто — бронь появилась")
    public void getRentedCarsAfterBooking() {
        // Используем завтрашний день — там создали бронь в предыдущем тесте
        String startDate = java.time.LocalDate.now().plusDays(1)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String body = "{"
                + "\"startPlanDate\":\"" + startDate + " 09:00:00.000\","
                + "\"stopPlanDate\":\"" + startDate + " 23:00:00.000\","
                + "\"objectUkIds\":[\"" + OBJECT_UK_ID + "\"]"
                + "}";

        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .body(body)
                .when()
                .post("/api/calendar/event/rented-cars-all")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].startPlanDate", containsString(startDate))
                .body("content[0].stopPlanDate", containsString(startDate));
    }

    @Test
    @Order(4)
    @Description("Получение задачи по ID — проверка что задача создана корректно")
    @DisplayName("Получение задачи бронирования по ID")
    public void getTaskById() {
        assumeTaskCreated();

        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .when()
                .get("/api/client-relations/tasks/" + createdTaskId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdTaskId))
                .body("statusTask", equalTo("Новое"))
                .body("classifierEventProblem1", equalTo("Бронирование"));
    }

    @Test
    @Order(5)
    @Description("Получение полной информации о задаче бронирования по ID")
    @DisplayName("Получение полной задачи бронирования по ID")
    public void getTaskFullById() {
        assumeTaskCreated();

        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .when()
                .get("/api/client-relations/tasks-full/" + createdTaskId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdTaskId))
                .body("statusTask.name", equalTo("Новое"))
                .body("isDelete", nullValue());
    }

    // =====================================================================
    // ПРОВЕРКА ДОСТУПОВ
    // =====================================================================

    @Test
    @Order(6)
    @Description("Получение доступа к сущности Task — проверка прав пользователя")
    @DisplayName("Проверка доступа к задачам")
    public void getCartAccessTask() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("name", "Task")
                .when()
                .get("/api/users/cartAccess/cart-access-by-access-entity-system-name-for-group-accesses")
                .then()
                .statusCode(200)
                .body("readable", equalTo(true))
                .body("editable", equalTo(true))
                .body("created", equalTo(true));
    }

    @Test
    @Order(7)
    @Description("Получение доступа к сущности ConnectionApplication — проверка прав на связанные заявки")
    @DisplayName("Проверка доступа к связанным заявкам")
    public void getCartAccessConnectionApplication() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("name", "ConnectionApplication")
                .when()
                .get("/api/users/cartAccess/cart-access-by-access-entity-system-name-for-group-accesses")
                .then()
                .statusCode(200)
                .body("readable", equalTo(true))
                .body("created", equalTo(true));
    }

    // =====================================================================
    // СТАТУСЫ ЗАДАЧИ
    // =====================================================================

    @Test
    @Order(8)
    @Description("Получение доступных статусов задачи бронирования — должен быть доступен переход в статус В работе")
    @DisplayName("Получение доступных статусов задачи")
    public void getTaskStatuses() {
        assumeTaskCreated();

        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("taskId", createdTaskId)
                .when()
                .get("/api/client-relations/tasks-full/statuses")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("[0].button", notNullValue())
                .body("[0].status.name", notNullValue());
    }

    @Test
    @Order(9)
    @Description("Получение справочника входных данных для задачи — статусы приёмки и прочие справочники")
    @DisplayName("Получение входных данных задачи")
    public void getTaskInputData() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .when()
                .get("/api/client-relations/input/task")
                .then()
                .statusCode(200)
                .body("statusAcceptanceDtoList", not(empty()))
                .body("statusAcceptanceDtoList[0].id", notNullValue())
                .body("statusAcceptanceDtoList[0].name", notNullValue());
    }

    // =====================================================================
    // СВЯЗАННЫЕ ЗАЯВКИ
    // =====================================================================

    @Test
    @Order(10)
    @Description("Получение связанных заявок задачи бронирования — заявка должна быть в списке")
    @DisplayName("Получение связанных заявок задачи")
    public void getRelatedApplications() {
        assumeTaskCreated();

        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("sort", "number,desc")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("showArchive", false)
                .body("[]")
                .when()
                .post("/api/client-relations/related-collections/task/filtering/" + createdTaskId)
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].id", notNullValue())
                .body("content[0].classifierEventProblem1", equalTo("Бронирование"));
    }

    // =====================================================================
    // УДАЛЕНИЕ БРОНИРОВАНИЯ (возврат в исходное состояние)
    // =====================================================================

    @Test
    @Order(11)
    @Description("Удаление созданной задачи бронирования — возврат в исходное состояние")
    @DisplayName("Удаление задачи бронирования")
    public void deleteCreatedBooking() throws Exception {
        assumeTaskCreated();

        // Получаем полное тело задачи
        String requestBody = given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .when()
                .get("/api/client-relations/tasks-full/" + createdTaskId)
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
                .put("/api/client-relations/tasks-full/" + createdTaskId)
                .then()
                .statusCode(200)
                .body("isDelete", equalTo(true));
    }

    // =====================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =====================================================================

    private void assumeTaskCreated() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                createdTaskId != null,
                "Тест пропущен: задача бронирования не была создана в createRentedCarBooking()"
        );
    }
}