package vn.edu.utc.hotel_booking.modules.finance.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.finance.entity.Payment;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    List<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByTransactionReference(String transactionReference);

    /** Tra cứu Payment theo mã đơn phía gateway (vnp_TxnRef / MoMo orderId) khi webhook gọi về */
    Optional<Payment> findByGatewayTxnId(String gatewayTxnId);

    /** Khóa bi quan chống race condition giữa IPN và Return URL gọi đồng thời */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.gatewayTxnId = :gatewayTxnId")
    Optional<Payment> findByGatewayTxnIdWithLock(@Param("gatewayTxnId") String gatewayTxnId);
}
