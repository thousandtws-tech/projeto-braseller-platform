package com.example.infrastructure.persistence;

import com.example.application.port.out.InvoiceEntryRepository;
import com.example.domain.model.InvoiceEntry;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JdbcInvoiceEntryRepository implements InvoiceEntryRepository {

    @Inject
    AgroalDataSource dataSource;

    @Override
    public void upsert(InvoiceEntry entry) {
        String sql = """
                INSERT INTO invoice_entries
                    (id, tenant_id, platform, order_id, invoice_number, access_key, issued_at, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, platform, order_id) DO UPDATE
                SET invoice_number = EXCLUDED.invoice_number,
                    access_key     = EXCLUDED.access_key,
                    issued_at      = EXCLUDED.issued_at,
                    status         = EXCLUDED.status
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.id() != null ? entry.id() : UUID.randomUUID().toString());
            statement.setString(2, entry.tenantId());
            statement.setString(3, entry.platform());
            statement.setString(4, entry.orderId());
            statement.setString(5, entry.invoiceNumber());
            statement.setString(6, entry.accessKey());
            statement.setDate(7, entry.issuedAt() != null ? Date.valueOf(entry.issuedAt()) : null);
            statement.setString(8, entry.status());
            statement.setDate(9, Date.valueOf(entry.createdAt() != null ? entry.createdAt() : LocalDate.now()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("invoice_entry_upsert_failed", e);
        }
    }

    @Override
    public Optional<InvoiceEntry> findByOrderId(String tenantId, String platform, String orderId) {
        String sql = """
                SELECT id, tenant_id, platform, order_id, invoice_number, access_key, issued_at, status, created_at
                FROM invoice_entries
                WHERE tenant_id = ? AND platform = ? AND order_id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tenantId);
            statement.setString(2, platform);
            statement.setString(3, orderId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("invoice_entry_find_failed", e);
        }
    }

    @Override
    public List<InvoiceEntry> findByPeriod(String tenantId, LocalDate from, LocalDate to) {
        String sql = """
                SELECT id, tenant_id, platform, order_id, invoice_number, access_key, issued_at, status, created_at
                FROM invoice_entries
                WHERE tenant_id = ? AND issued_at BETWEEN ? AND ?
                ORDER BY issued_at DESC
                """;
        return query(sql, tenantId, from, to);
    }

    @Override
    public List<InvoiceEntry> findUnmatched(String tenantId, LocalDate from, LocalDate to) {
        // Invoices that exist in invoice_entries but have no corresponding report_entry (unreconciled)
        String sql = """
                SELECT ie.id, ie.tenant_id, ie.platform, ie.order_id, ie.invoice_number,
                       ie.access_key, ie.issued_at, ie.status, ie.created_at
                FROM invoice_entries ie
                LEFT JOIN report_entries re ON re.tenant_id = ie.tenant_id
                    AND re.platform = ie.platform AND re.order_id = ie.order_id
                WHERE ie.tenant_id = ? AND ie.issued_at BETWEEN ? AND ?
                  AND re.id IS NULL
                ORDER BY ie.issued_at DESC
                """;
        return query(sql, tenantId, from, to);
    }

    private List<InvoiceEntry> query(String sql, String tenantId, LocalDate from, LocalDate to) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tenantId);
            statement.setDate(2, Date.valueOf(from));
            statement.setDate(3, Date.valueOf(to));
            try (ResultSet rs = statement.executeQuery()) {
                List<InvoiceEntry> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RepositoryException("invoice_entry_query_failed", e);
        }
    }

    private InvoiceEntry map(ResultSet rs) throws SQLException {
        Date issuedAt = rs.getDate("issued_at");
        Date createdAt = rs.getDate("created_at");
        return new InvoiceEntry(
                rs.getString("id"),
                rs.getString("tenant_id"),
                rs.getString("platform"),
                rs.getString("order_id"),
                rs.getString("invoice_number"),
                rs.getString("access_key"),
                issuedAt != null ? issuedAt.toLocalDate() : null,
                rs.getString("status"),
                createdAt != null ? createdAt.toLocalDate() : null
        );
    }
}
