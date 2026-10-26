package vn.edu.utc.hotel_booking.common.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;
import vn.edu.utc.hotel_booking.common.storage.policy.StoragePolicyRegistry;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StorageKeyResolver — Unit Tests")
class StorageKeyResolverTest {

    private StorageKeyResolver resolver;

    @BeforeEach
    void setUp() {
        StoragePolicyRegistry registry = new StoragePolicyRegistry();
        resolver = new StorageKeyResolver(registry);
    }

    @Test
    @DisplayName("Sinh Object Key cho ảnh phòng (Public + Hotel Scope)")
    void resolveKey_forRoomPhoto_shouldBePublicAndHotelScoped() {
        String key = resolver.resolveKey(StorageCategory.HOTEL_ROOM_PHOTO, (short) 1, 101L, "phong_deluxe.jpg");

        assertThat(key).startsWith("public/hotels/1/rooms/101/");
        assertThat(key).endsWith("_phong_deluxe.jpg");
    }

    @Test
    @DisplayName("Sinh Object Key cho hợp đồng (Private + Hotel Scope)")
    void resolveKey_forContract_shouldBePrivateAndHotelScoped() {
        String key = resolver.resolveKey(StorageCategory.SECURE_CONTRACT, (short) 2, 5001L, "hop_dong_doan.pdf");

        assertThat(key).startsWith("private/hotels/2/contracts/5001/");
        assertThat(key).endsWith("_hop_dong_doan.pdf");
    }

    @Test
    @DisplayName("Sinh Object Key cho Avatar người dùng (Private + User ID Scope)")
    void resolveKey_forUserAvatar_shouldBePrivateAndUserScoped() {
        String key = resolver.resolveKey(StorageCategory.USER_AVATAR, null, 999L, "avatar_staff.png");

        assertThat(key).startsWith("private/avatars/999/");
        assertThat(key).endsWith("_avatar_staff.png");
    }

    @Test
    @DisplayName("Sinh Object Key cho Brand Assets toàn chuỗi (Public + Common Scope)")
    void resolveKey_forBrandAsset_shouldBePublicAndCommonScoped() {
        String key = resolver.resolveKey(StorageCategory.BRAND_ASSET, null, null, "logo_hotel.png");

        assertThat(key).startsWith("public/common/brand/");
        assertThat(key).endsWith("_logo_hotel.png");
    }

    @Test
    @DisplayName("Chuẩn hóa tên file: khử dấu tiếng Việt và ký tự đặc biệt")
    void sanitizeFilename_vietnameseAndSpecialChars_shouldNormalize() {
        String clean = resolver.sanitizeFilename("Hợp Đồng Khách Đoàn #2026 (Bản Ký).pdf");
        assertThat(clean).isEqualTo("hop_dong_khach_doan_2026_ban_ky.pdf");
    }

    @Test
    @DisplayName("Chuẩn hóa tên file: ngăn chặn triệt để Path Traversal")
    void sanitizeFilename_pathTraversal_shouldStripDirectoryTraversal() {
        String clean = resolver.sanitizeFilename("../../etc/shadow");
        assertThat(clean).doesNotContain("/");
        assertThat(clean).doesNotContain("..");
        assertThat(clean).isEqualTo("shadow");
    }
}
