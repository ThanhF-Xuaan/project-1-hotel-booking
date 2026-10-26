package vn.edu.utc.hotel_booking.modules.identity.personalization.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.util.StringUtils;

public class PersonalizationConsentUpdateRequest {

    @NotNull
    @Min(1)
    @Max(32767)
    private Short hotelId;

    @NotNull
    private Boolean granted;

    @NotNull
    @PositiveOrZero
    private Long expectedRevision;

    @NotNull
    @Positive
    private Long expectedBindingVersion;

    @Schema(type = "string", maxLength = 100)
    private String policyVersion;

    @JsonIgnore
    private boolean policyVersionTypeValid = true;

    @JsonIgnore
    private boolean policyVersionProvided;

    @JsonIgnore
    private final Set<String> unknownProperties = new LinkedHashSet<>();

    public Short getHotelId() {
        return hotelId;
    }

    public void setHotelId(Short hotelId) {
        this.hotelId = hotelId;
    }

    public Boolean getGranted() {
        return granted;
    }

    public void setGranted(Boolean granted) {
        this.granted = granted;
    }

    public Long getExpectedRevision() {
        return expectedRevision;
    }

    public void setExpectedRevision(Long expectedRevision) {
        this.expectedRevision = expectedRevision;
    }

    public Long getExpectedBindingVersion() {
        return expectedBindingVersion;
    }

    public void setExpectedBindingVersion(Long expectedBindingVersion) {
        this.expectedBindingVersion = expectedBindingVersion;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    @JsonSetter("policyVersion")
    public void setPolicyVersion(Object policyVersion) {
        this.policyVersionProvided = true;
        if (policyVersion instanceof String value) {
            this.policyVersion = value;
        } else {
            this.policyVersionTypeValid = false;
        }
    }

    @JsonIgnore
    public boolean isPolicyVersionProvided() {
        return policyVersionProvided;
    }

    @JsonIgnore
    public boolean isPolicyVersionTypeValid() {
        return policyVersionTypeValid;
    }

    @JsonAnySetter
    public void captureUnknownProperty(String propertyName, Object ignoredValue) {
        unknownProperties.add(propertyName);
    }

    @JsonIgnore
    public Set<String> getUnknownProperties() {
        return Collections.unmodifiableSet(unknownProperties);
    }

    @AssertTrue(message = "Consent request fields are invalid")
    @JsonIgnore
    public boolean isContractValid() {
        if (!unknownProperties.isEmpty() || granted == null) {
            return false;
        }
        if (Boolean.TRUE.equals(granted)) {
            return policyVersionProvided
                    && policyVersionTypeValid
                    && StringUtils.hasText(policyVersion)
                    && policyVersion.codePointCount(0, policyVersion.length()) <= 100;
        }
        return !policyVersionProvided;
    }
}
