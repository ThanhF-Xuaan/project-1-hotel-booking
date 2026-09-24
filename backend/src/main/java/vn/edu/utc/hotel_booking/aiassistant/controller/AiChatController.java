package vn.edu.utc.hotel_booking.aiassistant.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.hotel_booking.aiassistant.service.ChatService;
import vn.edu.utc.hotel_booking.aiassistant.tool.RoomQuoteTool.RoomQuery;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService;

@RestController
@RequestMapping("/api/v1/ai/chat")
@Tag(name = "AI assistant")
public class AiChatController {
    private final ObjectProvider<ChatService> chats;
    private final StaffAccessService access;

    public AiChatController(ObjectProvider<ChatService> chats, StaffAccessService access) {
        this.chats = chats;
        this.access = access;
    }

    @PostMapping
    @Operation(summary = "Ask the staff assistant with scoped knowledge or live quote data")
    public ApiResponse<ChatService.ChatResult> chat(@Valid @RequestBody ChatRequest request) {
        ChatService service = chats.getIfAvailable();
        if (service == null) throw new AppException(ErrorCode.AI_UNAVAILABLE);
        return ApiResponse.<ChatService.ChatResult>builder().result(service.chat(
                access.current(), request.message(), request.hotelId(), request.roomQuery())).build();
    }

    public record ChatRequest(@NotBlank @Size(max = 1500) String message,
                              Integer hotelId, @Valid RoomQuery roomQuery) {}
}
