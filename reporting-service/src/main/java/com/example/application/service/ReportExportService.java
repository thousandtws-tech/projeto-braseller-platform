package com.example.application.service;

import com.example.application.exception.ValidationException;
import com.example.application.port.out.ReportEntryRepository;
import com.example.application.port.out.ReportExportRenderer;
import com.example.domain.model.FinancialSummary;
import com.example.domain.model.PlatformComparisonPoint;
import com.example.domain.model.ReportEntry;
import com.example.domain.model.ReportExportData;
import com.example.domain.model.ReportExportFile;
import com.example.domain.model.ReportExportFormat;
import com.example.domain.model.ReportFilter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class ReportExportService {
    private final ReportingService reportingService;
    private final ReportEntryRepository repository;
    private final Instance<ReportExportRenderer> renderers;

    @Inject
    public ReportExportService(
            ReportingService reportingService,
            ReportEntryRepository repository,
            Instance<ReportExportRenderer> renderers) {
        this.reportingService = reportingService;
        this.repository = repository;
        this.renderers = renderers;
    }

    public ReportExportFile exportMonthly(String tenantId, YearMonth month, ReportExportFormat format) {
        String safeTenantId = requireText(tenantId, "tenantId");
        YearMonth safeMonth = month == null ? YearMonth.now() : month;
        LocalDate from = safeMonth.atDay(1);
        LocalDate to = safeMonth.atEndOfMonth();
        ReportFilter filter = new ReportFilter(from, to, null, null, null, null, "platform", "ASC", null, null);
        String periodLabel = safeMonth.toString();
        return export(
                safeTenantId,
                "Relatorio mensal consolidado",
                "relatorio-mensal-" + safeTenantId + "-" + periodLabel,
                periodLabel,
                filter,
                requiredFormat(format)
        );
    }

    public ReportExportFile exportPlatform(
            String tenantId,
            String platform,
            LocalDate from,
            LocalDate to,
            ReportExportFormat format) {
        String safeTenantId = requireText(tenantId, "tenantId");
        String safePlatform = requireText(platform, "platform").toLowerCase(Locale.ROOT);
        ReportFilter filter = new ReportFilter(from, to, safePlatform, null, null, null, "sale_date", "ASC", null, null);
        return export(
                safeTenantId,
                "Relatorio por plataforma - " + safePlatform,
                "relatorio-" + safeTenantId + "-" + safePlatform + "-" + filePeriod(from, to),
                periodLabel(from, to),
                filter,
                requiredFormat(format)
        );
    }

    private ReportExportFile export(
            String tenantId,
            String title,
            String filePrefix,
            String periodLabel,
            ReportFilter filter,
            ReportExportFormat format) {
        FinancialSummary summary = reportingService.summary(tenantId, filter);
        List<PlatformComparisonPoint> platformSummaries = reportingService.platformComparison(tenantId, filter);
        List<ReportEntry> entries = repository.listForExport(tenantId, filter);
        ReportExportData data = new ReportExportData(
                tenantId,
                title,
                periodLabel,
                Instant.now(),
                summary,
                platformSummaries,
                entries
        );
        byte[] content = renderer(format).render(data);
        String fileName = sanitizeFileName(filePrefix) + "." + format.extension();
        return new ReportExportFile(fileName, format.mediaType(), content);
    }

    private ReportExportRenderer renderer(ReportExportFormat format) {
        for (ReportExportRenderer renderer : renderers) {
            if (renderer.format() == format) {
                return renderer;
            }
        }
        throw new ValidationException("export_format_not_available");
    }

    private ReportExportFormat requiredFormat(ReportExportFormat format) {
        return format == null ? ReportExportFormat.PDF : format;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
        return value.trim();
    }

    private String periodLabel(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return "todos-os-periodos";
        }
        if (from == null) {
            return "ate " + to;
        }
        if (to == null) {
            return "desde " + from;
        }
        return from + " a " + to;
    }

    private String filePeriod(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return "todos-os-periodos";
        }
        if (from == null) {
            return "ate-" + to;
        }
        if (to == null) {
            return "desde-" + from;
        }
        return from + "-ate-" + to;
    }

    private String sanitizeFileName(String value) {
        String sanitized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        return sanitized.isBlank() ? "relatorio" : sanitized;
    }
}
