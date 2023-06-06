package searchengine.components;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import searchengine.dto.LemmaDTO;
import searchengine.engines.LemmaEngine;
import searchengine.model.PageModel;
import searchengine.repository.PageRepository;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@Getter
public class LemmaIndexer {
    private final PageRepository pageRepository;
    private final LemmaEngine lemmaEngine;
    private List<LemmaDTO> lemmaDtoList;

    public void startLemmaIndexer() {
        lemmaDtoList = new CopyOnWriteArrayList<>();
        Iterable<PageModel> pageList = pageRepository.findAll();

        Map<String, Integer> lemmaList = new TreeMap<>();
        Map<String, Integer> titleSiteList;
        Map<String, Integer> bodySiteList;
        Set<String> allWordsInIndexingSite = new HashSet<>();
        String content;
        String title;
        String body;
        for (var page : pageList) {
            content = page.getContent();
            title = clearCodeFromTag(content, "title");
            body = clearCodeFromTag(content, "body");
            titleSiteList = lemmaEngine.getLemmaMap(title);
            bodySiteList = lemmaEngine.getLemmaMap(body);
            allWordsInIndexingSite.addAll(titleSiteList.keySet());
            allWordsInIndexingSite.addAll(bodySiteList.keySet());
            allWordsInIndexingSite.forEach(word -> {
                int frequency = lemmaList.getOrDefault(word, 0) + 1;
                lemmaList.put(word, frequency);
            });
        }
        for (String lemma : lemmaList.keySet()) {
            Integer frequency = lemmaList.get(lemma);
            lemmaDtoList.add(new LemmaDTO(lemma, frequency));
        }
        log.info(lemmaDtoList.size() + " lemmas found");
    }

    public String clearCodeFromTag(String content, String tag) {
        Document doc = Jsoup.parse(content);
        Elements elements = doc.select(tag);
        String html = elements.stream().map(Element::html).collect(Collectors.joining());
        return Jsoup.parse(html).text();
    }
}
