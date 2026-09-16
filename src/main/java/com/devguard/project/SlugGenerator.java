package com.devguard.project;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugGenerator {
    private SlugGenerator() {

    }

    public static String from(String name) {
        String slug = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (slug.length() > 50) {
            slug = slug.substring(0, 50).replaceAll("-+$", "");
        }

        if (slug.length() < 3) {
            throw new IllegalArgumentException(
                    "El nombre no produce un identificador valido: use al menos "
                    + "tres caracteres alfanumericos");
        }
        return slug;
    }
}
