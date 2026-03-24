import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import utils.TestDataJson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(BaseTest.class)
public class ContactsTest {

    AuthTokenTest authService = new AuthTokenTest();
    String accessToken = authService.getAccessToken();

    @Test
    @Description("Контакты")
    @DisplayName("Создание и удаление физ.лица")
    public void createContactIndividual() {

        String body = TestDataJson.createContactIndividual;
        Response createTask = RestAssured
                .given()
                .body(body)
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .post("http://172.20.207.16/api/client/client-full")
                .andReturn();

        int statusCode = createTask.getStatusCode();
        assertEquals(200, statusCode);
        String contactId = createTask.jsonPath().getString("id");

        // Проверяем, что ID получен
        assertNotNull(contactId, "ID созданного контакта не должен быть null");

        Response deleteResponse = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .delete("http://172.20.207.16/api/client/client-full/" + contactId)
                .andReturn();

        int deleteStatusCode = deleteResponse.getStatusCode();
        assertEquals(201, deleteStatusCode);

        Response getDeletedResponse = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.16/api/client/client-full/" + contactId)
                .andReturn();

        int getAfterDeleteStatusCode = getDeletedResponse.getStatusCode();
        assertEquals(500, getAfterDeleteStatusCode, "После удаления контакт не должен находиться в системе");
    }

    @Test
    @Description("Контакты")
    @DisplayName("Создание и удаление юр.лица")
    public void createContactLegalEntity() {

        String body = TestDataJson.createContactLegalEntity;
        Response createTask = RestAssured
                .given()
                .body(body)
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .post("http://172.20.207.16/api/client/client-full")
                .andReturn();

        int statusCode = createTask.getStatusCode();
        assertEquals(200, statusCode);
        String contactId = createTask.jsonPath().getString("id");

        // Проверяем, что ID получен
        assertNotNull(contactId, "ID созданного контакта не должен быть null");

        Response deleteResponse = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .delete("http://172.20.207.16/api/client/client-full/" + contactId)
                .andReturn();

        int deleteStatusCode = deleteResponse.getStatusCode();
        assertEquals(201, deleteStatusCode);

        Response getDeletedResponse = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.16/api/client/client-full/" + contactId)
                .andReturn();

        int getAfterDeleteStatusCode = getDeletedResponse.getStatusCode();
        assertEquals(500, getAfterDeleteStatusCode, "После удаления контакт не должен находиться в системе");
    }
}