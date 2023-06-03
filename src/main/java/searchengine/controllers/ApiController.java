package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.ResultDTO;
import searchengine.dto.SearchDTO;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.repository.SiteRepository;
import searchengine.search.SearchStarter;
import searchengine.services.StatisticsService;
import searchengine.services.IndexingService;

import java.util.List;

@RestController
@RequestMapping("/api")
public record ApiController(StatisticsService statisticsService, IndexingService indexingService, SiteRepository siteRepository, SearchStarter searchStarter) {

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
        if (url.isEmpty()) {
            //log.info("Страница не указана");
            return new ResultDTO(false, "Страница не указана", HttpStatus.BAD_REQUEST);
        } else {
            if (indexingService.indexPage(url)) {
                //log.info("Страница - " + url + " - добавлена на переиндексацию");
                return new ResultDTO(true, HttpStatus.OK);
            } else {
                //log.info("Указанная страница" + "за пределами конфигурационного файла");
                return new ResultDTO(false, "Указанная страница" + "за пределами конфигурационного файла", HttpStatus.BAD_REQUEST);
            }
        }
    }

    @GetMapping("/search")
    public ResultDTO search(@RequestParam(name = "query", required = false, defaultValue = "") String query,
                            @RequestParam(name = "site", required = false, defaultValue = "") String site,
                            @RequestParam(name = "offset", required = false, defaultValue = "0") int offset) {
        List<SearchDTO> searchData;
        if (!site.isEmpty()) {
            if (siteRepository.findByUrl(site) == null) {

                return new ResultDTO(false, "Данная страница находится за пределами сайтов,\n" +
                        "указанных в конфигурационном файле", HttpStatus.BAD_REQUEST) ;
            } else {
                searchData = searchStarter.getSearchFromOneSite(query, site, offset, 30);
            }
        } else {
            searchData = searchStarter.getFullSearch(query, offset, 30);
        }
        return new ResultDTO(true, searchData.size(), searchData, HttpStatus.OK);
    }
}
