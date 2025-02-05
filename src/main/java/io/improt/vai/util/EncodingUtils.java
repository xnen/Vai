package io.improt.vai.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class EncodingUtils {
    public static String encodeMp3ToBase64(String filePath) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Path.of(filePath));
        return Base64.getEncoder().encodeToString(fileBytes);
    }
}
