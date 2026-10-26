package vn.edu.utc.hotel_booking.common.storage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.common.storage.domain.MediaLifecycleStatus;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;
import vn.edu.utc.hotel_booking.common.storage.entity.MediaObject;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaObjectRepository extends JpaRepository<MediaObject, Long> {

    Optional<MediaObject> findByStorageKeyAndIsDeletedFalse(String storageKey);

    Optional<MediaObject> findByIdAndIsDeletedFalse(Long id);

    List<MediaObject> findByCategoryAndReferenceIdAndIsDeletedFalse(StorageCategory category, Long referenceId);

    List<MediaObject> findByHotelIdAndCategoryAndIsDeletedFalse(Short hotelId, StorageCategory category);

    List<MediaObject> findByLifecycleStatusAndIsDeletedFalse(MediaLifecycleStatus lifecycleStatus);
}
