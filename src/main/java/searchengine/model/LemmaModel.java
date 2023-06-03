package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "lemma")
public class LemmaModel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private SiteModel siteModelId;

    private String lemma;

    private int frequency;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL)
    private List<IndexModel> index = new ArrayList<>();

    public LemmaModel(String lemma, int frequency, SiteModel siteModelId) {
        this.lemma = lemma;
        this.frequency = frequency;
        this.siteModelId = siteModelId;
    }

    public LemmaModel() {
    }
}
