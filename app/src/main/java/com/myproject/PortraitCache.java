package com.myproject;

import java.io.IOException;
import java.nio.file.Path;

@FunctionalInterface
public interface PortraitCache {
    Path cache(String imageUrl) throws IOException;
}
