package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "shifts")
public class Shift {
    @Id
    private String id;

    private String name;
    private LocalTime startTime;
    private LocalTime endTime;

    @Builder.Default
    private Integer gracePeriodMinutes = 15;

    @Builder.Default
    private Integer standardBreakMinutes = 60;

    @Builder.Default
    private Integer maxBreakMinutes = 90;

    @Builder.Default
    private Integer requiredWorkingMinutes = 480;

    @Builder.Default
    private Integer overtimeThresholdMinutes = 480;

    @Builder.Default
    private Integer halfDayThresholdMinutes = 240;

    // Same as gracePeriodMinutes by default; kept separate for future flexibility.
    @Builder.Default
    private Integer lateThresholdMinutes = 15;

    // 0 = any departure before shift end counts as an early checkout.
    @Builder.Default
    private Integer earlyCheckoutThresholdMinutes = 0;

    // 0=Sunday .. 6=Saturday
    @Builder.Default
    private List<Integer> weeklyOffDays = Arrays.asList(0, 6);
}
