package searchengine.components;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.SitesList;
import searchengine.dto.PageDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;


@Slf4j
@RequiredArgsConstructor
public class PageSearcher extends RecursiveTask<List<PageDTO>> {
    private final String url;
    private final List<String> urlList;
    private final List<PageDTO> pageDtoList;
    private final SitesList config;

    @Override
    protected List<PageDTO> compute() {
        try {
            Thread.sleep(100);
            Document doc = null;
            try {
                Thread.sleep(100);
                doc = Jsoup.connect(url)
                        .userAgent(config.getUserAgent())
                        .referrer(config.getReferrer())
                        .get();
            } catch (Exception e) {
                e.getMessage();
            }
            assert doc != null;
            String html = doc.outerHtml();
            Connection.Response response = doc.connection().response();
            int status = response.statusCode();
            PageDTO pageDto = new PageDTO(url, html, status);
            pageDtoList.add(pageDto);
            Elements elements = doc.select("body")
                    .select("a");
            List<PageSearcher> taskList = new ArrayList<>();
            String link;
            PageSearcher task;
            for (Element el : elements) {
                link = el.attr("abs:href");
                if (isSiteElementsType(link)
                        && link.startsWith(el.baseUri())
                        && !link.equals(el.baseUri())
                        && !link.contains("#")
                        && !urlList.contains(link)) {
                    urlList.add(link);
                    task = new PageSearcher(link, urlList, pageDtoList, config);
                    task.fork();
                    taskList.add(task);
                }
            }
            taskList.forEach(ForkJoinTask::join);
        } catch (Exception e) {
            log.debug("Error parsing from ".concat(url));
            PageDTO pageDto = new PageDTO(url, "", 500);
            pageDtoList.add(pageDto);
        }
        return pageDtoList;
    }

    private boolean isSiteElementsType(String pathPage) {
        List<String> WRONG_TYPES = Arrays.asList(
                "JPG", "gif", "gz", "jar", "jpeg", "jpg", "pdf", "png", "ppt", "pptx", "svg", "svg", "tar", "zip");
        return !WRONG_TYPES.contains(pathPage.substring(pathPage.lastIndexOf(".") + 1));
    }


}