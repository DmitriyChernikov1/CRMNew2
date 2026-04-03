import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(BaseTest.class)
public class ApplicationTaskTest {

    private static String accessToken;

    // ID созданных сущностей — передаются между тестами
    private static String createdApplicationId;
    private static String createdTaskId;

    private static final String BASE_URL      = "http://172.20.207.16";
    private static final String CR_API        = BASE_URL + "/api/client-relations";
    private static final String MSG_API       = BASE_URL + "/api/message";

    // ── Справочные ID (статичные данные системы из HAR) ──────────────────────

    // Клиент
    private static final String CLIENT_ID     = "54532e9c-2676-4059-bb37-1361fd21be43";
    private static final String CLIENT_NAME   = "Черников Дмитрий Витальевич";
    private static final String CLIENT_PHONE  = "+79304136769";

    // Сотрудники
    private static final String EMPLOYEE_MAIN_ID    = "b69f0af0-43bd-4a37-b3c1-f68c123fde0c"; // Черников
    private static final String EMPLOYEE_SECOND_ID  = "e8f0d81b-6e7a-4f5e-a5d7-8ff504b8e8c9"; // Сопин

    // Объект УК
    private static final String OBJECT_UK_ID   = "04b961a6-d12f-4865-9912-a9cdd713f9c4";
    private static final String OBJECT_UK_NAME = "д.1, корп.1, эт. 1, кв. 33";
    private static final String MAILING_ADDRESS = "г. Москва, ул. Рублево-Архангельская, д.1., корп.1., кв. 33";

    // Классификатор
    private static final String CLASSIFIER_TYPE_ID   = "ea51b5e2-04cf-4e11-ab4a-bdbadd31780e"; // "Благодарность"
    private static final String CLASSIFIER_MESTO1_ID = "654b09d3-0fac-410d-b69d-2a475241b535"; // "Алина/Дима. sla 2 мин"

    // Каналы
    private static final String CHANNEL_APPLICATION_ID = "5688c70d-cf0a-4bd3-892a-73fa260294b8"; // "Почта"
    private static final String CHANNEL_TASK_ID        = "db1ceb48-883a-4ae3-8a3c-52d08873c33f"; // "Телефон"

    // Статусы задачи
    private static final String STATUS_NEW_ID      = "ef1e31bd-35a5-4f84-90bc-776cb2f1f682"; // "Новое"
    private static final String STATUS_IN_WORK_ID  = "fd86640c-a261-4edf-b7e7-827215eca67d"; // "В работе"
    private static final String STATUS_PENDING_ID  = "54fe2ece-7a8b-4fcc-a7ae-486d9835f84e"; // "Ожидает выполнения"
    private static final String STATUS_DONE_ID     = "50daa35e-5d15-4ed5-9966-77b06f187357"; // "Выполнено"

    // Приоритет задачи
    private static final String PRIORITY_NORMAL_ID = "fefcb1ce-c0c9-48f9-b120-f5d6b678cbba"; // "Обычная"

    // ── Форматтер дат ─────────────────────────────────────────────────────────
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static String now() {
        return LocalDateTime.now().format(DT_FMT);
    }

    private static String nowPlusMinutes(int minutes) {
        return LocalDateTime.now().plusMinutes(minutes).format(DT_FMT);
    }

    private static String nowPlusDays(int days) {
        return LocalDateTime.now().plusDays(days).format(DT_FMT);
    }

    // ── Уникальные имена на каждый запуск ─────────────────────────────────────
    private static final long RUN_ID = System.currentTimeMillis();
    private static final String APPLICATION_NAME = "автотест_заявка_" + RUN_ID;
    private static final String TASK_NAME        = "автотест_задача_" + RUN_ID;
    private static final String DESCRIPTION      = "<p>автотест_" + RUN_ID + "</p>";

    // ─────────────────────────────────────────────────────────────────────────

    @BeforeAll
    static void setUp() {
        accessToken = new AuthTokenTest().getAccessToken();
        RestAssured.baseURI = BASE_URL;
        System.setProperty("java.net.useSystemProxies", "false");
    }

    private RequestSpecification authRequest() {
        return RestAssured.given()
                .baseUri(BASE_URL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "ru,ru-RU;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Authorization", "Bearer " + accessToken);
    }

    // ── Вспомогательные билдеры справочных объектов ───────────────────────────

    private Map<String, Object> buildClassifierType() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("createdBy", "user");
        m.put("createdDate", "2024-04-23 18:00:00.000");
        m.put("lastModifiedBy", "Черников Дмитрий Витальевич");
        m.put("lastModifiedDate", "2026-02-10 10:21:23.023");
        m.put("deletedDate", null); m.put("deletedBy", null); m.put("isDelete", null);
        m.put("id", CLASSIFIER_TYPE_ID);
        m.put("number", 2651);
        m.put("name", "Благодарность");
        m.put("parentId", null); m.put("parentName", null);
        m.put("typeNode", "Тип события");
        m.put("needAct", false); m.put("needCheck", false);
        m.put("needCheckList", false); m.put("maybeWait", false);
        m.put("slaStart", 60); m.put("slaClose", 120);
        m.put("favourId", null); m.put("keywordBot", null);
        m.put("stopEta", 119); m.put("domaAiId", null);
        m.put("standardTask", 1); m.put("andromedaIds", List.of());
        m.put("cmtReceipt", null);
        return m;
    }

    private Map<String, Object> buildClassifierMesto1() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("createdBy", "Черников Дмитрий Витальевич");
        m.put("createdDate", "2025-11-27 11:15:54.962");
        m.put("lastModifiedBy", "Черников Дмитрий Витальевич");
        m.put("lastModifiedDate", "2026-04-02 12:05:47.197");
        m.put("deletedDate", null); m.put("deletedBy", null); m.put("isDelete", null);
        m.put("id", CLASSIFIER_MESTO1_ID);
        m.put("number", 4469);
        m.put("name", "Алина/Дима. sla 2 мин");
        m.put("parentId", CLASSIFIER_TYPE_ID);
        m.put("parentName", "Благодарность");
        m.put("typeNode", "Тип объекта");
        m.put("needAct", false); m.put("needCheck", false);
        m.put("needCheckList", false); m.put("maybeWait", true);
        m.put("slaStart", 1); m.put("slaClose", 2);
        m.put("favourId", null); m.put("keywordBot", null);
        m.put("stopEta", 1); m.put("domaAiId", null);
        m.put("standardTask", 1); m.put("andromedaIds", List.of());
        m.put("cmtReceipt", null);
        return m;
    }

    private Map<String, Object> buildObjectUk() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", OBJECT_UK_ID);
        m.put("name", OBJECT_UK_NAME);
        m.put("createdBy", "AAntipov@sbercity.ru");
        m.put("createdDate", "2024-04-08 17:38:02.231");
        m.put("lastModifiedBy", "Белашов Дмитрий Александрович");
        m.put("lastModifiedDate", "2025-12-18 14:11:04.241");
        m.put("deletedDate", null); m.put("deletedBy", null); m.put("isDelete", null);
        m.put("quarter", "B10"); m.put("frame", "1"); m.put("number", "33");
        m.put("purposeObject", "Квартира"); m.put("floor", "1"); m.put("section", "2");
        m.put("totalArea", 76.78); m.put("livingArea", 31.21); m.put("summerPremisesArea", 18);
        m.put("auxiliaryArea", null); m.put("areaWithSummerRoomsKoff", 65);
        m.put("areaWithSummerRoomsNoKoff", null); m.put("areaWithoutBalcony", null);
        m.put("ceilingHeight", 4.5); m.put("location", "тестовй");
        m.put("typeEquipment", null); m.put("purposeEquipment", null);
        m.put("externalSystem", null); m.put("externalId", null); m.put("isRentedCar", null);
        m.put("parent", "e8a0b508-d0b3-4d32-9e60-0482aca7e930");
        m.put("parentAndName", null);
        m.put("typeObjectUk", "Квартира");
        m.put("buildingAddress", "г. Москва, вн.тер.г. муниципальный округ Кунцево, кв-л 120, з/у 88");
        m.put("mailingAddress", MAILING_ADDRESS);
        m.put("purpose", null); m.put("totalAreaAllOn", null); m.put("oumArea", "м2");
        m.put("accountsReceivable", null); m.put("accountsPayable", null);
        m.put("project", null); m.put("region", "тестовый");
        m.put("customer", null); m.put("developer", null);
        m.put("generalContractor", null); m.put("serviceOrganization", null);
        m.put("buildingNumber", null); m.put("cadastralNumber", "18122025-56");
        m.put("startSettlement", "2023-12-01 00:00:00.000");
        m.put("stopSettlement", "2024-01-02 00:00:00.000");
        m.put("numberSections", null); m.put("numberFloorsInSectionEqual", null);
        m.put("numberFloors", null); m.put("numberLifts", null);
        m.put("areaAllResidentialOnQuarter", null); m.put("areaAllNonResidentialOnQuarter", null);
        m.put("areaMop", null); m.put("totalOn", null);
        m.put("numberApartments", null); m.put("numberNonResidentialRooms", null);
        m.put("numberPantries", null); m.put("numberParkingAuto", null);
        m.put("numberParkingMoto", null); m.put("energyEfficiencyClass", null);
        m.put("factRecognizingHouseEmergency", null); m.put("reasonRecognitionHouseEmergency", null);
        m.put("documentRecognitionHouseEmergency", null); m.put("landArea", null);
        m.put("areaDrivewaysPaths", null); m.put("areaPlayground", null);
        m.put("areaSportsField", null); m.put("numberBarriers", null);
        m.put("numberBollards", null); m.put("numberCamcorders", null);
        m.put("areaLawn", null); m.put("areaFlowerGarden", 0);
        m.put("numberShrubs", 0); m.put("numberTrees", 0);
        m.put("numberInlets", null); m.put("numberInletsTko", null);
        m.put("numberInletsPlastic", null); m.put("numberContainersTko", null);
        m.put("valueContainersTko", null); m.put("numberContainersMetal", null);
        m.put("valueContainersMetal", null); m.put("numberContainersGlass", null);
        m.put("valueContainersGlass", null); m.put("numberContainersPepper", null);
        m.put("valueContainersPepper", null); m.put("numberContainersTetrapak", null);
        m.put("valueContainersTetrapak", null); m.put("presenceVendingMachine", null);
        m.put("presenceElectricChargesAuto", null); m.put("numberElectricChargesAuto", null);
        m.put("comments", null); m.put("housingClass", "1");
        m.put("numberFloorsInSections", 9); m.put("numberRooms", 2);
        m.put("typeBalcony", "Крытый"); m.put("typeBathroom", "Раздельный");
        m.put("numberPerFloor", null); m.put("areaEstimatedFact", null);
        m.put("areaCoefficientMop", null); m.put("isRented", null);
        m.put("areaAdultRecreation", null); m.put("conditionalNumber", "33");
        m.put("btiHouseCode", "111111"); m.put("isTireService", null);
        m.put("totalSummerPremisesAreaQuarter", null); m.put("unitType", null);
        m.put("sectionType", null); m.put("propertyId", null); m.put("btiArea", 76.78);
        m.put("typeParking", null); m.put("driverEmployeeId", null);
        m.put("driverFIO", null); m.put("nameCalendar", null);
        return m;
    }

    private Map<String, Object> buildStatus(String id, String name) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("createdBy", "user");
        m.put("createdDate", "2023-01-09 12:28:56.767");
        m.put("lastModifiedBy", "user");
        m.put("lastModifiedDate", "2023-01-09 12:28:56.767");
        m.put("deletedDate", null); m.put("deletedBy", null); m.put("isDelete", null);
        m.put("id", id);
        m.put("name", name);
        return m;
    }

    private Map<String, Object> buildChannelTask() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("createdBy", "user"); m.put("createdDate", "2023-01-09 12:28:56.767");
        m.put("lastModifiedBy", "user"); m.put("lastModifiedDate", "2023-01-09 12:28:56.767");
        m.put("deletedDate", null); m.put("deletedBy", null); m.put("isDelete", null);
        m.put("id", CHANNEL_TASK_ID);
        m.put("name", "Телефон");
        return m;
    }

    private Map<String, Object> buildPriorityNormal() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("createdBy", "user"); m.put("createdDate", "2023-01-09 12:28:56.767");
        m.put("lastModifiedBy", "user"); m.put("lastModifiedDate", "2023-01-09 12:28:56.767");
        m.put("deletedDate", null); m.put("deletedBy", null); m.put("isDelete", null);
        m.put("color", null); m.put("backgroundColor", null); m.put("icon", null);
        m.put("id", PRIORITY_NORMAL_ID);
        m.put("name", "Обычная");
        return m;
    }

    // ── Тесты ─────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @Description("Получение справочных данных для создания заявки")
    @DisplayName("GET /input/application — справочник статусов и каналов")
    public void getApplicationInputData() {
        Response response = authRequest()
                .get(CR_API + "/input/application")
                .then()
                .statusCode(200)
                .extract().response();

        assertNotNull(response.jsonPath().getList("statusAcceptanceDtoList"),
                "Список статусов принятия не должен быть null");
    }

    @Test
    @Order(2)
    @Description("Поиск клиента для заявки по имени")
    @DisplayName("GET /autocomplete/client — автодополнение клиента")
    public void autocompleteClient() {
        Response response = authRequest()
                .queryParam("page", 0)
                .queryParam("size", 20)
                .queryParam("name", "черн")
                .get(CR_API + "/autocomplete/client")
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertNotNull(content, "Список клиентов не должен быть null");

        boolean found = content.stream()
                .anyMatch(c -> CLIENT_ID.equals(c.get("id")));
        assertTrue(found, "Клиент " + CLIENT_NAME + " должен присутствовать в результатах поиска");
    }

    @Test
    @Order(3)
    @Description("Получение корневых классификаторов событий")
    @DisplayName("GET /classifier-event/root-classifier-event")
    public void getRootClassifierEvent() {
        Response response = authRequest()
                .get(CR_API + "/classifier-event/root-classifier-event")
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> classifiers = response.jsonPath().getList("$");
        assertNotNull(classifiers, "Список классификаторов не должен быть null");
        assertFalse(classifiers.isEmpty(), "Список классификаторов не должен быть пустым");

        boolean foundType = classifiers.stream()
                .anyMatch(c -> CLASSIFIER_TYPE_ID.equals(c.get("id")));
        assertTrue(foundType, "Классификатор 'Благодарность' должен присутствовать в корневых");
    }

    @Test
    @Order(4)
    @Description("Получение дочерних классификаторов для типа 'Благодарность'")
    @DisplayName("GET /classifier-event/childs-classifier-event/{id} — дочерние элементы")
    public void getChildsClassifierEvent() {
        // Ответ — объект вида {"classifierEventType": [...], "classifierEventMesto1": [...]}
        // а не массив, поэтому getList("$") здесь неверен
        Response response = authRequest()
                .get(CR_API + "/classifier-event/childs-classifier-event/" + CLASSIFIER_TYPE_ID)
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> mesto1List = response.jsonPath().getList("classifierEventMesto1");
        assertNotNull(mesto1List, "classifierEventMesto1 не должен быть null");
        assertFalse(mesto1List.isEmpty(), "classifierEventMesto1 не должен быть пустым");

        boolean foundMesto = mesto1List.stream()
                .anyMatch(c -> CLASSIFIER_MESTO1_ID.equals(c.get("id")));
        assertTrue(foundMesto, "Классификатор 'Алина/Дима. sla 2 мин' должен быть в classifierEventMesto1");
    }

    @Test
    @Order(4)
    @Description("Запрос дочерних классификаторов для листового узла возвращает 400")
    @DisplayName("GET /classifier-event/childs-classifier-event/{leafId} — ожидаем 400")
    public void getChildsClassifierEventLeafReturns400() {
        // CLASSIFIER_MESTO1_ID — листовой узел, у него нет дочерних → сервер возвращает 400
        Response response = authRequest()
                .get(CR_API + "/classifier-event/childs-classifier-event/" + CLASSIFIER_MESTO1_ID)
                .then()
                .statusCode(400)
                .extract().response();

        String message = response.jsonPath().getString("message");
        assertEquals("Дочерних узлов классификатора нет", message,
                "Сообщение об ошибке должно соответствовать ожидаемому");
    }

    @Test
    @Order(5)
    @Description("Создание заявки по клиенту с классификатором и объектом УК")
    @DisplayName("POST /application-full — создание заявки")
    public void createApplication() {
        Map<String, Object> channelApp = new LinkedHashMap<>();
        channelApp.put("id", CHANNEL_APPLICATION_ID);
        channelApp.put("name", "Почта");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clientId", CLIENT_ID);
        body.put("clientIdName", CLIENT_NAME);
        body.put("description", DESCRIPTION);
        body.put("responsibleId", null);
        body.put("responsibleIdName", null);
        body.put("objectUk", buildObjectUk());
        body.put("classifierEventType", buildClassifierType());
        body.put("classifierEventMesto1", buildClassifierMesto1());
        body.put("channelApplication", channelApp);
        body.put("domaId", null);

        Response response = authRequest()
                .body(body)
                .post(CR_API + "/application-full")
                .then()
                .statusCode(200)
                .extract().response();

        createdApplicationId = response.jsonPath().getString("id");
        String statusName    = response.jsonPath().getString("statusApplication.name");
        Integer number       = response.jsonPath().getInt("number");

        assertNotNull(createdApplicationId, "ID заявки не должен быть null");
        assertEquals("Новое", statusName, "Начальный статус заявки должен быть 'Новое'");
        assertNotNull(number, "Номер заявки должен быть присвоен");

        System.out.println("Создана заявка: id=" + createdApplicationId + ", number=" + number);
    }

    @Test
    @Order(6)
    @Description("Получение созданной заявки по ID")
    @DisplayName("GET /application-full/{id} — проверка созданной заявки")
    public void getCreatedApplication() {
        assertNotNull(createdApplicationId, "Требуется createdApplicationId из предыдущего шага");

        Response response = authRequest()
                .get(CR_API + "/application-full/" + createdApplicationId)
                .then()
                .statusCode(200)
                .extract().response();

        String id          = response.jsonPath().getString("id");
        String classType   = response.jsonPath().getString("classifierEvent.name");
        String objectName  = response.jsonPath().getString("objectUk.name");

        assertEquals(createdApplicationId, id, "ID заявки должен совпадать");
        assertNotNull(classType, "Классификатор заявки не должен быть null");
        assertEquals(OBJECT_UK_NAME, objectName, "Объект УК заявки должен совпадать");
    }

    @Test
    @Order(7)
    @Description("Проверка прав диспетчера для текущего сотрудника")
    @DisplayName("GET /member-group-application/is-dispatcher")
    public void checkIsDispatcher() {
        authRequest()
                .queryParam("employeeId", EMPLOYEE_MAIN_ID)
                .get(CR_API + "/member-group-application/is-dispatcher")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(8)
    @Description("Создание задачи к заявке (addTask)")
    @DisplayName("POST /related-collections/applicationFull/addTask/{applicationId} — создание задачи")
    public void createTaskForApplication() {
        assertNotNull(createdApplicationId, "Требуется createdApplicationId из предыдущего шага");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dutyStatus", null);
        body.put("mailingAddress", MAILING_ADDRESS);
        body.put("name", TASK_NAME);
        body.put("clientId", CLIENT_ID);
        body.put("clientIdName", CLIENT_NAME);
        body.put("description", DESCRIPTION);
        body.put("classifierEventType", buildClassifierType());
        body.put("classifierEventMesto1", buildClassifierMesto1());
        body.put("classifierEventMesto2", null);
        body.put("classifierEventMesto3", null);
        body.put("classifierEventProblem1", null);
        body.put("classifierEventProblem2", null);
        body.put("classifierEventPriority", null);
        body.put("easyDate", null);
        body.put("planExecutionDate", null);
        body.put("easyDateStop", null);
        body.put("channelCommunication", null);
        body.put("clientPhone", CLIENT_PHONE);
        body.put("clientEmail", null);
        body.put("objectUk", buildObjectUk());
        body.put("channelTask", buildChannelTask());

        Response response = authRequest()
                .body(body)
                .post(CR_API + "/related-collections/applicationFull/addTask/" + createdApplicationId)
                .then()
                .statusCode(200)
                .extract().response();

        createdTaskId     = response.jsonPath().getString("id");
        String taskStatus = response.jsonPath().getString("statusTask.name");
        String clientId   = response.jsonPath().getString("clientId");

        assertNotNull(createdTaskId, "ID задачи не должен быть null");
        assertEquals("Новое", taskStatus, "Начальный статус задачи должен быть 'Новое'");
        assertEquals(CLIENT_ID, clientId, "Клиент задачи должен совпадать с клиентом заявки");

        System.out.println("Создана задача: id=" + createdTaskId);
    }

    @Test
    @Order(9)
    @Description("Получение созданной задачи по ID")
    @DisplayName("GET /tasks-full/{id} — проверка созданной задачи")
    public void getCreatedTask() {
        assertNotNull(createdTaskId, "Требуется createdTaskId из предыдущего шага");

        Response response = authRequest()
                .get(CR_API + "/tasks-full/" + createdTaskId)
                .then()
                .statusCode(200)
                .extract().response();

        String id         = response.jsonPath().getString("id");
        Integer number    = response.jsonPath().getInt("number");
        String statusName = response.jsonPath().getString("statusTask.name");

        assertEquals(createdTaskId, id, "ID задачи должен совпадать");
        assertNotNull(number, "Номер задачи должен быть присвоен");
        assertEquals("Новое", statusName, "Начальный статус задачи должен быть 'Новое'");
    }

    @Test
    @Order(10)
    @Description("Получение доступных статусов для задачи")
    @DisplayName("GET /tasks-full/statuses?taskId={id} — список доступных статусов")
    public void getTaskStatuses() {
        assertNotNull(createdTaskId, "Требуется createdTaskId из предыдущего шага");

        Response response = authRequest()
                .queryParam("taskId", createdTaskId)
                .get(CR_API + "/tasks-full/statuses")
                .then()
                .statusCode(200)
                .extract().response();

        assertNotNull(response.body(), "Список доступных статусов не должен быть null");
    }

    @Test
    @Order(11)
    @Description("Назначение исполнителя и перевод задачи в статус 'В работе'")
    @DisplayName("PUT /tasks-full/{id} — статус 'В работе', исполнитель Сопин")
    public void updateTaskStatusInWork() {
        assertNotNull(createdTaskId, "Требуется createdTaskId из предыдущего шага");

        // Получаем актуальное состояние задачи
        Map<String, Object> task = authRequest()
                .get(CR_API + "/tasks-full/" + createdTaskId)
                .then().statusCode(200)
                .extract().response().jsonPath().getMap("$");

        // Динамические плановые даты: старт через 1 минуту, окончание через 2
        task.put("executorId", EMPLOYEE_MAIN_ID);
        task.put("executorIdName", "Черников Дмитрий Витальевич");
        task.put("executorPhone", "+79304136740");
        task.put("planStartDate", nowPlusMinutes(1));
        task.put("planExecutionDate", nowPlusMinutes(2));
        task.put("planStartDateCal", nowPlusMinutes(15));
        task.put("planStopDateCal", nowPlusMinutes(16));
        task.put("statusTask", buildStatus(STATUS_IN_WORK_ID, "В работе"));

        Response response = authRequest()
                .body(task)
                .put(CR_API + "/tasks-full/" + createdTaskId)
                .then()
                .statusCode(200)
                .extract().response();

        String newStatus   = response.jsonPath().getString("statusTask.name");
        String executorId  = response.jsonPath().getString("executorId");

        assertEquals("В работе", newStatus, "Статус задачи должен стать 'В работе'");
        assertEquals(EMPLOYEE_MAIN_ID, executorId, "Исполнитель должен быть Черников");
    }

    @Test
    @Order(12)
    @Description("Перевод задачи в статус 'Ожидает выполнения' со сменой исполнителя")
    @DisplayName("PUT /tasks-full/{id} — статус 'Ожидает выполнения'")
    public void updateTaskStatusPending() {
        assertNotNull(createdTaskId, "Требуется createdTaskId из предыдущего шага");

        Map<String, Object> task = authRequest()
                .get(CR_API + "/tasks-full/" + createdTaskId)
                .then().statusCode(200)
                .extract().response().jsonPath().getMap("$");

        // Плановое завтра
        task.put("executorId", EMPLOYEE_MAIN_ID);
        task.put("executorIdName", CLIENT_NAME);
        task.put("executorPhone", "+79304136740");
        task.put("startDate", now());
        task.put("dependenceDate", nowPlusDays(1));
        task.put("planStartDateCal", now());
        task.put("planStopDateCal", nowPlusMinutes(1));
        task.put("statusTask", buildStatus(STATUS_PENDING_ID, "Ожидает выполнения"));

        Response response = authRequest()
                .body(task)
                .put(CR_API + "/tasks-full/" + createdTaskId)
                .then()
                .statusCode(200)
                .extract().response();

        assertEquals("Ожидает выполнения", response.jsonPath().getString("statusTask.name"),
                "Статус задачи должен стать 'Ожидает выполнения'");
    }

    @Test
    @Order(13)
    @Description("Отправка чат-сообщения о переносе срока задачи")
    @DisplayName("POST /message/chat/create — сообщение 'ПЕРЕНЕСТИ СРОК'")
    public void sendChatMessageReschedule() {
        assertNotNull(createdTaskId, "Требуется createdTaskId из предыдущего шага");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("link", "chat");
        body.put("text", "Нажата кнопка ПЕРЕНЕСТИ СРОК. автотест_" + RUN_ID);
        body.put("employeeId", EMPLOYEE_MAIN_ID);
        body.put("module", "Задача");
        body.put("objectId", createdTaskId);
        body.put("documentIds", List.of());

        Response response = authRequest()
                .body(body)
                .post(MSG_API + "/chat/create")
                .then()
                .statusCode(200)
                .extract().response();

        String messageId     = response.jsonPath().getString("id");
        String statusMessage = response.jsonPath().getString("statusMessage");
        String taskId        = response.jsonPath().getString("taskId");

        assertNotNull(messageId, "ID чат-сообщения не должен быть null");
        assertEquals("Отправлено", statusMessage, "Статус сообщения должен быть 'Отправлено'");
        assertEquals(createdTaskId, taskId, "Сообщение должно быть привязано к задаче");
    }

    @Test
    @Order(14)
    @Description("Перевод задачи в статус 'В работе' обратно (исполнитель Сопин) с новыми плановыми датами")
    @DisplayName("PUT /tasks-full/{id} — возврат в 'В работе' с перенесёнными датами")
    public void updateTaskStatusBackToInWork() {
        assertNotNull(createdTaskId, "Требуется createdTaskId из предыдущего шага");

        Map<String, Object> task = authRequest()
                .get(CR_API + "/tasks-full/" + createdTaskId)
                .then().statusCode(200)
                .extract().response().jsonPath().getMap("$");

        // Новые плановые даты — завтра
        task.put("executorId", EMPLOYEE_SECOND_ID);
        task.put("executorIdName", "Сопин Денис Алексеевич");
        task.put("executorPhone", "+79323333333");
        task.put("planStartDate", nowPlusDays(1));
        task.put("planExecutionDate", nowPlusDays(1) ); // +1 мин через строку
        task.put("planStartDateCal", nowPlusDays(1));
        task.put("planStopDateCal", nowPlusDays(1));
        task.put("statusTask", buildStatus(STATUS_IN_WORK_ID, "В работе"));

        Response response = authRequest()
                .body(task)
                .put(CR_API + "/tasks-full/" + createdTaskId)
                .then()
                .statusCode(200)
                .extract().response();

        assertEquals("В работе", response.jsonPath().getString("statusTask.name"),
                "Статус задачи должен вернуться в 'В работе'");
    }

    @Test
    @Order(15)
    @Description("Перевод задачи в финальный статус 'Выполнено'")
    @DisplayName("PUT /tasks-full/{id} — статус 'Выполнено'")
    public void completeTask() {
        assertNotNull(createdTaskId, "Требуется createdTaskId из предыдущего шага");

        Map<String, Object> task = authRequest()
                .get(CR_API + "/tasks-full/" + createdTaskId)
                .then().statusCode(200)
                .extract().response().jsonPath().getMap("$");

        task.put("startDate", now());
        task.put("statusTask", buildStatus(STATUS_DONE_ID, "Выполнено"));
        task.put("priorityTask", buildPriorityNormal());
        task.put("channelTask", buildChannelTask());

        Response response = authRequest()
                .body(task)
                .put(CR_API + "/tasks-full/" + createdTaskId)
                .then()
                .statusCode(200)
                .extract().response();

        String finalStatus = response.jsonPath().getString("statusTask.name");
        assertEquals("Выполнено", finalStatus, "Итоговый статус задачи должен быть 'Выполнено'");
    }

    @Test
    @Order(16)
    @Description("Отправка чат-сообщения о выполнении задачи")
    @DisplayName("POST /message/chat/create — сообщение 'ВЫПОЛНИТЬ'")
    public void sendChatMessageComplete() {
        assertNotNull(createdTaskId, "Требуется createdTaskId из предыдущего шага");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("link", "chat");
        body.put("text", "Нажата кнопка ВЫПОЛНИТЬ. автотест_" + RUN_ID);
        body.put("employeeId", EMPLOYEE_MAIN_ID);
        body.put("module", "Задача");
        body.put("objectId", createdTaskId);
        body.put("documentIds", List.of());

        Response response = authRequest()
                .body(body)
                .post(MSG_API + "/chat/create")
                .then()
                .statusCode(200)
                .extract().response();

        assertEquals("Отправлено", response.jsonPath().getString("statusMessage"),
                "Статус финального сообщения должен быть 'Отправлено'");
        assertEquals(createdTaskId, response.jsonPath().getString("taskId"),
                "Сообщение должно быть привязано к задаче");
    }

    @Test
    @Order(17)
    @Description("Проверка: заявка присутствует в списке связанных заявок задачи")
    @DisplayName("POST /related-collections/task/filtering/{taskId} — связанная заявка найдена")
    public void checkApplicationInTaskRelatedCollections() {
        assertNotNull(createdTaskId, "Требуется createdTaskId из предыдущего шага");
        assertNotNull(createdApplicationId, "Требуется createdApplicationId из предыдущего шага");

        // Endpoint принимает массив фильтров (пустой = без фильтров), НЕ объект с полем dtos
        // Возвращает связанные ЗАЯВКИ задачи, а не саму задачу
        Response response = authRequest()
                .queryParam("sort", "number,desc")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("showArchive", false)
                .body(List.of())
                .post(CR_API + "/related-collections/task/filtering/" + createdTaskId)
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertNotNull(content, "Список связанных заявок не должен быть null");
        assertFalse(content.isEmpty(), "Список связанных заявок не должен быть пустым");

        // В content лежат заявки — ищем createdApplicationId по полю "id"
        boolean found = content.stream()
                .anyMatch(item -> createdApplicationId.equals(item.get("id")));
        assertTrue(found,
                "Созданная заявка " + createdApplicationId +
                        " должна присутствовать в связанных заявках задачи. " +
                        "Найденные ID: " + content.stream().map(i -> (String) i.get("id")).toList());
    }

    @Test
    @Order(18)
    @Description("Проверка: задача присутствует в списке связанных задач заявки")
    @DisplayName("POST /related-collections/application/filtering/{applicationId} — связанная задача найдена")
    public void checkTaskInApplicationRelatedCollections() {
        assertNotNull(createdApplicationId, "Требуется createdApplicationId из предыдущего шага");
        assertNotNull(createdTaskId, "Требуется createdTaskId из предыдущего шага");

        // Зеркальный endpoint — связанные задачи заявки
        Response response = authRequest()
                .queryParam("sort", "number,desc")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("showArchive", false)
                .body(List.of())
                .post(CR_API + "/related-collections/application/filtering/" + createdApplicationId)
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertNotNull(content, "Список связанных задач не должен быть null");
        assertFalse(content.isEmpty(), "Список связанных задач не должен быть пустым");

        boolean found = content.stream()
                .anyMatch(item -> createdTaskId.equals(item.get("id")));
        assertTrue(found,
                "Созданная задача " + createdTaskId +
                        " должна присутствовать в связанных задачах заявки. " +
                        "Найденные ID: " + content.stream().map(i -> (String) i.get("id")).toList());
    }

    @Test
    @Order(19)
    @Description("Итоговая проверка: заявка присутствует и содержит корректные данные")
    @DisplayName("GET /application-full/{id} — финальная проверка заявки")
    public void finalCheckApplication() {
        assertNotNull(createdApplicationId, "Требуется createdApplicationId из предыдущего шага");

        Response response = authRequest()
                .get(CR_API + "/application-full/" + createdApplicationId)
                .then()
                .statusCode(200)
                .extract().response();

        String id        = response.jsonPath().getString("id");
        String clientId  = response.jsonPath().getString("clientId");
        String objectId  = response.jsonPath().getString("objectUk.id");

        assertEquals(createdApplicationId, id, "ID заявки должен совпадать");
        assertEquals(CLIENT_ID, clientId, "Клиент заявки должен совпадать");
        assertEquals(OBJECT_UK_ID, objectId, "Объект УК заявки должен совпадать");
    }
}