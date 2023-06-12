package searchengine.engines;

import org.springframework.stereotype.Component;
import searchengine.config.LemmaLanguageConfiguration;

import java.io.IOException;
import java.util.*;

@Component

public record LemmaEngine(LemmaLanguageConfiguration lemmaLanguageConfiguration) {


    public Map<String, Integer> getLemmaMap(String text) {
        text = arrayContainsWords(text);
        Map<String, Integer> lemmaList = new HashMap<>();
        String[] elements = text.toLowerCase(Locale.ROOT).split("\\s+");
        List<String> wordsList;
        int count;
        for (String el : elements) {
            try {
                wordsList = getLemma(el);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
            for (String word : wordsList) {
                count = lemmaList.getOrDefault(word, 0);
                lemmaList.put(word, count + 1);
            }
        }
        return lemmaList;
    }

    public List<String> getLemma(String word) throws IOException {
        List<String> lemmaList = new ArrayList<>();
        if (checkLanguage(word).equals("Russian")) {
            List<String> baseRusForm = lemmaLanguageConfiguration.russianLuceneMorphology().getNormalForms(word);
            if (!word.isEmpty() && isCorrectWordForm(word, "Russian")) {
                lemmaList.add(baseRusForm.get(0));
            }
        } else if (checkLanguage(word).equals("English")) {
            List<String> baseEngForm = lemmaLanguageConfiguration.englishLuceneMorphology().getNormalForms(word);
            if (!word.isEmpty() && isCorrectWordForm(word, "English")) {
                lemmaList.add(baseEngForm.get(0));
            }
        }

        return lemmaList;
    }

    private boolean isCorrectWordForm(String word, String language) throws IOException {
        if (language.equals("Russian")) {
            List<String> morphForm = lemmaLanguageConfiguration.russianLuceneMorphology().getMorphInfo(word);
            return checkRussianPartOfSpeech(morphForm.get(0));
        } else if (language.equals("English")) {
            List<String> morphForm = lemmaLanguageConfiguration.englishLuceneMorphology().getMorphInfo(word);
            return checkEnglishPartOfSpeech(morphForm.get(0));
        }
        return false;
    }

    private boolean checkRussianPartOfSpeech(String s) {
        return !s.contains("ПРЕДЛ") && !s.contains("СОЮЗ") && !s.contains("МЕЖД") && !s.contains("ВВОДН") && !s.contains("ЧАСТ");
    }

    private boolean checkEnglishPartOfSpeech(String s) {
        return !s.contains("МС") && !s.contains("CONJ") && !s.contains("PART") && !s.contains("PREP");
    }


    private String checkLanguage(String word) {
        String russianAlphabet = "[а-яА-Я]+";
        String englishAlphabet = "[a-zA-Z]+";

        if (word.matches(russianAlphabet)) {
            return "Russian";
        } else if (word.matches(englishAlphabet)) {
            return "English";
        } else {
            return "";
        }
    }

    private String arrayContainsWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-яa-z\\s])", " ").trim();
    }

    public Collection<Integer> findLemmaIndexInText(String content, String lemma) throws IOException {
        List<Integer> lemmaIndexList = new ArrayList<>();
        String[] elements = content.toLowerCase(Locale.ROOT).split("\\p{Punct}|\\s");
        int index = 0;
        List<String> lemmas;
        for (String el : elements) {
            lemmas = getLemma(el);
            for (String lem : lemmas) {
                if (lem.equals(lemma)) {
                    lemmaIndexList.add(index);
                }
            }
            index += el.length() + 1;
        }
        return lemmaIndexList;
    }
}
