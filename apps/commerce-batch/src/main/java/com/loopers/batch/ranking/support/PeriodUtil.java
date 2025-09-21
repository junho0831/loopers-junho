package com.loopers.batch.ranking.support;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;

public class PeriodUtil {
    public record DateRange(LocalDate start, LocalDate end) {}

    public static DateRange weekRange(LocalDate date) {
        WeekFields wf = WeekFields.ISO;
        LocalDate start = date.with(wf.dayOfWeek(), 1);
        LocalDate end = date.with(wf.dayOfWeek(), 7);
        return new DateRange(start, end);
    }

    public static DateRange monthRange(LocalDate date) {
        LocalDate start = date.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate end = date.with(TemporalAdjusters.lastDayOfMonth());
        return new DateRange(start, end);
    }

    public static String yearWeek(LocalDate date) {
        WeekFields wf = WeekFields.ISO;
        int week = date.get(wf.weekOfWeekBasedYear());
        int year = date.get(wf.weekBasedYear());
        return String.format("%dW%02d", year, week);
    }

    public static String yearMonth(LocalDate date) {
        YearMonth ym = YearMonth.from(date);
        return ym.toString().replace("-", ""); // yyyyMM
    }
}

