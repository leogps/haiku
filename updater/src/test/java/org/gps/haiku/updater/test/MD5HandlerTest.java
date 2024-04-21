package org.gps.haiku.updater.test;

import org.gps.haiku.updater.checksum.MD5Handler;
import org.gps.haiku.utils.OSInfo;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class MD5HandlerTest {

    @Test
    public void testHandler() throws NoSuchAlgorithmException, IOException {
        MD5Handler handler = new MD5Handler();
        Assert.assertTrue(handler.canHandle("MD5SUMS"));
        File testFile = new File(Objects.requireNonNull(MD5HandlerTest.class.getClassLoader().getResource("checksum_test_file")).getFile());
        String checksum = handler.calculateChecksum(testFile);
        if (!OSInfo.isOSWin()) {
            Assert.assertEquals(checksum, "e479bd09463260ef298d7092ca6ed132");
        } else {
            Assert.assertEquals(checksum, "2136ae694cbd4835cc9c6480a384afae");
        }
    }
}
