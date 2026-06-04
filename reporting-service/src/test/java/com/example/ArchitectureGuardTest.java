package com.example;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureGuardTest {
    private static final Pattern FLOATING_POINT_SQL_TYPE =
            Pattern.compile("\\b(FLOAT|REAL|DOUBLE(?:\\s+PRECISION)?)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLOATING_POINT_JAVA_TYPE =
            Pattern.compile("\\b(float|double)\\b");

    @Test
    void financialMigrationsUseDecimalOrNumericTypes() throws IOException {
        List<String> offenders = offendingLines(
                Path.of("src/main/resources/db/migration"),
                ".sql",
                FLOATING_POINT_SQL_TYPE
        );

        assertTrue(offenders.isEmpty(), () ->
                "Financial persistence must use DECIMAL or NUMERIC, never floating point types:\n"
                        + String.join("\n", offenders));
    }

    @Test
    void productionJavaCodeDoesNotUseFloatOrDouble() throws IOException {
        List<String> offenders = offendingLines(
                Path.of("src/main/java"),
                ".java",
                FLOATING_POINT_JAVA_TYPE
        );

        assertTrue(offenders.isEmpty(), () ->
                "Financial domain code must use BigDecimal, never float or double:\n"
                        + String.join("\n", offenders));
    }

    private List<String> offendingLines(Path root, String suffix, Pattern pattern) throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(suffix)).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (int index = 0; index < lines.size(); index++) {
                    String line = lines.get(index);
                    if (pattern.matcher(line).find()) {
                        offenders.add(file + ":" + (index + 1) + " " + line.strip());
                    }
                }
            }
        }
        return offenders;
    }
}
