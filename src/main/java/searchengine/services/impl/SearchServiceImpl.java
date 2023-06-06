package searchengine.services.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.dto.ResultDTO;
import searchengine.dto.SearchDTO;
import searchengine.model.LemmaModel;
import searchengine.model.SiteModel;
import searchengine.repository.SiteRepository;
import searchengine.engines.SearchEngine;
import searchengine.services.SearchService;

import java.util.ArrayList;
import java.util.List;

@Service
public record SearchServiceImpl(SiteRepository siteRepository, SearchEngine searchEngine) implements SearchService {

    public List<SearchDTO> getSearchFromOneSite(String text, String url, int start, int limit) {
        SiteModel site = siteRepository.findByUrl(url);
        List<String> textLemmaList = searchEngine.getLemmaFromSearchText(text);
        List<LemmaModel> foundLemmaList = searchEngine.getLemmaModelFromSite(textLemmaList, site);
        List<SiteModel> sites = new ArrayList<>();
        sites.add(site);
        return searchEngine.createSearchDTOList(foundLemmaList, textLemmaList, start, limit, sites);
    }

    public List<SearchDTO> getFullSearch(String text, int start, int limit) {
        List<SiteModel> siteList = siteRepository.findAll();
        List<SearchDTO> result = new ArrayList<>();
        List<LemmaModel> foundLemmaList = new ArrayList<>();
        List<String> textLemmaList = searchEngine.getLemmaFromSearchText(text);

        int i = 0;
        while (i < siteList.size()) {
            SiteModel site = siteList.get(i);
            foundLemmaList.addAll(searchEngine.getLemmaModelFromSite(textLemmaList, site));
            i++;
        }

        List<SearchDTO> searchData = new ArrayList<>();
        for (LemmaModel l : foundLemmaList) {
            searchData = searchEngine.createSearchDTOList(foundLemmaList, textLemmaList, start, limit, siteList);
            searchData.sort((o1, o2) -> Float.compare(o2.relevance(), o1.relevance()));
            if (searchData.size() > limit) {
                int y = start;
                while (y < limit) {
                    result.add(searchData.get(y));
                    y++;
                }
                return result;
            }
        }
        return searchData;
    }
    @Override
    public ResultDTO searchSiteSelect(String query, String site, int offset) {
        List<SearchDTO> searchData;
        if (!site.isEmpty()) {
            if (siteRepository.findByUrl(site) == null) {

                return new ResultDTO(false, "Данная страница находится за пределами сайтов,\n" +
                        "указанных в конфигурационном файле", HttpStatus.BAD_REQUEST) ;
            } else {
                searchData = getSearchFromOneSite(query, site, offset, 30);
            }
        } else {
            searchData = getFullSearch(query, offset, 30);
        }
        return new ResultDTO(true, searchData.size(), searchData, HttpStatus.OK);
    }
}
