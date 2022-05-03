package ru.sequoio.library.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class IOUtils {

    public static InputStream getInputStream(Path path) {
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Scanner getScanner(Path path) {
        try {
            return new Scanner(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
