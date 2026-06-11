package com.example.infrastructure.persistence;

import com.example.application.port.out.BankTransactionRepository;
import com.example.domain.model.BankTransaction;
import com.example.domain.model.BankTransactionCategory;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class JdbcBankTransactionRepository implements BankTransactionRepository {

    @Inject
    AgroalDataSource dataSource;

    @Override
    public void saveAll(List<BankTransaction> transactions) {
        String sql = """
                INSERT INTO bank_transactions (id, tenant_id, fit_id, tran_type, amount, posted_date, description, category, imported_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (tenant_id, fit_id) DO NOTHING
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (BankTransaction t : transactions) {
                ps.setString(1, t.id());
                ps.setString(2, t.tenantId());
                ps.setString(3, t.fitId());
                ps.setString(4, t.tranType());
                ps.setBigDecimal(5, t.amount());
                ps.setDate(6, Date.valueOf(t.postedDate()));
                ps.setString(7, t.description());
                ps.setString(8, t.category().name());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RepositoryException("bank_transactions_save_failed", e);
        }
    }

    @Override
    public List<BankTransaction> findByPeriod(String tenantId, LocalDate from, LocalDate to) {
        String sql = "SELECT id, tenant_id, fit_id, tran_type, amount, posted_date, description, category, imported_at FROM bank_transactions WHERE tenant_id = ? AND posted_date BETWEEN ? AND ? ORDER BY posted_date DESC";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                List<BankTransaction> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RepositoryException("bank_transactions_find_failed", e);
        }
    }

    @Override
    public BigDecimal sumExpenses(String tenantId, LocalDate from, LocalDate to) {
        // Only debits (expenses) are summed; credits (receipts) are excluded
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM bank_transactions WHERE tenant_id = ? AND tran_type = 'DEBIT' AND posted_date BETWEEN ? AND ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new RepositoryException("bank_transactions_sum_failed", e);
        }
    }

    @Override
    public BigDecimal balanceAsOf(String tenantId, LocalDate asOf) {
        // Credits increase the balance, debits decrease it
        String sql = """
                SELECT COALESCE(SUM(
                    CASE
                        WHEN tran_type = 'CREDIT' THEN amount
                        WHEN tran_type = 'DEBIT' THEN -amount
                        ELSE 0
                    END
                ), 0)
                FROM bank_transactions
                WHERE tenant_id = ? AND posted_date <= ?
                """;
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setDate(2, Date.valueOf(asOf));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new RepositoryException("bank_transactions_balance_failed", e);
        }
    }

    private BankTransaction map(ResultSet rs) throws SQLException {
        Timestamp importedAt = rs.getTimestamp("imported_at");
        return new BankTransaction(
                rs.getString("id"), rs.getString("tenant_id"), rs.getString("fit_id"),
                rs.getString("tran_type"), rs.getBigDecimal("amount"),
                rs.getDate("posted_date").toLocalDate(), rs.getString("description"),
                BankTransactionCategory.valueOf(rs.getString("category")),
                importedAt != null ? importedAt.toLocalDateTime() : null);
    }
}
