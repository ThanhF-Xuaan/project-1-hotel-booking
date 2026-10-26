package vn.edu.utc.hotel_booking.common.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageAccessLevel;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;
import vn.edu.utc.hotel_booking.common.storage.policy.StoragePolicy;
import vn.edu.utc.hotel_booking.common.storage.policy.StoragePolicyRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StoragePolicyRegistry — Unit Tests")
class StoragePolicyRegistryTest {

    private final StoragePolicyRegistry registry = new StoragePolicyRegistry();

    @Test
    @DisplayName("Tất cả 10 danh mục StorageCategory đều phải được cấu hình chính sách đầy đủ")
    void allCategoriesMustBeConfigured() {
        for (StorageCategory category : StorageCategory.values()) {
            StoragePolicy policy = registry.getPolicy(category);
            assertThat(policy).isNotNull();
            assertThat(policy.getCategory()).isEqualTo(category);
            assertThat(policy.getFolderPrefix()).isNotBlank();
            assertThat(policy.getAllowedMimeTypes()).isNotEmpty();
            assertThat(policy.getMaxSizeBytes()).isPositive();
        }
    }

    @Test
    @DisplayName("USER_AVATAR và GUEST_REVIEW_PHOTO bắt buộc mặc định là PRIVATE_RESTRICTED")
    void sensitiveCategoriesMustDefaultToPrivate() {
        StoragePolicy avatarPolicy = registry.getPolicy(StorageCategory.USER_AVATAR);
        assertThat(avatarPolicy.getDefaultAccessLevel()).isEqualTo(StorageAccessLevel.PRIVATE_RESTRICTED);

        StoragePolicy reviewPolicy = registry.getPolicy(StorageCategory.GUEST_REVIEW_PHOTO);
        assertThat(reviewPolicy.getDefaultAccessLevel()).isEqualTo(StorageAccessLevel.PRIVATE_RESTRICTED);

        StoragePolicy contractPolicy = registry.getPolicy(StorageCategory.SECURE_CONTRACT);
        assertThat(contractPolicy.getDefaultAccessLevel()).isEqualTo(StorageAccessLevel.PRIVATE_RESTRICTED);

        StoragePolicy identityPolicy = registry.getPolicy(StorageCategory.GUEST_IDENTITY);
        assertThat(identityPolicy.getDefaultAccessLevel()).isEqualTo(StorageAccessLevel.PRIVATE_RESTRICTED);
    }

    @Test
    @DisplayName("BRAND_ASSET và HOTEL_ROOM_PHOTO có mặc định là PUBLIC_READ")
    void publicCategoriesMustDefaultToPublic() {
        StoragePolicy brandPolicy = registry.getPolicy(StorageCategory.BRAND_ASSET);
        assertThat(brandPolicy.getDefaultAccessLevel()).isEqualTo(StorageAccessLevel.PUBLIC_READ);

        StoragePolicy roomPolicy = registry.getPolicy(StorageCategory.HOTEL_ROOM_PHOTO);
        assertThat(roomPolicy.getDefaultAccessLevel()).isEqualTo(StorageAccessLevel.PUBLIC_READ);
    }
}
