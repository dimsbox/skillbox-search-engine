package searchengine.services;

import searchengine.dto.ResultDTO;

public interface SearchService {

    ResultDTO searchSiteSelect(String query, String site, int offset);
}
