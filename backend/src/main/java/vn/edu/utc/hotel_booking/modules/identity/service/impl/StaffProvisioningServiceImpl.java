package vn.edu.utc.hotel_booking.modules.identity.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.utc.hotel_booking.modules.identity.entity.Role;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.RoleRepository;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.StaffProvisioningService;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffProvisioningServiceImpl implements StaffProvisioningService {

    private final StaffRepository staffRepository;
    private final RoleRepository roleRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Staff provisionOrUpdateFromJwt(String accessToken, String fallbackUsername) {
        if (!StringUtils.hasText(accessToken)) {
            return null;
        }

        try {
            JsonNode claims = extractClaims(accessToken);
            if (claims == null) {
                return null;
            }

            String subStr = claims.has("sub") ? claims.get("sub").asText() : null;
            if (!StringUtils.hasText(subStr)) {
                return null;
            }
            UUID keycloakId;
            try {
                keycloakId = UUID.fromString(subStr);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID in JWT sub claim: {}", subStr);
                return null;
            }

            String username = claims.has("preferred_username") && StringUtils.hasText(claims.get("preferred_username").asText())
                    ? claims.get("preferred_username").asText().trim()
                    : (StringUtils.hasText(fallbackUsername) ? fallbackUsername.trim() : null);

            if (!StringUtils.hasText(username)) {
                return null;
            }

            String email = claims.has("email") && !claims.get("email").isNull() ? claims.get("email").asText().trim() : null;
            String firstName = claims.has("given_name") && !claims.get("given_name").isNull() ? claims.get("given_name").asText().trim() : null;
            String lastName = claims.has("family_name") && !claims.get("family_name").isNull() ? claims.get("family_name").asText().trim() : null;
            String name = claims.has("name") && !claims.get("name").isNull() ? claims.get("name").asText().trim() : null;

            // 1. Kiểm tra xem đã có Staff theo keycloak_id chưa
            Optional<Staff> existingByKeycloakId = staffRepository.findByKeycloakIdAndIsDeletedFalse(keycloakId);
            if (existingByKeycloakId.isPresent()) {
                Staff staff = existingByKeycloakId.get();
                boolean changed = false;
                if (StringUtils.hasText(email) && !email.equalsIgnoreCase(staff.getEmail())) {
                    staff.setEmail(email.toLowerCase());
                    changed = true;
                }
                if (StringUtils.hasText(name) && !name.equals(staff.getFullName())) {
                    staff.setFullName(name);
                    changed = true;
                }
                return changed ? staffRepository.save(staff) : staff;
            }

            // 2. Kiểm tra xem đã có Staff theo username chưa (tài khoản đã tạo nhưng chưa gán keycloakId)
            Optional<Staff> existingByUsername = staffRepository.findByUsernameAndIsDeletedFalse(username);
            if (existingByUsername.isPresent()) {
                Staff staff = existingByUsername.get();
                staff.setKeycloakId(keycloakId);
                if (StringUtils.hasText(email) && !StringUtils.hasText(staff.getEmail())) {
                    staff.setEmail(email.toLowerCase());
                }
                if (StringUtils.hasText(name) && !StringUtils.hasText(staff.getFullName())) {
                    staff.setFullName(name);
                }
                log.info("Linked existing staff to Keycloak UUID: username={}, keycloakId={}", username, keycloakId);
                return staffRepository.save(staff);
            }

            // 3. JIT Provisioning nhân sự mới vào PostgreSQL
            List<String> roles = extractRoles(claims);
            Role role = resolveRole(roles, username);

            if (!StringUtils.hasText(firstName)) {
                firstName = "System";
            }
            if (!StringUtils.hasText(lastName)) {
                lastName = "Admin";
            }
            if (!StringUtils.hasText(name)) {
                name = lastName + " " + firstName;
            }

            // scopeType mặc định là CHAIN cho tài khoản quản trị hoặc tài khoản chưa xác định cơ sở
            String scopeType = "CHAIN";

            Staff newStaff = Staff.builder()
                    .keycloakId(keycloakId)
                    .username(username)
                    .email(StringUtils.hasText(email) ? email.toLowerCase() : null)
                    .firstName(firstName)
                    .lastName(lastName)
                    .fullName(name)
                    .role(role)
                    .scopeType(scopeType)
                    .scopeEntityId(null)
                    .status("ACTIVE")
                    .build();

            Staff saved = staffRepository.save(newStaff);
            log.info("JIT Provisioned staff from Keycloak login: username={}, id={}, keycloakId={}, role={}",
                    username, saved.getId(), keycloakId, role != null ? role.getCode() : "NONE");
            return saved;

        } catch (Exception e) {
            log.error("Failed to JIT provision staff from JWT: {}", e.getMessage(), e);
            return null;
        }
    }

    private JsonNode extractClaims(String accessToken) {
        String[] parts = accessToken.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("Cannot parse JWT payload: {}", e.getMessage());
            return null;
        }
    }

    private List<String> extractRoles(JsonNode claims) {
        List<String> result = new ArrayList<>();
        if (claims.has("realm_access") && claims.get("realm_access").has("roles")) {
            JsonNode rolesNode = claims.get("realm_access").get("roles");
            if (rolesNode.isArray()) {
                for (JsonNode r : rolesNode) {
                    result.add(r.asText());
                }
            }
        }
        return result;
    }

    private Role resolveRole(List<String> roles, String username) {
        for (String rCode : roles) {
            Optional<Role> roleOpt = roleRepository.findByCodeAndIsDeletedFalse(rCode);
            if (roleOpt.isPresent()) {
                return roleOpt.get();
            }
        }
        if ("admin".equalsIgnoreCase(username)) {
            return roleRepository.findByCodeAndIsDeletedFalse("CHAIN_ADMIN").orElse(null);
        }
        return roleRepository.findAll().stream().findFirst().orElse(null);
    }
}
