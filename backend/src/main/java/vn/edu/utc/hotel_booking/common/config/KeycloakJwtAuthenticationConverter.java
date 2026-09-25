package vn.edu.utc.hotel_booking.common.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converter giải mã Claim 'realm_access.roles' và 'resource_access' từ Keycloak JWT Token
 * thành danh sách GrantedAuthority với prefix 'ROLE_' phục vụ phân quyền RBAC và @PreAuthorize.
 */
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                defaultGrantedAuthoritiesConverter.convert(jwt).stream(),
                extractResourceRoles(jwt).stream()
        ).collect(Collectors.toSet());

        return new JwtAuthenticationToken(jwt, authorities, getPrincipalClaimName(jwt));
    }

    private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
        // 1. Trích xuất Realm Roles (realm_access.roles)
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        Collection<String> realmRoles = Collections.emptyList();
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof List<?>) {
                realmRoles = ((List<?>) rolesObj).stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .toList();
            }
        }

        // 2. Chuyển đổi thành Spring Security GrantedAuthority với prefix "ROLE_"
        return realmRoles.stream()
                .map(role -> {
                    String roleName = role.toUpperCase();
                    if (!roleName.startsWith("ROLE_")) {
                        roleName = "ROLE_" + roleName;
                    }
                    return new SimpleGrantedAuthority(roleName);
                })
                .collect(Collectors.toSet());
    }

    private String getPrincipalClaimName(Jwt jwt) {
        String claimName = JwtClaimNames.SUB;
        if (jwt.hasClaim("preferred_username")) {
            claimName = "preferred_username";
        }
        return jwt.getClaimAsString(claimName);
    }
}
