package searchengine.dto.statistics;

import lombok.Data;

public record TotalStatistics(long sites, long pages, long lemmas, boolean indexing) {

}
