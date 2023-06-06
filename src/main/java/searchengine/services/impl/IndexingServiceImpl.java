package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.components.LemmaIndexer;
import searchengine.components.WebParser;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.ResultDTO;
import searchengine.engines.SiteIndexingEngine;
import searchengine.model.SiteModel;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static searchengine.model.Status.INDEXING;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {
    private ExecutorService executorService;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaIndexer lemmaIndexer;
    private final WebParser webParser;
    private final SitesList config;
    @Override
    public ResultDTO startIndexing() {
        if (isIndexingActive()) {
            log.debug("Indexing is already running.");
            return new ResultDTO(false, "Индексация уже запущена");

        } else {

            List<Site> siteList = config.getSites();
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            for (Site site : siteList) {
                String url = site.getUrl();
                SiteModel siteModel = new SiteModel();
                siteModel.setName(site.getName());
                log.info("Indexing web site ".concat(site.getName()));
                executorService.submit(new SiteIndexingEngine(pageRepository, siteRepository, lemmaRepository,
                        indexRepository, lemmaIndexer, webParser, url, config));
            }
            executorService.shutdown();
            log.info("Все сайты проиндексированы");
        }
        return new ResultDTO(true);
    }
    @Override
    public ResultDTO stopIndexing() {
        if (!isIndexingActive()) {
            log.info("Site indexing is already running!");
            return new ResultDTO(false, "Индексация не запущена");
        } else {
            log.info("Index stopping.");
            executorService.shutdown();
            return new ResultDTO(true);
        }
    }

    private boolean isIndexingActive() {
        siteRepository.flush();
        Iterable<SiteModel> siteList = siteRepository.findAll();
        for (SiteModel site : siteList) {
            if (site.getStatus() == INDEXING) {
                return true;
            }
        }
        return false;
    }

    public boolean indexPage(String urlPage) {

        if (isUrlSiteEquals(urlPage)) {
            log.info("Начата переиндексация сайта - " + urlPage);
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            executorService.submit(new SiteIndexingEngine(pageRepository, siteRepository, lemmaRepository, indexRepository, lemmaIndexer, webParser, urlPage, config));
            executorService.shutdown();
            return true;
        } else {
            return false;
        }

    }

    private boolean isUrlSiteEquals(String url) {
        return config.getSites().stream().anyMatch(site ->  site.getUrl().equals(url));
    }
    @Override
    public ResultDTO urlCheckAndPaging(String url) {
        if (url.isEmpty()) {
            //log.info("Страница не указана");
            return new ResultDTO(false, "Страница не указана", HttpStatus.BAD_REQUEST);
        } else {
            if (indexPage(url)) {
                //log.info("Страница - " + url + " - добавлена на переиндексацию");
                return new ResultDTO(true, HttpStatus.OK);
            } else {
                //log.info("Указанная страница" + "за пределами конфигурационного файла");
                return new ResultDTO(false, "Указанная страница" + "за пределами конфигурационного файла", HttpStatus.BAD_REQUEST);
            }
        }
    }

}
