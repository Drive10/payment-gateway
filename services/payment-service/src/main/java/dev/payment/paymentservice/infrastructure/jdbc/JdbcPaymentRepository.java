package dev.payment.paymentservice.infrastructure.jdbc;
import dev.payment.paymentservice.domain.*; import org.springframework.jdbc.core.*; import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository public class JdbcPaymentRepository implements PaymentRepository {
  private final JdbcTemplate jdbc; private final RowMapper<Payment> mapper = (rs,n)-> new Payment(
    rs.getString("id"), rs.getString("order_id"), rs.getString("customer_id"), rs.getString("currency"), rs.getBigDecimal("amount"), rs.getString("status"));
  public JdbcPaymentRepository(JdbcTemplate jdbc){ this.jdbc=jdbc; }
  public void save(Payment p){ jdbc.update("""INSERT INTO payments(id, order_id, customer_id, currency, amount, status)
VALUES (?,?,?,?,?,?)
ON CONFLICT (id) DO UPDATE SET status=EXCLUDED.status
""", p.id(), p.orderId(), p.customerId(), p.currency(), p.amount(), p.status()); }
  public Optional<Payment> findById(String id){ return jdbc.query("SELECT * FROM payments WHERE id = ?", mapper, id).stream().findFirst(); }
}
