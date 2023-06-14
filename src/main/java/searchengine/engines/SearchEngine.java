package searchengine.engines;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import searchengine.dto.SearchDTO;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public record SearchEngine(LemmaEngine lemmaEngine, LemmaRepository lemmaRepository, PageRepository pageRepository,
                           IndexRepository indexRepository) {

    private static final int TITLE_MAX_LENGTH = 300;
    private static final int SNIPPET_STRINGS_LIMIT = 3;
    private static final int SNIPPET_PREPEND_WORDS_LIMIT = 4;
    private static final int SNIPPET_POST_WORDS_LIMIT = 3;

    private List<SearchDTO> getSearchDtoList(ConcurrentHashMap<PageModel, Float> pageList,
                                             List<String> textLemmaList) {
        List<SearchDTO> searchDtoList = new ArrayList<>();
        StringBuilder titleStringBuilder = new StringBuilder();
        for (PageModel page : pageList.keySet()) {
            String uri = page.getPath();
            String content = page.getContent();
            SiteModel pageSite = page.getSiteId();
            String site = pageSite.getUrl();
            String siteName = pageSite.getName();
            String title = clearCodeFromTag(content, "title");
            if (title.length() > TITLE_MAX_LENGTH) {
                title = title.substring(0, TITLE_MAX_LENGTH);
            }
            String body = clearCodeFromTag(content, "body");
            titleStringBuilder.append(title).append(body);
            float pageValue = pageList.get(page);
            List<Integer> lemmaIndex = new ArrayList<>();
            for (String lemma : textLemmaList) {
                try {
                    lemmaIndex.addAll(lemmaEngine.findLemmaIndexInText(titleStringBuilder.toString(), lemma));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            Collections.sort(lemmaIndex);
            List<String> wordList = getWordsFromSiteContent(titleStringBuilder.toString(), lemmaIndex);
            StringBuilder snippetBuilder = buildSnippet(wordList);
            searchDtoList.add(new SearchDTO(site, siteName, uri, title, snippetBuilder.toString(), pageValue));
        }
        return searchDtoList;
    }

    private StringBuilder buildSnippet(List<String> wordList) {
        StringBuilder snippet = new StringBuilder();
        for (String s : wordList) {
            snippet.append(s).append(" ");
        }
        return snippet;
    }

    private List<String> getWordsFromSiteContent(String content, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        System.out.println("Поиск на странице, количество лемм " + lemmaIndex.size());
        Collections.shuffle(lemmaIndex);
        for (int i = 0; i < SNIPPET_STRINGS_LIMIT; i++) {
            int start = lemmaIndex.get(i);
            int end = content.indexOf(" ", start);
            int next = i + 1;
            while (next < lemmaIndex.size() && 0 < lemmaIndex.get(next) - end && lemmaIndex.get(next) - end < 5) {
                end = content.indexOf(" ", lemmaIndex.get(next));
                next += 1;
            }
            i = next - 1;
            String word = content.substring(start, end);
            int endTextIndex = end;
            for (int j = 0; j < SNIPPET_POST_WORDS_LIMIT; j++) {
                if (content.lastIndexOf(" ", endTextIndex) != -1) {
                    endTextIndex = content.indexOf(" ", endTextIndex + 1);
                }
            }
            int startTextIndex = start;
            String prependWordContent = content.substring(0, startTextIndex);
            for (int j = 0; j < SNIPPET_PREPEND_WORDS_LIMIT; j++) {
                if (prependWordContent.lastIndexOf(" ") != -1) {
                    startTextIndex = prependWordContent.lastIndexOf(" ");
                    prependWordContent = content.substring(0, startTextIndex);
                }
            }
            String text = content.substring(startTextIndex, endTextIndex).replace(word, "<b>".concat(word).concat("</b>"));
            result.add("..." + text + "..." + "\n");
        }
        result.sort(Comparator.comparing(String::length).reversed());
        return result;
    }

    private Map<PageModel, Float> getRelevanceFromPage(List<PageModel> pageList,
                                                       List<IndexModel> indexList) {
        Map<PageModel, Float> relevanceMap = new HashMap<>();
        int i = 0;
        while (i < pageList.size()) {
            PageModel page = pageList.get(i);
            float relevance = 0;
            for (IndexModel index : indexList) {
                if (index.getPage() == page) {
                    relevance += index.getRank();
                }
            }
            relevanceMap.put(page, relevance);
            i++;
        }
        Map<PageModel, Float> allRelevanceMap = new HashMap<>();
        relevanceMap.keySet().forEach(page -> {
            float relevance = relevanceMap.get(page) / Collections.max(relevanceMap.values());
            allRelevanceMap.put(page, relevance);
        });

        List<Entry<PageModel, Float>> sortList = new ArrayList<>(allRelevanceMap.entrySet());
        sortList.sort(Entry.comparingByValue(Comparator.reverseOrder()));
        Map<PageModel, Float> map = new ConcurrentHashMap<>();
        Entry<PageModel, Float> pageModelFloatEntry;
        int y = 0;
        while (y < sortList.size()) {
            pageModelFloatEntry = sortList.get(y);
            map.putIfAbsent(pageModelFloatEntry.getKey(), pageModelFloatEntry.getValue());
            y++;
        }
        return map;
    }

    public List<LemmaModel> getLemmaModelFromSite(List<String> lemmas, SiteModel site) {
        lemmaRepository.flush();
        List<LemmaModel> lemmaModels = lemmaRepository.findLemmaListBySite(lemmas, site);
        List<LemmaModel> result = new ArrayList<>(lemmaModels);
        result.sort(Comparator.comparingInt(LemmaModel::getFrequency));
        return result;
    }

    public List<String> getLemmaFromSearchText(String text) {
        String[] words = text.toLowerCase(Locale.ROOT).split(" ");
        List<String> lemmaList = new ArrayList<>();
        int i = 0;
        List<String> list;
        while (i < words.length) {
            String lemma = words[i];
            try {
                list = lemmaEngine.getLemma(lemma);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            lemmaList.addAll(list);
            i++;
        }
        return lemmaList;
    }

    public List<SearchDTO> createSearchDTOList(List<LemmaModel> lemmaList, List<String> textLemmaList,
                                               int start, int limit, List<SiteModel> sites) {
        List<SearchDTO> result = new ArrayList<>();
        pageRepository.flush();

        List<PageModel> pagesList = new ArrayList<>();
        for (int i = 0; i < textLemmaList.size(); i++) {
            List<String> currentLemma = new ArrayList<>();
            currentLemma.add(textLemmaList.get(i));
            List<LemmaModel> foundLemmaList = new ArrayList<>();
            for (SiteModel siteModel : sites) {
                foundLemmaList.addAll(getLemmaModelFromSite(currentLemma, siteModel));
            }
            if (i == 0) {
                pagesList = pageRepository.findByLemma(foundLemmaList, sites);
            } else {
                pagesList = pageRepository.findByLemmaFromPagesList(foundLemmaList, pagesList, sites);
            }
        }
        indexRepository.flush();

        List<IndexModel> indexesList = indexRepository.findByPageAndLemmas(lemmaList, pagesList);
        Map<PageModel, Float> relevanceMap = getRelevanceFromPage(pagesList, indexesList);
        List<Entry<PageModel, Float>> list = new ArrayList<>(relevanceMap.entrySet());
        list.sort((c1, c2) -> c2.getValue().compareTo(c1.getValue()));
        List<Entry<PageModel, Float>> listWithTreshold = new ArrayList<>(list);
        if (list.size() > limit - start) {
            listWithTreshold = list.subList(start, limit);
        }
        relevanceMap.clear();
        relevanceMap = listWithTreshold.stream().collect(Collectors.toConcurrentMap(Entry::getKey, Entry::getValue));

        List<SearchDTO> searchDtos = getSearchDtoList((ConcurrentHashMap<PageModel, Float>) relevanceMap, textLemmaList);
        if (start > searchDtos.size()) {
            return new ArrayList<>();
        }
        if (searchDtos.size() > limit) {
            int i = start;
            while (i < limit) {
                result.add(searchDtos.get(i));
                i++;
            }
            return result;
        } else return searchDtos;
    }

    public String clearCodeFromTag(String text, String element) {
        Document doc = Jsoup.parse(text);
        Elements elements = doc.select(element);
        String html = elements.stream().map(Element::html).collect(Collectors.joining());
        return Jsoup.parse(html).text();
    }
}
