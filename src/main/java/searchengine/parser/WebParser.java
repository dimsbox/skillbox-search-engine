package searchengine.parser;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import searchengine.dto.IndexDTO;
import searchengine.engines.LemmaEngine;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@Getter
public class WebParser {

    private final float BODY_TO_TITLE_WEIGHT_COEFFICIENT = 0.8f;
    private final int ERROR_CODE = 400;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaEngine lemmaEngine;
    private List<IndexDTO> indexDTOList;

    public void startWebParser(SiteModel site) {
        Iterable<PageModel> pageList = pageRepository.findBySiteId(site);
        List<LemmaModel> lemmaList = lemmaRepository.findBySiteModelId(site);
        indexDTOList = new ArrayList<>();

        for (PageModel page : pageList) {
            if (page.getCode() < ERROR_CODE) {
                onPageLemmaRanking(page, lemmaList, indexDTOList);
            } else {
                log.debug("Bad status code - " + page.getCode());
            }
        }
    }

    private void onPageLemmaRanking(PageModel page, List<LemmaModel> lemmaList, List<IndexDTO> indexDTOList) {
        long pageId = page.getId();
        String content = page.getContent();
        String title = clearCodeFromTag(content, "title");
        String body = clearCodeFromTag(content, "body");
        Map<String, Integer> titleSiteList = lemmaEngine.getLemmaMap(title);
        Map<String, Integer> bodySiteList = lemmaEngine.getLemmaMap(body);
        float totalRank = 0.0f;
        float titleRank;
        float bodyRank;
        for (LemmaModel lemma : lemmaList) {
            long lemmaId = lemma.getId();
            String keyWord = lemma.getLemma();
            if (titleSiteList.containsKey(keyWord) || bodySiteList.containsKey(keyWord)) {
                if (titleSiteList.get(keyWord) != null) {
                    titleRank = titleSiteList.get(keyWord);
                    totalRank += titleRank;
                }
                if (bodySiteList.get(keyWord) != null) {
                    bodyRank = bodySiteList.get(keyWord) * BODY_TO_TITLE_WEIGHT_COEFFICIENT;
                    totalRank += bodyRank;
                }
                indexDTOList.add(new IndexDTO(pageId, lemmaId, totalRank));
            } else {
                log.debug("Lemma not found");
            }
        }
    }

    public String clearCodeFromTag(String content, String s) {
        Document doc = Jsoup.parse(content);
        Elements elements = doc.select(s);
        String html = elements.stream().map(Element::html).collect(Collectors.joining());
        return Jsoup.parse(html).text();
    }
}
