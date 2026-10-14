package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.modules.booking.dto.request.CancelSessionRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.InitiateDraftSessionRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.DraftSessionResponse;

public interface BookingSessionService {

    DraftSessionResponse initiateOrUpdateDraftSession(InitiateDraftSessionRequest request);

    DraftSessionResponse getDraftSession(String sessionId);

    void cancelSession(CancelSessionRequest request);
}
