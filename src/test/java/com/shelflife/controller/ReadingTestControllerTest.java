package com.shelflife.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shelflife.dto.ReadingTestCompletionRequest;
import com.shelflife.dto.ReadingTestDailyPlanRequest;
import com.shelflife.dto.ReadingTestResponse;
import com.shelflife.filter.FirebaseAuthFilter;
import com.shelflife.model.ReadingTestStatus;
import com.shelflife.service.ReadingTestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReadingTestController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReadingTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReadingTestService readingTestService;

    @MockBean
    private FirebaseAuthFilter firebaseAuthFilter;

    @Test
    void startTest_shouldReturnCreated() throws Exception {
        ReadingTestResponse response = ReadingTestResponse.builder()
                .id("test-1")
                .status(ReadingTestStatus.IN_PROGRESS)
                .promptText("Sample text")
                .promptWordCount(250)
                .createdAt(LocalDateTime.now())
                .build();

        when(readingTestService.startTest("uid-1")).thenReturn(response);

        mockMvc.perform(post("/api/reading-tests/start")
                        .principal(new UsernamePasswordAuthenticationToken("uid-1", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("test-1"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.promptWordCount").value(250));
    }

    @Test
    void completeTest_shouldValidatePayload() throws Exception {
        ReadingTestCompletionRequest request = ReadingTestCompletionRequest.builder()
                .sampleReadSeconds(0)
                .bookEntryIds(List.of())
                .build();

        mockMvc.perform(post("/api/reading-tests/test-1/complete")
                        .principal(new UsernamePasswordAuthenticationToken("uid-1", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void completeTest_shouldReturnEstimatedResult() throws Exception {
        ReadingTestCompletionRequest request = ReadingTestCompletionRequest.builder()
                .sampleReadSeconds(75)
                .bookEntryIds(List.of("book-1"))
                .build();

        ReadingTestResponse response = ReadingTestResponse.builder()
                .id("test-1")
                .status(ReadingTestStatus.SUBMITTED)
                .wordsPerMinute(200.0)
                .totalEstimatedHours(8.5)
                .build();

        when(readingTestService.completeTest(eq("uid-1"), eq("test-1"), eq(75), any())).thenReturn(response);

        mockMvc.perform(post("/api/reading-tests/test-1/complete")
                        .principal(new UsernamePasswordAuthenticationToken("uid-1", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.wordsPerMinute").value(200.0));
    }

    @Test
    void applyDailyPlan_shouldReturnUpdatedResult() throws Exception {
        ReadingTestDailyPlanRequest request = ReadingTestDailyPlanRequest.builder()
                .dailyReadingMinutes(45)
                .build();

        ReadingTestResponse response = ReadingTestResponse.builder()
                .id("test-1")
                .status(ReadingTestStatus.SCORED)
                .dailyReadingMinutes(45)
                .totalEstimatedDaysAtDailyReading(12.3)
                .build();

        when(readingTestService.applyDailyPlan("uid-1", "test-1", 45)).thenReturn(response);

        mockMvc.perform(post("/api/reading-tests/test-1/daily-plan")
                        .principal(new UsernamePasswordAuthenticationToken("uid-1", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCORED"))
                .andExpect(jsonPath("$.dailyReadingMinutes").value(45));
    }

    @Test
    void listTests_shouldReturnFilteredResults() throws Exception {
        when(readingTestService.listTestsForUser(eq("uid-1"), eq(ReadingTestStatus.SCORED), any(), any()))
                .thenReturn(List.of(ReadingTestResponse.builder().id("test-1").status(ReadingTestStatus.SCORED).build()));

        mockMvc.perform(get("/api/reading-tests")
                        .principal(new UsernamePasswordAuthenticationToken("uid-1", null))
                        .param("status", "SCORED")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("test-1"));
    }
}
