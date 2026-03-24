import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NewTest {
    @Test
    public void  test(){
        given()
                .baseUri("https://reqres.in")
                .when()
                .get("/api/users?page=2")
                .then()
                .statusCode(200)
                .body("total",equalTo(12))
                .body("data.size()",equalTo(6))
                .body("data[0].id", equalTo(7));

    }

}
