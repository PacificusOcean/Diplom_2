import com.github.javafaker.Faker;

import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class  UserManagementTests {

    private Faker faker;
    private String createdUserToken;

    private final String BASE_URL = "https://stellarburgers.nomoreparties.site";
    private final String REGISTER_ENDPOINT = "/api/auth/register";
    private final String USER_ENDPOINT = "/api/auth/user";
    private static final String LOGIN_ENDPOINT = "/auth/login";

    DataGenerator dataGenerator = new DataGenerator();
    Map<String, String> userData = dataGenerator.generateUserData();

    String email = userData.get("email");
    String password = userData.get("password");
    String name = userData.get("name");

    @Before
    public void setUp() {
        RestAssured.baseURI = BASE_URL;
        faker = new Faker();
    }

    @Step("Регистрация пользователя и проверка успешного результата")

    public Response registerUser(String email, String password, String name) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", email);
        requestBody.put("password", password);
        requestBody.put("name", name);

        Response response = given()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post(REGISTER_ENDPOINT);

        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("accessToken", startsWith("Bearer "))
                .body("refreshToken", notNullValue())
                .body("user.email", equalTo(email))
                .body("user.name", equalTo(name));

        return response;
    }

    @Step("Обновление и проверка данных пользователя")

    public Response updateUserAndValidate(String token, String newEmail, String newName) {

        Map<String, String> requestBody = new HashMap<>();
        if (newEmail != null) {
            requestBody.put("email", newEmail);
        }
        if (newName != null) {
            requestBody.put("name", newName);
        }

        // Проверка на пустой запрос
        if (requestBody.isEmpty()) {
            return given()
                    .header("Content-Type", "application/json")
                    .header("Authorization", token)
                    .when()
                    .patch(USER_ENDPOINT);
        } else {
            return given()
                    .header("Content-Type", "application/json")
                    .header("Authorization", token)
                    .body(requestBody)
                    .when()
                    .patch(USER_ENDPOINT);
        }
    }


    @Step("Ошибка без авторизации")
    public static void validateUnauthorizedResponse(Response response) {
        response
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @Step("Email без авторизации")
    public static Response updateEmailWithOutAuthorization(String newEmail, String password, String name) {
        String requestBody = "{\"email\":\"" + newEmail + "\", \"name\":\"" + name + "\", \"password\":\"" + password + "\"}";

        return given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .patch("/api/auth/user");
    }

    @Step("Name  без авторизации")
    public static Response updateNameWithOutAuthorization(String email, String password, String newName) {
        String requestBody = "{\"email\":\"" + email + "\", \"name\":\"" + newName + "\", \"password\":\"" + password + "\"}";
        return given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .patch("/api/auth/user");
    }

    // Пробуем менять имя без авторизации
    @Test
    @Description("Изменение имени пользователя без авторизации")
    public void changeNameWithOutAuthorization() {
        String password = faker.internet().password();
        String Email = faker.internet().emailAddress();

        String NewName = faker.name().fullName();

        Response updateResponse = updateNameWithOutAuthorization(Email, password, NewName);
        validateUnauthorizedResponse(updateResponse);
    }
    // Меняем Email буз авторизации
    @Test
    @Description("Попытка измененить email без авторизации ")
    public void changeEmailWithOutAuthorization() {
        String password = faker.internet().password();
        String name = faker.name().fullName();

        String newEmail = faker.internet().emailAddress();

        Response updateResponse = updateEmailWithOutAuthorization(newEmail, password, name);

        validateUnauthorizedResponse(updateResponse);
    }

    @Test // Изменили имя и email
    @Description("Изменение email и имени пользователя")
    public void updateUserInformation() {
// Регистрация пользователя
        Response registrationResponse = registerUser(email, password, name);
        createdUserToken = registrationResponse.jsonPath().getString("accessToken");
        // Генерация новых данных пользователя
        String newEmail = faker.internet().emailAddress();
        String newName = faker.name().fullName();

        // Обновление данных пользователя
        Response updateResponse = updateUserAndValidate(createdUserToken, newEmail, newName);

        // Проверка успешного обновления данных
        updateResponse.then().statusCode(200);
    }

    @Test // Изменили  email
    @Description("Изменение email пользователя")
    public void updateUserEmail() {
        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();

        Response registrationResponse = registerUser(email, password, name);
        createdUserToken = registrationResponse.jsonPath().getString("accessToken");

        String newEmail = faker.internet().emailAddress();

        Response updateResponse = updateUserAndValidate(createdUserToken, newEmail, null);

        // Проверка успешного обновления email
        updateResponse.then().statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(newEmail)); // Проверяем новое значение email
    }

    @Test // Изменили имя
    @Description("Изменение имени пользователя")
    public void updateUserName() {
        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();

        Response registrationResponse = registerUser(email, password, name);
        createdUserToken = registrationResponse.jsonPath().getString("accessToken");

        String newName = faker.name().fullName();

        Response updateResponse = updateUserAndValidate(createdUserToken, null, newName);

        // Проверка успешного обновления имени
        updateResponse.then().statusCode(200)
                .body("success", equalTo(true))
                .body("user.name", equalTo(newName));
    }

    @Test // Изменили пароль
    @Description("Обнавление пароля пользователя")
    public void updateUserPassword() {
        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();

        Response registrationResponse = registerUser(email, password, name);
        createdUserToken = registrationResponse.jsonPath().getString("accessToken");

        String newPassword = faker.internet().password();

        // Отправляем запрос на изменение пароля
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("password", newPassword);

        Response updateResponse = given()
                .header("Content-Type", "application/json")
                .header("Authorization", createdUserToken)
                .body(requestBody)
                .when()
                .patch(USER_ENDPOINT);

        // Проверка успешного изменения пароля
        updateResponse.then().statusCode(200)
                .body("success", equalTo(true));

        // После изменения пароля, обновляем accessToken
        Response loginResponse = given()
                .header("Content-Type", "application/json")
                .body("{\"email\": \"" + email + "\", \"password\": \"" + newPassword + "\"}")
                .when()
                .post(LOGIN_ENDPOINT);

    }
    @Test// Изменили пароль без авторизации
    @Description("Изменение пароля без авторизации")
    public void updateUserPasswordUnauthenticated() {
        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();
        Response registrationResponse = registerUser(email, password, name);

        String newPassword = faker.internet().password();

        // Отправляем запрос на изменение пароля без авторизации
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("password", newPassword);

        Response updateResponse = given()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .patch(USER_ENDPOINT);


        updateResponse.then().statusCode(401);
    }


    @Step("Удалить пользователя")
    public void deleteUserByToken(String token) {
        String cleanToken = token.replace("Bearer ", "");
        // Отправляем запрос на удаление пользователя
        Response deleteResponse = given()
                .header("Authorization", "Bearer " + cleanToken)
                .when()
                .delete("/api/auth/user");

        assertThat(deleteResponse.getStatusCode(), is(202));
        assertThat(deleteResponse.jsonPath().getBoolean("success"), is(true));
        String expectedMessage = "User successfully removed";
        assertThat(deleteResponse.jsonPath().getString("message"), is(expectedMessage));
        System.out.println("Пользователь успешно удален");
    }


    @After
    public void tearDown() {
        if (createdUserToken != null && !createdUserToken.isEmpty()) {
            deleteUserByToken(createdUserToken);
        }
    }
}



