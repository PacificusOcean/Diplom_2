import com.github.javafaker.Faker;
import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;

import pojo.CreateUser;



public class LoginUserTest {
    private Faker faker;

    @Before
    public void setUp() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
        faker = new Faker();
    }

    @Test
    public void testSuccessfulLogin() {
        //  Создаем данные пользователя для успешной авторизации
        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();

        //Создаём POST запрос к эндпоинту регистрации
        CreateUser request = new CreateUser(email, password, name);

        Response registrationResponse = given()
                .contentType("application/json")
                .body(request)
                .when()
                .post("/api/auth/register");

        assertEquals(200, registrationResponse.getStatusCode()); //проверяем успешную регистрацию


        // Используем сгенерированные email и пароль для авторизации
        given()
                .contentType("application/json")
                .body("{\n" +
                        "\"email\":\"" + email + "\",\n" +
                        "\"password\":\"" + password + "\"\n" +
                        "}")
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("accessToken", notNullValue());
    }


    @Test
    @Description("Не корректная попытка залогинется")
    public void testLoginIncorrectCredentials() {
        //  Используем сгенерированные данные
        String email = faker.internet().emailAddress();
        String password = faker.internet().password();


        given()
                .contentType("application/json")
                .body("{\n" +
                        "\"email\":\"" + email + "\",\n" +
                        "\"password\":\"" + password + "\"\n" +
                        "}")
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("email or password are incorrect"));
    }

    //Test для отсутствия полей
    @Test
    @Description("Попытка залогинется с отсутстующими полями")
    public void testLoginMissingFields() {
        given()
                .contentType("application/json")
                .body("{\n\"email\": \"\"}")
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("email or password are incorrect"));

    }
}



