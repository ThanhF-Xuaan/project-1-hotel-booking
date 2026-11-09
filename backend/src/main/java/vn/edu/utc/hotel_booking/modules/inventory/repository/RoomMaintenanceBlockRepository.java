package vn.edu.utc.hotel_booking.modules.inventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomMaintenanceBlock;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomMaintenanceBlockRepository extends JpaRepository<RoomMaintenanceBlock, Long> {

    List<RoomMaintenanceBlock> findByRoomInstanceId(Integer roomInstanceId);

    List<RoomMaintenanceBlock> findByRoomInstanceIdOrderByCreatedAtDesc(Integer roomInstanceId);

    boolean existsByRoomInstanceIdAndStatus(Integer roomInstanceId, String status);

    @Query("SELECT COUNT(DISTINCT rmb.roomInstance.id) FROM RoomMaintenanceBlock rmb " +
           "WHERE rmb.roomInstance.hotelRoomType.id = :hotelRoomTypeId " +
           "AND rmb.status = 'ACTIVE' AND rmb.blockType = 'OOO' " +
           "AND :date >= rmb.startDate AND :date <= rmb.endDate")
    int countActiveOooByHotelRoomTypeIdAndDate(
            @Param("hotelRoomTypeId") Integer hotelRoomTypeId,
            @Param("date") LocalDate date
    );

    @Query("SELECT b FROM RoomMaintenanceBlock b " +
           "WHERE b.roomInstance.id = :roomInstanceId " +
           "AND b.status = 'ACTIVE' " +
           "AND b.startDate <= :endDate " +
           "AND b.endDate >= :startDate")
    List<RoomMaintenanceBlock> findActiveOverlap(@Param("roomInstanceId") Integer roomInstanceId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    @Query("SELECT b FROM RoomMaintenanceBlock b " +
           "WHERE b.roomInstance.id = :roomInstanceId " +
           "AND b.id <> :excludeId " +
           "AND b.status = 'ACTIVE' " +
           "AND b.startDate <= :endDate " +
           "AND b.endDate >= :startDate")
    List<RoomMaintenanceBlock> findActiveOverlapExcluding(@Param("roomInstanceId") Integer roomInstanceId,
                                                         @Param("excludeId") Long excludeId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    List<RoomMaintenanceBlock> findByRoomInstanceIdAndStatus(Integer roomInstanceId, String status);

    @Query("SELECT b FROM RoomMaintenanceBlock b " +
           "JOIN FETCH b.roomInstance ri " +
           "JOIN FETCH ri.hotelRoomType hrt " +
           "WHERE b.status = 'ACTIVE' AND b.endDate < :today")
    List<RoomMaintenanceBlock> findActiveBlocksEndingBefore(@Param("today") LocalDate today);

    @Query(value = "SELECT b FROM RoomMaintenanceBlock b " +
           "JOIN FETCH b.roomInstance ri " +
           "JOIN FETCH ri.hotelRoomType hrt " +
           "WHERE (CAST(:roomInstanceId AS integer) IS NULL OR ri.id = :roomInstanceId) " +
           "AND (CAST(:hotelId AS short) IS NULL OR ri.hotel.id = :hotelId) " +
           "AND (CAST(:blockType AS string) IS NULL OR b.blockType = :blockType) " +
           "AND (CAST(:status AS string) IS NULL OR b.status = :status) " +
           "AND (CAST(:fromDate AS java.time.LocalDate) IS NULL OR b.endDate >= :fromDate) " +
           "AND (CAST(:toDate AS java.time.LocalDate) IS NULL OR b.startDate <= :toDate)",
           countQuery = "SELECT count(b) FROM RoomMaintenanceBlock b " +
           "WHERE (CAST(:roomInstanceId AS integer) IS NULL OR b.roomInstance.id = :roomInstanceId) " +
           "AND (CAST(:hotelId AS short) IS NULL OR b.roomInstance.hotel.id = :hotelId) " +
           "AND (CAST(:blockType AS string) IS NULL OR b.blockType = :blockType) " +
           "AND (CAST(:status AS string) IS NULL OR b.status = :status) " +
           "AND (CAST(:fromDate AS java.time.LocalDate) IS NULL OR b.endDate >= :fromDate) " +
           "AND (CAST(:toDate AS java.time.LocalDate) IS NULL OR b.startDate <= :toDate)")
    Page<RoomMaintenanceBlock> filter(@Param("roomInstanceId") Integer roomInstanceId,
                                     @Param("hotelId") Short hotelId,
                                     @Param("blockType") String blockType,
                                     @Param("status") String status,
                                     @Param("fromDate") LocalDate fromDate,
                                     @Param("toDate") LocalDate toDate,
                                     Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM RoomMaintenanceBlock b " +
           "WHERE b.roomInstance.id = :roomInstanceId " +
           "AND b.status = 'ACTIVE' " +
           "AND b.startDate <= :endDate " +
           "AND b.endDate >= :startDate")
    boolean isRoomBlockedBetween(@Param("roomInstanceId") Integer roomInstanceId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM RoomMaintenanceBlock b " +
           "WHERE b.roomInstance.id = :roomInstanceId " +
           "AND b.status = 'ACTIVE' " +
           "AND b.startDate <= :date " +
           "AND b.endDate >= :date")
    boolean isRoomBlockedOnDate(@Param("roomInstanceId") Integer roomInstanceId,
                                @Param("date") LocalDate date);
}
