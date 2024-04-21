package org.gps.haiku.utils.test;

import org.gps.haiku.utils.OSInfo;
import org.gps.haiku.utils.PropertyManager;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PropertyManagerTest {

    @Test
    public void testWindowsRelativePathWithAbsolutePath() {
        if (!OSInfo.isOSWin()) {
            return;
        }
        boolean isRelative = PropertyManager
                .isRelativePath("C:\\Program Files\\haiku\\config\\win-app.properties");
        Assert.assertFalse(isRelative);
    }

    @Test
    public void testWindowsRelativePathWithRelativePath() {
        if (!OSInfo.isOSWin()) {
            return;
        }
        boolean isRelative = PropertyManager
                .isRelativePath(".\\config\\win-app.properties");
        Assert.assertTrue(isRelative);
    }

    @Test
    public void testNixRelativePathWithAbsolutePath() {
        if (OSInfo.isOSWin()) {
            return;
        }
        boolean isRelative = PropertyManager
                .isRelativePath("/home/user/haiku/config/mac-app.properties");
        Assert.assertFalse(isRelative);
    }

    @Test
    public void testNixRelativePathWithRelativePath() {
        if (OSInfo.isOSWin()) {
            return;
        }
        boolean isRelative = PropertyManager
                .isRelativePath("config/mac-app.properties");
        Assert.assertTrue(isRelative);
    }
}
