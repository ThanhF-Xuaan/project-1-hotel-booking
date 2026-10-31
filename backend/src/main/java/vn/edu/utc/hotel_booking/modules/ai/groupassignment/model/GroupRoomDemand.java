package vn.edu.utc.hotel_booking.modules.ai.groupassignment.model;

/** Opaque synthetic demand, not a guest record. */
public record GroupRoomDemand(String demandKey, int persons, int requiredBedUnits) { }
