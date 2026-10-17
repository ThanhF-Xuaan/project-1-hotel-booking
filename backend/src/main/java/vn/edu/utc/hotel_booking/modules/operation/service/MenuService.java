package vn.edu.utc.hotel_booking.modules.operation.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MenuCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MenuSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MenuUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MenuResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.Menu;

import java.util.List;

public interface MenuService {

    MenuResponse create(MenuCreateRequest request);

    MenuResponse update(Integer id, MenuUpdateRequest request);

    MenuResponse getById(Integer id);

    Menu getEntityById(Integer id);

    void deductStock(Integer menuId, int quantity);

    void restoreStock(Integer menuId, int quantity);

    PageResponse<MenuResponse> filter(MenuSearchDto searchDto);

    void deleteBatch(List<Integer> ids);
}
