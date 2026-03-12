import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class BaseTest implements TestWatcher, BeforeEachCallback {

    private static final AtomicInteger totalTests = new AtomicInteger(0);
    private static final AtomicInteger passedTests = new AtomicInteger(0);
    private static final AtomicInteger failedTests = new AtomicInteger(0);
    private static final String REPORT_FILE = "unified-test-report.txt";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private long startTime;

    @Override
    public void beforeEach(ExtensionContext context) {
        startTime = System.currentTimeMillis();
        totalTests.incrementAndGet();
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        passedTests.incrementAndGet();
        logResult(context, "✅ PASSED", null);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        failedTests.incrementAndGet();
        logResult(context, "❌ FAILED", cause);
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        logResult(context, "⏸️ ABORTED", cause);
    }

    // Убираем testDisabled метод, так как он вызывает проблемы

    private void logResult(ExtensionContext context, String status, Throwable error) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(REPORT_FILE, true))) {
            String className = context.getRequiredTestClass().getSimpleName();
            String testName = context.getDisplayName();
            long duration = System.currentTimeMillis() - startTime;

            writer.println("┌─────────────────────────────────────────────────────────");
            writer.printf("│ Class: %s%n", className);
            writer.printf("│ Test: %s%n", testName);
            writer.printf("│ Status: %s%n", status);
            writer.printf("│ Time: %s%n", LocalDateTime.now().format(formatter));
            writer.printf("│ Duration: %d ms%n", duration);

            if (error != null) {
                writer.printf("│ Error: %s%n", error.getMessage());
                if (error instanceof AssertionError) {
                    String errorMessage = error.getMessage();
                    if (errorMessage != null && errorMessage.contains("expected:")) {
                        writer.printf("│ Details: %s%n", errorMessage);
                    }
                }
            }

            writer.println("└─────────────────────────────────────────────────────────");
            writer.println();

        } catch (Exception e) {
            System.err.println("Failed to write test result: " + e.getMessage());
        }
    }

    static {
        // Инициализация файла при загрузке класса
        try (PrintWriter writer = new PrintWriter(new FileWriter(REPORT_FILE))) {
            writer.println("==================================================");
            writer.println("           UNIFIED TEST EXECUTION REPORT");
            writer.println("==================================================");
            writer.printf("Test execution started: %s%n", LocalDateTime.now().format(formatter));
            writer.println();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Запись итогов при завершении JVM
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try (PrintWriter writer = new PrintWriter(new FileWriter(REPORT_FILE, true))) {
                writer.println();
                writer.println("==================================================");
                writer.println("                   TEST SUMMARY");
                writer.println("==================================================");
                writer.printf("Total Tests: %d%n", totalTests.get());
                writer.printf("Passed: %d%n", passedTests.get());
                writer.printf("Failed: %d%n", failedTests.get());
                writer.printf("Success Rate: %.2f%%%n",
                        totalTests.get() > 0 ? (double) passedTests.get() / totalTests.get() * 100 : 0);
                writer.printf("Finished: %s%n", LocalDateTime.now().format(formatter));
                writer.println("==================================================");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }
}