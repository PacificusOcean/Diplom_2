import com.github.javafaker.Faker;
import io.qameta.allure.Step;

import java.util.Map;
import java.util.HashMap;

public class DataGenerator {

    private final Faker faker = new Faker(); // Инициализируем Faker здесь


    @Step("Генерация данных пользователя (email, пароль, имя)")
    public Map<String, String> generateUserData() {
        Map<String, String> userData = new HashMap<>();
        userData.put("email", faker.internet().emailAddress());
        userData.put("password", faker.internet().password(8, 16));
        userData.put("name", faker.name().firstName());
        return userData;
    }
}