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

class ConnectorArchitectureGuardTest {
    private static final Pattern MARKETPLACE_SPECIFIC_NAME =
            Pattern.compile("\\b(mercado[-_ ]?livre|mercadolivre|mercadolibre|shopee|amazon|magalu)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONNECTOR_IMPLEMENTATION_IMPORT =
            Pattern.compile("infrastructure\\.connector");

    @Test
    void applicationAndDomainStayMarketplaceAgnostic() throws IOException {
        List<String> offenders = new ArrayList<>();
        offenders.addAll(offendingLines(Path.of("src/main/java/com/example/application"), MARKETPLACE_SPECIFIC_NAME));
        offenders.addAll(offendingLines(Path.of("src/main/java/com/example/domain"), MARKETPLACE_SPECIFIC_NAME));

        assertTrue(offenders.isEmpty(), () ->
                "Application/domain code must stay connector-agnostic. Put marketplace-specific logic in infrastructure connectors:\n"
                        + String.join("\n", offenders));
    }

    @Test
    void applicationAndDomainDoNotImportConnectorImplementations() throws IOException {
        List<String> offenders = new ArrayList<>();
        offenders.addAll(offendingLines(Path.of("src/main/java/com/example/application"), CONNECTOR_IMPLEMENTATION_IMPORT));
        offenders.addAll(offendingLines(Path.of("src/main/java/com/example/domain"), CONNECTOR_IMPLEMENTATION_IMPORT));

        assertTrue(offenders.isEmpty(), () ->
                "Application/domain code must depend on connector ports, not infrastructure implementations:\n"
                        + String.join("\n", offenders));
    }

    private List<String> offendingLines(Path root, Pattern pattern) throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java")).toList()) {
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
