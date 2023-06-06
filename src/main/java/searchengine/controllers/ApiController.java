package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.ResultDTO;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.repository.SiteRepository;
import searchengine.services.impl.SearchServiceImpl;
import searchengine.services.impl.StatisticsServiceImpl;
import searchengine.services.impl.IndexingServiceImpl;

@RestController
@RequestMapping("/api")
public record ApiController(StatisticsServiceImpl statisticsService, IndexingServiceImpl indexingService, SiteRepository siteRepository, SearchServiceImpl searchService) {

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatisticsResponse());
    }

    @GetMapping("/startIndexing")
    public ResultDTO startIndexing() {
        return indexingService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public ResultDTO stopIndexing() {
        //log.info("ОСТАНОВКА ИНДЕКСАЦИИ");
        return indexingService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public ResultDTO indexPage(@RequestParam(name = "url") String url) {
        return indexingService.urlCheckAndPaging(url);
    }

    @GetMapping("/search")
    public ResultDTO search(@RequestParam(name = "query", required = false, defaultValue = "") String query,
                            @RequestParam(name = "site", required = false, defaultValue = "") String site,
                            @RequestParam(name = "offset", required = false, defaultValue = "0") int offset) {
        return searchService.searchSiteSelect(query, site, offset);
    }
}
