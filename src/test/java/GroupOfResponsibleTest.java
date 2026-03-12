import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(BaseTest.class)
public class GroupOfResponsibleTest {
    String validUuid = "3909df72-e6ee-49fa-a117-793b6b4789cc";
    AuthTokenTest authService = new AuthTokenTest();
    String accessToken = authService.getAccessToken();
    String time = "автотест " + System.currentTimeMillis();

    @Test
    @Description("Получение группы сотрудников по заявкам по UUID")
    @DisplayName("Успешное получение группы сотрудников по заявкам")
    public void getUserGroupApplication_Success() {

        Response getUserGroup = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.16/client-relations/user-group-application/" + validUuid)
                .andReturn();

        int statusCode = getUserGroup.getStatusCode();
        assertEquals(200, statusCode);

        String responseBody = getUserGroup.getBody().asString();
        assertNotNull(responseBody);

    }

    @Test
    @Description("Обновление группы сотрудников по заявкам")
    @DisplayName("Успешное обновление группы сотрудников по заявкам")
    public void updateUserGroupApplication_Success() {


        // Формируем JSON-тело с подстановкой переменной time в поле "name"
        String requestBody = "{\"createdBy\": \"Черников Дмитрий Витальевич\", " +
                "\"createdDate\": \"2025-11-25 10:26:42.524\", " +
                "\"lastModifiedBy\": \"Черников Дмитрий Витальевич\", " +
                "\"lastModifiedDate\": \"2025-11-25 10:26:42.524\", " +
                "\"deletedDate\": null, " +
                "\"deletedBy\": null, " +
                "\"isDelete\": null, " +
                "\"id\": \"d9bd85fc-b97e-41d8-a6a5-b0e32b61e77f\", " +
                "\"name\": \"" + time + "\", " +
                "\"number\": 54, " +
                "\"shortName\": null, " +
                "\"isDispatcher\": null}";

        Response updateUserGroup = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .body(requestBody)
                .put("http://172.20.207.16:9000/client-relations/user-group-application/" + validUuid)
                .andReturn();

        int statusCode = updateUserGroup.getStatusCode();
        assertEquals(200, statusCode);

        String responseBody = updateUserGroup.getBody().asString();
        assertNotNull(responseBody);
    }


    @Test
    @Description("Создание и удаление группы сотрудников по заявкам")
    @DisplayName("Успешное удаление группы сотрудников по заявкам")
    public void deleteUserGroupApplication_Success() {


        String validUuid = "123e4567-e89b-12d3-a456-426614174000"; // Заменить на реальный UUID

        Response deleteUserGroup = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .delete("http://172.20.207.16:9000/client-relations/user-group-application/" + validUuid)
                .andReturn();

        int statusCode = deleteUserGroup.getStatusCode();
        assertEquals(201, statusCode);
    }

    @Test
    @Description("Получение группы сотрудников по заявкам - неавторизованный доступ")
    @DisplayName("Ошибка авторизации при получении группы")
    public void getUserGroupApplication_Unauthorized() {


        Response getUserGroup = RestAssured
                .given()
                .headers("Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.16:9000/client-relations/user-group-application/" + validUuid)
                .andReturn();

        int statusCode = getUserGroup.getStatusCode();
        assertEquals(401, statusCode);
    }

    @Test
    @Description("Обновление группы сотрудников - неавторизованный доступ")
    @DisplayName("Ошибка авторизации при обновлении группы")
    public void updateUserGroupApplication_Unauthorized() {
        String requestBody = "{\"name\": \"Test Group\"}";

        Response updateUserGroup = RestAssured
                .given()
                .headers("Content-Type", "application/json; charset=UTF-8")
                .body(requestBody)
                .put("http://172.20.207.16:9000/client-relations/user-group-application/" + validUuid)
                .andReturn();

        int statusCode = updateUserGroup.getStatusCode();
        assertEquals(401, statusCode);
    }

    @Test
    @Description("Удаление группы сотрудников - неавторизованный доступ")
    @DisplayName("Ошибка авторизации при удалении группы")
    public void deleteUserGroupApplication_Unauthorized() {
        String validUuid = "123e4567-e89b-12d3-a456-426614174000";

        Response deleteUserGroup = RestAssured
                .given()
                .headers("Content-Type", "application/json; charset=UTF-8")
                .delete("http://172.20.207.16:9000/client-relations/user-group-application/" + validUuid)
                .andReturn();

        int statusCode = deleteUserGroup.getStatusCode();
        assertEquals(401, statusCode);
    }

    @Test
    @Description("Получение группы с неверным UUID")
    @DisplayName("Ошибка при неверном формате UUID")
    public void getUserGroupApplication_InvalidUuid() {
        AuthTokenTest authService = new AuthTokenTest();
        String accessToken = authService.getAccessToken();

        String invalidUuid = "invalid-uuid-format";

        Response getUserGroup = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.16:9000/client-relations/user-group-application/" + invalidUuid)
                .andReturn();

        int statusCode = getUserGroup.getStatusCode();
        assertEquals(500, statusCode);
    }

    @Test
    @Description("Обновление группы с неверным UUID")
    @DisplayName("Ошибка при обновлении с неверным UUID")
    public void updateUserGroupApplication_InvalidUuid() {
        AuthTokenTest authService = new AuthTokenTest();
        String accessToken = authService.getAccessToken();

        String invalidUuid = "invalid-uuid-format";
        String requestBody = "{\"name\": \"Test Group\"}";

        Response updateUserGroup = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .body(requestBody)
                .put("http://172.20.207.16:9000/client-relations/user-group-application/" + invalidUuid)
                .andReturn();

        int statusCode = updateUserGroup.getStatusCode();
        assertEquals(500, statusCode);
    }

    @Test
    @Description("Получение несуществующей группы")
    @DisplayName("Ошибка при запросе несуществующей группы")
    public void getUserGroupApplication_NotFound() {

        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";

        Response getUserGroup = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.16:9000/client-relations/user-group-application/" + nonExistentUuid)
                .andReturn();

        int statusCode = getUserGroup.getStatusCode();
        assertEquals(500, statusCode);
    }

    @Test
    @Description("Получение списка групп сотрудников по заявкам с пагинацией")
    @DisplayName("Успешное получение списка групп сотрудников")
    public void getUserGroupApplicationList_Success() {

        Response getUserGroupList = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.16:9000/client-relations/user-group-application")
                .andReturn();

        int statusCode = getUserGroupList.getStatusCode();
        assertEquals(200, statusCode);

        String responseBody = getUserGroupList.getBody().asString();
        assertNotNull(responseBody);
    }

    @Test
    @Description("Получение списка групп сотрудников с указанием страницы")
    @DisplayName("Успешное получение списка с пагинацией")
    public void getUserGroupApplicationList_WithPagination() {

        Response getUserGroupList = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .get("http://172.20.207.16:9000/client-relations/user-group-application")
                .andReturn();

        int statusCode = getUserGroupList.getStatusCode();
        assertEquals(200, statusCode);

        String responseBody = getUserGroupList.getBody().asString();
        assertNotNull(responseBody);
    }

    @Test
    @Description("Получение списка групп сотрудников с сортировкой по имени")
    @DisplayName("Успешное получение списка с сортировкой")
    public void getUserGroupApplicationList_WithSorting() {

        Response getUserGroupList = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .queryParam("sort", "name")
                .get("http://172.20.207.16:9000/client-relations/user-group-application")
                .andReturn();

        int statusCode = getUserGroupList.getStatusCode();
        assertEquals(200, statusCode);

        String responseBody = getUserGroupList.getBody().asString();
        assertNotNull(responseBody);
    }

    @Test
    @Description("Получение списка групп сотрудников с сортировкой по убыванию")
    @DisplayName("Успешное получение списка с сортировкой по убыванию")
    public void getUserGroupApplicationList_WithDescSorting() {

        Response getUserGroupList = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .queryParam("sort", "name,desc")
                .get("http://172.20.207.16:9000/client-relations/user-group-application")
                .andReturn();

        int statusCode = getUserGroupList.getStatusCode();
        assertEquals(200, statusCode);

        String responseBody = getUserGroupList.getBody().asString();
        assertNotNull(responseBody);
    }

    @Test
    @Description("Получение списка групп сотрудников с параметрами по умолчанию")
    @DisplayName("Успешное получение списка с параметрами по умолчанию")
    public void getUserGroupApplicationList_WithDefaultParams() {

        Response getUserGroupList = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                // Без параметров - должны использоваться значения по умолчанию
                .get("http://172.20.207.16:9000/client-relations/user-group-application")
                .andReturn();

        int statusCode = getUserGroupList.getStatusCode();
        assertEquals(200, statusCode);

        // Проверка что используется размер страницы по умолчанию (20)
        Integer size = getUserGroupList.jsonPath().getInt("size");
        assertEquals(20, size);

        // Проверка что используется страница по умолчанию (0)
        Integer number = getUserGroupList.jsonPath().getInt("number");
        assertEquals(0, number);
    }

    @Test
    @Description("Создание новой группы сотрудников по заявкам и удаление")
    @DisplayName("Успешное создание и удаление группы сотрудников")
    public void createUserGroupApplication_Success() {
        AuthTokenTest authService = new AuthTokenTest();
        String accessToken = authService.getAccessToken();

        String requestBody = "{\"name\":\"" + time + "\",\"isDispatcher\":null}";

        Response createUserGroup = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .body(requestBody)
                .post("http://172.20.207.16:9000/client-relations/user-group-application")
                .andReturn();

        int statusCode = createUserGroup.getStatusCode();
        assertEquals(200, statusCode);
        createUserGroup.prettyPrint();

        String responseBody = createUserGroup.getBody().asString();
        assertNotNull(responseBody);

        // Извлекаем id из ответа создания группы
        String id = createUserGroup.jsonPath().getString("id");
        assertNotNull("ID созданной группы не должен быть null", id);


        // Используем полученный id для удаления
        Response deleteUserGroup = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .delete("http://172.20.207.16:9000/client-relations/user-group-application/" + id)
                .andReturn();

        int deleteStatusCode = deleteUserGroup.getStatusCode(); // исправлено: убрал дублирование переменной
        assertEquals(201, deleteStatusCode);
    }

}
