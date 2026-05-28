package com.example.domain.model;

public record ReportExportFile(
        String fileName,
        String mediaType,
        byte[] content) {
    public ReportExportFile {
        content = content == null ? new byte[0] : content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
