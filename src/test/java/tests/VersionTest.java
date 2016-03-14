package tests;

import org.junit.Test;
import ui.UI;
import util.Version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class VersionTest {
    @Test
    public void currentVersion_CompareWithUiValue_VersionEqual() {
        Version currentVersion = Version.getCurrentVersion();

        assertEquals(currentVersion.getMajor(), UI.VERSION_MAJOR);
        assertEquals(currentVersion.getMinor(), UI.VERSION_MINOR);
        assertEquals(currentVersion.getPatch(), UI.VERSION_PATCH);
    }
}
