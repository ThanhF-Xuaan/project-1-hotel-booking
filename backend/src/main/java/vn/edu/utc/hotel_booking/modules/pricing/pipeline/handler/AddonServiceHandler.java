package vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.operation.entity.Menu;
import vn.edu.utc.hotel_booking.modules.operation.repository.MenuRepository;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SelectedServiceRequest;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.AbstractPricingHandler;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.PricingContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.ServiceItemContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.ServicePricingType;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AddonServiceHandler extends AbstractPricingHandler {

    private final MenuRepository menuRepository;

    @Override
    public void handle(PricingContext context) {
        if (context.getRequest() != null) {
            List<SelectedServiceRequest> selected = context.getRequest().getSelectedServices();
            if (selected != null && !selected.isEmpty()) {
                long nights = 1;
                if (context.getRequest().getCheckInDate() != null && context.getRequest().getCheckOutDate() != null) {
                    nights = ChronoUnit.DAYS.between(context.getRequest().getCheckInDate(), context.getRequest().getCheckOutDate());
                    if (nights <= 0) {
                        nights = 1;
                    }
                }

                int guests = (context.getRequest().getAdults() != null ? context.getRequest().getAdults() : 1)
                        + (context.getRequest().getChildren() != null ? context.getRequest().getChildren() : 0);

                for (SelectedServiceRequest req : selected) {
                    // 1. Tự động truy vấn thông tin dịch vụ từ bảng menus trong Database
                    Menu menu = null;
                    if (menuRepository != null && req.getServiceId() != null) {
                        menu = menuRepository.findByIdAndIsDeletedFalse(req.getServiceId()).orElse(null);
                    }

                    String serviceName = req.getServiceName();
                    if (serviceName == null && menu != null) {
                        serviceName = menu.getName();
                    }

                    BigDecimal unitPrice = req.getUnitPrice();
                    if (unitPrice == null && menu != null) {
                        unitPrice = menu.getBasePrice();
                    }
                    if (unitPrice == null) {
                        unitPrice = BigDecimal.ZERO;
                    }

                    ServicePricingType pricingType = req.getPricingType();
                    if (pricingType == null && menu != null && menu.getHotelServiceItem() != null && menu.getHotelServiceItem().getPricingType() != null) {
                        try {
                            pricingType = ServicePricingType.valueOf(menu.getHotelServiceItem().getPricingType().name());
                        } catch (IllegalArgumentException ignored) {
                            pricingType = ServicePricingType.PER_STAY;
                        }
                    }
                    if (pricingType == null) {
                        pricingType = ServicePricingType.PER_STAY;
                    }

                    // 2. Tự động lấy taxCategoryId từ bảng menus (Backend xử lý hoàn toàn)
                    Integer taxCategoryId = 2; // Mặc định nhóm thuế Dịch vụ F&B nếu không set
                    if (menu != null && menu.getTaxCategory() != null) {
                        taxCategoryId = menu.getTaxCategory().getId();
                    }

                    // 3. Tính toán số lượng theo pricingType
                    int calculatedQty = 1;
                    switch (pricingType) {
                        case PER_STAY:
                            calculatedQty = req.getQuantity() != null && req.getQuantity() > 0 ? req.getQuantity() : 1;
                            break;
                        case PER_NIGHT:
                            calculatedQty = (int) nights * (req.getQuantity() != null && req.getQuantity() > 0 ? req.getQuantity() : 1);
                            break;
                        case PER_PERSON:
                            calculatedQty = guests * (req.getQuantity() != null && req.getQuantity() > 0 ? req.getQuantity() : 1);
                            break;
                        case PER_PERSON_PER_NIGHT:
                            calculatedQty = (int) (guests * nights);
                            break;
                    }

                    BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(calculatedQty));

                    ServiceItemContext item = ServiceItemContext.builder()
                            .serviceId(req.getServiceId())
                            .serviceName(serviceName)
                            .pricingType(pricingType)
                            .unitPrice(unitPrice)
                            .quantity(calculatedQty)
                            .subtotal(subtotal)
                            .taxCategoryId(taxCategoryId)
                            .serviceFeeRate(context.getServiceFeePercent() != null ? context.getServiceFeePercent() : new BigDecimal("5.00"))
                            .serviceFeeAmount(BigDecimal.ZERO)
                            .subtotalWithServiceFee(BigDecimal.ZERO)
                            .vatPercent(BigDecimal.ZERO)
                            .vatAmount(BigDecimal.ZERO)
                            .grossServiceTotal(BigDecimal.ZERO)
                            .build();

                    context.addServiceItem(item);
                }
            }
        }

        executeNext(context);
    }
}
