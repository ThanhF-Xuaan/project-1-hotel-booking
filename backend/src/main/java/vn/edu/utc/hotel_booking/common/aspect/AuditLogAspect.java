package vn.edu.utc.hotel_booking.common.aspect;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
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
import vn.edu.utc.hotel_booking.common.util.AuditDataMasker;
import vn.edu.utc.hotel_booking.common.util.ClientInfoResolver;
import vn.edu.utc.hotel_booking.common.util.SecurityUtils;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.AuditLogService;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * AOP Aspect tự động đón bắt và ghi vết các thao tác quản trị nhạy cảm vào bảng audit_logs.
 * Tuân thủ quy chuẩn vận hành chuỗi khách sạn và pháp luật Việt Nam:
 * - Nghị định 13/2023/NĐ-CP (PDPD - Data Masking).
 * - Luật An ninh mạng 2018 (Điều 26 - IP và User-Agent).
 * - Luật Kế toán 2015 (Điều 41 - Bất biến chứng từ).
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final StaffRepository staffRepository;

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        // 1. Thực thi phương thức nghiệp vụ chính trước
        Object result = joinPoint.proceed();

        // 2. Ghi nhật ký kiểm toán với cơ chế Fail-Safe (Không bao giờ làm gián đoạn nghiệp vụ chính)
        try {
            processAuditLog(joinPoint, auditable, result);
        } catch (Exception e) {
            log.error("Lỗi khi ghi AOP Audit Log cho actionType={}, entityName={}: {}",
                    auditable.actionType(), auditable.entityName(), e.getMessage(), e);
        }

        return result;
    }

    private void processAuditLog(ProceedingJoinPoint joinPoint, Auditable auditable, Object result) {
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

        // 3. Phân giải newValues (dữ liệu sau thay đổi / tham số gửi lên)
        Map<String, Object> payloadMap = new HashMap<>();
        if (StringUtils.hasText(description)) {
            payloadMap.put("actionDescription", description);
        }
        if (auditable.captureRequest() && !requestParams.isEmpty()) {
            if (requestParams.size() == 1) {
                // Nếu chỉ có 1 body request chính
                payloadMap.put("request", requestParams.values().iterator().next());
            } else {
                payloadMap.put("params", requestParams);
            }
        }
        if (auditable.captureResponse() && result != null) {
            payloadMap.put("response", result);
        }

        String newValuesJson = AuditDataMasker.maskToJson(payloadMap, auditable.maskFields());

        // 4. Bóc tách Staff ID từ Keycloak UUID
        Integer staffId = resolveStaffId();

        // 5. Bóc tách IP và User-Agent
        ClientInfoResolver.ClientInfo clientInfo = ClientInfoResolver.getCurrentClientInfo();

        // 6. Ghi vết vào database qua AuditLogService (Propagation.REQUIRES_NEW)
        auditLogService.recordLog(
                staffId,
                auditable.actionType(),
                auditable.entityName(),
                entityId,
                null, // oldValues: snapshot ban đầu (nếu có)
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
                return staffRepository.findByKeycloakIdAndIsDeletedFalse(keycloakIdOpt.get())
                        .map(Staff::getId)
                        .orElse(null);
            }
        } catch (Exception e) {
            log.debug("Không thể tra cứu staffId từ SecurityContext: {}", e.getMessage());
        }
        return null;
    }
}
