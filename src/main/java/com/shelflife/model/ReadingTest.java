package com.shelflife.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Stores one user-scoped reading calibration and planning session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reading_tests")
@CompoundIndexes({
        @CompoundIndex(name = "rt_user_created_idx", def = "{'userId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "rt_user_status_created_idx", def = "{'userId': 1, 'status': 1, 'createdAt': -1}")
})
public class ReadingTest {

    @Id
    private String id;
    private String userId;
    private ReadingTestStatus status;
    private String promptText;
    private Integer promptWordCount;
    private Integer sampleReadSeconds;
    private Double wordsPerMinute;
    private List<String> selectedBookEntryIds;
    private List<BookPlanSnapshot> bookPlans;
    private Double totalEstimatedHours;
    private Double totalEstimatedDays;
    private Integer dailyReadingMinutes;
    private Double totalEstimatedDaysAtDailyReading;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Snapshot of a book duration estimate produced from a calibrated reading speed.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookPlanSnapshot {
        private String bookEntryId;
        private String title;
        private Integer pageCount;
        private Double estimatedHours;
        private Double estimatedDays;
        private Double estimatedDaysAtDailyReading;
    }
}
