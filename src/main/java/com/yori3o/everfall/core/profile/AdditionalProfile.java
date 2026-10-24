package com.yori3o.everfall.core.profile;

import java.time.LocalDateTime;
import java.util.List;

/** An independent layer, combined with the main seasonal profile. */
public record AdditionalProfile(String id, String name, Motion motion, int maxParticles,
        float speed, float size, int color, Conditions conditions) {
    public enum Motion {
        FIREFLIES
    }

    public record Conditions(List<String> profiles, int startHour, int endHour,
            int startDay, int endDay) {
        public boolean matches(String mainProfile, LocalDateTime now) {
            if (!profiles.isEmpty() && !profiles.contains(mainProfile)) return false;

            int hour = now.getHour();
            boolean timeMatches = startHour == endHour || (startHour < endHour
                    ? hour >= startHour && hour < endHour : hour >= startHour || hour < endHour);

            if (!timeMatches) return false;

            if (startDay == 0) return true;

            int day = now.getMonthValue() * 100 + now.getDayOfMonth();
            return startDay <= endDay ? day >= startDay && day <= endDay
                    : day >= startDay || day <= endDay;
        }
    }
}
