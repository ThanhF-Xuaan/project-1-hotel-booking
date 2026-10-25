package vn.edu.utc.hotel_booking.modules.ai.personalization.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.GuestPersonalizationRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.service.GuestPersonalizationService;
import vn.edu.utc.hotel_booking.modules.ai.personalization.service.PersonalizationBaselineEvaluator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class GuestPersonalizationExposureTest {

    private static final String SYNTHETIC_MODE =
            "hotel.ai.personalization.synthetic-evaluation-enabled=true";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(EvaluationConfiguration.class);

    private final GuestPersonalizationRequestDto request = GuestPersonalizationRequestDto.builder()
            .guestId(999L)
            .consentGranted(true)
            .stayHistory(List.of())
            .build();

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void defaultDeploymentDoesNotExposeController() {
        runner.run(context -> assertThat(
                context.getBeansOfType(GuestPersonalizationController.class)).isEmpty());
    }

    @Test
    void explicitFalseDoesNotExposeController() {
        runner.withPropertyValues("hotel.ai.personalization.synthetic-evaluation-enabled=false")
                .run(context -> assertThat(
                        context.getBeansOfType(GuestPersonalizationController.class)).isEmpty());
    }

    @Test
    void enabledSyntheticModeAllowsAdminOnBothEndpoints() {
        authenticate("CHAIN_ADMIN");
        runner.withPropertyValues(SYNTHETIC_MODE).run(context -> {
            GuestPersonalizationController controller = context.getBean(GuestPersonalizationController.class);
            assertThat(controller.recommend(request).getStatusCode().value()).isEqualTo(200);
            assertThat(controller.evaluateBaseline(request, 1).getStatusCode().value()).isEqualTo(200);
            verify(context.getBean(GuestPersonalizationService.class)).personalize(request);
            verify(context.getBean(PersonalizationBaselineEvaluator.class)).evaluateHoldout(request, 1);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"GUEST", "CUSTOMER", "PROPERTY_MANAGER", "RECEPTIONIST", "SALES_GROUP", "REGION_MANAGER"})
    void consentAndGuestIdCannotBypassRoleRestriction(String role) {
        authenticate(role);
        runner.withPropertyValues(SYNTHETIC_MODE).run(context -> {
            GuestPersonalizationController controller = context.getBean(GuestPersonalizationController.class);
            assertThatThrownBy(() -> controller.recommend(request)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> controller.evaluateBaseline(request, 1)).isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(context.getBean(GuestPersonalizationService.class),
                    context.getBean(PersonalizationBaselineEvaluator.class));
        });
    }

    @Test
    void anonymousCannotInvokeEitherEndpoint() {
        SecurityContextHolder.clearContext();
        runner.withPropertyValues(SYNTHETIC_MODE).run(context -> {
            GuestPersonalizationController controller = context.getBean(GuestPersonalizationController.class);
            assertThatThrownBy(() -> controller.recommend(request))
                    .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
            assertThatThrownBy(() -> controller.evaluateBaseline(request, 1))
                    .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
            verifyNoInteractions(context.getBean(GuestPersonalizationService.class),
                    context.getBean(PersonalizationBaselineEvaluator.class));
        });
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("synthetic-user", null,
                        AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    @Import(GuestPersonalizationController.class)
    static class EvaluationConfiguration {
        @Bean
        GuestPersonalizationService personalizationService() {
            return mock(GuestPersonalizationService.class);
        }

        @Bean
        PersonalizationBaselineEvaluator baselineEvaluator() {
            return mock(PersonalizationBaselineEvaluator.class);
        }
    }
}
