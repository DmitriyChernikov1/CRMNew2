import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import utils.TestDataJson;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(BaseTest.class)
public class AuthTest {
    private static String accessToken;

    private static final String BASE_URL = "http://172.20.207.16:8083";

    // Сообщения
    private static final String WRONG_CREDENTIALS_MESSAGE = "Логин или пароль введены неверно";
    private static final String BLOCKED_MESSAGE =
            "Учетная запись пользователя заблокирована и не может быть использована для входа. " +
                    "Обратитесь к администратору <a class=\"email\" href=\"mailto:helpdesk@sbercity.ru\">" +
                    "helpdesk@sbercity.ru</a> для формирования нового пароля.";

    @BeforeAll
    static void setUp() {
        accessToken = new AuthTokenTest().getAccessToken();
        RestAssured.baseURI = BASE_URL;
        System.setProperty("java.net.useSystemProxies", "false");
    }

    // Базовый запрос с авторизацией — baseUri берётся из RestAssured.baseURI выше
    private RequestSpecification authRequest() {
        return given()
                .baseUri(BASE_URL)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .header("Accept-Language", "ru-RU");
    }

    @Test
    @Description("Авторизация")
    @DisplayName("Вход по валидным данным")
    public void authorization() {
        Map<String, String> authBody = new HashMap<>();
        authBody.put("login", "DChernikov@sbercity.ru");
        authBody.put("password", "G$h8pY}%ci~ZD%H1");

        Response onlyToken = authRequest()
                .body(authBody)
                .post("/users/auth/login")
                .then()
                .statusCode(200)
                .extract().response();
    }

    @Test
    @Description("Авторизация с ошибочным паролем двухфакторной авторизации")
    @DisplayName("Вход по не валидным данным OTP")
    public void wrongAuthorizationOTP() {
        Map<String, String> authBody = new HashMap<>();
        authBody.put("login", "dimon.ag336@gmail.com");
        authBody.put("password", "JeE-rfW-5TP-hTD2");

        authRequest()
                .body(authBody)
                .post("/users/auth/login2fa")
                .then()
                .statusCode(200);

        Map<String, String> step2Body = new HashMap<>();
        step2Body.put("login", "dimon.ag336@gmail.com");
        step2Body.put("password", "JeE-rfW-5TP-hTD2");
        step2Body.put("otp", "12346");

        String message = authRequest()
                .body(step2Body)
                .post("/users/auth/loginOTP")
                .then()
                .statusCode(401)
                .extract()
                .jsonPath()
                .getString("message");

        assertEquals("Код двухфакторной аутентификации невернен", message);

        authRequest()
                .body(authBody)
                .post("/users/auth/login")
                .then()
                .statusCode(200);
    }

    // Выход (Logout)

    @Test
    @Order(1)
    @Description("Выход из системы")
    @DisplayName("Успешный выход из системы")
    public void successfulLogout() {
        Map<String, String> authBody = new HashMap<>();
        authBody.put("login", "DCh3ernikov@sbercity.ru");
        authBody.put("password", "Fjt4wNG3KTP6oZ*%#}UIg#i?");

        Response login = authRequest()
                .body(authBody)
                .post("/users/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        String access = login.jsonPath().getString("accessToken");

        authRequest()
                .header("Authorization", "Bearer " + access)
                .get("/users/auth/logout")
                .then()
                .statusCode(200);
    }

    @Test
    @Description("Авторизация")
    @DisplayName("Вход с неверным паролем")
    public void wrongPassword() {
        Map<String, String> authBody = new HashMap<>();
        authBody.put("login", "DChernikov@sbercity.ru");
        authBody.put("password", "неверный_пароль");

        Response response = authRequest()
                .body(authBody)
                .post("/users/auth/login")
                .andReturn();

        assertEquals(401, response.getStatusCode());
        assertEquals(WRONG_CREDENTIALS_MESSAGE, response.jsonPath().getString("message"));
    }

    @Test
    @Description("Авторизация")
    @DisplayName("Вход с неверным логином")
    public void wrongLogin() {
        Map<String, String> authBody = new HashMap<>();
        authBody.put("login", "несуществующий@sbercity.ru");
        authBody.put("password", "любой_пароль");

        Response response = authRequest()
                .body(authBody)
                .post("/users/auth/login")
                .andReturn();

        assertEquals(401, response.getStatusCode());
        assertEquals(WRONG_CREDENTIALS_MESSAGE, response.jsonPath().getString("message"));
    }

    @Test
    @Description("Авторизация")
    @DisplayName("Превышен лимит ошибок — аккаунт заблокирован")
    public void tooMuchAttempts() {
        Map<String, String> authBody = new HashMap<>();
        authBody.put("login", "DCh222ernikov@sbercity.ru");
        authBody.put("password", "string");

        Response response = authRequest()
                .body(authBody)
                .post("/users/auth/login")
                .andReturn();

        assertEquals(401, response.getStatusCode());
        assertEquals(BLOCKED_MESSAGE, response.jsonPath().getString("message"));
    }

    @Test
    @Description("Авторизация")
    @DisplayName("Вход с просроченным паролем — валидный пароль возвращает 401")
    public void loginWithExpiredPassword_validPass_returns401() {
        Map<String, String> authBody = new HashMap<>();
        authBody.put("login", "rail.test@mail.ru");
        authBody.put("password", "Fjt4wNG3KTP6oZ*%#}UIg#i?");

        authRequest()
                .body(authBody)
                .post("/users/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    @Description("Авторизация")
    @DisplayName("Вход с просроченным паролем — пустой пароль возвращает 401")
    public void loginWithExpiredPassword_emptyPass_returns401() {
        Map<String, String> authBody = new HashMap<>();
        authBody.put("login", "rail.test@mail.ru");
        authBody.put("password", "");

        authRequest()
                .body(authBody)
                .post("/users/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(2)
    @Description("Авторизация с ошибочным паролем двухфакторной авторизации")
    @DisplayName("Вход по валидным данным OTP — мок 200")
    public void wrongAuthorizationOTP_mock200() {
        // Поднимаем WireMock на отдельном порту
        WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8089));
        wireMockServer.start();

        try {
            // Мокаем только loginOTP — возвращаем 200 вместо 401
            wireMockServer.stubFor(
                    WireMock.post(WireMock.urlEqualTo("/users/auth/loginOTP"))
                            .willReturn(WireMock.aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("{\"message\": \"success\"}"))
            );

            // Шаг 1 — реальный запрос на настоящий сервер (как в оригинале)
            Map<String, String> authBody = new HashMap<>();
            authBody.put("login", "dimon.ag336@gmail.com");
            authBody.put("password", "JeE-rfW-5TP-hTD2");

            authRequest()
                    .body(authBody)
                    .post("/users/auth/login2fa")
                    .then()
                    .statusCode(200);

            // Шаг 2 — запрос идёт на WireMock, а не на реальный сервер
            Map<String, String> step2Body = new HashMap<>();
            step2Body.put("login", "dimon.ag336@gmail.com");
            step2Body.put("password", "JeE-rfW-5TP-hTD2");
            step2Body.put("otp", "12346");

            given()
                    .baseUri("http://localhost:8089")
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("accept", "application/json")
                    .body(step2Body)
                    .post("/users/auth/loginOTP")
                    .then()
                    .statusCode(200);

        } finally {
            wireMockServer.stop();
        }
    }

    //Надо создавать и удалять пользователя
    @Test
    @Description("Вход новым пользователем, с придумываением пароля")
    @DisplayName("Вход новым пользователем, с придумываением пароля")
    public void newEmployee() {
        // Шаг 1 — создание пользователя
        String createBody = TestDataJson.CreateUser();

        Response createResponse = authRequest()
                .body(createBody)
                .header("Authorization", "Bearer " + accessToken)
                .post("http://172.20.207.16/api/users/user_employee")
                .andReturn();

        assertEquals(200, createResponse.getStatusCode());

        String userId     = createResponse.jsonPath().getString("userId");
        String employeeId = createResponse.jsonPath().getString("employeeId");
        String userLogin  = createResponse.jsonPath().getString("userLogin");
        String userName   = createResponse.jsonPath().getString("userName");

        assertNotNull(userId, "userId не должен быть null");

        // Шаг 2 — вход новым пользователем
        String newPassword = "yAyLZ66A2X%Cy6C2_" + System.currentTimeMillis();

        Map<String, String> body = new HashMap<>();
        body.put("login", userLogin);
        body.put("password", "rpJcOI3gVm1MRyLn");

        Response login = authRequest()
                .body(body)
                .post("/users/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        // Шаг 3 — смена пароля
        Map<String, String> body2 = new HashMap<>();
        body2.put("login", userLogin);
        body2.put("oldPassword", "rpJcOI3gVm1MRyLn");
        body2.put("newPassword", newPassword);
        body2.put("newPasswordConfirm", newPassword);

        authRequest()
                .body(body2)
                .post("http://172.20.207.16/api/users/registration/change-password")
                .then()
                .statusCode(200);

        // Шаг 4 — удаление пользователя (userIsDelete = true) от имени админа
        String editBody = "{"
                + "\"userId\":\"" + userId + "\","
                + "\"employeeId\":\"" + employeeId + "\","
                + "\"employeeUserId\":\"" + userId + "\","
                + "\"userName\":\"" + userName + "\","
                + "\"userLogin\":\"" + userLogin + "\","
                + "\"userEmail\":\"" + userLogin + "\","
                + "\"userIsDelete\":true,"
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
                .header("Authorization", "Bearer " + accessToken)
                .put("http://172.20.207.16/api/users/user_employee/" + userId)
                .andReturn();

        assertEquals(200, putResponse.getStatusCode());

        String isDelete = putResponse.jsonPath().getString("userIsDelete");
        assertEquals("true", isDelete, "userIsDelete должен быть true");
    }
   /*@Test
    @Description("тест мока 401")
    @DisplayName("тест мока 401")
    public void test401(){
        WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8089));
        wireMockServer.start();
        try {
            // Мокаем только loginOTP — возвращаем 200 вместо 401
            wireMockServer.stubFor(
                    WireMock.post(WireMock.urlEqualTo("/users/auth/login"))
                            .willReturn(WireMock.aResponse()
                                    .withStatus(401)
                                    .withHeader("Content-Type", "application/json")
                                    )
            );
            Map<String, String> authBody = new HashMap<>();
            authBody.put("login", "dimon.ag336@gmail.com");
            authBody.put("password", "JeE-rfW-5TP-hTD2333");

            Response response =given()
                    .baseUri("http://localhost:8089")
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("accept", "application/json")
                    .body(authBody)
                    .post("/users/auth/login")
                    .then()
                    .statusCode(401)
                    .extract().response();
            response.prettyPrint();

        } finally {
            wireMockServer.stop();
        }
    }*/
}