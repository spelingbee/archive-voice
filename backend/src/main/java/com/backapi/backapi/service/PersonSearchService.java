package com.backapi.backapi.service;

import com.backapi.backapi.entity.Person;
import com.backapi.backapi.enums.PersonStatus;
import com.backapi.backapi.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Нечёткий поиск по ФИО.
 *
 * Логика:
 * 1. Запрос разбивается на токены (по пробелам): "Умрат Бай" → ["умрат", "бай"]
 * 2. ФИО в БД тоже разбивается: "Байтурсынов Мурат Алиевич" → ["байтурсынов", "мурат", "алиевич"]
 * 3. Для каждого токена запроса ищется ЛУЧШЕЕ совпадение среди токенов имени
 * 4. Итого скор = среднее лучших совпадений
 * 5. Токен считается "подходящим" только если его сходство > 0.55
 */
@Service
@RequiredArgsConstructor
public class PersonSearchService {

    private final PersonRepository personRepository;

    private final LevenshteinDistance levenshtein = new LevenshteinDistance();
    private final JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();

    // Минимальный порог для общего скора записи
    private static final double RECORD_THRESHOLD = 0.45;
    // Минимальный порог для пары токенов (ниже — не считается совпадением)
    private static final double TOKEN_THRESHOLD = 0.55;

    // ─── Публичные методы ──────────────────────────────────────────

    public List<Long> fuzzySearchIds(String query) {
        String target = query.toLowerCase().trim();
        if (target.isEmpty()) return List.of();

        return personRepository.findByModerationStatus(PersonStatus.APPROVED,
                        org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .map(person -> new ScoredId(person.getId(),
                        calculateScore(target, person.getFullName().toLowerCase())))
                .filter(sp -> sp.score > RECORD_THRESHOLD)
                .sorted(Comparator.comparingDouble(ScoredId::score).reversed())
                .map(ScoredId::id)
                .toList();
    }

    public List<Long> fuzzySearchAllIds(String query) {
        String target = query.toLowerCase().trim();
        if (target.isEmpty()) return List.of();

        return personRepository.findAll().stream()
                .map(person -> new ScoredId(person.getId(),
                        calculateScore(target, person.getFullName().toLowerCase())))
                .filter(sp -> sp.score > RECORD_THRESHOLD)
                .sorted(Comparator.comparingDouble(ScoredId::score).reversed())
                .map(ScoredId::id)
                .toList();
    }

    // ─── Основной алгоритм ─────────────────────────────────────────

    double calculateScore(String query, String fullName) {
        String[] queryTokens = query.split("\\s+");
        String[] nameTokens = fullName.split("\\s+");

        if (queryTokens.length == 0 || nameTokens.length == 0) return 0;

        // 1. Для каждого токена запроса находим лучший match среди токенов имени
        //    Если лучший match ниже TOKEN_THRESHOLD — этот токен "не совпал"
        int matchedCount = 0;
        double matchedScoreSum = 0;

        for (String qt : queryTokens) {
            double bestMatch = 0;
            for (String nt : nameTokens) {
                double score = tokenSimilarity(qt, nt);
                bestMatch = Math.max(bestMatch, score);
            }
            if (bestMatch >= TOKEN_THRESHOLD) {
                matchedCount++;
                matchedScoreSum += bestMatch;
            }
        }

        // Если ни один токен не совпал — 0
        if (matchedCount == 0) return 0;

        // Средний скор совпавших токенов
        double avgScore = matchedScoreSum / matchedCount;

        // Штраф за несовпавшие токены: если из 3 токенов совпал 1 — это хуже
        double matchRatio = (double) matchedCount / queryTokens.length;

        // Бонус за подстроку (точное вхождение в имя)
        double containsBonus = fullName.contains(query) ? 0.15 : 0;

        return Math.min(1.0, (avgScore * 0.7) + (matchRatio * 0.3) + containsBonus);
    }

    // ─── Сходство между двумя токенами ─────────────────────────────

    double tokenSimilarity(String a, String b) {
        // Если один токен — короткий префикс другого (>=2 символа): высокий скор
        if (a.length() >= 2 && b.startsWith(a)) return 0.85;
        if (b.length() >= 2 && a.startsWith(b)) return 0.85;

        // Jaro-Winkler: хорош для "Иурат" → "Мурат"
        double jwScore = jaroWinkler.apply(a, b);

        // Левенштейн нормализованный: "Умрат" → "Мурат"
        double levScore = levenshteinSimilarity(a, b);

        // Триграм
        double trigramScore = trigramSimilarity(a, b);

        // Комбинация: JW=45%, Lev=30%, Trigram=25%
        return (jwScore * 0.45) + (levScore * 0.30) + (trigramScore * 0.25);
    }

    // ─── Алгоритмы ─────────────────────────────────────────────────

    double levenshteinSimilarity(String a, String b) {
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 1.0;
        int distance = levenshtein.apply(a, b);
        return 1.0 - ((double) distance / maxLen);
    }

    double trigramSimilarity(String a, String b) {
        Set<String> trigramsA = generateTrigrams(a);
        Set<String> trigramsB = generateTrigrams(b);

        if (trigramsA.isEmpty() && trigramsB.isEmpty()) return 1.0;
        if (trigramsA.isEmpty() || trigramsB.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(trigramsA);
        intersection.retainAll(trigramsB);

        Set<String> union = new HashSet<>(trigramsA);
        union.addAll(trigramsB);

        return (double) intersection.size() / union.size();
    }

    private Set<String> generateTrigrams(String s) {
        String padded = " " + s + " ";
        Set<String> trigrams = new HashSet<>();
        for (int i = 0; i <= padded.length() - 3; i++) {
            trigrams.add(padded.substring(i, i + 3));
        }
        return trigrams;
    }

    // ─── Поиск дублей (все статусы) ──────────────────────────────

    /**
     * Ищет дубли среди ВСЕХ персон (любой статус модерации).
     * Возвращает отсортированный список с персонами и скорами.
     */
    public List<ScoredPerson> findFuzzyDuplicates(String fullName, Long excludeId) {
        String target = fullName.toLowerCase().trim();
        if (target.isEmpty()) return List.of();

        return personRepository.findAll().stream()
                .filter(person -> !person.getId().equals(excludeId))
                .map(person -> new ScoredPerson(person,
                        calculateScore(target, person.getFullName().toLowerCase())))
                .filter(sp -> sp.score() > RECORD_THRESHOLD)
                .sorted(Comparator.comparingDouble(ScoredPerson::score).reversed())
                .toList();
    }

    public record ScoredPerson(Person person, double score) {}

    private record ScoredId(Long id, double score) {}
}
