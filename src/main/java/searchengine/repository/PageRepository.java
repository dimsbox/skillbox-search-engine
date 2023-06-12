package searchengine.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;

import java.util.List;


@Repository
public interface PageRepository extends JpaRepository<PageModel, Long> {
    @Transactional
    long countBySiteId(SiteModel siteId);

    @Transactional
    PageModel getById(long pageID);

    @Transactional
    Iterable<PageModel> findBySiteId(SiteModel sitePath);

    @Transactional
    @Query(value = "SELECT * FROM Page JOIN Words_index ON Page.id = Words_index.page_id WHERE Words_index.lemma_id IN (:lemma) AND Page.site_id IN (:siteList)", nativeQuery = true)
    List<PageModel> findByLemma(@Param("lemma") List<LemmaModel> lemma, @Param("siteList") List<SiteModel> siteList);

    @Transactional
    @Query(value = "SELECT * FROM Page JOIN Words_index ON Page.id = Words_index.page_id WHERE Words_index.lemma_id IN (:lemma) AND Page.id IN (:pagesList) AND Page.site_id IN (:siteList)", nativeQuery = true)
    List<PageModel> findByLemmaFromPagesList(@Param("lemma") List<LemmaModel> lemma, @Param("pagesList") List<PageModel> pages,
                                             @Param("siteList") List<SiteModel> siteList);

}
