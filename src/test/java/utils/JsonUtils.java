package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper();
    // Изменить значение поля
    public static String changeField(String json, String field, Object value) throws Exception {
        ObjectNode node = (ObjectNode) mapper.readTree(json);
        node.putPOJO(field, value);
        return mapper.writeValueAsString(node);
    }
    // Получить значение поля
    public static String getField(String json, String field) throws Exception {
        ObjectNode node = (ObjectNode) mapper.readTree(json);
        return node.get(field).asText();
    }

    // Удалить поле
    public static String removeField(String json, String field) throws Exception {
        ObjectNode node = (ObjectNode) mapper.readTree(json);
        node.remove(field);
        return mapper.writeValueAsString(node);
    }
}
