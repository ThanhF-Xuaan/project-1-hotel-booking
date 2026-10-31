package vn.edu.utc.hotel_booking.modules.ai.groupassignment.model;

public record RoomAssignmentCandidate(int roomInstanceId, int hotelId, long roomBlockId, int hotelRoomTypeId,
                                      int capacityPersons, int bedUnits, Integer floorNumber, boolean eligibleForEntireStay) { }
