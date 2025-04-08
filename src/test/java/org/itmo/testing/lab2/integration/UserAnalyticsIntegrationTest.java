package org.itmo.testing.lab2.integration;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import org.itmo.testing.lab2.controller.UserAnalyticsController;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserAnalyticsIntegrationTest {

    private Javalin app;

    private static final Gson gson = new Gson();

    private static final int PORT = 7002;
    private static final String USER_ID = "user1";
    private static final long MINUTES = 1440;

    @BeforeAll
    void setUp() {
        app = UserAnalyticsController.createApp();
        app.start(PORT);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = PORT;
    }

    @AfterAll
    void tearDown() {
        app.stop();
    }

    @Test
    @Order(1)
    @DisplayName("Проверка успешной регистрации пользователя")
    void registerTest() {
        given()
            .queryParam("userId", USER_ID)
            .queryParam("userName", "Alice")
            .when()
            .post("/register")
            .then()
            .statusCode(200)
            .body(is("User registered: true"));
    }

    @Test
    @Order(2)
    @DisplayName("Проверка успешного добавления записи сессии")
    void recordSessionTest() {
        given()
            .queryParam("userId", USER_ID)
            .queryParam("loginTime", "2025-02-04T17:57:58.928497400")
            .queryParam("logoutTime", "2025-02-05T17:57:58.928497400")
            .when()
            .post("/recordSession")
            .then()
            .statusCode(200)
            .body(is("Session recorded"));
    }

    @Test
    @Order(3)
    @DisplayName("Проверка успешного получения общего времени активности")
    void totalActivityTest() {
        given()
            .queryParam("userId", USER_ID)
            .when()
            .get("/totalActivity")
            .then()
            .statusCode(200)
            .body(containsString("Total activity:"))
            .body(containsString("minutes"))
            .body(containsString(String.valueOf(MINUTES)));
    }

    @Test
    @Order(4)
    @DisplayName("Проверка успешного получения списка неактивных пользователей")
    void inactiveUsersTest() {
        given()
            .queryParam("days", 1)
            .when()
            .get("/inactiveUsers")
            .then()
            .statusCode(200)
            .body(is(gson.toJson(List.of(USER_ID))));
    }

    @Test
    @Order(5)
    @DisplayName("Проверка успешного получения месячной активности пользователя по дням")
    void monthlyActivityTest() {
        given()
            .queryParam("userId", USER_ID)
            .queryParam("month", "2025-02")
            .when()
            .get("/monthlyActivity")
            .then()
            .statusCode(200)
            .body(is(gson.toJson(Map.of("2025-02-04", MINUTES))));
    }

    @Test
    @Order(6)
    @DisplayName("Проверка ошибки существования пользователя при повторной регистрации")
    void errorRegisterDuplicationTest() {
        given()
            .queryParam("userId", USER_ID)
            .queryParam("userName", "Alice")
            .when()
            .post("/register")
            .then()
            .statusCode(400)
            .body(is("User already exists"));
    }

    @Order(7)
    @ParameterizedTest(name = "Проверка ошибки отсутствия параметра запроса при регистрации пользователя")
    @CsvSource(value = {"null,Alice", "userId,null"}, nullValues = {"null"})
    void errorRegisterNullParamTest(String userId, String userName) {
        var given = given();
        if (userId != null) given.queryParam("userId", userId);
        if (userName != null) given.queryParam("userName", userName);

        given
            .when()
            .post("/register")
            .then()
            .statusCode(400)
            .body(is("Missing parameters"));
    }

    @Order(8)
    @ParameterizedTest(name = "Проверка ошибки отсутствия параметра запроса при добавлении сессий")
    @CsvSource(value = {"null,1,1", "1,null,1", "1,1,null"}, nullValues = {"null"})
    void errorRecordSessionNullParamTest(String userId, String loginTime, String logoutTime) {
        var given = given();
        if (userId != null) given.queryParam("userId", userId);
        if (loginTime != null) given.queryParam("loginTime", loginTime);
        if (logoutTime != null) given.queryParam("logoutTime", logoutTime);

        given
            .when()
            .post("/recordSession")
            .then()
            .statusCode(400)
            .body(is("Missing parameters"));
    }

    @Order(9)
    @ParameterizedTest(name = "Проверка ошибки парсинга параметров запроса при добавлении сессий")
    @CsvSource(value = {"2025-02-04T17:57:58.928497400,1", "1,2025-02-04T17:57:58.928497400"})
    void errorRecordSessionParsingParamTest(String loginTime, String logoutTime) {
        given()
            .queryParam("userId", USER_ID)
            .queryParam("loginTime", loginTime)
            .queryParam("logoutTime", logoutTime)
            .when()
            .post("/recordSession")
            .then()
            .statusCode(400)
            .body(is("Invalid data: Text '1' could not be parsed at index 0"));
    }

    @Test
    @Order(10)
    @DisplayName("Проверка ошибки отсутствия пользователя в системе при добавлении сессий")
    void errorRecordSessionUserNotFoundTest() {
        given()
            .queryParam("userId", "nullUser")
            .queryParam("loginTime", "2025-02-04T17:57:58.928497400")
            .queryParam("logoutTime", "2025-02-05T17:57:58.928497400")
            .when()
            .post("/recordSession")
            .then()
            .statusCode(400)
            .body(is("Invalid data: User not found"));
    }

    @Test
    @Order(11)
    @DisplayName("Проверка ошибки отсутствия userId при получении общего времени активности")
    void errorTotalActivityMissingUserIdTest() {
        given()
            .when()
            .get("/totalActivity")
            .then()
            .statusCode(400)
            .body(is("Missing userId"));
    }

    @Test
    @Order(12)
    @DisplayName("Проверка ошибки отсутствия пользователя при получении общего времени активности")
    void errorTotalActivityUserNotFoundTest() {
        given()
            .queryParam("userId", "nonExistentUser")
            .when()
            .get("/totalActivity")
            .then()
            .statusCode(400)
            .body(is("Invalid data: User not found"));
    }

    @Test
    @Order(13)
    @DisplayName("Проверка ошибки отсутствия параметра days при получении списка неактивных пользователей")
    void errorInactiveUsersMissingDaysTest() {
        given()
            .when()
            .get("/inactiveUsers")
            .then()
            .statusCode(400)
            .body(is("Missing days parameter"));
    }

    @Test
    @Order(14)
    @DisplayName("Проверка ошибки неверного формата параметра days при получении списка неактивных пользователей")
    void errorInactiveUsersInvalidDaysFormatTest() {
        given()
            .queryParam("days", "invalid")
            .when()
            .get("/inactiveUsers")
            .then()
            .statusCode(400)
            .body(is("Invalid number format for days"));
    }

    @Order(15)
    @ParameterizedTest(name = "Проверка ошибки отсутствия параметра запроса при получении месячной активности")
    @CsvSource(value = {"null,1", "1,null"}, nullValues = {"null"})
    void errorMonthlyActivityMissingUserIdTest(String userId, String month) {
        var given = given();
        if (userId != null) given.queryParam("userId", userId);
        if (month != null) given.queryParam("month", month);

        given
            .when()
            .get("/monthlyActivity")
            .then()
            .statusCode(400)
            .body(is("Missing parameters"));
    }

    @Test
    @Order(16)
    @DisplayName("Проверка ошибки неверного формата параметра month при получении месячной активности")
    void errorMonthlyActivityInvalidMonthFormatTest() {
        given()
            .queryParam("userId", USER_ID)
            .queryParam("month", "invalid")
            .when()
            .get("/monthlyActivity")
            .then()
            .statusCode(400)
            .body(is("Invalid data: Text 'invalid' could not be parsed at index 0"));
    }

    @Test
    @Order(17)
    @DisplayName("Проверка ошибки отсутствия пользователя при получении месячной активности")
    void errorMonthlyActivityUserNotFoundTest() {
        given()
            .queryParam("userId", "nonExistentUser")
            .queryParam("month", "2025-02")
            .when()
            .get("/monthlyActivity")
            .then()
            .statusCode(400)
            .body(is("Invalid data: No sessions found for user"));
    }

}
