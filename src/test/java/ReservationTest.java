import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import utils.TimeGenerated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(BaseTest.class)
public class ReservationTest {

    // UUID объектов
    private static final String OBJECT_UK_ID_VATS    = "f239537b-c3bf-4ded-8b10-ef1dc8ff55ac";
    private static final String OBJECT_UK_ID_FORD    = "b71a6e71-da3a-46e7-a86f-0e0f0f8ccf67";
    private static final String OBJECT_UK_ID_USUAL   = "8a65a600-a0e8-4526-9fb0-d6d87d0eca84";
    private static final String OBJECT_UK_ID_BOOKING = "e3e8c7dd-3085-47ce-a0dc-7cbaca264705";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // Токен получается ОДИН раз для всего класса
    private static String accessToken;

    @BeforeAll
    static void setUp() {
        accessToken = new AuthTokenTest().getAccessToken();
    }

    // ===== Вспомогательные методы =====

    public static Map<String, Object> createReservationRequestBody() {
        return createReservationRequestBody(OBJECT_UK_ID_VATS, 9, 23);
    }

    public static Map<String, Object> createReservationRequestBody(
            String objectUkId,
            int startHour,
            int endHour
    ) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("startPlanDate", getFormattedDateTime(startHour));
        requestBody.put("stopPlanDate", getFormattedDateTime(endHour));
        requestBody.put("objectUkIds", Collections.singletonList(objectUkId));
        return requestBody;
    }

    private static String getFormattedDateTime(int hour) {
        return LocalDateTime.now()
                .withHour(hour)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .format(DATE_FORMATTER);
    }

    // ===== Тесты =====

    @Test
    @Description("Бронирование")
    @DisplayName("Создание бронирования авто")
    public void createReservation() {
        String requestBody = String.format(
                "{\n" +
                        "\"destination\": \"Автотест\",\n" +
                        "\"startDate\": \"%s\",\n" +
                        "\"endDate\": \"%s\"\n" +
                        "}", TimeGenerated.generateTimeStart(), TimeGenerated.generateTimeEnd());

        Response response = given()
                .log().all()
                .body(requestBody)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json; charset=UTF-8")
                .post("http://172.20.207.16/api/client-relations/tasks-full/booking-rented-car")
                .andReturn();

        response.prettyPrint();
        assertEquals(201, response.getStatusCode());
    }

    @Test
    @Description("Бронирование")
    @DisplayName("Получение списка записей")
    public void getReservation() {
        String date = TimeGenerated.planeDate();

        Response response = given()
                .body(date)
                .header("accept", "*/*")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json; charset=UTF-8")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .post("http://172.20.207.16:5555/calendar/event/rented-cars");

        response.prettyPrint();
        assertEquals(200, response.getStatusCode());
    }

    @Test
    @Description("Бронирование")
    @DisplayName("Создание бронирования авто Форд")
    public void createReservationFord() {
        String requestBody = String.format(
                "{\n" +
                        "\"destination\": \"Автотест\",\n" +
                        "\"startDate\": \"%s\",\n" +
                        "\"endDate\": \"%s\",\n" +
                        "\"objectUkId\": \"%s\"\n" +
                        "}", TimeGenerated.generateTimeStart(), TimeGenerated.generateTimeEnd(), OBJECT_UK_ID_FORD);

        Response response = given()
                .log().all()
                .body(requestBody)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json; charset=UTF-8")
                .post("http://172.20.207.16/api/client-relations/tasks-full/booking-rented-car-ford")
                .then()
                .log().all()
                .extract()
                .response();

        assertEquals(201, response.getStatusCode());
    }

    @Test
    @Description("Бронирование")
    @DisplayName("Получение списка записей Форд")
    public void getReservationFord() {
        String requestBody = "{\"startPlanDate\":\"" + TimeGenerated.getTodayStartTime() + "\","
                + "\"stopPlanDate\":\"" + TimeGenerated.getTodayEndTime() + "\","
                + "\"objectUkIds\":[\"" + OBJECT_UK_ID_FORD + "\"]}";

        Response response = given()
                .log().all()
                .body(requestBody)
                .header("accept", "*/*")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json; charset=UTF-8")
                .post("http://172.20.207.16/api/calendar/event/rented-cars-ford")
                .then()
                .log().all()
                .extract()
                .response();

        response.prettyPrint();
        assertEquals(200, response.getStatusCode());
    }

    @Test
    @Description("Бронирование")
    @DisplayName("Создание бронирования авто Vats")
    public void createReservationVats() {
        String requestBody = String.format(
                "{\n" +
                        "\"destination\": \"Автотест\",\n" +
                        "\"startDate\": \"%s\",\n" +
                        "\"endDate\": \"%s\",\n" +
                        "\"objectUkId\": \"%s\"\n" +
                        "}", TimeGenerated.generateTimeStart(), TimeGenerated.generateTimeEnd(), OBJECT_UK_ID_VATS);

        Response response = given()
                .log().all()
                .body(requestBody)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json; charset=UTF-8")
                .post("http://172.20.207.16/api/client-relations/tasks-full/booking-rented-car-hav")
                .then()
                .log().all()
                .extract()
                .response();

        assertEquals(201, response.getStatusCode());
    }

    @Test
    @Description("Бронирование")
    @DisplayName("Получение списка записей Ватс")
    public void getReservationVats() {
        Map<String, Object> requestBody = ReservationTest.createReservationRequestBody();

        Response response = given()
                .body(requestBody)
                .header("accept", "*/*")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json; charset=UTF-8")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .post("http://172.20.207.16/api/calendar/event/rented-cars-hav");

        response.prettyPrint();
        assertEquals(200, response.getStatusCode());
    }

    @Test
    @Description("Создание обычного бронирования объекта")
    @DisplayName("Создание обычного бронирования объекта")
    public void createUsualReservation() {
        String requestBody = "{\"startDate\":\"" + TimeGenerated.generateTimeStart() + "\","
                + "\"endDate\":\"" + TimeGenerated.generateTimeEnd() + "\","
                + "\"destination\":\"Штаб строительства (центральный вход) => А23\","
                + "\"objectUkId\":\"" + OBJECT_UK_ID_USUAL + "\"}";

        Response response = RestAssured
                .given()
                .body(requestBody)
                .header("accept", "*/*")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json; charset=UTF-8")
                .post("http://172.20.207.16/api/client-relations/tasks-full/booking-rented-car-by-object");
        response.prettyPrint();
        if (response.getStatusCode() == 201) {
            // Успешное бронирование
            assertEquals(201, response.getStatusCode());
        } else if (response.getStatusCode() == 400) {
            // Проверяем, что это ожидаемая ошибка
            String errorMessage = response.jsonPath().getString("message");
            assertEquals("At this time, all cars are booked", errorMessage);
        } else {
            // Любой другой код - ошибка
            fail("Unexpected status code: " + response.getStatusCode());
        }
    }

    @Test
    @Description("Получение списка объектов")
    @DisplayName("Получение списка объектов")
    public void getRentedCarsObjectUk() {
        Response response = RestAssured
                .given()
                .header("accept", "*/*")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json; charset=UTF-8")
                .post("http://172.20.207.16/api/calendar/event/rented-cars-objectUk");

        response.prettyPrint();
        assertEquals(200, response.getStatusCode());
    }

    @Test
    @Description("Бронирование авто")
    @DisplayName("Общее бронирование")
    public void bookingRentedCarByObject() {
        String requestBody = "{\"startDate\":\"" + TimeGenerated.generateTimeStart() + "\","
                + "\"endDate\":\"" + TimeGenerated.generateTimeEnd() + "\","
                + "\"destination\":\"Штаб строительства (центральный вход) => А23\","
                + "\"objectUkId\":\"" + OBJECT_UK_ID_BOOKING + "\"}";

        Response response = RestAssured
                .given()
                .body(requestBody)
                .header("accept", "*/*")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json; charset=UTF-8")
                .post("http://172.20.207.16/api/client-relations/tasks-full/booking-rented-car-by-object")
                .andReturn();

        response.prettyPrint();
        assertEquals(201, response.getStatusCode());
    }
}