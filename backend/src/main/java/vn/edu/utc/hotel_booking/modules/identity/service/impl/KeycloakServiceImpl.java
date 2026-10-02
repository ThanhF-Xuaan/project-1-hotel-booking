package vn.edu.utc.hotel_booking.modules.identity.service.impl;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.identity.entity.KeycloakSyncDeadLetter;
import vn.edu.utc.hotel_booking.modules.identity.repository.KeycloakSyncDeadLetterRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.KeycloakService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakServiceImpl implements KeycloakService {

    private final Keycloak keycloak;
    private final KeycloakSyncDeadLetterRepository deadLetterRepository;

    @Value("${keycloak.realm}")
    private String realm;

    private RealmResource getRealmResource() {
        return keycloak.realm(realm);
    }

    private UsersResource getUsersResource() {
        return getRealmResource().users();
    }

    @Override
    public UUID createUser(String username, String email, String firstName, String lastName, String roleCode, String password) {
        log.info("Creating user on Keycloak IAM: username={}, email={}, roleCode={}", username, email, roleCode);

        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(username);
        userRep.setEmail(email);
        userRep.setFirstName(firstName);
        userRep.setLastName(lastName);
        userRep.setEnabled(true);
        userRep.setEmailVerified(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);

        if (password != null && !password.isBlank()) {
            credential.setValue(password);
            credential.setTemporary(false);
        } else {
            String tempPassword = UUID.randomUUID().toString().substring(0, 8);
            credential.setValue(tempPassword);
            credential.setTemporary(true);
            userRep.setRequiredActions(Collections.singletonList("UPDATE_PASSWORD"));
            log.info("Generated temporary password for user: {}", username);
        }
        userRep.setCredentials(Collections.singletonList(credential));

        Response response = null;
        try {
            response = getUsersResource().create(userRep);
        } catch (Exception e) {
            log.error("Failed to connect to Keycloak while creating user: {}", username, e);
            throw new AppException(ErrorCode.KEYCLOAK_COMMUNICATION_ERROR);
        }

        if (response.getStatus() == 409) {
            log.warn("User already exists on Keycloak: username={}", username);
            throw new AppException(ErrorCode.KEYCLOAK_USER_ALREADY_EXISTS);
        }

        if (response.getStatus() != 201) {
            log.error("Failed to create user on Keycloak: username={}, httpStatus={}", username, response.getStatus());
            throw new AppException(ErrorCode.KEYCLOAK_USER_CREATION_FAILED);
        }

        String path = response.getLocation().getPath();
        String userIdStr = path.substring(path.lastIndexOf('/') + 1);
        UUID keycloakId = UUID.fromString(userIdStr);
        log.info("Successfully created user on Keycloak with ID: {}", keycloakId);

        // Assign realm role if roleCode is provided
        if (roleCode != null && !roleCode.isBlank()) {
            try {
                RoleRepresentation roleRep = getRealmResource().roles().get(roleCode).toRepresentation();
                getUsersResource().get(userIdStr).roles().realmLevel().add(Collections.singletonList(roleRep));
                log.info("Assigned role '{}' to user '{}'", roleCode, keycloakId);
            } catch (NotFoundException e) {
                log.warn("Role '{}' not found in Keycloak realm '{}', skipping role assignment", roleCode, realm);
            } catch (Exception e) {
                log.error("Failed to assign role '{}' to user '{}' on Keycloak", roleCode, keycloakId, e);
            }
        }

        return keycloakId;
    }

    @Override
    public void updateUser(UUID keycloakId, String email, String firstName, String lastName, String roleCode, String password, Boolean enabled) {
        log.info("Updating user on Keycloak: keycloakId={}, email={}", keycloakId, email);
        try {
            UserResource userResource = getUsersResource().get(keycloakId.toString());
            UserRepresentation userRep = userResource.toRepresentation();

            if (email != null && !email.isBlank()) {
                userRep.setEmail(email);
            }
            if (firstName != null && !firstName.isBlank()) {
                userRep.setFirstName(firstName);
            }
            if (lastName != null && !lastName.isBlank()) {
                userRep.setLastName(lastName);
            }
            if (enabled != null) {
                userRep.setEnabled(enabled);
            }

            userResource.update(userRep);

            // Update password if provided
            if (password != null && !password.isBlank()) {
                CredentialRepresentation cred = new CredentialRepresentation();
                cred.setType(CredentialRepresentation.PASSWORD);
                cred.setValue(password);
                cred.setTemporary(false);
                userResource.resetPassword(cred);
                log.info("Reset password for Keycloak user: {}", keycloakId);
            }

            // Sync realm role if provided
            if (roleCode != null && !roleCode.isBlank()) {
                try {
                    RoleRepresentation newRole = getRealmResource().roles().get(roleCode).toRepresentation();
                    List<RoleRepresentation> existingRoles = userResource.roles().realmLevel().listAll();
                    if (existingRoles != null && !existingRoles.isEmpty()) {
                        userResource.roles().realmLevel().remove(existingRoles);
                    }
                    userResource.roles().realmLevel().add(Collections.singletonList(newRole));
                    log.info("Updated role to '{}' for Keycloak user: {}", roleCode, keycloakId);
                } catch (NotFoundException e) {
                    log.warn("Role '{}' not found in Keycloak realm '{}', skipping role update", roleCode, realm);
                }
            }
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                log.warn("User '{}' not found on Keycloak during update, skipping", keycloakId);
                return;
            }
            log.error("WebApplicationException during Keycloak user update: keycloakId={}", keycloakId, e);
            throw new AppException(ErrorCode.KEYCLOAK_USER_UPDATE_FAILED);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update user on Keycloak: keycloakId={}", keycloakId, e);
            throw new AppException(ErrorCode.KEYCLOAK_USER_UPDATE_FAILED);
        }
    }

    @Override
    public void disableUser(UUID keycloakId) {
        log.info("Disabling user on Keycloak: keycloakId={}", keycloakId);
        try {
            UserResource userResource = getUsersResource().get(keycloakId.toString());
            UserRepresentation userRep = userResource.toRepresentation();
            userRep.setEnabled(false);
            userResource.update(userRep);
            log.info("Disabled user on Keycloak: keycloakId={}", keycloakId);
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                log.info("User '{}' not found on Keycloak (404), considered already disabled/removed", keycloakId);
                return;
            }
            log.error("Failed to disable user on Keycloak: keycloakId={}", keycloakId, e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to disable user on Keycloak: keycloakId={}", keycloakId, e);
            throw e;
        }
    }

    @Override
    public void logoutUser(UUID keycloakId) {
        log.info("Logging out all active sessions for user on Keycloak: keycloakId={}", keycloakId);
        try {
            getUsersResource().get(keycloakId.toString()).logout();
            log.info("Logged out user on Keycloak: keycloakId={}", keycloakId);
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                log.info("User '{}' not found on Keycloak (404), considered already logged out", keycloakId);
                return;
            }
            log.error("Failed to logout user on Keycloak: keycloakId={}", keycloakId, e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to logout user on Keycloak: keycloakId={}", keycloakId, e);
            throw e;
        }
    }

    @Override
    public void deleteUser(UUID keycloakId) {
        log.info("Deleting user permanently from Keycloak (Compensating action): keycloakId={}", keycloakId);
        try {
            getUsersResource().get(keycloakId.toString()).remove();
            log.info("Deleted user from Keycloak: keycloakId={}", keycloakId);
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                log.info("User '{}' not found on Keycloak (404), considered already deleted", keycloakId);
                return;
            }
            log.error("Failed to delete user on Keycloak: keycloakId={}", keycloakId, e);
            throw new AppException(ErrorCode.KEYCLOAK_USER_DELETE_FAILED);
        } catch (Exception e) {
            log.error("Failed to delete user on Keycloak: keycloakId={}", keycloakId, e);
            throw new AppException(ErrorCode.KEYCLOAK_USER_DELETE_FAILED);
        }
    }

    @Override
    @Async
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void asyncDisableAndLogoutUsers(List<UUID> keycloakIds) {
        if (keycloakIds == null || keycloakIds.isEmpty()) {
            return;
        }
        log.info("Executing async disable and logout for {} users on Keycloak", keycloakIds.size());
        for (UUID keycloakId : keycloakIds) {
            disableUser(keycloakId);
            logoutUser(keycloakId);
        }
        log.info("Completed async disable and logout for {} users on Keycloak", keycloakIds.size());
    }

    @Override
    @Recover
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recoverAsyncDisable(Exception e, List<UUID> keycloakIds) {
        log.error("CRITICAL: Async disable and logout failed after 3 attempts for userIds: {}. Storing into Dead Letter Table.", keycloakIds, e);
        if (keycloakIds == null) {
            return;
        }
        for (UUID id : keycloakIds) {
            try {
                KeycloakSyncDeadLetter deadLetter = KeycloakSyncDeadLetter.builder()
                        .keycloakId(id)
                        .action("DISABLE_AND_LOGOUT")
                        .status("PENDING")
                        .errorMessage(e != null ? e.getMessage() : "Retry limit reached")
                        .retryCount(3)
                        .build();
                deadLetterRepository.save(deadLetter);
                log.info("Saved dead letter record for keycloakId: {}", id);
            } catch (Exception dbEx) {
                log.error("CRITICAL: Failed to save dead letter record for keycloakId: {}", id, dbEx);
            }
        }
    }
}
