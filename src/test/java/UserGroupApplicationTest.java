import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserGroupApplicationTest {

    private static String accessToken;
    private static String createdUuid;

    private static final String BASE_URL = "http://172.20.207.16:9000";
    private static final String ENDPOINT = "/client-relations/user-group-application";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        accessToken = new AuthTokenTest().getAccessToken();
    }

    private RequestSpecification authRequest() {
        return RestAssured.given()
                .baseUri(BASE_URL)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .header("Accept-Language", "ru,ru-RU;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Authorization", "Bearer " + accessToken);
    }

    // =====================================================================
    // 1. POST — Создание группы
    // =====================================================================

    @Test
    @Order(1)
    @DisplayName("POST | 200 — Создание группы пользователей по заявкам")
    void createUserGroupApplication() {
        String body = """
                {
                  "name": "Тестовая группа заявок авто",
                  "shortName": "ТГЗ",
                  "isDispatcher": false
                }
                """;

        Response response = authRequest()
                .body(body)
                .when()
                .post(ENDPOINT)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("Тестовая группа заявок авто"))
                .body("shortName", equalTo("ТГЗ"))
                .body("isDispatcher", equalTo(false))
                .extract()
                .response();

        // Сохраняем UUID для следующих тестов
        createdUuid = response.jsonPath().getString("id");
    }

    // =====================================================================
    // 2. GET list — Проверка созданной группы в списке
    // =====================================================================

    @Test
    @Order(2)
    @DisplayName("GET list | 200 — Созданная группа присутствует в списке")
    void getListContainsCreatedGroup() {
        authRequest()
                .when()
                .get(ENDPOINT + "?page=0&size=200")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("content.id", hasItem(createdUuid))
                .body("content.name", hasItem("Тестовая группа заявок авто"));
    }

    // =====================================================================
    // 3. POST /filtering — Поиск созданной группы через фильтр
    // =====================================================================

    @Test
    @Order(3)
    @DisplayName("POST /filtering | 200 — Поиск группы по имени")
    void filterUserGroupApplication() {
        String body = """
                [
                  {
                    "field": "name",
                    "operator": "LIKE",
                    "value": "Тестовая группа заявок авто",
                    "logicalOperator": "AND"
                  }
                ]
                """;

        authRequest()
                .body(body)
                .when()
                .post(ENDPOINT + "/filtering")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("content.id", hasItem(createdUuid))
                .body("content.name", hasItem("Тестовая группа заявок авто"));
    }

    // =====================================================================
    // 4. GET /{uuid} — Получение группы по ID
    // =====================================================================

    @Test
    @Order(4)
    @DisplayName("GET /{uuid} | 200 — Получение созданной группы по UUID")
    void getUserGroupApplicationById() {
        authRequest()
                .when()
                .get(ENDPOINT + "/" + createdUuid)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdUuid))
                .body("name", equalTo("Тестовая группа заявок авто"))
                .body("shortName", equalTo("ТГЗ"))
                .body("isDispatcher", equalTo(false));
    }

    // =====================================================================
    // 5. GET /active — Проверка в списке активных
    // =====================================================================

    @Test
    @Order(5)
    @DisplayName("GET /active | 200 — Созданная группа есть в списке активных")
    void getActiveListContainsCreatedGroup() {
        authRequest()
                .when()
                .get(ENDPOINT + "/active?page=0&size=200")
                .then()
                .statusCode(200)
                .body("content.id", hasItem(createdUuid));
    }

    // =====================================================================
    // 6. PUT /{uuid} — Изменение группы
    // =====================================================================

    @Test
    @Order(6)
    @DisplayName("PUT /{uuid} | 200 — Изменение имени группы и флага isDispatcher")
    void updateUserGroupApplication() {
        String body = """
                {
                  "id": "%s",
                  "name": "Тестовая группа заявок авто — изменено",
                  "shortName": "ТГЗ-И",
                  "isDispatcher": true
                }
                """.formatted(createdUuid);

        authRequest()
                .body(body)
                .when()
                .put(ENDPOINT + "/" + createdUuid)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdUuid))
                .body("name", equalTo("Тестовая группа заявок авто — изменено"))
                .body("shortName", equalTo("ТГЗ-И"))
                .body("isDispatcher", equalTo(true));
    }

    // =====================================================================
    // 7. GET /{uuid} — Проверка что изменения сохранились
    // =====================================================================

    @Test
    @Order(7)
    @DisplayName("GET /{uuid} | 200 — Проверка изменённых данных")
    void verifyUpdatedUserGroupApplication() {
        authRequest()
                .when()
                .get(ENDPOINT + "/" + createdUuid)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdUuid))
                .body("name", equalTo("Тестовая группа заявок авто — изменено"))
                .body("shortName", equalTo("ТГЗ-И"))
                .body("isDispatcher", equalTo(true));
    }

    // =====================================================================
    // 8. DELETE /{uuid} — Удаление группы
    // =====================================================================

     @Test
    @Order(8)
    @DisplayName("DELETE /{uuid} | 201 — Удаление группы")
    void deleteUserGroupApplication() {
        authRequest()
                .when()
                .delete(ENDPOINT + "/" + createdUuid)
                .then()
                .statusCode(201);
    }

    // =====================================================================
    // 9. GET /{uuid} — Проверка что группа удалена
    // =====================================================================

   @Test
    @Order(9)
    @DisplayName("GET /{uuid} | 500 — Удалённая группа не найдена")
    void verifyDeletedUserGroupApplication() {
        authRequest()
                .when()
                .get(ENDPOINT + "/" + createdUuid)
                .then()
                .statusCode(500);
    }
}