package com.nowayup.system;

import com.nowayup.NoWayUpMod;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraftforge.fml.loading.FMLPaths;

public final class ExternalTextScareSystem {
    private static final String BASE_NAME = "READ_ME_NOWAYUP";
    private static final String EXTENSION = ".txt";

    private ExternalTextScareSystem() {
    }

    public static boolean createDesktopMessage() {
        Path directory = findDesktopDirectory();
        try {
            Files.createDirectories(directory);
            Path target = nextAvailableFile(directory);
            Files.writeString(target, message(), StandardCharsets.UTF_8);
            NoWayUpMod.LOGGER.info("No Way Up created scare text file at {}", target);
            return true;
        } catch (IOException exception) {
            NoWayUpMod.LOGGER.warn("No Way Up could not create scare text file", exception);
            return false;
        }
    }

    private static Path findDesktopDirectory() {
        String home = System.getProperty("user.home");
        if (home != null && !home.isBlank()) {
            Path desktop = Path.of(home, "Desktop");
            if (Files.isDirectory(desktop) && Files.isWritable(desktop)) {
                return desktop;
            }
        }
        return FMLPaths.GAMEDIR.get().resolve("config").resolve("nowayup").resolve("messages");
    }

    private static Path nextAvailableFile(Path directory) {
        Path first = directory.resolve(BASE_NAME + EXTENSION);
        if (!Files.exists(first)) {
            return first;
        }

        for (int i = 2; i < 100; i++) {
            Path candidate = directory.resolve(BASE_NAME + "_" + i + EXTENSION);
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }

        return directory.resolve(BASE_NAME + "_" + System.currentTimeMillis() + EXTENSION);
    }

    private static String message() {
        return """
            You will never get out.
            You thought climbing would save you.
            Come back inside.
            """;
    }
}
