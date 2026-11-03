package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.utc.hotel_booking.common.exception.*;
import vn.edu.utc.hotel_booking.modules.operation.controller.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Error-envelope boundary only; real role/scope checks run in sandbox acceptance. */
class OperationConflictHttpFollowupTest {
    @Test void stayConflictUsesFixed409WithoutDatabaseDetails() throws Exception {
        var service=mock(StayGuestService.class);when(service.update(anyLong(),any())).thenThrow(new AppException(ErrorCode.STAY_GUEST_CONFLICT));
        MockMvcBuilders.standaloneSetup(new StayGuestController(service)).setControllerAdvice(new GlobalHandlerException()).build()
                .perform(put("/api/v1/stay-guests/update/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(9062))
                .andExpect(jsonPath("$.message").value(ErrorCode.STAY_GUEST_CONFLICT.getMessage()));
    }
    @Test void utilityConflictUsesFixed409WithoutDatabaseDetails() throws Exception {
        var service=mock(UtilityReadingService.class);when(service.update(anyLong(),any())).thenThrow(new AppException(ErrorCode.UTILITY_WRITE_CONFLICT));
        MockMvcBuilders.standaloneSetup(new UtilityReadingController(service)).setControllerAdvice(new GlobalHandlerException()).build()
                .perform(put("/api/v1/utility-readings/1").contentType(MediaType.APPLICATION_JSON).content("{\"readingValue\":1}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(9054))
                .andExpect(jsonPath("$.message").value(ErrorCode.UTILITY_WRITE_CONFLICT.getMessage()));
    }
}
