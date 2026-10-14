package vn.edu.utc.hotel_booking.modules.ai.advisor.llm;

import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.CriteriaPatchDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;

public interface RoomAdvisorLlmPort {

    /**
     * Extracts search criteria patch from redacted user message.
     */
    CriteriaPatchDto extractCriteriaPatch(String message, RoomSearchCriteriaDto currentCriteria);

    /**
     * Generates a polite, natural language Vietnamese explanation of the results,
     * grounded strictly in the verified AdvisorResult facts.
     */
    String generateReply(AdvisorResultDto result, String userMessage);
}
