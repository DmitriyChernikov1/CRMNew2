import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import utils.JsonUtils;
import utils.TestDataJson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdministrationTest {

    private static String accessToken;
    private static String createdUserId;
    private static String createdUserLogin;

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://172.20.207.16";
        accessToken = new AuthTokenTest().getAccessToken();
    }

    // =====================================================================
    // СПРАВОЧНИКИ — РОЛИ
    // =====================================================================

    @Test
    @Order(1)
    @Description("Получение списка групп доступа")
    @DisplayName("Получение групп доступа")
    public void getGroupAccess() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .when()
                .get("/api/users/groupAccess")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].id", notNullValue())
                .body("content[0].name", notNullValue());
    }

    @Test
    @Order(2)
    @Description("Получение списка ролей")
    @DisplayName("Получение ролей")
    public void getRoles() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .when()
                .get("/api/users/role")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].id", notNullValue())
                .body("content[0].name", notNullValue())
                .body("content[0].groupAccesses", notNullValue());
    }

    @Test
    @Order(3)
    @Description("Поиск ролей через автокомплит без фильтра")
    @DisplayName("Автокомплит ролей — пустой запрос")
    public void autocompleteRolesEmpty() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .queryParam("name", "")
                .when()
                .get("/api/users/autocomplete/role")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].id", notNullValue())
                .body("content[0].name", notNullValue());
    }

    @Test
    @Order(4)
    @Description("Поиск ролей через автокомплит с фильтром по букве А")
    @DisplayName("Автокомплит ролей — фильтр по букве")
    public void autocompleteRolesWithFilter() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .queryParam("name", "а")
                .when()
                .get("/api/users/autocomplete/role")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content.name", everyItem(containsStringIgnoringCase("а")));
    }

    // =====================================================================
    // АВТОКОМПЛИТ — ОРГАНИЗАЦИИ
    // =====================================================================

    @Test
    @Order(5)
    @Description("Поиск организаций через автокомплит без фильтра")
    @DisplayName("Автокомплит организаций — пустой запрос")
    public void autocompleteOrganizationsEmpty() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .queryParam("name", "")
                .when()
                .get("/api/users/autocomplete/organization")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].id", notNullValue())
                .body("content[0].name", notNullValue());
    }

    @Test
    @Order(6)
    @Description("Поиск организаций через автокомплит с фильтром — возвращает только совпадения")
    @DisplayName("Автокомплит организаций — фильтр по названию")
    public void autocompleteOrganizationsWithFilter() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .queryParam("name", "1")
                .when()
                .get("/api/users/autocomplete/organization")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].name", containsString("1"));
    }

    // =====================================================================
    // АВТОКОМПЛИТ — ОТДЕЛЫ
    // =====================================================================

    @Test
    @Order(7)
    @Description("Поиск отделов через автокомплит без фильтра")
    @DisplayName("Автокомплит отделов — пустой запрос")
    public void autocompleteDepartmentsEmpty() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .queryParam("name", "")
                .when()
                .get("/api/users/autocomplete/department")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].id", notNullValue())
                .body("content[0].name", notNullValue());
    }

    @Test
    @Order(8)
    @Description("Поиск отделов через автокомплит с фильтром — возвращает только совпадения")
    @DisplayName("Автокомплит отделов — фильтр по названию")
    public void autocompleteDepartmentsWithFilter() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .queryParam("name", "1")
                .when()
                .get("/api/users/autocomplete/department")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content.name", everyItem(containsString("1")));
    }

    // =====================================================================
    // АВТОКОМПЛИТ — ДОЛЖНОСТИ
    // =====================================================================

    @Test
    @Order(9)
    @Description("Поиск должностей через автокомплит без фильтра")
    @DisplayName("Автокомплит должностей — пустой запрос")
    public void autocompletePostsEmpty() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .queryParam("name", "")
                .when()
                .get("/api/users/autocomplete/post")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].id", notNullValue())
                .body("content[0].name", notNullValue());
    }

    @Test
    @Order(10)
    @Description("Поиск должностей через автокомплит с фильтром — возвращает только совпадения")
    @DisplayName("Автокомплит должностей — фильтр по названию")
    public void autocompletePostsWithFilter() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .queryParam("name", "1")
                .when()
                .get("/api/users/autocomplete/post")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content.name", everyItem(containsString("1")));
    }

    // =====================================================================
    // АВТОКОМПЛИТ — СОТРУДНИКИ
    // =====================================================================

    @Test
    @Order(11)
    @Description("Поиск сотрудников через автокомплит без фильтра")
    @DisplayName("Автокомплит сотрудников — пустой запрос")
    public void autocompleteEmployeesEmpty() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .queryParam("name", "")
                .when()
                .get("/api/users/autocomplete/employee")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].id", notNullValue())
                .body("content[0].name", notNullValue());
    }

    @Test
    @Order(12)
    @Description("Поиск сотрудников через автокомплит с фильтром по букве — возвращает только совпадения")
    @DisplayName("Автокомплит сотрудников — фильтр по букве")
    public void autocompleteEmployeesWithFilter() {
        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .queryParam("name", "ч")
                .when()
                .get("/api/users/autocomplete/employee")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content.name", everyItem(containsStringIgnoringCase("ч")));
    }

    // =====================================================================
    // СОЗДАНИЕ ПОЛЬЗОВАТЕЛЯ
    // =====================================================================

    @Test
    @Order(13)
    @Description("Создание пользователя с базовыми полями")
    @DisplayName("Создание пользователя — базовый")
    public void createUser() {
        Response response = given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .body(TestDataJson.CreateUser)
                .when()
                .post("/api/users/user_employee")
                .then()
                .statusCode(200)
                .body("userId", notNullValue())
                .body("userIsActive", equalTo(true))
                .body("userRole.name", equalTo("Администратор"))
                .extract()
                .response();

        createdUserId = response.jsonPath().getString("userId");
        createdUserLogin = response.jsonPath().getString("userLogin");

        assertNotNull(createdUserId, "userId не должен быть null после создания");
        assertNotNull(createdUserLogin, "userLogin не должен быть null после создания");
    }



    // ПОИСК / ФИЛЬТРАЦИЯ ПОЛЬЗОВАТЕЛЕЙ


    @Test
    @Order(14)
    @Description("Поиск пользователя по имени через фильтрацию")
    @DisplayName("Фильтрация пользователей по имени")
    public void filterUsers() {
        String filterBody = "[{"
                + "\"value\":\"автотест\","
                + "\"field\":\"name\","
                + "\"operator\":\"like\","
                + "\"logicalOperator\":\"and\""
                + "}]";

        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("sort", "name,asc")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("showArchive", false)
                .body(filterBody)
                .when()
                .post("/api/users/user/flat/filtering")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].name", notNullValue());
    }

    @Test
    @Order(15)
    @Description("Поиск созданного пользователя по логину")
    @DisplayName("Фильтрация пользователей — поиск созданного")
    public void filterUsersByLogin() {
        assumeUserCreated();

        String filterBody = "[{"
                + "\"value\":\"" + createdUserLogin + "\","
                + "\"field\":\"login\","
                + "\"operator\":\"like\","
                + "\"logicalOperator\":\"and\""
                + "}]";

        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .queryParam("sort", "name,asc")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("showArchive", false)
                .body(filterBody)
                .when()
                .post("/api/users/user/flat/filtering")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].login", equalTo(createdUserLogin));
    }

    // =====================================================================
    // УДАЛЕНИЕ ПОЛЬЗОВАТЕЛЯ (возврат в исходное состояние)
    // =====================================================================

    @Test
    @Order(16)
    @Description("Удаление созданного пользователя — возврат в исходное состояние")
    @DisplayName("Удаление пользователя")
    public void deleteCreatedUser() throws Exception {
        assumeUserCreated();

        String requestBody = given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .when()
                .get("/api/users/user_employee/" + createdUserId)
                .then()
                .statusCode(200)
                .extract()
                .body().asString();

        String updatedBody = JsonUtils.changeField(requestBody, "userIsDelete", true);

        given()
                .headers("Authorization", "Bearer " + accessToken,
                        "Content-Type", "application/json; charset=UTF-8")
                .body(updatedBody)
                .when()
                .put("/api/users/user_employee/" + createdUserId)
                .then()
                .statusCode(200)
                .body("userIsDelete", equalTo(true));
    }

    // =====================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =====================================================================

    private void assumeUserCreated() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                createdUserId != null,
                "Тест пропущен: пользователь не был создан в createUser()"
        );
    }
}