package vn.edu.utc.hotel_booking.modules.operation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.operation.entity.Menu;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Integer>, JpaSpecificationExecutor<Menu> {

    Optional<Menu> findByIdAndIsDeletedFalse(Integer id);

    List<Menu> findByHotelIdAndIsDeletedFalse(Short hotelId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Menu m " +
           "WHERE m.hotel.id = :hotelId AND m.masterItem.id = :masterItemId AND m.isDeleted = false")
    boolean existsActiveOverrideByHotelAndMasterItem(@Param("hotelId") Short hotelId, @Param("masterItemId") Integer masterItemId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Menu m " +
           "WHERE m.hotel.id = :hotelId AND m.isDeleted = false " +
           "AND (m.masterItem.id = :masterItemId OR LOWER(m.name) = LOWER(:masterItemName))")
    boolean existsActiveOverrideByHotelAndMasterItemOrName(@Param("hotelId") Short hotelId,
                                                          @Param("masterItemId") Integer masterItemId,
                                                          @Param("masterItemName") String masterItemName);

    @Query("SELECT DISTINCT m.masterItem.id FROM Menu m " +
           "WHERE m.hotel.id = :hotelId AND m.masterItem IS NOT NULL AND m.isDeleted = false")
    List<Integer> findOverriddenMasterItemIdsByHotelId(@Param("hotelId") Short hotelId);
}
