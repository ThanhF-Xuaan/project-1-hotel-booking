package vn.edu.utc.hotel_booking.modules.ai.advisor.adapter;

import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;

public interface RoomAdvisorCorePort {

    /**
     * Resolves location query string into specific hotelId or regionId,
     * or flags ambiguous destinations.
     */
    DestinationResolutionResult resolveDestination(String locationQuery, Integer hotelId, Short regionId);

    /**
     * Discovers public available room candidates and retrieves authoritative pricing quotes.
     */
    CoreCandidateDiscoveryResult discoverAndQuoteCandidates(
            RoomSearchCriteriaDto criteria,
            Short regionId,
            int maxCandidates
    );
}
