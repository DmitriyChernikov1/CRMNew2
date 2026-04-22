import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserGroupApplicationTest {

    private static String accessToken;
    private static String createdUuid;
    private static String createdMemberUuid;
    private static String employeeUuid;

    private static final String BASE_URL = "http://172.20.207.16:9000";
    private static final String ENDPOINT = "/client-relations/user-group-application";
    private static final String MEMBER_ENDPOINT = "/client-relations/member-group-application";

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
    // БЛОК 2: Участники группы (MemberGroupApplication)
    // =====================================================================

    // 8. GET autocomplete/employee — Получаем первого доступного сотрудника
    // =====================================================================

    @Test
    @Order(8)
    @DisplayName("MEMBER | GET /autocomplete/employee | 200 — Получение первого сотрудника для добавления в группу")
    void getFirstEmployee() {
        Response response = authRequest()
                .when()
                .get("/client-relations/autocomplete/employee?page=0&size=1")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("content.size()", greaterThan(0))
                .extract()
                .response();

        employeeUuid = response.jsonPath().getString("content[0].id");
    }

    // =====================================================================
    // 9. POST — Добавление участника в группу
    // =====================================================================

    @Test
    @Order(9)
    @DisplayName("MEMBER | POST | 200 — Добавление участника в группу")
    void createMemberGroupApplication() {
        String body = """
                {
                  "userGroupApplication": {
                    "id": "%s"
                  },
                  "employeeId": {
                    "id": "%s"
                  }
                }
                """.formatted(createdUuid, employeeUuid);

        Response response = authRequest()
                .body(body)
                .when()
                .post(MEMBER_ENDPOINT)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("employee", equalTo(employeeUuid))
                .body("userGroupApplication.id", equalTo(createdUuid))
                .extract()
                .response();

        createdMemberUuid = response.jsonPath().getString("id");
    }

    // =====================================================================
    // 10. GET list — Добавленный участник присутствует в общем списке
    // =====================================================================

    @Test
    @Order(10)
    @DisplayName("MEMBER | GET list | 200 — Добавленный участник присутствует в общем списке")
    void getMemberListContainsCreatedMember() {
        authRequest()
                .when()
                .get(MEMBER_ENDPOINT + "?page=0&size=200")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("content.id", hasItem(createdMemberUuid))
                .body("content.employee", hasItem(employeeUuid));
    }

    // =====================================================================
    // 11. GET /by-group — Участник виден в списке участников группы
    // =====================================================================

    @Test
    @Order(11)
    @DisplayName("MEMBER | GET /by-group | 200 — Участник виден в списке участников группы")
    void getMembersByGroup() {
        authRequest()
                .queryParam("groupId", createdUuid)
                .when()
                .get(MEMBER_ENDPOINT + "/by-group")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("content.id", hasItem(createdMemberUuid))
                .body("content.employee", hasItem(employeeUuid));
    }



    // =====================================================================
    // 13. GET /{uuid} — Получение участника по UUID
    // =====================================================================

    @Test
    @Order(13)
    @DisplayName("MEMBER | GET /{uuid} | 200 — Получение участника по UUID")
    void getMemberGroupApplicationById() {
        authRequest()
                .when()
                .get(MEMBER_ENDPOINT + "/" + createdMemberUuid)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdMemberUuid))
                .body("employee", equalTo(employeeUuid))
                .body("userGroupApplication.id", equalTo(createdUuid));
    }

    // =====================================================================
    // 14. DELETE /{uuid} — Удаление участника из группы
    // =====================================================================

    @Test
    @Order(14)
    @DisplayName("MEMBER | DELETE /{uuid} | 201 — Удаление участника из группы")
    void deleteMemberGroupApplication() {
        authRequest()
                .when()
                .delete(MEMBER_ENDPOINT + "/" + createdMemberUuid)
                .then()
                .statusCode(201);
    }

    // =====================================================================
    // 15. GET /{uuid} — Проверка что участник удалён
    // =====================================================================

    @Test
    @Order(15)
    @DisplayName("MEMBER | GET /{uuid} | 500 — Удалённый участник не найден")
    void verifyDeletedMemberGroupApplication() {
        authRequest()
                .when()
                .get(MEMBER_ENDPOINT + "/" + createdMemberUuid)
                .then()
                .statusCode(500);
    }

    // =====================================================================
    // БЛОК 3: Удаление группы
    // =====================================================================

    @Test
    @Order(16)
    @DisplayName("DELETE /{uuid} | 201 — Удаление группы")
    void deleteUserGroupApplication() {
        authRequest()
                .when()
                .delete(ENDPOINT + "/" + createdUuid)
                .then()
                .statusCode(201);
    }

    @Test
    @Order(17)
    @DisplayName("GET /{uuid} | 500 — Удалённая группа не найдена")
    void verifyDeletedUserGroupApplication() {
        authRequest()
                .when()
                .get(ENDPOINT + "/" + createdUuid)
                .then()
                .statusCode(500);
    }
}