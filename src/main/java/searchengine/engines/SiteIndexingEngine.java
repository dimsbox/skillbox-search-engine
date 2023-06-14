package searchengine.engines;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.lemmaindexer.LemmaIndexer;
import searchengine.pagesearch.PageSearcher;
import searchengine.parser.WebParser;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.IndexDTO;
import searchengine.dto.LemmaDTO;
import searchengine.dto.PageDTO;
import searchengine.exception.CurrentInterruptedException;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
@Slf4j
public class SiteIndexingEngine implements Callable<Boolean> {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaIndexer lemmaIndexer;
    private final WebParser webParser;
    private final String url;
    private final SitesList sitesListConfiguration;


    @Override
    public Boolean call() {
        if (siteRepository.findByUrl(url) != null) {
            log.info("start site data delete from ".concat(url));
            SiteModel site = siteRepository.findByUrl(url);
            site.setStatus(Status.INDEXING);
            site.setName(getSiteName());
            site.setStatusTime(new Date());
            siteRepository.saveAndFlush(site);
            siteRepository.delete(site);
        }
        log.info("Site indexing start ".concat(url).concat(" ").concat(getSiteName()) );
        SiteModelIndexing siteModelIndexing = new SiteModelIndexing();
        SiteModel site = siteModelIndexing.getSiteModelRecord();
        try {
            if (!Thread.interrupted()) {
                List<PageDTO> pageDtoList;
                if (!Thread.interrupted()) {
                    String urls = url.concat("/");
                    List<PageDTO> pageDtosList = new CopyOnWriteArrayList<>();
                    List<String> urlList = new CopyOnWriteArrayList<>();
                    ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
                    List<PageDTO> pages = forkJoinPool.invoke(new PageSearcher(urls,urlList, pageDtosList, sitesListConfiguration));
                    log.info(pages.size() + " pages found in " + url);
                    pageDtoList = new CopyOnWriteArrayList<>(pages);
                } else throw new CurrentInterruptedException("Fork join exception!");
                List<PageModel> pageList = new CopyOnWriteArrayList<>();
                int start;
                String pagePath;
                for (PageDTO page : pageDtoList) {
                    start = checkUrlWww(page, url);
                    pagePath = page.url().substring(start);
                    pageList.add(new PageModel(site, pagePath, page.code(), page.content()));
                }
                pageRepository.saveAllAndFlush(pageList);
                log.info("All pages saved " + url);
            } else {
                throw new CurrentInterruptedException("Local interrupted exception.");
            }
            new LemmaIndexing().saveLemmasInLemmaDTO();
            log.info("All lemmas saved " + url);
            new AllSiteIndexing().getSiteAllIndexing(site);
        } catch (CurrentInterruptedException e) {
            log.error("WebParser stopped from ".concat(url).concat(". ").concat(e.getMessage()));
            new SiteModelIndexing().getErrorSiteModelRecord(site);
            new CurrentInterruptedException("Interrupted exception");
        }
        return true;
    }

    private int checkUrlWww(PageDTO page, String url) {
        if (!page.url().contains(url)) {
            String urlWithWww = url.replaceFirst("://","://www.");
            return page.url().indexOf(urlWithWww) + urlWithWww.length();
        }
        return page.url().indexOf(url) + url.length();
    }

    private String getSiteName() {
        return sitesListConfiguration.getSites().stream()
                .filter(site -> site.getUrl().equals(url))
                .findFirst()
                .map(Site::getName)
                .orElse("");
    }

    private class SiteModelIndexing {
        protected SiteModel getSiteModelRecord() {
            SiteModel site = new SiteModel();
            site.setUrl(url);
            site.setName(getSiteName());
            site.setStatus(Status.INDEXING);
            site.setStatusTime(new Date());
            siteRepository.saveAndFlush(site);
            return site;
        }

        protected void getErrorSiteModelRecord(SiteModel site) {
            SiteModel sites = new SiteModel();
            sites.setLastError("WebParser stopped");
            sites.setStatus(Status.FAILED);
            sites.setStatusTime(new Date());
            siteRepository.saveAndFlush(site);
        }
    }
    private class LemmaIndexing {

        protected void saveLemmasInLemmaDTO() throws CurrentInterruptedException {
            if (!Thread.interrupted()) {
                SiteModel siteModel = siteRepository.findByUrl(url);
                siteModel.setStatusTime(new Date());
                lemmaIndexer.startLemmaIndexer(siteModel);
                List<LemmaDTO> lemmaDtoList = lemmaIndexer.getLemmaDtoList();
                List<LemmaModel> lemmaList = new CopyOnWriteArrayList<>();
                for (LemmaDTO lemmaDto : lemmaDtoList) {
                    lemmaList.add(new LemmaModel(lemmaDto.lemma(), lemmaDto.frequency(), siteModel));
                }
                lemmaRepository.saveAllAndFlush(lemmaList);
            } else {
                throw new CurrentInterruptedException("Invalid saveLemmasInLemmaDTO");
            }
        }
    }

    private class AllSiteIndexing {
        protected void getSiteAllIndexing(SiteModel site) throws CurrentInterruptedException {
            if (!Thread.interrupted()) {
                webParser.startWebParser(site);
                List<IndexDTO> indexDtoList = new CopyOnWriteArrayList<>(webParser.getIndexDTOList());
                List<IndexModel> indexModels = new CopyOnWriteArrayList<>();
                site.setStatusTime(new Date());
                PageModel page;
                LemmaModel lemma;
                log.info(site.getUrl() + " indexList size: " + indexDtoList.size());
                int count = 1;
                for (IndexDTO indexDto : indexDtoList) {
                    page = pageRepository.getById(indexDto.pageID());
                    lemma = lemmaRepository.getById(indexDto.lemmaID());
                    indexModels.add(new IndexModel(page, lemma, indexDto.rank()));
                    counter(count, indexDtoList.size());
                    count++;
                }
                log.info("indexModels filled");
                indexRepository.saveAllAndFlush(indexModels);
                log.info("WebParser stopping ".concat(url));
                site.setStatusTime(new Date());
                site.setStatus(Status.INDEXED);
                siteRepository.saveAndFlush(site);

            } else {
                throw new CurrentInterruptedException("Invalid getSiteAllIndexing");
            }
        }

        private void counter(int count, int size) {
            if (count % 100 == 0) {
                System.out.print(count + " from " + size + "\r");
            }
        }
    }
}

