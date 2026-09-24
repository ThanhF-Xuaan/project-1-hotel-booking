package vn.edu.utc.hotel_booking.audit.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService.StaffActor;

import java.util.UUID;

@Service
public class AuditService {
    private final JdbcTemplate jdbc;

    public AuditService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void knowledgeChange(StaffActor actor, String action, UUID documentId) {
        jdbc.update("""
                INSERT INTO audit_logs(staff_id, action_type, entity_name, entity_id, new_values)
                VALUES (?, ?, 'ai_documents', ?, jsonb_build_object('scopeType', ?::text, 'scopeEntityId', ?::integer))
                """, actor.id(), action, documentId.toString(), actor.scopeType(), actor.scopeEntityId());
    }
}
