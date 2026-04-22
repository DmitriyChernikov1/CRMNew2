import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GroupEmployeeTest {

    private static String accessToken;
    private static String createdGroupUuid;
    private static String createdMemberUuid;
    private static String employeeUuid;

    private static final String BASE_URL = "http://172.20.207.16:9000";
    private static final String GROUP_ENDPOINT  = "/client-relations/group-employee";
    private static final String MEMBER_ENDPOINT = "/client-relations/member-group-employee";

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
    // БЛОК 1: Группа сотрудников классификатора (GroupEmployee)
    // =====================================================================

    @Test
    @Order(1)
    @DisplayName("GROUP | POST | 200 — Создание группы сотрудников классификатора")
    void createGroupEmployee() {
        String body = """
                {
                  "name": "Тестовая группа классификатора авто",
                  "shortName": "ТГК"
                }
                """;

        Response response = authRequest()
                .body(body)
                .when()
                .post(GROUP_ENDPOINT)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("Тестовая группа классификатора авто"))
                .body("shortName", equalTo("ТГК"))
                .extract()
                .response();

        createdGroupUuid = response.jsonPath().getString("id");
    }

    @Test
    @Order(2)
    @DisplayName("GROUP | GET list | 200 — Созданная группа присутствует в списке")
    void getGroupListContainsCreatedGroup() {
        authRequest()
                .when()
                .get(GROUP_ENDPOINT + "?page=0&size=200")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("content.id", hasItem(createdGroupUuid))
                .body("content.name", hasItem("Тестовая группа классификатора авто"));
    }

    @Test
    @Order(3)
    @DisplayName("GROUP | POST /filtering | 200 — Поиск группы по имени")
    void filterGroupEmployee() {
        String body = """
                [
                  {
                    "field": "name",
                    "operator": "LIKE",
                    "value": "Тестовая группа классификатора авто",
                    "logicalOperator": "AND"
                  }
                ]
                """;

        authRequest()
                .body(body)
                .when()
                .post(GROUP_ENDPOINT + "/filtering")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("content.id", hasItem(createdGroupUuid))
                .body("content.name", hasItem("Тестовая группа классификатора авто"));
    }

    @Test
    @Order(4)
    @DisplayName("GROUP | GET /{uuid} | 200 — Получение созданной группы по UUID")
    void getGroupEmployeeById() {
        authRequest()
                .when()
                .get(GROUP_ENDPOINT + "/" + createdGroupUuid)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdGroupUuid))
                .body("name", equalTo("Тестовая группа классификатора авто"))
                .body("shortName", equalTo("ТГК"));
    }

    @Test
    @Order(5)
    @DisplayName("GROUP | GET /active | 200 — Созданная группа есть в списке активных")
    void getActiveGroupListContainsCreatedGroup() {
        authRequest()
                .when()
                .get(GROUP_ENDPOINT + "/active?page=0&size=200")
                .then()
                .statusCode(200)
                .body("content.id", hasItem(createdGroupUuid));
    }

    @Test
    @Order(6)
    @DisplayName("GROUP | PUT /{uuid} | 200 — Изменение имени и shortName группы")
    void updateGroupEmployee() {
        String body = """
                {
                  "id": "%s",
                  "name": "Тестовая группа классификатора авто — изменено",
                  "shortName": "ТГК-И"
                }
                """.formatted(createdGroupUuid);

        authRequest()
                .body(body)
                .when()
                .put(GROUP_ENDPOINT + "/" + createdGroupUuid)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdGroupUuid))
                .body("name", equalTo("Тестовая группа классификатора авто — изменено"))
                .body("shortName", equalTo("ТГК-И"));
    }

    @Test
    @Order(7)
    @DisplayName("GROUP | GET /{uuid} | 200 — Проверка изменённых данных группы")
    void verifyUpdatedGroupEmployee() {
        authRequest()
                .when()
                .get(GROUP_ENDPOINT + "/" + createdGroupUuid)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdGroupUuid))
                .body("name", equalTo("Тестовая группа классификатора авто — изменено"))
                .body("shortName", equalTo("ТГК-И"));
    }

    // =====================================================================
    // БЛОК 2: Исполнители классификатора (MemberGroupEmployee)
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

    @Test
    @Order(9)
    @DisplayName("MEMBER | POST | 200 — Добавление исполнителя в группу классификатора")
    void createMemberGroupEmployee() {
        String body = """
                {
                  "groupEmployee": {
                    "id": "%s"
                  },
                  "employeeId": {
                    "id": "%s"
                  },
                  "priorityExecutor": 1
                }
                """.formatted(createdGroupUuid, employeeUuid);

        Response response = authRequest()
                .body(body)
                .when()
                .post(MEMBER_ENDPOINT)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("employee", equalTo(employeeUuid))
                .body("groupEmployee.id", equalTo(createdGroupUuid))
                .body("priorityExecutor", equalTo(1))
                .extract()
                .response();

        createdMemberUuid = response.jsonPath().getString("id");
    }

    @Test
    @Order(10)
    @DisplayName("MEMBER | GET list | 200 — Добавленный исполнитель присутствует в общем списке")
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

    @Test
    @Order(11)
    @DisplayName("MEMBER | GET /by-group | 200 — Исполнитель виден в списке участников группы")
    void getMembersByGroup() {
        authRequest()
                .queryParam("page", 0)
                .queryParam("size", 200)
                .queryParam("groupId", createdGroupUuid)
                .when()
                .get(MEMBER_ENDPOINT + "/by-group")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("content.id", hasItem(createdMemberUuid))
                .body("content.employee", hasItem(employeeUuid));
    }



    @Test
    @Order(13)
    @DisplayName("MEMBER | GET /{uuid} | 200 — Получение исполнителя по UUID")
    void getMemberGroupEmployeeById() {
        authRequest()
                .when()
                .get(MEMBER_ENDPOINT + "/" + createdMemberUuid)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdMemberUuid))
                .body("employee", equalTo(employeeUuid))
                .body("groupEmployee.id", equalTo(createdGroupUuid))
                .body("priorityExecutor", equalTo(1));
    }

    @Test
    @Order(14)
    @DisplayName("MEMBER | GET /active | 200 — Исполнитель есть в списке активных")
    void getActiveMemberListContainsCreatedMember() {
        authRequest()
                .when()
                .get(MEMBER_ENDPOINT + "/active?page=0&size=200")
                .then()
                .statusCode(200)
                .body("content.id", hasItem(createdMemberUuid));
    }




    @Test
    @Order(17)
    @DisplayName("MEMBER | DELETE /{uuid} | 201 — Удаление исполнителя из группы")
    void deleteMemberGroupEmployee() {
        authRequest()
                .when()
                .delete(MEMBER_ENDPOINT + "/" + createdMemberUuid)
                .then()
                .statusCode(201);
    }

    @Test
    @Order(18)
    @DisplayName("MEMBER | GET /{uuid} | 500 — Удалённый исполнитель не найден")
    void verifyDeletedMemberGroupEmployee() {
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
    @Order(19)
    @DisplayName("GROUP | DELETE /{uuid} | 201 — Удаление группы классификатора")
    void deleteGroupEmployee() {
        authRequest()
                .when()
                .delete(GROUP_ENDPOINT + "/" + createdGroupUuid)
                .then()
                .statusCode(201);
    }

    @Test
    @Order(20)
    @DisplayName("GROUP | GET /{uuid} | 500 — Удалённая группа не найдена")
    void verifyDeletedGroupEmployee() {
        authRequest()
                .when()
                .get(GROUP_ENDPOINT + "/" + createdGroupUuid)
                .then()
                .statusCode(500);
    }
}