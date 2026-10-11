package com.yori3o.everfall.core.api;

import java.util.List;
import java.util.Optional;

public interface AssetReader {
    Optional<String> readText(String path);

    List<String> list(String directory);
}
