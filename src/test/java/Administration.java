import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utils.TestDataJson;

import static org.junit.jupiter.api.Assertions.*;

public class Administration {
    private static String accessToken;

    private static final String BASE_URL = "http://172.20.207.16";


    @BeforeAll
    static void setUp() {
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
    @Test
    @Description("putUsers")
    @DisplayName("Изменение пользователя")
    public void putUser() {

        String body = TestDataJson.bodyForUser;
        Response putUser = authRequest()
                .given()
                .body(body)
                .put("/api/users/user_employee/c1934b45-4752-4c4d-8d52-86c749bfacba")
                .andReturn();

        int statusCode = putUser.getStatusCode();
        assertEquals(200, statusCode);
        putUser.prettyPrint();
    }
    @Test
    @Description("CreateUsers")
    @DisplayName("Создание и удаление пользователя")
    public void createUser() {

        // Шаг 1 — создание пользователя
        String createBody = TestDataJson.CreateUser;

        Response createResponse = authRequest()
                .body(createBody)
                .post("/api/users/user_employee")
                .andReturn();

        createResponse.prettyPrint();
        assertEquals(200, createResponse.getStatusCode());

        String userId     = createResponse.jsonPath().getString("userId");
        String employeeId = createResponse.jsonPath().getString("employeeId");
        String userLogin  = createResponse.jsonPath().getString("userLogin");
        String userName   = createResponse.jsonPath().getString("userName");

        System.out.println("Создан пользователь с id: " + userId);
        assertNotNull(userId, "userId не должен быть null");

        // Шаг 2 — строим тело PUT явно с userIsDelete: true
        String editBody = "{"
                + "\"userId\":\"" + userId + "\","
                + "\"employeeId\":\"" + employeeId + "\","
                + "\"employeeUserId\":\"" + userId + "\","
                + "\"userName\":\"" + userName + "\","
                + "\"userLogin\":\"" + userLogin + "\","
                + "\"userEmail\":\"" + userLogin + "\","
                + "\"userIsDelete\":true,"           // <-- ключевое поле
                + "\"userIsNew\":true,"
                + "\"userIsActive\":true,"
                + "\"userIsContractor\":null,"
                + "\"userIsLdap\":null,"
                + "\"userStartDate\":\"2026-03-21\","
                + "\"userStopDate\":\"2026-06-21\","
                + "\"userFinishDate\":null,"
                + "\"employeeFirstName\":\"автотестович\","
                + "\"employeeLastName\":\"автотест\","
                + "\"employeeMidName\":null,"
                + "\"employeePhone\":null,"
                + "\"employeeEmail\":\"" + userLogin + "\","
                + "\"employeeIsSharedCalendar\":null,"
                + "\"employeeIsOutlookSync\":null,"
                + "\"userRole\":{"
                + "\"id\":\"cced4585-e358-40bf-8614-01b0bd39439b\","
                + "\"name\":\"Администратор\","
                + "\"groupAccesses\":[{"
                + "\"id\":\"140a0210-d1d1-439e-b3a4-0ee5be6d29d2\","
                + "\"name\":\"Администратор - полный доступ\""
                + "}]}"
                + "}";

        Response putResponse = authRequest()
                .body(editBody)
                .put("/api/users/user_employee/" + userId)
                .andReturn();

        assertEquals(200, putResponse.getStatusCode());

        // Проверяем userIsDelete = true в ответе
        String isDelete = putResponse.jsonPath().getString("userIsDelete");
        assertEquals("true", isDelete, "userIsDelete должен быть true");
    }
}
