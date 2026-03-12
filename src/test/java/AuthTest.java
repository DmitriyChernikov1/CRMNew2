import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(BaseTest.class)
public class AuthTest {

    private static AuthTokenTest authService;
    private static String accessToken;

    // Сообщения вынесены в константы — не дублируются в коде
    private static final String WRONG_CREDENTIALS_MESSAGE = "Логин или пароль введены неверно";
    private static final String BLOCKED_MESSAGE =
            "Учетная запись пользователя заблокирована и не может быть использована для входа. " +
                    "Обратитесь к администратору <a class=\"email\" href=\"mailto:helpdesk@sbercity.ru\">" +
                    "helpdesk@sbercity.ru</a> для формирования нового пароля.";

    @BeforeAll
    static void setUp() {
        // Токен
        authService = new AuthTokenTest();
        RestAssured.baseURI = AuthTokenTest.BASE_URL;
        accessToken = authService.getAccessToken();
    }


    @Test
    @Description("Авторизация")
    @DisplayName("Вход по валидным данным")
    public void authorization() {
        // Данные берём из файла
        String[] credentials = authService.readCredentialsFromFile("src/test/java/txtFiles/credentials.txt");

        Map<String, String> authBody = new HashMap<>();
        authBody.put("login", credentials[0]);
        authBody.put("password", credentials[1]);

        authService.baseRequest()
                .body(authBody)
                .post("/users/auth/login")
                .then()
                .statusCode(200);
    }

    @Test
    @Description("Выход из системы")
    @DisplayName("Успешный выход из системы")
    public void successfulLogout() {
        String[] credentials = authService.readCredentialsFromFile("src/test/java/txtFiles/credentials.txt");

        Map<String, String> authBody = new HashMap<>();
        authBody.put("login", credentials[0]);
        authBody.put("password", credentials[1]);

        authService.baseRequest()
                .body(authBody)
                .post("/users/auth/login")
                .then()
                .statusCode(200);

        authService.baseRequest()
                .header("Authorization", "Bearer " + accessToken)
                .get("/users/auth/logout")
                .then()
                .log().all()
                .statusCode(200); // исправлен порядок: было assertEquals(statuscode, 200)
    }



    @Test
    @Description("Авторизация")
    @DisplayName("Вход с неверным паролем")
    public void wrongPassword() {
        Map<String, String> authBody = new HashMap<>();
        authBody.put("login", "DChernikov@sbercity.ru");
        authBody.put("password", "неверный_пароль");

        Response response = authService.baseRequest()
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

        Response response = authService.baseRequest()
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

        Response response = authService.baseRequest()
                .body(authBody)
                .post("/users/auth/login")
                .andReturn();

        assertEquals(401, response.getStatusCode());
        assertEquals(BLOCKED_MESSAGE, response.jsonPath().getString("message"));
    }

    @Test
    @Description("Авторизация")
    @DisplayName("Вход с просроченным паролем — валидный пароль возвращает 200")
    public void loginWithExpiredPassword_validPass_returns200() {
        // Разбили if/else на два теста — каждый тест проверяет одну вещь
        Map<String, String> authBody = new HashMap<>();
        authBody.put("login", "rail.test@mail.ru");
        authBody.put("password", "Fjt4wNG3KTP6oZ*%#}UIg#i?");

        authService.baseRequest()
                .body(authBody)
                .post("/users/auth/login")
                .then()
                .statusCode(200);
    }

    @Test
    @Description("Авторизация")
    @DisplayName("Вход с просроченным паролем — пустой пароль возвращает 401")
    public void loginWithExpiredPassword_emptyPass_returns401() {
        Map<String, String> authBody = new HashMap<>();
        authBody.put("login", "rail.test@mail.ru");
        authBody.put("password", "");

        authService.baseRequest()
                .body(authBody)
                .post("/users/auth/login")
                .then()
                .statusCode(401);
    }
}