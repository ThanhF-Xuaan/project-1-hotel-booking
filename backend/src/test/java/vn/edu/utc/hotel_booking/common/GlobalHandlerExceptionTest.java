package vn.edu.utc.hotel_booking.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.exception.GlobalHandlerException;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalHandlerExceptionTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalHandlerException()).build();

    @Test
    void unsupportedContentTypeKeeps415() throws Exception {
        mvc.perform(post("/test").contentType(MediaType.TEXT_PLAIN).content("invalid"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode()));
    }

    @Test
    void unacceptableRepresentationKeeps406() throws Exception {
        mvc.perform(get("/test").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void unexpectedFailureStillReturns500WithoutInternalDetails() throws Exception {
        mvc.perform(get("/test/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage()));
    }

    @RestController
    static class TestController {
        @PostMapping(value = "/test", consumes = MediaType.APPLICATION_JSON_VALUE)
        Map<String, String> post(@RequestBody Map<String, String> input) { return input; }

        @GetMapping(value = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, String> get() { return Map.of("status", "ok"); }

        @GetMapping("/test/failure")
        void failure() { throw new IllegalStateException("internal diagnostic detail"); }
    }
}
