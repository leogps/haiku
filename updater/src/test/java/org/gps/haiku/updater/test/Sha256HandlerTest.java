package org.gps.haiku.updater.test;

import org.gps.haiku.updater.checksum.Sha256Handler;
import org.gps.haiku.utils.OSInfo;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class Sha256HandlerTest {

    @Test
    public void testHandler() throws NoSuchAlgorithmException, IOException {
        Sha256Handler handler = new Sha256Handler();
        Assert.assertTrue(handler.canHandle("SHA2-256SUMS"));
        File testFile = new File(Objects.requireNonNull(Sha256HandlerTest.class.getClassLoader().getResource("checksum_test_file")).getFile());
        String checksum = handler.calculateChecksum(testFile);
        if (!OSInfo.isOSWin()) {
            Assert.assertEquals(checksum, "02791abd91e43ddff141b262bad736036c7878e7207df436cac2ebb9edcb9dd5");
        } else {
            Assert.assertEquals(checksum, "77545ad4e3005b37b67d1c1f6f325dce740c46efdae1bacdf3372c9010260dcd");
        }
    }
}
