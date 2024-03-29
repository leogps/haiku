package org.gps.haiku.updater.checksum;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Handler implements ChecksumHandler {

    private static final String NAME = "MD5SUMS";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean canHandle(String checksumName) {
        return StringUtils.startsWithIgnoreCase(checksumName, NAME);
    }

    @Override
    public String calculateChecksum(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        return FileChecksumCalculator.calculateFileChecksum(digest, file);
    }
}
