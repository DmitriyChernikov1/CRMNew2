import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Вспомогательный класс для авторизации.
 * Предоставляет токен и базовый запрос с общими заголовками.
 */
public class AuthTokenTest {

    // URL в одном месте — если изменится, меняем только здесь
    public static final String BASE_URL = "http://172.20.207.16:8083";
    private static final String CREDENTIALS_PATH = "src/test/java/txtFiles/credentials.txt";


     // Получаем accessToken через логин.

    public String getAccessToken() {
        String[] credentials = readCredentialsFromFile(CREDENTIALS_PATH);

        Map<String, String> auth = new HashMap<>();
        auth.put("login", credentials[0]);
        auth.put("password", credentials[1]);

        return RestAssured
                .given()
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .header("Accept-Language", "ru,ru-RU;q=0.9,en-US;q=0.8,en;q=0.7")
                .body(auth)
                .post(BASE_URL + "/users/auth/login")
                .then()
                .statusCode(200)        // упадёт сразу если логин не прошёл
                .extract()
                .jsonPath()
                .getString("accessToken");
    }


     // Базовый запрос с общими заголовками.

    public RequestSpecification baseRequest() {
        return RestAssured
                .given()
                .baseUri(BASE_URL)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .header("Accept-Language", "ru-RU");
    }

    /**
     * Читает логин и пароль из файла формата:
     * login=user@mail.ru
     * password=secret
     */
    public String[] readCredentialsFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            String login = null;
            String password = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("login=")) {
                    login = line.substring(6).trim();
                } else if (line.startsWith("password=")) {
                    password = line.substring(9).trim();
                }
            }

            if (login == null || password == null) {
                throw new RuntimeException("Не найдены login или password в файле: " + filePath);
            }

            return new String[]{login, password};

        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения файла: " + filePath, e);
        }
    }
}