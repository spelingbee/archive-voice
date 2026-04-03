package com.backapi.backapi.util;

import org.springframework.stereotype.Component;

@Component
public class FileNameUtils {

    public String cleanFileName(String fullName) {
        if (fullName == null) return "unknown";

        // Убираем всё, кроме букв, цифр, пробелов и дефисов
        String cleaned = fullName
                .replaceAll("[^а-яА-Яa-zA-Z0-9\\s-]", "")  // оставляем кириллицу, латиницу, цифры, пробел, дефис
                .trim()
                .replaceAll("\\s+", "_");                  // пробелы → _

        return cleaned.length() > 50 ? cleaned.substring(0, 50) : cleaned;
    }

    public String generateFileName(Long personId, String fullName, String originalExtension) {
        String cleanName = cleanFileName(fullName);
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        return String.format("person_%d_%s_%s.%s",
                personId, cleanName, timestamp, originalExtension);
    }
}
