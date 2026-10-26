package vn.edu.utc.hotel_booking.modules.identity.personalization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.identity.personalization.dto.PersonalizationConsentUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.personalization.dto.PersonalizationConsentView;
import vn.edu.utc.hotel_booking.modules.identity.personalization.error.PersonalizationConsentError;
import vn.edu.utc.hotel_booking.modules.identity.personalization.error.PersonalizationConsentException;
import vn.edu.utc.hotel_booking.modules.identity.personalization.service.PersonalizationConsentService;

import java.net.URL;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/me/personalization-consent", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(prefix = "hotel.ai.personalization", name = "local-demo-enabled", havingValue = "true")
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "U14 personalization")
public class PersonalizationConsentController {

    private static final String CACHE_CONTROL_VALUE = "private, no-store";

    private final PersonalizationConsentService consentService;

    public PersonalizationConsentController(PersonalizationConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping
    @Operation(
            summary = "Đọc trạng thái consent cá nhân hoá của tài khoản hiện tại",
            parameters = @Parameter(
                    name = "hotelId",
                    in = ParameterIn.QUERY,
                    required = true,
                    schema = @Schema(type = "integer", minimum = "1", maximum = "32767")
            ),
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Consent và policy hiện hành")
    )
    public ResponseEntity<ApiResponse<PersonalizationConsentView>> getCurrentConsent(
            HttpServletRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        Short hotelId = parseHotelId(request.getParameterMap());
        AccountClaims account = accountClaims(principal);
        PersonalizationConsentView view = consentService.getCurrentConsent(account.issuer(), account.subject(), hotelId);
        return success(view);
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Cấp hoặc rút consent cá nhân hoá",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Trạng thái consent sau commit")
    )
    public ResponseEntity<ApiResponse<PersonalizationConsentView>> updateConsent(
            @Valid @RequestBody PersonalizationConsentUpdateRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        AccountClaims account = accountClaims(principal);
        PersonalizationConsentView view = consentService.updateConsent(
                account.issuer(), account.subject(), request);
        return success(view);
    }

    private Short parseHotelId(Map<String, String[]> parameters) {
        if (parameters.size() != 1 || !parameters.containsKey("hotelId")) {
            throw invalidRequest();
        }
        String[] values = parameters.get("hotelId");
        if (values == null || values.length != 1 || !StringUtils.hasText(values[0])) {
            throw invalidRequest();
        }
        try {
            int parsed = Integer.parseInt(values[0]);
            if (parsed < 1 || parsed > Short.MAX_VALUE) {
                throw invalidRequest();
            }
            return (short) parsed;
        } catch (NumberFormatException exception) {
            throw invalidRequest();
        }
    }

    private AccountClaims accountClaims(Jwt principal) {
        URL issuer = principal == null ? null : principal.getIssuer();
        String subject = principal == null ? null : principal.getSubject();
        if (issuer == null || !StringUtils.hasText(subject)) {
            throw new PersonalizationConsentException(PersonalizationConsentError.IDENTITY_LINK_REQUIRED);
        }
        return new AccountClaims(issuer.toString(), subject);
    }

    private ResponseEntity<ApiResponse<PersonalizationConsentView>> success(PersonalizationConsentView view) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE)
                .body(ApiResponse.success(view));
    }

    private PersonalizationConsentException invalidRequest() {
        return new PersonalizationConsentException(PersonalizationConsentError.INVALID_REQUEST);
    }

    private record AccountClaims(String issuer, String subject) {
    }
}
