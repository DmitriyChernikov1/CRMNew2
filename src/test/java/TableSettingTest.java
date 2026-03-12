import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BaseTest.class)
public class TableSettingTest {
    @Test
    @Description("get setting")
    @DisplayName("Получение настроек Заявок")
    public void  getSettingTable() {
        AuthTokenTest authService = new AuthTokenTest();
        String accessToken = authService.getAccessToken();
        Response GetSetting = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .queryParam("userId","01bb66cc-6456-41ce-9fb7-ad38c25fea43")
                .queryParam("tableName","Application")
                .get("http://172.20.207.16/api/users/settings-table/settings")
                .andReturn();
        GetSetting.prettyPrint();

        int statusCode = GetSetting.getStatusCode();
        assertEquals(200, statusCode);
    }
    @Test
    @Description("Шапка страницы")
    @DisplayName("Отображение данных шапки")
    public void header(){
        AuthTokenTest authService = new AuthTokenTest();
        String accessToken = authService.getAccessToken();
        Response GetHeader = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .get("http://172.20.207.16/api/users/topmenu")
                .andReturn();
        int statusCode = GetHeader.getStatusCode();
        assertEquals(200,statusCode);
        String content = GetHeader.jsonPath().getString("content");
        assertNotNull(content);
    }
}
