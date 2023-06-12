package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "page")
public class PageModel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "site_id", nullable = false, referencedColumnName = "id")
    private SiteModel siteId;

    @Column(columnDefinition = "VARCHAR(256)", length = 256, nullable = false)
    private String path;
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
    private List<IndexModel> index = new LinkedList<>();

    public PageModel(SiteModel siteId, String path, int code, String content) {
        this.siteId = siteId;
        this.path = path;
        this.code = code;
        this.content = content;
    }

    public PageModel() {
    }
}
