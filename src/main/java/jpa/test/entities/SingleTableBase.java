package jpa.test.entities;

import java.io.Serializable;
import java.util.*;
import javax.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class SingleTableBase implements Serializable, Base<SingleTableBase, SingleTableEmbeddable> {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private SingleTableBase parent;
    private SingleTableEmbeddable embeddable = new SingleTableEmbeddable();
    private List<SingleTableBase> list = new ArrayList<SingleTableBase>();
    private Set<SingleTableBase> children = new HashSet<SingleTableBase>();
    private Map<String, SingleTableBase> map = new HashMap<String, SingleTableBase>();

    public SingleTableBase() {
    }

    public SingleTableBase(String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    public SingleTableBase getParent() {
        return parent;
    }

    public void setParent(SingleTableBase parent) {
        this.parent = parent;
    }

    @Embedded
    public SingleTableEmbeddable getEmbeddable() {
        return embeddable;
    }

    public void setEmbeddable(SingleTableEmbeddable embeddable) {
        this.embeddable = embeddable;
    }

    @OneToMany
    @OrderColumn(name = "list_idx", nullable = false)
    @JoinTable(name = "single_table_list")
    public List<SingleTableBase> getList() {
        return list;
    }

    public void setList(List<? extends SingleTableBase> list) {
        this.list = (List<SingleTableBase>) list;
    }

    @OneToMany(mappedBy = "parent")
    public Set<SingleTableBase> getChildren() {
        return children;
    }

    public void setChildren(Set<? extends SingleTableBase> children) {
        this.children = (Set<SingleTableBase>) children;
    }

    @OneToMany
    @JoinTable(name = "single_table_map")
    @MapKeyColumn(table = "single_table_map", nullable = false, length = 20)
    public Map<String, SingleTableBase> getMap() {
        return map;
    }

    public void setMap(Map<String, ? extends SingleTableBase> map) {
        this.map = (Map<String, SingleTableBase>) map;
    }
}
