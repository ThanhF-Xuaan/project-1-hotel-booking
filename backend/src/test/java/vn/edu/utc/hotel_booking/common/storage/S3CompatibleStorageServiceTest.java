package vn.edu.utc.hotel_booking.common.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import vn.edu.utc.hotel_booking.common.config.StorageProperties;
import vn.edu.utc.hotel_booking.common.storage.spi.impl.S3CompatibleStorageService;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("S3CompatibleStorageService — Unit Tests")
class S3CompatibleStorageServiceTest {

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private StorageProperties properties;
    private S3CompatibleStorageService storageService;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        s3Presigner = mock(S3Presigner.class);

        properties = new StorageProperties();
        properties.getS3().setBucketName("test-hotel-bucket");
        properties.getS3().setEndpoint("http://localhost:9000");
        properties.getS3().setCdnPublicBaseUrl("https://cdn.hotel.vn");

        storageService = new S3CompatibleStorageService(s3Client, s3Presigner, properties);
    }

    @Test
    @DisplayName("Tải tệp lên S3 Client thành công")
    void upload_shouldCallS3ClientPutObject() {
        byte[] data = "test image bytes".getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(data);

        storageService.upload("public/rooms/room_1.jpg", bais, data.length, "image/jpeg");

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Tải byte tệp từ S3 Client thành công")
    void download_shouldReturnByteArray() {
        byte[] expected = "file content".getBytes();
        GetObjectResponse response = GetObjectResponse.builder().build();
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response, expected);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        byte[] actual = storageService.download("private/contracts/contract_1.pdf");

        assertThat(actual).isEqualTo(expected);
        verify(s3Client, times(1)).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("Xóa tệp từ S3 Client thành công")
    void delete_shouldCallS3ClientDeleteObject() {
        storageService.delete("public/rooms/old.jpg");

        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("Kiểm tra tệp tồn tại trên S3")
    void exists_whenHeadObjectSucceeds_shouldReturnTrue() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());

        boolean exists = storageService.exists("public/banners/banner_1.jpg");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Kiểm tra tệp không tồn tại trên S3")
    void exists_whenNoSuchKey_shouldReturnFalse() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

        boolean exists = storageService.exists("public/banners/not_found.jpg");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Sinh Presigned URL SigV4 có thời hạn")
    void generatePresignedUrl_shouldReturnSignedUrl() throws MalformedURLException {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://s3.amazonaws.com/test-hotel-bucket/private/contracts/c1.pdf?X-Amz-Signature=123"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String url = storageService.generatePresignedUrl("private/contracts/c1.pdf", Duration.ofMinutes(15));

        assertThat(url).contains("X-Amz-Signature=123");
    }

    @Test
    @DisplayName("Lấy CDN Public URL cho tệp công khai")
    void getPublicUrl_withCdn_shouldReturnCdnUrl() {
        String publicUrl = storageService.getPublicUrl("public/rooms/room_101.webp");

        assertThat(publicUrl).isEqualTo("https://cdn.hotel.vn/public/rooms/room_101.webp");
    }
}
