import com.github.javafaker.Faker;
import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;


import pojo.CreateUser;

public class RegistrationUserTest {
    private Faker faker;

    private String saveUserToken = null;
    private final String DELETE_USER = "/api/auth/user";
    @Before
    public void setUp() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
        faker = new Faker();
    }

    @Test
    @Description("Успешная регистрация")
    public void testSuccessfulRegistration() {
        CreateUser request = new CreateUser(
                faker.internet().emailAddress(),
                faker.internet().password(),
                faker.name().firstName() + " " + faker.name().lastName());

        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200)
                .extract().response();


        saveUserToken = response.jsonPath().getString("accessToken");
    }

    @Test
    @Description("Попытка зарегистрироваться тем же пользователем")
    public void testRegistrationExistingUser() {
        // Создаем временного пользователя для проверки
        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();

        // Создаем запрос для регистрации временного пользователя
        CreateUser request = new CreateUser(email, password, name);

        // Регистрируем временного пользователя для проверки
        Response registerResponse = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/auth/register");

        // Проверяем, что временный пользователь был успешно зарегистрирован
        assertEquals(200, registerResponse.getStatusCode());

        // Пытаемся зарегистрировать того же пользователя снова (должен быть 403 Forbidden)
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("User already exists"));
    }

    @Test
    @Description("Попытка зарегистрироваться без обязательного поля")
    public void testRegistrationMissingFields() {
        CreateUser request = new CreateUser(
                faker.internet().emailAddress(),
                faker.internet().password(),
                "");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("Email, password and name are required fields"));
    }


    @After
    public void tearDown() {
        if (saveUserToken != null) {
            given()
                    .header("Authorization", "Bearer " + saveUserToken)
                    .when()
                    .delete(DELETE_USER)
                    .then()
                    .statusCode(403);
                    saveUserToken = null;
        }
    }

}

