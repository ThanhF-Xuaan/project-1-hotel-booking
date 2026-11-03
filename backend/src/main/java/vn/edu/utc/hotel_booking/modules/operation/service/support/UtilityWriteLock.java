package vn.edu.utc.hotel_booking.modules.operation.service.support;

import jakarta.persistence.EntityManager;
import vn.edu.utc.hotel_booking.common.exception.*;
import vn.edu.utc.hotel_booking.modules.operation.entity.UtilityMeter;
import vn.edu.utc.hotel_booking.modules.operation.repository.UtilityMeterRepository;

/** All utility writers use the same parent-row lock before reading adjacent values. */
public final class UtilityWriteLock {
    private UtilityWriteLock() { }
    public static UtilityMeter acquire(EntityManager em, UtilityMeterRepository repository, Integer id) {
        try {
            em.createNativeQuery("SELECT set_config('lock_timeout', '5s', true)").getSingleResult();
            UtilityMeter meter=repository.findByIdForUpdate(id)
                    .orElseThrow(()->new AppException(ErrorCode.UTILITY_METER_NOT_FOUND));
            em.refresh(meter);
            if (Boolean.TRUE.equals(meter.getIsDeleted())) throw new AppException(ErrorCode.UTILITY_METER_NOT_FOUND);
            return meter;
        } catch (RuntimeException error) {
            for (Throwable cause=error; cause!=null; cause=cause.getCause()) {
                if (cause instanceof org.springframework.dao.PessimisticLockingFailureException
                        || cause instanceof jakarta.persistence.PessimisticLockException
                        || cause instanceof jakarta.persistence.LockTimeoutException
                        || cause instanceof java.sql.SQLException sql &&
                        ("55P03".equals(sql.getSQLState()) || "40P01".equals(sql.getSQLState()) || "40001".equals(sql.getSQLState()))) {
                    throw new AppException(ErrorCode.UTILITY_WRITE_CONFLICT);
                }
            }
            throw error;
        }
    }
}
