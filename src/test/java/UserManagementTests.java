import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;

public class UserManagementTests {

    private Faker faker;
    private String accessToken;
    private String initialEmail;
    private String initialPassword;
    private final String BASE_URI = "https://stellarburgers.nomoreparties.site";
    private final String REGISTER_ENDPOINT = "/api/auth/register";
    private final String LOGIN_ENDPOINT = "/api/auth/login";
    private final String USER_ENDPOINT = "/api/auth/user";

    @Before
    public void setup() {
        RestAssured.baseURI = BASE_URI;
        faker = new Faker();
        initialEmail = faker.internet().emailAddress();
        initialPassword = faker.internet().password();
    }

    @After
    public void tearDown() {
        deleteUser();
    }


    private void deleteUser() {
        if (initialEmail != null && accessToken != null) {
            given()
                    .header("Authorization", "Bearer " + accessToken)
                    .when()
                    .delete(USER_ENDPOINT)
                    .then()
                    .statusCode(200);

        }
    }


    private String registerUser() {
        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();


        Response registrationResponse = given()
                .contentType(ContentType.JSON)
                .body("{\n" +
                        "\"email\":\"" + email + "\",\n" +
                        "\"password\":\"" + password + "\",\n" +
                        "\"name\":\"" + name + "\"\n" +
                        "}")
                .when()
                .post(REGISTER_ENDPOINT);

        registrationResponse.then().statusCode(200)
                .body("success", equalTo(true));


        initialEmail = email;
        initialPassword = password;
        return registrationResponse.jsonPath().getString("accessToken");


    }

    private String authenticateUser() {

        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body("{\n\"email\":\"" + initialEmail + "\",\n\"password\":\"" + initialPassword + "\"\n}")
                .when()
                .post(LOGIN_ENDPOINT);

        loginResponse.then().statusCode(200);
        return loginResponse.jsonPath().getString("accessToken");
    }



    @Test
    public void testSuccessfulUserDataUpdate() {
        accessToken = authenticateUser();
        String newEmail = faker.internet().emailAddress();
        String newName = faker.name().fullName();

        Response updateResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body("{\n\"email\":\"" + newEmail + "\",\n\"name\":\"" + newName + "\"\n}")
                .when()
                .put(USER_ENDPOINT);

        updateResponse.then().statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(newEmail))
                .body("user.name", equalTo(newName));


    }

    //Другие тесты аналогично
}

