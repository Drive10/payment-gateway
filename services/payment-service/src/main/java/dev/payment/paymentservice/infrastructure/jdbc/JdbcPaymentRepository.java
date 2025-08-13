package dev.payment.paymentservice.infrastructure.jdbc;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.PaymentRepository;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPaymentRepository implements PaymentRepository {

  private static final RowMapper<Payment> ROW_MAPPER =
      (rs, n) ->
          new Payment(
              rs.getString("id"),
              rs.getString("order_id"),
              rs.getString("customer_id"),
              rs.getString("currency"),
              rs.getBigDecimal("amount"),
              rs.getString("status"));
  // Java text block must start on a new line after the opening delimiter
  private static final String UPSERT_SQL =
      """
        INSERT INTO payments (id, order_id, customer_id, currency, amount, status)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
          order_id   = EXCLUDED.order_id,
          customer_id= EXCLUDED.customer_id,
          currency   = EXCLUDED.currency,
          amount     = EXCLUDED.amount,
          status     = EXCLUDED.status
        """;
  private static final String SELECT_BY_ID_SQL =
      """
        SELECT id, order_id, customer_id, currency, amount, status
        FROM payments
        WHERE id = ?
        """;
  private final JdbcTemplate jdbc;

  public JdbcPaymentRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void save(Payment p) {
    jdbc.update(
        UPSERT_SQL, p.id(), p.orderId(), p.customerId(), p.currency(), p.amount(), p.status());
  }

  @Override
  public Optional<Payment> findById(String id) {
    return jdbc.query(SELECT_BY_ID_SQL, ROW_MAPPER, id).stream().findFirst();
  }
}
