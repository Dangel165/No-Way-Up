package com.nowayup.system;

import com.nowayup.NoWayUpMod;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraftforge.fml.loading.FMLPaths;

public final class ExternalTextScareSystem {
    private static final String BASE_NAME = "READ_ME_NOWAYUP";
    private static final String EXTENSION = ".txt";

    private ExternalTextScareSystem() {
    }

    public static boolean createDesktopMessage() {
        return createDesktopMessagePath().isPresent();
    }

    public static boolean createAndOpenDesktopMessage() {
        Optional<Path> created = createDesktopMessagePath();
        if (created.isEmpty()) {
            return false;
        }

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(created.get().toFile());
            }
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            NoWayUpMod.LOGGER.warn("No Way Up created scare text file but could not open it", exception);
        }
        return true;
    }

    private static Optional<Path> createDesktopMessagePath() {
        Path directory = findDesktopDirectory();
        try {
            Files.createDirectories(directory);
            Path target = nextAvailableFile(directory);
            Files.writeString(target, message(), StandardCharsets.UTF_8);
            NoWayUpMod.LOGGER.info("No Way Up created scare text file at {}", target);
            return Optional.of(target);
        } catch (IOException exception) {
            NoWayUpMod.LOGGER.warn("No Way Up could not create scare text file", exception);
            return Optional.empty();
        }
    }

    private static Path findDesktopDirectory() {
        Path oneDriveDesktop = envDesktop("OneDrive");
        if (oneDriveDesktop != null) {
            return oneDriveDesktop;
        }

        Path userProfileDesktop = envDesktop("USERPROFILE");
        if (userProfileDesktop != null) {
            return userProfileDesktop;
        }

        String home = System.getProperty("user.home");
        if (home != null && !home.isBlank()) {
            Path desktop = Path.of(home, "Desktop");
            if (Files.isDirectory(desktop) && Files.isWritable(desktop)) {
                return desktop;
            }
        }
        return FMLPaths.GAMEDIR.get().resolve("config").resolve("nowayup").resolve("messages");
    }

    private static Path envDesktop(String envName) {
        String root = System.getenv(envName);
        if (root == null || root.isBlank()) {
            return null;
        }
        Path desktop = Path.of(root, "Desktop");
        return Files.isDirectory(desktop) && Files.isWritable(desktop) ? desktop : null;
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
