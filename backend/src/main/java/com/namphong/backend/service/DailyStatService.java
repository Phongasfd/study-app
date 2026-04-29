package com.namphong.backend.service;

import com.namphong.backend.dto.DailyStatResponse;
import com.namphong.backend.dto.WeeklyAggregateResponse;
import com.namphong.backend.entity.DailyStat;
import com.namphong.backend.entity.SessionStatus;
import com.namphong.backend.entity.StudySession;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.DailyStatRepository;
import com.namphong.backend.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyStatService {
    private final DailyStatRepository dailyStatRepository;
    private final StudySessionRepository studySessionRepository;

    /**
     * Sums all COMPLETED sessions that started today and upserts the DailyStat row.
     * 
     */
    public DailyStatResponse syncDailyStat(UserEntity user) {
        LocalDate today = LocalDate.now(); // now date
        LocalDateTime startOfDay = today.atStartOfDay(); // 00:00:00
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX); // 23:59:59 

        // Find every completed session that started today
        List<StudySession> todaySessions = studySessionRepository
                .findByUserAndStatusAndStartTimeBetween(user, SessionStatus.COMPLETED, startOfDay, endOfDay);

        // sum duration seconds of today sessions 
        int totalSeconds = todaySessions.stream()
                .mapToInt(s -> s.getDurationSeconds() != null ? s.getDurationSeconds() : 0)
                .sum();

        // Upsert: update existing row or create a new one
        DailyStat stat = dailyStatRepository.findByUserAndDate(user, today)
                .orElseGet(() -> {
                    DailyStat s = new DailyStat();
                    s.setUser(user);
                    s.setDate(today);
                    return s;
                }); 

        stat.setTotalDuration(totalSeconds);
        stat = dailyStatRepository.save(stat);

        return new DailyStatResponse(stat.getId(), user.getId(), stat.getDate(), stat.getTotalDuration());
    }

    public List<DailyStatResponse> getWeeklyStats(UserEntity user) {
        LocalDate today = LocalDate.now(); // now date
        LocalDate monday = today.with(DayOfWeek.MONDAY); // monday this week
        LocalDate sunday = today.with(DayOfWeek.SUNDAY); // sunday this week

        List<DailyStat> existing = dailyStatRepository.findByUserAndDateBetween(user, monday, sunday);
        // find daily stat by user and date between monday and sunday (include both)

        Map<LocalDate, DailyStat> statByDate = existing.stream()
                .collect(Collectors.toMap(DailyStat::getDate, s -> s));
        // .collect: gathering data to turn into another type
        // DailyStat::getDate  == s -> s.getDate(): get date as a key
        // s -> s: get daily stat as a value

        // loop from monday to sunday
        // if daily stat is null, return 0 as total duration
        // if daily stat is not null, return total duration
        // Add to list
        List<DailyStatResponse> result = new ArrayList<>();
        for (LocalDate date = monday; !date.isAfter(sunday); date = date.plusDays(1)) {
            DailyStat stat = statByDate.get(date);
            if (stat != null) {
                result.add(new DailyStatResponse(stat.getId(), user.getId(), stat.getDate(), stat.getTotalDuration()));
            } else {
                result.add(new DailyStatResponse(null, user.getId(), date, 0));
            }
        }
        return result;
    }

    public List<WeeklyAggregateResponse> getMonthlyStats(UserEntity user) {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        // Fetch all daily stats for the current month
        List<DailyStat> monthlyStats = dailyStatRepository.findByUserAndDateBetween(user, monthStart, monthEnd);
        Map<LocalDate, DailyStat> statByDate = monthlyStats.stream()
                .collect(Collectors.toMap(DailyStat::getDate, s -> s));

        List<WeeklyAggregateResponse> result = new ArrayList<>();
        
        // Split month into 4 weeks (or less if month has fewer days)
        LocalDate weekStart = monthStart;
        int weekNum = 1;
        
        while (!weekStart.isAfter(monthEnd)) {
            LocalDate weekEnd = weekStart.plusDays(6); // 7 days per week
            if (weekEnd.isAfter(monthEnd)) {
                weekEnd = monthEnd;
            }

            // Sum all daily stats in this week
            int weekTotalSeconds = 0;
            for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
                DailyStat stat = statByDate.get(date);
                if (stat != null) {
                    weekTotalSeconds += stat.getTotalDuration() != null ? stat.getTotalDuration() : 0;
                }
            }

            result.add(new WeeklyAggregateResponse(
                    "Week " + weekNum,
                    weekStart,
                    weekEnd,
                    weekTotalSeconds
            ));

            weekStart = weekEnd.plusDays(1);
            weekNum++;
        }

        return result;
    }
}
