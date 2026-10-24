package vn.edu.utc.hotel_booking.modules.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;

import java.time.OffsetDateTime;

@Entity
@Table(name = "contract_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContractAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    GroupContract contract;

    @Column(name = "file_name", nullable = false, length = 255)
    String fileName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    String originalFileName;

    @Column(name = "file_path", nullable = false, length = 500)
    String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 50)
    @Builder.Default
    ContractAttachmentType fileType = ContractAttachmentType.CONTRACT_MAIN;

    @Column(name = "content_type", nullable = false, length = 100)
    String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    Long fileSizeBytes;

    @Column(name = "sha256_checksum", nullable = false, length = 64)
    String sha256Checksum;

    @Column(name = "is_digitally_signed", nullable = false)
    @Builder.Default
    Boolean isDigitallySigned = false;

    @Column(name = "signature_algorithm", length = 50)
    String signatureAlgorithm;

    @Column(name = "certificate_serial", length = 100)
    String certificateSerial;

    @Column(name = "signed_by_subject", length = 255)
    String signedBySubject;

    @Column(name = "signing_timestamp")
    OffsetDateTime signingTimestamp;

    @Column(name = "version_number", nullable = false)
    @Builder.Default
    Integer versionNumber = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_staff_id", nullable = false)
    Staff uploadedByStaff;

    @Column(name = "retention_years", nullable = false)
    @Builder.Default
    Integer retentionYears = 10;
}
