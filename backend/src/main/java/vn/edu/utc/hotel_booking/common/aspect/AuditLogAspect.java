package vn.edu.utc.hotel_booking.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import vn.edu.utc.hotel_booking.common.annotation.Auditable;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.util.AuditDataMasker;
import vn.edu.utc.hotel_booking.common.util.ClientInfoResolver;
import vn.edu.utc.hotel_booking.common.util.SecurityUtils;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.StaffResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.AuditLogService;

import java.lang.reflect.Method;
import java.util.*;

/**
 * AOP Aspect tự động đón bắt và ghi vết các thao tác quản trị nhạy cảm vào bảng audit_logs.
 * Tuân thủ quy chuẩn vận hành chuỗi khách sạn và pháp luật Việt Nam:
 * - Nghị định 13/2023/NĐ-CP (PDPD - Data Masking).
 * - Luật An ninh mạng 2018 (Điều 26 - IP và User-Agent).
 * - Luật Kế toán 2015 (Điều 41 - Bất biến chứng từ).
 */
@Aspect
@Component
@Slf4j
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final StaffRepository staffRepository;
    private final ObjectMapper objectMapper;

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public AuditLogAspect(AuditLogService auditLogService, StaffRepository staffRepository) {
        this(auditLogService, staffRepository, new ObjectMapper());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuditLogAspect(AuditLogService auditLogService, StaffRepository staffRepository, ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.staffRepository = staffRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    private record OldSnapshot(String json, String targetAccount) {}

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        OldSnapshot oldSnapshot = null;
        try {
            oldSnapshot = captureOldSnapshot(joinPoint, auditable);
        } catch (Exception e) {
            log.debug("Không thể capture oldValues trước khi thực thi: {}", e.getMessage());
        }

        // 1. Thực thi phương thức nghiệp vụ chính trước
        Object result = joinPoint.proceed();

        // 2. Ghi nhật ký kiểm toán với cơ chế Fail-Safe (Không bao giờ làm gián đoạn nghiệp vụ chính)
        try {
            processAuditLog(joinPoint, auditable, result, oldSnapshot);
        } catch (Exception e) {
            log.error("Lỗi khi ghi AOP Audit Log cho actionType={}, entityName={}: {}",
                    auditable.actionType(), auditable.entityName(), e.getMessage(), e);
        }

        return result;
    }

    private OldSnapshot captureOldSnapshot(ProceedingJoinPoint joinPoint, Auditable auditable) {
        if (!"Staff".equalsIgnoreCase(auditable.entityName())) {
            return null;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);

        Map<String, Object> params = new HashMap<>();
        if (paramNames != null && args != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if (i < args.length) {
                    params.put(paramNames[i], args[i]);
                }
            }
        }

        // 1. Trường hợp id đơn lẻ (UPDATE, STATUS_CHANGED, PASSWORD_RESET)
        Object idVal = params.get("id");
        if (idVal instanceof Integer id) {
            return staffRepository.findByIdWithDetails(id)
                    .map(this::buildStaffSnapshot)
                    .orElse(null);
        } else if (idVal instanceof Long longId) {
            return staffRepository.findByIdWithDetails(longId.intValue())
                    .map(this::buildStaffSnapshot)
                    .orElse(null);
        }

        // 2. Trường hợp xóa nhiều (DELETE Batch)
        Object idsVal = params.get("ids");
        if (idsVal instanceof List<?> idList && !idList.isEmpty()) {
            List<Integer> intIds = idList.stream()
                    .filter(o -> o instanceof Number)
                    .map(o -> ((Number) o).intValue())
                    .toList();
            if (!intIds.isEmpty()) {
                List<Staff> staffs = staffRepository.findAllById(intIds);
                if (!staffs.isEmpty()) {
                    return buildStaffListSnapshot(staffs);
                }
            }
        }

        return null;
    }

    private OldSnapshot buildStaffSnapshot(Staff staff) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", staff.getId());
        map.put("username", staff.getUsername());
        map.put("fullName", staff.getFullName());
        map.put("email", staff.getEmail());
        map.put("phone", staff.getPhone());
        map.put("status", staff.getStatus());
        if (staff.getRole() != null) {
            map.put("roleName", staff.getRole().getName());
        }
        if (staff.getDepartment() != null) {
            map.put("departmentName", staff.getDepartment().getName());
        }
        map.put("scopeType", staff.getScopeType());
        map.put("scopeEntityId", staff.getScopeEntityId());
        String targetAccount = staff.getFullName() + " (@" + staff.getUsername() + ")";
        map.put("targetAccount", targetAccount);

        try {
            return new OldSnapshot(objectMapper.writeValueAsString(map), targetAccount);
        } catch (Exception e) {
            return new OldSnapshot(null, targetAccount);
        }
    }

    private OldSnapshot buildStaffListSnapshot(List<Staff> staffs) {
        List<Map<String, Object>> list = new ArrayList<>();
        List<String> targetAccounts = new ArrayList<>();
        for (Staff staff : staffs) {
            Map<String, Object> sMap = new LinkedHashMap<>();
            sMap.put("id", staff.getId());
            sMap.put("username", staff.getUsername());
            sMap.put("fullName", staff.getFullName());
            sMap.put("status", staff.getStatus());
            list.add(sMap);
            targetAccounts.add(staff.getFullName() + " (@" + staff.getUsername() + ")");
        }
        String targetAccount = String.join(", ", targetAccounts);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("deletedStaffs", list);
        map.put("targetAccount", targetAccount);

        try {
            return new OldSnapshot(objectMapper.writeValueAsString(map), targetAccount);
        } catch (Exception e) {
            return new OldSnapshot(null, targetAccount);
        }
    }

    private void processAuditLog(ProceedingJoinPoint joinPoint, Auditable auditable, Object result, OldSnapshot oldSnapshot) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);

        // Chuẩn bị ngữ cảnh SpEL
        StandardEvaluationContext context = new StandardEvaluationContext();
        Map<String, Object> requestParams = new HashMap<>();

        if (paramNames != null && args != null) {
            for (int i = 0; i < paramNames.length; i++) {
                String name = paramNames[i];
                Object value = i < args.length ? args[i] : null;
                context.setVariable(name, value);

                // Loại trừ các đối tượng hạ tầng của Servlet/Spring Web không serialize được
                if (value != null
                        && !(value instanceof ServletRequest)
                        && !(value instanceof ServletResponse)
                        && !(value instanceof BindingResult)) {
                    requestParams.put(name, value);
                }
            }
        }
        context.setVariable("result", result);

        // 1. Phân giải entityId
        String entityId = resolveEntityId(auditable.entityId(), context, requestParams, result);

        // 2. Phân giải description
        String description = resolveDescription(auditable.description(), context);

        String targetAccount = oldSnapshot != null ? oldSnapshot.targetAccount() : null;

        // 3. Phân giải newValues (dữ liệu sau thay đổi / tham số gửi lên)
        Map<String, Object> payloadMap = new HashMap<>();
        if (StringUtils.hasText(description)) {
            payloadMap.put("actionDescription", description);
        }
        if (auditable.captureRequest() && !requestParams.isEmpty()) {
            if (requestParams.size() == 1) {
                payloadMap.put("request", requestParams.values().iterator().next());
            } else {
                payloadMap.put("params", requestParams);
            }
        }
        if (auditable.captureResponse() && result != null) {
            payloadMap.put("response", result);
        }

        // 4. Bóc tách Staff ID từ Keycloak UUID hoặc từ username nếu là LOGIN
        Integer staffId = resolveStaffId();
        if ("LOGIN".equals(auditable.actionType())) {
            for (Object arg : requestParams.values()) {
                if (arg instanceof vn.edu.utc.hotel_booking.modules.identity.dto.request.LoginRequest loginReq) {
                    Staff loginStaff = staffRepository.findByUsernameAndIsDeletedFalse(loginReq.getUsername()).orElse(null);
                    if (loginStaff != null) {
                        if (staffId == null) {
                            staffId = loginStaff.getId();
                        }
                        if (targetAccount == null) {
                            targetAccount = loginStaff.getFullName() + " (@" + loginStaff.getUsername() + ")";
                        }
                    }
                    break;
                }
            }
        } else if ("CREATE".equals(auditable.actionType())) {
            if (result instanceof ApiResponse<?> apiResp && apiResp.getResult() instanceof StaffResponse staffResp) {
                targetAccount = staffResp.getFullName() + " (@" + staffResp.getUsername() + ")";
            }
        }

        if (targetAccount != null) {
            payloadMap.put("targetAccount", targetAccount);
        }

        String oldValuesJson = oldSnapshot != null ? oldSnapshot.json() : null;
        String newValuesJson = AuditDataMasker.maskToJson(payloadMap, auditable.maskFields());

        // 5. Bóc tách IP và User-Agent
        ClientInfoResolver.ClientInfo clientInfo = ClientInfoResolver.getCurrentClientInfo();

        // 6. Ghi vết vào database qua AuditLogService (Propagation.REQUIRES_NEW)
        auditLogService.recordLog(
                staffId,
                auditable.actionType(),
                auditable.entityName(),
                entityId,
                oldValuesJson,
                newValuesJson,
                clientInfo.ipAddress(),
                clientInfo.userAgent()
        );
    }

    private String resolveEntityId(String entityIdExpression,
                                   StandardEvaluationContext context,
                                   Map<String, Object> requestParams,
                                   Object result) {
        if (StringUtils.hasText(entityIdExpression)) {
            try {
                Expression expression = expressionParser.parseExpression(entityIdExpression);
                Object val = expression.getValue(context);
                if (val != null) {
                    return val.toString();
                }
            } catch (Exception e) {
                log.debug("Không thể phân giải SpEL entityId '{}': {}", entityIdExpression, e.getMessage());
                return entityIdExpression;
            }
        }

        // Fallback tự động dò tìm ID từ tham số
        for (String candidate : new String[]{"id", "bookingRoomId", "bookingId", "entityId", "staffId"}) {
            if (requestParams.containsKey(candidate) && requestParams.get(candidate) != null) {
                return requestParams.get(candidate).toString();
            }
        }

        return "N/A";
    }

    private String resolveDescription(String descriptionExpression, StandardEvaluationContext context) {
        if (!StringUtils.hasText(descriptionExpression)) {
            return null;
        }

        if (descriptionExpression.contains("#") || descriptionExpression.contains("'")) {
            try {
                Expression expression = expressionParser.parseExpression(descriptionExpression);
                Object val = expression.getValue(context);
                return val != null ? val.toString() : descriptionExpression;
            } catch (Exception e) {
                log.debug("Không thể phân giải SpEL description '{}': {}", descriptionExpression, e.getMessage());
            }
        }

        return descriptionExpression;
    }

    private Integer resolveStaffId() {
        try {
            Optional<UUID> keycloakIdOpt = SecurityUtils.getCurrentUserKeycloakId();
            if (keycloakIdOpt.isPresent()) {
                Optional<Staff> staffOpt = staffRepository.findByKeycloakIdAndIsDeletedFalse(keycloakIdOpt.get());
                if (staffOpt.isPresent()) {
                    return staffOpt.get().getId();
                }
            }

            // Fallback: Tra cứu theo preferred_username trong JWT nếu keycloak_id chưa khớp
            Optional<org.springframework.security.core.Authentication> authOpt = SecurityUtils.getAuthentication();
            if (authOpt.isPresent() && authOpt.get().getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                String preferredUsername = jwt.getClaimAsString("preferred_username");
                if (StringUtils.hasText(preferredUsername)) {
                    Optional<Staff> staffOpt = staffRepository.findByUsernameAndIsDeletedFalse(preferredUsername);
                    if (staffOpt.isPresent()) {
                        return staffOpt.get().getId();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Không thể tra cứu staffId từ SecurityContext: {}", e.getMessage());
        }
        return null;
    }
}
