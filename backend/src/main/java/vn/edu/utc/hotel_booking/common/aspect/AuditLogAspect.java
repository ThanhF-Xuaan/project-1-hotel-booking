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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.context.ApplicationContext applicationContext;

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

    private <T> T getBean(Class<T> beanClass) {
        if (applicationContext == null) return null;
        try {
            return applicationContext.getBean(beanClass);
        } catch (Exception ignored) {
            return null;
        }
    }

    private OldSnapshot captureOldSnapshot(ProceedingJoinPoint joinPoint, Auditable auditable) {
        String entityName = auditable.entityName();
        if (!StringUtils.hasText(entityName)) {
            return null;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);

        Map<String, Object> params = new HashMap<>();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                String name = (paramNames != null && i < paramNames.length && StringUtils.hasText(paramNames[i]))
                        ? paramNames[i]
                        : "p" + i;
                params.put(name, args[i]);
                params.put("p" + i, args[i]);
            }
        }

        Object idVal = params.get("id");
        if (idVal == null) {
            idVal = params.get("p0");
        }

        // 1. Thực thể Staff
        if ("Staff".equalsIgnoreCase(entityName)) {
            if (idVal instanceof Integer id) {
                return staffRepository.findByIdWithDetails(id)
                        .map(this::buildStaffSnapshot)
                        .orElse(null);
            } else if (idVal instanceof Long longId) {
                return staffRepository.findByIdWithDetails(longId.intValue())
                        .map(this::buildStaffSnapshot)
                        .orElse(null);
            }

            // Trường hợp xóa nhiều (DELETE Batch)
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
        }

        // 2. Thực thể Guest / BookingGuest
        if ("Guest".equalsIgnoreCase(entityName) || "BookingGuest".equalsIgnoreCase(entityName)) {
            vn.edu.utc.hotel_booking.modules.identity.repository.BookingGuestRepository guestRepo =
                    getBean(vn.edu.utc.hotel_booking.modules.identity.repository.BookingGuestRepository.class);
            if (guestRepo != null && idVal instanceof Number numId) {
                return guestRepo.findByIdAndIsDeletedFalse(numId.longValue())
                        .map(this::buildGuestSnapshot)
                        .orElse(null);
            }
        }

        // 3. Thực thể StayGuest
        if ("StayGuest".equalsIgnoreCase(entityName)) {
            vn.edu.utc.hotel_booking.modules.booking.repository.StayGuestRepository stayRepo =
                    getBean(vn.edu.utc.hotel_booking.modules.booking.repository.StayGuestRepository.class);
            if (stayRepo != null && idVal instanceof Number numId) {
                return stayRepo.findById(numId.longValue())
                        .map(this::buildStayGuestSnapshot)
                        .orElse(null);
            }
        }

        // 4. Thực thể PricingRule
        if ("PricingRule".equalsIgnoreCase(entityName) || "Pricing_Rule".equalsIgnoreCase(entityName)) {
            vn.edu.utc.hotel_booking.modules.pricing.repository.PricingRuleRepository ruleRepo =
                    getBean(vn.edu.utc.hotel_booking.modules.pricing.repository.PricingRuleRepository.class);
            if (ruleRepo != null && idVal instanceof Number numId) {
                return ruleRepo.findById(numId.intValue())
                        .map(this::buildPricingRuleSnapshot)
                        .orElse(null);
            }
        }

        return null;
    }

    private OldSnapshot buildGuestSnapshot(vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest guest) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", guest.getId());
        map.put("fullName", guest.getFullName());
        map.put("phone", guest.getPhone());
        map.put("email", guest.getEmail());
        map.put("identityNumber", guest.getIdentityNumber());
        map.put("status", guest.getStatus());
        String targetAccount = guest.getFullName() + (StringUtils.hasText(guest.getPhone()) ? " (@" + guest.getPhone() + ")" : "");
        map.put("targetAccount", targetAccount);
        map.put("targetResource", targetAccount);

        try {
            return new OldSnapshot(objectMapper.writeValueAsString(map), targetAccount);
        } catch (Exception e) {
            return new OldSnapshot(null, targetAccount);
        }
    }

    private OldSnapshot buildStayGuestSnapshot(vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest stayGuest) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", stayGuest.getId());
        map.put("fullName", stayGuest.getFullName());
        map.put("roomNumber", stayGuest.getRoomNumber());
        map.put("identityNumber", stayGuest.getIdentityNumber());
        map.put("nationality", stayGuest.getNationality());
        String targetAccount = "Khách " + stayGuest.getFullName() + (StringUtils.hasText(stayGuest.getRoomNumber()) ? " (Phòng " + stayGuest.getRoomNumber() + ")" : "");
        map.put("targetAccount", targetAccount);
        map.put("targetResource", targetAccount);

        try {
            return new OldSnapshot(objectMapper.writeValueAsString(map), targetAccount);
        } catch (Exception e) {
            return new OldSnapshot(null, targetAccount);
        }
    }

    private OldSnapshot buildPricingRuleSnapshot(vn.edu.utc.hotel_booking.modules.pricing.entity.PricingRule rule) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rule.getId());
        map.put("name", rule.getName());
        map.put("adjustmentType", rule.getAdjustmentType());
        map.put("adjustmentValue", rule.getAdjustmentValue());
        map.put("startDate", rule.getStartDate());
        map.put("endDate", rule.getEndDate());
        map.put("isActive", rule.getIsActive());
        String targetAccount = rule.getName() + " (#" + rule.getId() + ")";
        map.put("targetAccount", targetAccount);
        map.put("targetResource", targetAccount);

        try {
            return new OldSnapshot(objectMapper.writeValueAsString(map), targetAccount);
        } catch (Exception e) {
            return new OldSnapshot(null, targetAccount);
        }
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
        map.put("targetResource", targetAccount);

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
        map.put("targetResource", targetAccount);

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

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                String name = (paramNames != null && i < paramNames.length && StringUtils.hasText(paramNames[i]))
                        ? paramNames[i]
                        : "p" + i;
                Object value = args[i];
                context.setVariable(name, value);
                context.setVariable("p" + i, value);

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
            Object[] currentArgs = joinPoint.getArgs();
            if (currentArgs != null) {
                for (Object arg : currentArgs) {
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
            }
        }

        String targetResource = targetAccount;
        if ("Staff".equalsIgnoreCase(auditable.entityName()) && "CREATE".equals(auditable.actionType())) {
            if (result instanceof ApiResponse<?> apiResp && apiResp.getResult() instanceof StaffResponse staffResp) {
                targetResource = staffResp.getFullName() + " (@" + staffResp.getUsername() + ")";
                targetAccount = targetResource;
            }
        } else if ("Guest".equalsIgnoreCase(auditable.entityName()) && "GUEST_CREATE".equals(auditable.actionType())) {
            if (result instanceof ApiResponse<?> apiResp && apiResp.getResult() instanceof vn.edu.utc.hotel_booking.modules.identity.dto.response.GuestResponse guestResp) {
                targetResource = guestResp.getFullName() + (StringUtils.hasText(guestResp.getPhone()) ? " (@" + guestResp.getPhone() + ")" : "");
                targetAccount = targetResource;
                if (entityId == null || "N/A".equals(entityId)) {
                    entityId = guestResp.getId() != null ? guestResp.getId().toString() : "N/A";
                }
            }
        } else if ("StayGuest".equalsIgnoreCase(auditable.entityName()) && "STAY_GUEST_CREATE".equals(auditable.actionType())) {
            if (result instanceof ApiResponse<?> apiResp && apiResp.getResult() instanceof vn.edu.utc.hotel_booking.modules.operation.dto.response.StayGuestResponse stayResp) {
                targetResource = "Khách " + stayResp.getFullName() + (StringUtils.hasText(stayResp.getRoomNumber()) ? " (Phòng " + stayResp.getRoomNumber() + ")" : "");
                targetAccount = targetResource;
                if (entityId == null || "N/A".equals(entityId)) {
                    entityId = stayResp.getId() != null ? stayResp.getId().toString() : "N/A";
                }
            }
        } else if ("Booking".equalsIgnoreCase(auditable.entityName()) && "WALK_IN_CREATE".equals(auditable.actionType())) {
            if (result instanceof ApiResponse<?> apiResp && apiResp.getResult() instanceof vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingResponse bResp) {
                targetResource = "Đơn " + (bResp.getBookingNumber() != null ? bResp.getBookingNumber() : "#" + bResp.getId());
                targetAccount = targetResource;
                if (entityId == null || "N/A".equals(entityId)) {
                    entityId = bResp.getId() != null ? bResp.getId().toString() : "N/A";
                }
            }
        } else if ("Invoice".equalsIgnoreCase(auditable.entityName()) && "INVOICE_CREATE".equals(auditable.actionType())) {
            if (result instanceof ApiResponse<?> apiResp && apiResp.getResult() instanceof vn.edu.utc.hotel_booking.modules.finance.dto.response.InvoiceResponse invResp) {
                targetResource = "Hóa đơn " + invResp.getInvoiceNumber();
                targetAccount = targetResource;
                if (entityId == null || "N/A".equals(entityId)) {
                    entityId = invResp.getId() != null ? invResp.getId().toString() : "N/A";
                }
            }
        }

        if (targetResource != null) {
            payloadMap.put("targetResource", targetResource);
            payloadMap.put("targetAccount", targetResource);
        } else if (targetAccount != null) {
            payloadMap.put("targetResource", targetAccount);
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
