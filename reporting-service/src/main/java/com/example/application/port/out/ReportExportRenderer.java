package com.example.application.port.out;

import com.example.domain.model.ReportExportData;
import com.example.domain.model.ReportExportFormat;

public interface ReportExportRenderer {
    ReportExportFormat format();

    byte[] render(ReportExportData data);
}
