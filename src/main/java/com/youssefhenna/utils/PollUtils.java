package com.youssefhenna.utils;

import com.youssefhenna.model.PollConfig;

import java.text.ParseException;
import java.time.Duration;
import java.util.Date;

public class PollUtils {

    public static Duration toDuration(PollConfig poll) {
        return switch (poll.getUnit()) {
            case DAYS -> Duration.ofDays(poll.getEvery());
            case HOURS -> Duration.ofHours(poll.getEvery());
            case MINUTES -> Duration.ofMinutes(poll.getEvery());
        };
    }

    public static Duration minDuration(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static boolean isPollElapsed(String lastRunTime, PollConfig poll) {
        if (lastRunTime == null) return true;
        try {
            Date lastRun = Common.parseDate(lastRunTime);
            Date nextRun = new Date(lastRun.getTime() + toDuration(poll).toMillis());
            return new Date().after(nextRun);
        } catch (ParseException e) {
            return true;
        }
    }
}