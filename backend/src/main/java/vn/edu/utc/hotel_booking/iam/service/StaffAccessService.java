package vn.edu.utc.hotel_booking.iam.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class StaffAccessService {
    private final JdbcTemplate jdbc;

    public StaffAccessService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public StaffActor current() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        UUID keycloakId;
        try {
            keycloakId = UUID.fromString(token.getToken().getSubject());
        } catch (RuntimeException exception) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        var actors = jdbc.query("""
                SELECT s.id, s.scope_type, s.scope_entity_id, r.code AS role_code
                FROM staffs s JOIN roles r ON r.id = s.role_id
                WHERE s.keycloak_id = ? AND s.status = 'ACTIVE' AND s.is_deleted = false
                  AND r.status = 'ACTIVE' AND r.is_deleted = false
                """, (rs, row) -> new StaffActor(rs.getInt("id"), rs.getString("scope_type"),
                (Integer) rs.getObject("scope_entity_id"), rs.getString("role_code"), Set.of()), keycloakId);
        if (actors.size() != 1) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        StaffActor actor = actors.getFirst();
        Set<String> permissions = new HashSet<>(jdbc.query("""
                SELECT p.action || ':' || p.resource AS permission
                FROM staffs s JOIN role_permissions rp ON rp.role_id = s.role_id
                JOIN permissions p ON p.id = rp.permission_id
                WHERE s.id = ? AND p.status = 'ACTIVE' AND p.is_deleted = false
                """, (rs, row) -> rs.getString("permission"), actor.id()));
        return new StaffActor(actor.id(), actor.scopeType(), actor.scopeEntityId(), actor.roleCode(), Set.copyOf(permissions));
    }

    public void require(StaffActor actor, String permission) {
        if (!actor.permissions().contains(permission)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    public void requireHotel(StaffActor actor, String permission, int hotelId, int regionId) {
        require(actor, permission);
        boolean allowed = switch (actor.scopeType()) {
            case "CHAIN" -> true;
            case "REGION" -> actor.scopeEntityId() != null && actor.scopeEntityId() == regionId;
            case "PROPERTY" -> actor.scopeEntityId() != null && actor.scopeEntityId() == hotelId;
            default -> false;
        };
        if (!allowed) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    public record StaffActor(int id, String scopeType, Integer scopeEntityId, String roleCode, Set<String> permissions) {}
}
