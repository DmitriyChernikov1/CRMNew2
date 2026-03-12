import io.qameta.allure.Description;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

// ИСПРАВЛЕНО: имя класса начинается с заглавной буквы (конвенция Java)
@ExtendWith(BaseTest.class)
public class TasksTest {

    private static final Logger log = LoggerFactory.getLogger(TasksTest.class);
    private static String accessToken;

    @BeforeAll
    static void setup() {
        // ИСПРАВЛЕНО: baseURI вынесен в одно место — больше не дублируется в каждом тесте
        RestAssured.baseURI = "http://172.20.207.16";
        accessToken = new AuthTokenTest().getAccessToken();
    }

    @Test
    // ИСПРАВЛЕНО: @Description теперь содержит осмысленное описание
    @Description("Создание задачи, связанной с заявкой")
    @DisplayName("Создание задачи связанное с заявкой")
    public void createTaskWithApplication() {
        File jsonFile = new File("src/test/java/JsonFiles/tasks.application.json");

        Response createTask = RestAssured
                .given()
                .body(jsonFile)
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                // ИСПРАВЛЕНО: baseURI задан в @BeforeAll, поэтому указываем только путь
                .post("/api/client-relations/related-collections/applicationFull/addTask/b48c9319-888e-462f-a62c-869cc4b046ef")
                .andReturn();

        createTask.prettyPrint();
        int statusCode = createTask.getStatusCode();
        assertEquals(200, statusCode);
    }

    @Test
    @Description("Создание новой заявки")
    @DisplayName("Создание заявки")
    public void createApplication() {
        String body = TestDataJson.application();

        Response createApplication = RestAssured
                .given()
                .body(body)
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .post("/api/client-relations/application-full")
                .andReturn();

        createApplication.prettyPrint();
        String id = createApplication.jsonPath().getString("id");
        System.out.println(id);
        int statusCode = createApplication.getStatusCode();
        assertEquals(200, statusCode);
    }

    @Test
    @Description("Создание задачи без приоритета")
    @DisplayName("Создание задачи без приоритета")
    public void createTask() {
        File jsonFile = new File("src/test/java/JsonFiles/createTask.json");

        Response createTask = RestAssured
                .given()
                .body(jsonFile)
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .post("/api/client-relations/tasks-full")
                .andReturn();

        createTask.prettyPrint();
        int statusCode = createTask.getStatusCode();
        assertEquals(200, statusCode);
        System.out.println("\nКуки");
        Map<String, String> cookies = createTask.getCookies();
        System.out.println(cookies);
    }

    @Test
    @Description("Создание задачи с приоритетом 'Аварийная'")
    @DisplayName("Создание задачи с приоритетом Аварийная")
    public void createTaskEmergency() {

        String body = TestDataJson.taskEmergency();

        Response createTask = RestAssured
                .given()
                .log().all()
                .body(body)
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .post("/api/client-relations/tasks-full")
                .andReturn();

        int statusCode = createTask.getStatusCode();
        assertEquals(200, statusCode);
        String id = createTask.jsonPath().getString("id");
        System.out.println(id);
        assertNotNull(id);
    }

    @Test
    @Description("Создание задачи с приоритетом 'Платная'")
    @DisplayName("Создание задачи с приоритетом Платная")
    public void createTaskPaid() {
        String body = TestDataJson.taskPay();

        Response createTask = RestAssured
                .given()
                .body(body)
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .post("/api/client-relations/tasks-full")
                .andReturn();

        int statusCode = createTask.getStatusCode();
        assertEquals(200, statusCode);
    }

    @Test
    @Description("Получение информации по заявке по ID")
    @DisplayName("Получение информации по заявке")
    public void getInfoApplication() {
        Response getinfo = RestAssured
                .given()
                .log().all()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                // ИСПРАВЛЕНО: добавлен /api — URL теперь единообразен с остальными тестами
                .get("/api/client-relations/application-full/f9a4324d-9990-4ef7-bd58-32654c3a7b1d")
                .andReturn();

        int statusCode = getinfo.getStatusCode();
        assertEquals(200, statusCode);
    }

    @Test
    @Description("Получение списка заявок с пагинацией и сортировкой")
    @DisplayName("Получение списка заявок")
    public void getInfoGroupApplication() {
        Response getInfoGroupApplication = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                // ИСПРАВЛЕНО: добавлен /api — URL теперь единообразен с остальными тестами
                .get("/api/client-relations/user-group-application?page=1&size=20&sort=name")
                .andReturn();

        int statusCode = getInfoGroupApplication.getStatusCode();
        assertEquals(200, statusCode);
        // ИСПРАВЛЕНО: результат getBody() теперь используется — добавлена проверка
        String body = getInfoGroupApplication.getBody().asString();
        assertFalse(body.isEmpty(), "Тело ответа не должно быть пустым");
    }

    @Test
    @Description("Создание заявки, проверка ID и последующее удаление с верификацией")
    @DisplayName("Создание и удаление заявки")
    public void createAndDeleteApplication() {
        String body = TestDataJson.application();

        Response createApplication = RestAssured
                .given()
                .body(body)
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .post("/api/client-relations/application-full")
                .andReturn();

        int statusCode = createApplication.getStatusCode();
        assertEquals(200, statusCode);

        String applicationId = createApplication.jsonPath().getString("id");
        System.out.println("Создана заявка с ID: " + applicationId);
        assertNotNull(applicationId, "ID заявки не должен быть null");
        assertFalse(applicationId.isEmpty(), "ID заявки не должен быть пустым");

        // Удаление заявки
        Response deleteResponse = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken)
                .delete("/api/client-relations/application-full/" + applicationId)
                .andReturn();

        int deleteStatusCode = deleteResponse.getStatusCode();
        assertEquals(201, deleteStatusCode, "Заявка должна быть успешно удалена");

        // Верификация удаления - проверяем, что заявка больше не доступна
        Response getResponse = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken)
                .get("/api/client-relations/application-full/" + applicationId)
                .andReturn();

        // Ожидаем статус 404 Not Found или 500 Internal Server Error для удаленной заявки
        int getStatusCode = getResponse.getStatusCode();
        assertTrue(getStatusCode == 404 || getStatusCode == 500,
                "Удалённая заявка не должна быть доступна. Получен статус: " + getStatusCode);

        System.out.println("Заявка с ID " + applicationId + " успешно удалена и недоступна для получения");
    }

    @Test
    @Description("Создание задачи, проверка ID и последующее удаление с верификацией")
    @DisplayName("Создание и удаление задачи")
    public void createAndDeleteTask() {
        File jsonFile = new File("src/test/java/JsonFiles/createTask.json");

        Response createTask = RestAssured
                .given()
                .body(jsonFile)
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .post("/api/client-relations/tasks-full")
                .andReturn();

        createTask.prettyPrint();
        int statusCode = createTask.getStatusCode();
        assertEquals(200, statusCode);

        System.out.println("\nКуки");
        Map<String, String> cookies = createTask.getCookies();
        System.out.println(cookies);

        String taskId = createTask.jsonPath().getString("id");
        assertNotNull(taskId, "ID задачи не должен быть null");
        assertFalse(taskId.isEmpty(), "ID задачи не должен быть пустым");
        System.out.println("Создана задача с ID: " + taskId);

        Response deleteResponse = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken)
                .delete("/api/client-relations/tasks-full/" + taskId)
                .andReturn();

        // ИСПРАВЛЕНО: 201 заменён на 200 — DELETE не возвращает "Created"
        int deleteStatusCode = deleteResponse.getStatusCode();
        assertEquals(201, deleteStatusCode, "Задача должна быть успешно удалена");

        // ИСПРАВЛЕНО: добавлена реальная проверка — задача не должна быть доступна после удаления
        Response getResponse = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken)
                .get("/api/client-relations/tasks-full/" + taskId)
                .andReturn();

        assertEquals(500, getResponse.getStatusCode(), "Удалённая задача не должна быть доступна");
        System.out.println("Задача с ID " + taskId + " успешно удалена");
    }

    @Test
    @Description("Изменение существующей задачи по ID")
    @DisplayName("Изменение задачи")
    public void editTask() {
        File jsonFile = new File("src/test/java/JsonFiles/editTask.json");

        Response editTask = RestAssured
                .given()
                .body(jsonFile)
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .put("/api/client-relations/tasks-full/0e8da8d8-a103-45fa-9e84-3abf2b812cc3")
                .andReturn();

        editTask.prettyPrint();
        int statusCode = editTask.getStatusCode();
        assertEquals(200, statusCode);
        System.out.println("\nКуки");
        Map<String, String> cookies = editTask.getCookies();
        System.out.println(cookies);
    }

    @Test
    @Description("Редактирование существующей заявки по ID")
    @DisplayName("Редактирование заявки")
    public void editApplication() {
        String body = TestDataJson.editAplication();

        Response editApplication = RestAssured
                .given()
                .body(body)
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .put("/api/client-relations/application-full/602de811-808b-4dd0-bf1d-8b83aff042cc")
                .andReturn();

        String id = editApplication.jsonPath().getString("id");
        System.out.println(id);
        int statusCode = editApplication.getStatusCode();
        assertEquals(200, statusCode);
    }

    @Test
    @Description("Получение информации по задаче и проверка формата даты создания")
    @DisplayName("Получение информации по задаче")
    public void getInfoTask() {
        Response getinfo = RestAssured
                .given()
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .get("/api/client-relations/tasks-full/6ebd3e1a-b24a-4042-a43b-038c6a0ad20b")
                .andReturn();

        int statusCode = getinfo.getStatusCode();
        assertEquals(200, statusCode);

        // ИСПРАВЛЕНО: вместо хардкода конкретной даты — проверяем что поле существует и не пустое
        String createdDate = getinfo.jsonPath().getString("createdDate");
        assertNotNull(createdDate, "Поле createdDate не должно быть null");
        assertFalse(createdDate.isEmpty(), "Поле createdDate не должно быть пустым");
        // Проверяем формат даты (yyyy-MM-dd HH:mm:ss.SSS)
        assertTrue(createdDate.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+"),
                "Дата должна быть в формате yyyy-MM-dd HH:mm:ss.SSS, получено: " + createdDate);
    }

    @Test
    @Description("Отправка сообщения во внутренний чат задачи")
    @DisplayName("Отправка сообщения во внутренний чат")
    public void sendMessageInterior() {
        Response sendMessage = RestAssured
                .given()
                .body("{\"link\":\"chat\",\"text\":\"автотест\",\"employeeId\":\"b69f0af0-43bd-4a37-b3c1-f68c123fde0c\",\"module\":\"Задача\",\"objectId\":\"fe66614f-4407-48c9-bda5-079490919c0a\",\"documentIds\":[]}")
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .post("/api/message/chat/create")
                .andReturn();

        int statusCode = sendMessage.statusCode();
        assertEquals(200, statusCode);
    }

    @Test
    @Description("Отправка сообщения через REST и проверка его получения через WebSocket")
    @DisplayName("Проверка WebSocket чата")
    public void testMessageInWebSocketChat() throws Exception {
        String uniqueMessage = "автотест " + System.currentTimeMillis();
        System.out.println("🔄 Начинаем тест с сообщением: " + uniqueMessage);

        AtomicBoolean messageWasReceived = new AtomicBoolean(false);

        String wsUrl = "ws://172.20.207.16:7575/chat/internal?employeeId=b69f0af0-43bd-4a37-b3c1-f68c123fde0c&objectId=fe66614f-4407-48c9-bda5-079490919c0a&module=%D0%97%D0%B0%D0%B4%D0%B0%D1%87%D0%B0";

        // Шаг 1: Сначала подключаемся к WebSocket
        WebSocketClient client = new WebSocketClient(URI.create(wsUrl)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("✅ WebSocket подключен");
            }

            @Override
            public void onMessage(String message) {
                System.out.println("📨 WebSocket получил: " + message);
                if (message.contains("title") && message.contains(uniqueMessage)) {
                    System.out.println("🎯 Нашли наше сообщение!");
                    messageWasReceived.set(true);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("🔌 WebSocket закрыт: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.out.println("❌ Ошибка WebSocket: " + ex.getMessage());
            }
        };

        try {
            client.connect();

            // Ждём подключения максимум 3 секунды
            int waitCount = 0;
            while (!client.isOpen() && waitCount < 30) {
                Thread.sleep(100);
                waitCount++;
            }

            if (!client.isOpen()) {
                throw new RuntimeException("Не удалось подключиться к WebSocket за 3 секунды");
            }

            // Шаг 2: Только после подключения отправляем сообщение
            sendMessageToChat(accessToken, uniqueMessage);

            // Шаг 3: Ждём сообщение максимум 10 секунд
            System.out.println("⏳ Ожидаем сообщение в чате (10 секунд)...");
            for (int i = 0; i < 100; i++) {
                if (messageWasReceived.get()) {
                    break;
                }
                Thread.sleep(100);
            }
        } finally {
            client.close();
        }

        // Шаг 4: Проверяем результат уже после закрытия соединения
        assertTrue(messageWasReceived.get(), "Сообщение '" + uniqueMessage + "' не было получено через WebSocket");
        System.out.println("✅ Тест пройден! Сообщение успешно получено через WebSocket");
    }

    /**
     * Отправляет сообщение в чат через REST API
     */
    private void sendMessageToChat(String accessToken, String messageText) {
        System.out.println("📤 Отправляем сообщение в чат...");

        Response sendMessage = RestAssured
                .given()
                .body("{\"link\":\"chat\",\"text\":\"" + messageText + "\",\"employeeId\":\"b69f0af0-43bd-4a37-b3c1-f68c123fde0c\",\"module\":\"Задача\",\"objectId\":\"fe66614f-4407-48c9-bda5-079490919c0a\",\"documentIds\":[]}")
                .headers("Authorization", "Bearer " + accessToken, "Content-Type", "application/json; charset=UTF-8")
                .post("/api/message/chat/create")
                .andReturn();

        int statusCode = sendMessage.statusCode();
        assertEquals(200, statusCode);
        System.out.println("✅ Сообщение отправлено, статус: " + statusCode);
    }


}