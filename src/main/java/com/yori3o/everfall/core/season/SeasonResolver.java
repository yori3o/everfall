package com.yori3o.everfall.core.season;

import com.yori3o.everfall.core.config.ParticleProfileId;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Local hints only: language preferences are weaker evidence than a geographic time zone. */
public final class SeasonResolver {
    public enum Hemisphere { NORTH, SOUTH, UNKNOWN }
    public record Region(String country, Hemisphere hemisphere) {}
    public record Result(ParticleProfileId profile, Hemisphere hemisphere, Region timeZoneRegion,
                         int northScore, int southScore, boolean fallback, String evidence) {}

    private final Map<String, Region> zones = new HashMap<>();
    private final Map<String, Hemisphere> countries = new HashMap<>();

    public SeasonResolver(Reader zoneTable, Reader backwardLinks) throws IOException {
        try (BufferedReader reader = new BufferedReader(zoneTable)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] fields = line.split("\\s+");
                Hemisphere hemisphere = fields[1].charAt(0) == '-' ? Hemisphere.SOUTH : Hemisphere.NORTH;
                zones.put(fields[2], new Region(fields[0], hemisphere));
                // Countries spanning both hemispheres are not useful without a geographic zone.
                countries.merge(fields[0], hemisphere, (a, b) -> a == b ? a : Hemisphere.UNKNOWN);
            }
        }
        Map<String, String> links = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(backwardLinks)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("Link")) continue;
                String[] fields = line.split("\\s+");
                links.put(fields[2], fields[1]);
            }
        }
        for (String alias : links.keySet()) {
            String target = alias;
            for (int depth = 0; depth < 16 && !zones.containsKey(target); depth++) {
                target = links.get(target);
                if (target == null) break;
            }
            Region region = zones.get(target);
            if (region != null) zones.putIfAbsent(alias, region);
        }
    }

    public Result resolve(LocalDate date, ZoneId zone, Locale systemLocale, Locale formatLocale,
                          Locale displayLocale, String minecraftLanguage) {
        int north = 0;
        int south = 0;
        Region zoneRegion = zones.get(zone.getId());
        StringBuilder evidence = new StringBuilder("zone=").append(zone.getId()).append(':').append(zoneRegion);
        if (zoneRegion != null) {
            if (zoneRegion.hemisphere() == Hemisphere.SOUTH) south += 8;
            else north += 8;
        }
        Set<String> seenCountries = new HashSet<>();
        Locale[] locales = {systemLocale, formatLocale, displayLocale};
        String[] names = {"system", "format", "display"};
        for (int i = 0; i < locales.length; i++) {
            String country = locales[i].getCountry().toUpperCase(Locale.ROOT);
            Hemisphere hint = countries.getOrDefault(country, Hemisphere.UNKNOWN);
            boolean counted = !country.isEmpty() && seenCountries.add(country);
            evidence.append("; ").append(names[i]).append('=').append(locales[i].toLanguageTag())
                    .append(':').append(hint).append(counted ? " (weight 2)" : " (duplicate/empty)");
            if (counted && hint == Hemisphere.NORTH) north += 2;
            if (counted && hint == Hemisphere.SOUTH) south += 2;
        }
        String language = minecraftLanguage == null ? "" : minecraftLanguage.toLowerCase(Locale.ROOT);
        // Pirate English and upside-down English are fictional locales, not geographic regions.
        Locale gameLocale = Locale.forLanguageTag(language.replace('_', '-'));
        Hemisphere gameHint = language.equals("en_pt") || language.equals("en_ud")
                ? Hemisphere.UNKNOWN : countries.getOrDefault(gameLocale.getCountry(), Hemisphere.UNKNOWN);
        evidence.append("; minecraft=").append(language).append(':').append(gameHint).append(" (weight 1)");
        if (gameHint == Hemisphere.NORTH) north++;
        if (gameHint == Hemisphere.SOUTH) south++;
        boolean fallback = north == south;
        Hemisphere hemisphere = south > north ? Hemisphere.SOUTH : Hemisphere.NORTH;
        return new Result(season(date, hemisphere), hemisphere, zoneRegion, north, south, fallback,
                evidence.toString());
    }

    public static ParticleProfileId season(LocalDate date, Hemisphere hemisphere) {
        int month = date.getMonthValue();
        if (hemisphere == Hemisphere.SOUTH) month = (month + 5) % 12 + 1;
        return switch (month) {
            case 3, 4, 5 -> ParticleProfileId.SPRING;
            case 6, 7, 8 -> ParticleProfileId.SUMMER;
            case 9, 10, 11 -> ParticleProfileId.AUTUMN;
            default -> ParticleProfileId.WINTER;
        };
    }
}
