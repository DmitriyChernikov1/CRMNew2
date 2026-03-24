package utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalTime;


public class TimeGenerated {

    // Метод для генерации времени начала
    public static String generateTimeStart() {
        // Получаем текущую дату и время
        LocalDateTime currentDateTime = LocalDateTime.now();

        // Добавляем 10 минут к текущему времени
        LocalDateTime timePlus10Minutes = currentDateTime.plus(10, ChronoUnit.MINUTES);

        // Округляем до ближайших 30 минут
        int minutes = timePlus10Minutes.getMinute();
        int remainder = minutes % 30;
        LocalDateTime roundedTime;
        if (remainder < 15) {
            roundedTime = timePlus10Minutes.minusMinutes(remainder);
        } else {
            roundedTime = timePlus10Minutes.plusMinutes(30 - remainder);
        }

        // Форматируем дату и время в нужный формат
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        return roundedTime.format(formatter);
    }

    // Метод для генерации времени окончания
    public static String generateTimeEnd() {
        // Получаем текущую дату и время
        LocalDateTime currentDateTime = LocalDateTime.now();

        // Добавляем 60 минут к текущему времени
        LocalDateTime timePlus60Minutes = currentDateTime.plus(60, ChronoUnit.MINUTES);

        // Округляем до ближайших 30 минут
        int minutes = timePlus60Minutes.getMinute();
        int remainder = minutes % 30;
        LocalDateTime roundedTime;
        if (remainder < 15) {
            roundedTime = timePlus60Minutes.minusMinutes(remainder);
        } else {
            roundedTime = timePlus60Minutes.plusMinutes(30 - remainder);
        }

        // Форматируем дату и время в нужный формат
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        return roundedTime.format(formatter);
    }

    public static void main(String[] args) {
        // Пример использования
        String startTime = generateTimeStart();
        String endTime = generateTimeEnd();

        System.out.println("Время начала: " + startTime);
        System.out.println("Время окончания: " + endTime);
    }

    public static String planeDate(){
        LocalDate today = LocalDate.now();
        LocalTime startTime = LocalTime.of(9, 0, 0, 0);
        LocalTime endTime = LocalTime.of(23, 0, 0, 0);

// Форматируем дату и время в нужный формат
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        // Создаем строки с отформатированными датой и временем
        String startPlanDate = today.atTime(startTime).format(formatter);
        String stopPlanDate = today.atTime(endTime).format(formatter);

        // Формируем JSON-строку
        return String.format("{\"startPlanDate\":\"%s\",\"stopPlanDate\":\"%s\"}", startPlanDate, stopPlanDate);



    }
    public static String planeDateFord(){
        LocalDate today = LocalDate.now();
        LocalTime startTime = LocalTime.of(9, 0, 0, 0);
        LocalTime endTime = LocalTime.of(23, 0, 0, 0);

// Форматируем дату и время в нужный формат
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        // Создаем строки с отформатированными датой и временем
        String startPlanDate = today.atTime(startTime).format(formatter);
        String stopPlanDate = today.atTime(endTime).format(formatter);

        // Формируем JSON-строку
        return String.format("{\n" +
                "    \"startPlanDate\": \"%s\",\n" +  // Первая строка
                "    \"stopPlanDate\": \"%s\",\n" +   // Вторая строка
                "    \"objectUkId\": [\"b71a6e71-da3a-46e7-a86f-0e0f0f8ccf67\"]\n" +  // Третья строка
                "}", startPlanDate, stopPlanDate);



    }
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public static String getTodayStartTime() {
        return LocalDateTime.now()
                .withHour(9)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .format(FORMATTER);
    }
    public static String getTodayEndTime() {
        return LocalDateTime.now()
                .withHour(23)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .format(FORMATTER);
    }

    // Дополнительный метод для получения времени с кастомными часами
    public static String getTodayTime(int hour, int minute) {
        return LocalDateTime.now()
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0)
                .format(FORMATTER);
    }
    public static String localTime() {
        // Получаем текущую дату и время
        LocalDateTime currentDateTime = LocalDateTime.now();

        // Форматируем дату и время в нужный формат
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        return currentDateTime.format(formatter);
    }
}