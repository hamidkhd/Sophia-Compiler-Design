package main;

import java.util.HashSet;
import java.util.Set;

public class SophiaErrors {
    public static Set<String> errors = new HashSet<>();

    public static void addError(String error) {
        errors.add(error);
    }

    public static void printErrors() {
        for (String error: errors)
            System.out.println(error);
    }

    public static boolean hasErrors() {
        return errors.size() != 0;
    }
}
