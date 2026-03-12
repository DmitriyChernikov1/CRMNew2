import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
@ExtendWith(BaseTest.class)
public class RegulationTest {
    @Test
    @Description("regulationTest job")
    @DisplayName("Создание регламентной работы")
    public void createReglamentJob(){
        AuthTokenTest authService = new AuthTokenTest();
        String accessToken = authService.getAccessToken();
        // Назначаем файл с телом запроса
        File jsonFile = new File("src/test/java/JsonFiles/reglament.json");
        //Параметры предаваемые
        Response createApplication = RestAssured
                .given()
                .body(jsonFile)
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .post("http://172.20.207.16/api/regulation/routine-work/create-with-time-slots")
                .andReturn();
        // Выводим результат и проверяем статус код
        createApplication.prettyPrint();
        int statusCode = createApplication.getStatusCode();
        assertEquals(200, statusCode);
    }
    @Test
    @Description("regulationTest kind-work")
    @DisplayName("Создание видов работ")
    public void createKindWork(){
        AuthTokenTest authService = new AuthTokenTest();
        String accessToken = authService.getAccessToken();
        Response createApplication = RestAssured
                .given()
                .body("{\"name\": \"автотест\"}")
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .post("http://172.20.207.16/api/regulation/kind-work")
                .andReturn();
        // Выводим результат и проверяем статус код
        createApplication.prettyPrint();
        int statusCode = createApplication.getStatusCode();
        assertEquals(200, statusCode);
    }
}
