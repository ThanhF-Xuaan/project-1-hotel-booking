package vn.edu.utc.hotel_booking.modules.identity.service;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.identity.entity.KeycloakSyncDeadLetter;
import vn.edu.utc.hotel_booking.modules.identity.repository.KeycloakSyncDeadLetterRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.impl.KeycloakServiceImpl;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakServiceTest {

    @Mock
    private Keycloak keycloak;

    @Mock
    private KeycloakSyncDeadLetterRepository deadLetterRepository;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private RolesResource rolesResource;

    @Mock
    private RoleResource roleResource;

    @Mock
    private RoleMappingResource roleMappingResource;

    @Mock
    private RoleScopeResource roleScopeResource;

    @InjectMocks
    private KeycloakServiceImpl keycloakService;

    private final String realm = "hotel-realm";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keycloakService, "realm", realm);
    }

    @Test
    @DisplayName("Create user successfully with custom password and assign role")
    void createUser_WithCustomPassword_Success() {
        UUID expectedId = UUID.randomUUID();
        when(keycloak.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        Response response = Response.status(201)
                .location(URI.create("http://localhost:8080/admin/realms/hotel-realm/users/" + expectedId))
                .build();
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get("ROLE_RECEPTIONIST")).thenReturn(roleResource);
        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName("ROLE_RECEPTIONIST");
        when(roleResource.toRepresentation()).thenReturn(roleRep);

        when(usersResource.get(expectedId.toString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

        UUID actualId = keycloakService.createUser("receptionist_01", "rec@utc.edu.vn", "Van", "A", "ROLE_RECEPTIONIST", "CustomSecret@123");

        assertThat(actualId).isEqualTo(expectedId);
        verify(usersResource, times(1)).create(any(UserRepresentation.class));
        verify(roleScopeResource, times(1)).add(anyList());
    }

    @Test
    @DisplayName("Create user with empty password generates temporary password and required action")
    void createUser_WithEmptyPassword_GeneratesTemporaryPassword() {
        UUID expectedId = UUID.randomUUID();
        when(keycloak.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        Response response = Response.status(201)
                .location(URI.create("http://localhost:8080/admin/realms/hotel-realm/users/" + expectedId))
                .build();

        ArgumentCaptor<UserRepresentation> captor = ArgumentCaptor.forClass(UserRepresentation.class);
        when(usersResource.create(captor.capture())).thenReturn(response);

        UUID actualId = keycloakService.createUser("receptionist_02", "rec2@utc.edu.vn", "Van", "B", null, "");

        assertThat(actualId).isEqualTo(expectedId);
        UserRepresentation captured = captor.getValue();
        assertThat(captured.getRequiredActions()).contains("UPDATE_PASSWORD");
        assertThat(captured.getCredentials().getFirst().isTemporary()).isTrue();
    }

    @Test
    @DisplayName("Create user throws AppException when user already exists (409)")
    void createUser_ThrowsWhenConflict() {
        when(keycloak.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        Response response = Response.status(409).build();
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

        assertThatThrownBy(() -> keycloakService.createUser("existing_user", "exist@utc.edu.vn", "Van", "C", null, "123456"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KEYCLOAK_USER_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("Disable user catches 404 gracefully without exception")
    void disableUser_Catches404Gracefully() {
        UUID keycloakId = UUID.randomUUID();
        when(keycloak.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(keycloakId.toString())).thenReturn(userResource);

        Response response404 = Response.status(404).build();
        when(userResource.toRepresentation()).thenThrow(new WebApplicationException(response404));

        // Should not throw exception
        keycloakService.disableUser(keycloakId);
    }

    @Test
    @DisplayName("Delete user catches 404 gracefully without exception")
    void deleteUser_Catches404Gracefully() {
        UUID keycloakId = UUID.randomUUID();
        when(keycloak.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(keycloakId.toString())).thenReturn(userResource);

        Response response404 = Response.status(404).build();
        doThrow(new WebApplicationException(response404)).when(userResource).remove();

        // Should not throw exception
        keycloakService.deleteUser(keycloakId);
    }

    @Test
    @DisplayName("Recover async disable saves dead letters to repository")
    void recoverAsyncDisable_SavesDeadLetters() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Exception ex = new RuntimeException("Network timeout");

        keycloakService.recoverAsyncDisable(ex, List.of(id1, id2));

        ArgumentCaptor<KeycloakSyncDeadLetter> captor = ArgumentCaptor.forClass(KeycloakSyncDeadLetter.class);
        verify(deadLetterRepository, times(2)).save(captor.capture());

        List<KeycloakSyncDeadLetter> saved = captor.getAllValues();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getKeycloakId()).isEqualTo(id1);
        assertThat(saved.get(0).getStatus()).isEqualTo("PENDING");
        assertThat(saved.get(0).getAction()).isEqualTo("DISABLE_AND_LOGOUT");
        assertThat(saved.get(1).getKeycloakId()).isEqualTo(id2);
    }
}
