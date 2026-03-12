import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Administration {
    AuthTokenTest authService = new AuthTokenTest();
    String accessToken = authService.getAccessToken();

    @Test
    @Description("putUsers")
    @DisplayName("Изменение пользователя")
    public void putUser() {

        String body = TestDataJson.bodyForUser;
        Response putUser = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .body(body)
                .put("http://172.20.207.16/api/users/user_employee/c1934b45-4752-4c4d-8d52-86c749bfacba")
                .andReturn();

        int statusCode = putUser.getStatusCode();
        assertEquals(200, statusCode);
        putUser.getBody().asString();
    }
}
