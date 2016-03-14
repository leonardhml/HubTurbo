package util;

import ui.UI;

import java.util.Optional;

/**
 * Represents a version with major, minor and patch number
 */
public class Version implements Comparable<Version> {

    private final int major;
    private final int minor;
    private final int patch;

    public Version(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    /**
     * Parses a version number string in the format V1.2.3.
     * @param versionString version number string
     * @return a Version object
     */
    public static Optional<Version> fromString(String versionString) {
        // Strip non-digits
        String numericVersion = versionString.replaceAll("[^0-9.]+", "");

        String[] temp = numericVersion.split("\\.");
        try {
            int major = temp.length > 0 ? Integer.parseInt(temp[0]) : 0;
            int minor = temp.length > 1 ? Integer.parseInt(temp[1]) : 0;
            int patch = temp.length > 2 ? Integer.parseInt(temp[2]) : 0;
            return Optional.of(new Version(major, minor, patch));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public String toString() {
        return String.format("V%d.%d.%d", major, minor, patch);
    }

    /**
     * Gets HubTurbo current version
     * @return version object of HubTurbo's current version
     */
    public static Version getCurrentVersion() {
        return new Version(UI.VERSION_MAJOR, UI.VERSION_MINOR, UI.VERSION_PATCH);
    }

    @Override
    public int compareTo(Version other) {
        return this.major != other.major ? this.major - other.major :
                this.minor != other.minor ? this.minor - other.minor :
                this.patch != other.patch ? this.patch - other.patch : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Version)) {
            return false;
        }
        final Version other = (Version) obj;

        return this.compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        String hash = String.format("%1$02d%2$02d%3$02d", major, minor, patch);
        return Integer.parseInt(hash);
    }
}
