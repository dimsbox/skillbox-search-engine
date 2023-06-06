package searchengine.services;

import searchengine.dto.ResultDTO;

public interface IndexingService {
    ResultDTO startIndexing();

    ResultDTO stopIndexing();

    ResultDTO urlCheckAndPaging(String url);
}
