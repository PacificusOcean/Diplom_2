import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static io.restassured.RestAssured.given;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
// удалил лишнее
import io.qameta.allure.Description;
import io.qameta.allure.Step;

public class OrderManagementTests {

    private Faker faker;
    private final String BASE_URL = "https://stellarburgers.nomoreparties.site";
    private final String REGISTER_ENDPOINT = "/api/auth/register";
    private final String LOGIN_ENDPOINT = "/api/auth/login";
    private final String ALL_ORDERS_ENDPOINT = "/api/orders/all";
    private final String INGREDIENTS_ENDPOINT = "/api/ingredients";
    private final String ORDER_ENDPOINT = "/api/orders";

    private String accessToken;

    private String createdUserToken;
    private List<String> ingredientHashes;

    @Before
    public void setUp() {
        RestAssured.baseURI = BASE_URL;
        faker = new Faker();
        ingredientHashes = getIngredientHashes();
    }

    @Step("Авторизуем пользователя")
    private void authorizeUser(String email, String password) {
        Map<String, String> user = Map.of(
                "email", email,
                "password", password
        );
        Response response = sendPostRequest(LOGIN_ENDPOINT, user);

        createdUserToken = response.jsonPath().getString("accessToken");
        response.then().statusCode(200);

    }

    @Step("Регистрация пользователя")
    private Response registerUser(String email, String password, String name) {
        Map<String, String> requestBody = Map.of(
                "email", email,
                "password", password,
                "name", name
        );
        Response response = sendPostRequest(REGISTER_ENDPOINT, requestBody);

        response.then().statusCode(200);

        return response;
    }

    @Step("Получаем хэши ингредиентов")
    private List<String> getIngredientHashes() {
        Response response = sendGetRequest(INGREDIENTS_ENDPOINT);

        response.then().statusCode(200);
        return response.jsonPath().getList("data._id", String.class);
    }

    @Step("Создаём заказ")
    private Response createOrder(String[] ingredients, boolean withAuthorization) {
        Map<String, Object> order = Map.of("ingredients", ingredients);
        Response response = withAuthorization
                ? sendAuthorizedPostRequest(ORDER_ENDPOINT, order)
                : sendPostRequest(ORDER_ENDPOINT, order);
        return response;
    }

    private Response sendAuthorizedPostRequest(String endpoint, Map<String, ?> body) {
        return given()
                .header("Authorization", createdUserToken)
                .header("Content-Type", "application/json")
                .body(body)
                .post(endpoint);
    }

    private Response sendPostRequest(String endpoint, Map<String, ?> body) {
        return given()
                .header("Content-Type", "application/json")
                .body(body)
                .post(endpoint);
    }


    @Test
    @Description("Получение заказов  пользователя авторизованного и с ингридиентами")
    public void getOrdersUserTest() {
        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();
        Response registrationResponse = registerUser(email, password, name); // Add a name to registration
        if (registrationResponse.statusCode() != 200) {
            System.err.println("Ошибка регистрации: " + registrationResponse.getBody().asString());
            return;
        }
        authorizeUser(email, password);

        String[] ingredients = ingredientHashes.subList(0, 2).toArray(new String[0]);
        Response response = createOrder(ingredients, true);
        response.then()
                .statusCode(200)
                .body("success", equalTo(true));//Create order


    }

    @Test
    @Description("Получение заказов  пользователя неавторизованного")
    public void getOrdersUserTestUnAuthorizedTest() {
        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();
        Response registrationResponse = registerUser(email, password, name); // Add a name to registration
        if (registrationResponse.statusCode() != 200) {
            System.err.println("Ошибка регистрации: " + registrationResponse.getBody().asString());
            return;
        }

        String[] ingredients = ingredientHashes.subList(0, 2).toArray(new String[0]);
        Response response = createOrder(ingredients, false);
        response.then()
                .statusCode(401)
                .body("success", equalTo(true));//Create order
    }

    @Test
    @Description("Получение заказов  без ингридиентов")
    public void createOrderWithoutIngredientsTest() {
        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();
        Response registrationResponse = registerUser(email, password, name); // Add a name to registration
        if (registrationResponse.statusCode() != 200) {
            System.err.println("Ошибка регистрации: " + registrationResponse.getBody().asString());
            authorizeUser(email, password);

            String[] emptyIngredients = {};
            Response response = createOrder(emptyIngredients, true);

            response.then()
                    .statusCode(400)
                    .body("success", equalTo(false))
                    .body("message", equalTo("Ingredient ids must be provided"));
        }
    }

    @Test
    public void getAllOrdersTest() {
        Response response = getAllOrders();
        response.then().statusCode(200)
                .body("success", equalTo(true))
                .body("orders", hasSize(greaterThanOrEqualTo(0)));
    }

    @Test
    @Description("Получение заказов  пользователя авторизованного и неверным хеш ингридиентов")
    public void InvalidIngredientHashTest() {
        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();
        Response registrationResponse = registerUser(email, password, name); // Add a name to registration
        if (registrationResponse.statusCode() != 200) {
            System.err.println("Ошибка регистрации: " + registrationResponse.getBody().asString());
            return;
        }
        authorizeUser(email, password);

        String[] ingredients = {"invalidtHash123456789"}; // не вылидный хеш
        Response response = createOrder(ingredients, true);
        response.then().statusCode(500);
    }

    private Response getAllOrders() {
        return given().get(ALL_ORDERS_ENDPOINT);
    }

    private Response sendGetRequest(String endpoint) {
        return given()
                .header("Content-Type", "application/json")
                .get(endpoint);
    }

    //Создание заказа с авторизацией
    @Test
    public void createOrderWithAuthorizationTest() {

        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();
        Response registrationResponse = registerUser(email, password, name); // Add a name to registration
        if (registrationResponse.statusCode() != 200) {
            System.err.println("Ошибка регистрации: " + registrationResponse.getBody().asString());
            return;
        }
        authorizeUser(email, password);

        String[] ingredients = ingredientHashes.subList(0, 2).toArray(new String[0]);
        Response response = createOrder(ingredients, true);
        response.then()
                .statusCode(200)
                .body("success", equalTo(true));

    }

    @Test
    public void createOrderWithOutAuthorizationTest() {

        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();
        Response registrationResponse = registerUser(email, password, name); // Add a name to registration
        if (registrationResponse.statusCode() != 200) {
            System.err.println("Ошибка регистрации: " + registrationResponse.getBody().asString());
            return;
        }

        String[] ingredients = ingredientHashes.subList(0, 2).toArray(new String[0]);
        Response response = createOrder(ingredients, false);
        response.then()
                .statusCode(401)
                .body("success", equalTo(true));//Create order

    }

    @Step("Удалить пользователя")
    public void deleteUserToken(String token) {
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
            deleteUserToken(createdUserToken);
        }
    }
}