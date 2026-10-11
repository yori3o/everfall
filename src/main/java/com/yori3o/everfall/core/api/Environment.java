package com.yori3o.everfall.core.api;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

public interface Environment {
    LocalDate today();

    ZoneId zone();

    Locale locale();

    boolean isModLoaded(String modId);
}
