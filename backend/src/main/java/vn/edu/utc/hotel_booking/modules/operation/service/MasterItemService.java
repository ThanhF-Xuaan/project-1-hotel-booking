package vn.edu.utc.hotel_booking.modules.operation.service;

import vn.edu.utc.hotel_booking.modules.operation.entity.MasterItem;

public interface MasterItemService {

    MasterItem getEntityById(Integer id);

    MasterItem saveEntity(MasterItem masterItem);
}
