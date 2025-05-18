package io.improt.vai.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class EncodingUtils {
    public static String encodeFileToBase64(String filePath) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Path.of(filePath));
        return Base64.getEncoder().encodeToString(fileBytes);
    }

    public static void sayHello() {
        System.out.println("Hello, World!");
    }
}