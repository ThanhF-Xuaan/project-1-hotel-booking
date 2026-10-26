package vn.edu.utc.hotel_booking.modules.ai.personalization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationRecommendationResponse;
import vn.edu.utc.hotel_booking.modules.ai.personalization.error.PersonalizationRecommendationError;
import vn.edu.utc.hotel_booking.modules.ai.personalization.error.PersonalizationRecommendationException;
import vn.edu.utc.hotel_booking.modules.ai.personalization.service.PersonalizationRecommendationService;

import java.net.URL;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/me/personalization/recommendations", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(prefix = "hotel.ai.personalization", name = "local-demo-enabled", havingValue = "true")
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "U14 personalization")
public class PersonalizationRecommendationController {

    private static final String CACHE_CONTROL_VALUE = "private, no-store";
    private static final int DEFAULT_MAX_SUGGESTIONS = 3;

    private final PersonalizationRecommendationService recommendationService;

    public PersonalizationRecommendationController(PersonalizationRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    @Operation(
            summary = "Gợi ý hạng phòng không ràng buộc cho khách hàng hiện tại",
            parameters = {
                    @Parameter(name = "hotelId", in = ParameterIn.QUERY, required = true,
                            schema = @Schema(type = "integer", minimum = "1", maximum = "32767")),
                    @Parameter(name = "maxSuggestions", in = ParameterIn.QUERY, required = false,
                            schema = @Schema(type = "integer", minimum = "1", maximum = "10", defaultValue = "3"))
            }
    )
    public ResponseEntity<ApiResponse<PersonalizationRecommendationResponse>> recommend(
            HttpServletRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        QueryParameters parameters = parseQueryParameters(request.getParameterMap());
        AccountClaims account = accountClaims(principal);
        PersonalizationRecommendationResponse response = recommendationService.recommend(
                account.issuer(), account.subject(), parameters.hotelId(), parameters.maxSuggestions());
        String message = switch (response.status()) {
            case CONSENT_REQUIRED -> "Cần cấp consent để xem gợi ý.";
            case COLD_START_FALLBACK -> "Chưa có hạng phòng phù hợp để gợi ý.";
            case PERSONALIZED_READY -> "Đã tạo gợi ý hạng phòng.";
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE)
                .body(ApiResponse.success(message, response));
    }

    private QueryParameters parseQueryParameters(Map<String, String[]> parameters) {
        if (parameters == null || !parameters.containsKey("hotelId") || parameters.size() > 2
                || parameters.keySet().stream().anyMatch(key -> !key.equals("hotelId") && !key.equals("maxSuggestions"))) {
            throw invalidRequest();
        }
        int hotelId = parseSingleInteger(parameters.get("hotelId"));
        if (hotelId < 1 || hotelId > Short.MAX_VALUE) {
            throw invalidRequest();
        }
        int maxSuggestions = parameters.containsKey("maxSuggestions")
                ? parseSingleInteger(parameters.get("maxSuggestions"))
                : DEFAULT_MAX_SUGGESTIONS;
        if (maxSuggestions < 1 || maxSuggestions > 10) {
            throw invalidRequest();
        }
        return new QueryParameters(hotelId, maxSuggestions);
    }

    private int parseSingleInteger(String[] values) {
        if (values == null || values.length != 1 || !StringUtils.hasText(values[0])) {
            throw invalidRequest();
        }
        try {
            return Integer.parseInt(values[0]);
        } catch (NumberFormatException exception) {
            throw invalidRequest();
        }
    }

    private AccountClaims accountClaims(Jwt principal) {
        URL issuer = principal == null ? null : principal.getIssuer();
        String subject = principal == null ? null : principal.getSubject();
        if (issuer == null || !StringUtils.hasText(subject)) {
            throw new PersonalizationRecommendationException(PersonalizationRecommendationError.IDENTITY_LINK_REQUIRED);
        }
        return new AccountClaims(issuer.toString(), subject);
    }

    private PersonalizationRecommendationException invalidRequest() {
        return new PersonalizationRecommendationException(PersonalizationRecommendationError.INVALID_REQUEST);
    }

    private record QueryParameters(int hotelId, int maxSuggestions) {
    }

    private record AccountClaims(String issuer, String subject) {
    }
}
