package io.github.ifenixi.supportflow.report;

import io.github.ifenixi.supportflow.identity.Actor;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
  public record StatusSummary(
      String status, long count, BigDecimal compensation, Double averageResolutionSeconds) {}

  public record Workload(long staffId, String username, long openTickets) {}

  private final JdbcTemplate jdbc;
  private final Actor actor;

  public ReportService(JdbcTemplate jdbc, Actor actor) {
    this.jdbc = jdbc;
    this.actor = actor;
  }

  public List<StatusSummary> summary() {
    actor.staff();
    return jdbc.query(
        """
        SELECT status,COUNT(*) AS total,COALESCE(SUM(compensation),0) AS amount,
        AVG(EXTRACT(EPOCH FROM (resolved_at-created_at))) AS average_seconds
        FROM ticket GROUP BY status ORDER BY status
        """,
        (r, n) ->
            new StatusSummary(
                r.getString("status"),
                r.getLong("total"),
                r.getBigDecimal("amount"),
                r.getObject("average_seconds") == null ? null : r.getDouble("average_seconds")));
  }

  public List<Workload> workload(int minimum) {
    actor.staff();
    if (minimum < 0) throw new IllegalArgumentException("minimum >= 0");
    return jdbc.query(
        """
        SELECT u.id,u.username,COUNT(t.id) AS open_count FROM app_user u
        LEFT JOIN ticket t ON t.assignee_id=u.id AND t.status IN ('NEW','IN_PROGRESS')
        WHERE u.role IN ('SUPPORT','ADMIN') GROUP BY u.id,u.username
        HAVING COUNT(t.id)>=? ORDER BY open_count DESC,u.id
        """,
        (r, n) -> new Workload(r.getLong("id"), r.getString("username"), r.getLong("open_count")),
        minimum);
  }
}
