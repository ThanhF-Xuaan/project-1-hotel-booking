package vn.edu.utc.hotel_booking.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hotel.storage.type", havingValue = "s3", matchIfMissing = true)
public class S3ClientConfig {

    private final StorageProperties storageProperties;

    @Bean
    public S3Client s3Client() {
        var s3Props = storageProperties.getS3();
        var credentials = AwsBasicCredentials.create(s3Props.getAccessKey(), s3Props.getSecretKey());

        var serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(s3Props.isPathStyleAccess())
                .build();

        return S3Client.builder()
                .endpointOverride(URI.create(s3Props.getEndpoint()))
                .region(Region.of(s3Props.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(serviceConfiguration)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        var s3Props = storageProperties.getS3();
        var credentials = AwsBasicCredentials.create(s3Props.getAccessKey(), s3Props.getSecretKey());

        var serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(s3Props.isPathStyleAccess())
                .build();

        String presignerEndpoint = (s3Props.getPublicEndpoint() != null && !s3Props.getPublicEndpoint().isBlank())
                ? s3Props.getPublicEndpoint()
                : s3Props.getEndpoint();

        return S3Presigner.builder()
                .endpointOverride(URI.create(presignerEndpoint))
                .region(Region.of(s3Props.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(serviceConfiguration)
                .build();
    }
}
