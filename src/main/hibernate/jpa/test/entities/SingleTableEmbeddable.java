package jpa.test.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

@Embeddable
public class SingleTableEmbeddable implements BaseEmbeddable<SingleTableBase>, Serializable {
    private static final long serialVersionUID = 1L;

    private SingleTableBase parent;
    private List<SingleTableBase> list = new ArrayList<>();
    private Set<SingleTableBase> children = new HashSet<>();
    private Map<String, SingleTableBase> map = new HashMap<>();

    public SingleTableEmbeddable() {
    }

    public SingleTableEmbeddable(SingleTableBase parent) {
        this.parent = parent;
    }

    @Override
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "embeddableParent")
    public SingleTableBase getParent() {
        return parent;
    }

    @Override
    public void setParent(SingleTableBase parent) {
        this.parent = parent;
    }

    @Override
    @ManyToMany
    @OrderColumn(name = "list_idx", nullable = false)
    @JoinTable(name = "single_table_embeddable_list")
    public List<SingleTableBase> getList() {
        return list;
    }

    @Override
    public void setList(List<? extends SingleTableBase> list) {
        this.list = (List<SingleTableBase>) list;
    }

    @Override
    @OneToMany
    @JoinColumn(name = "embeddableParent")
    public Set<SingleTableBase> getChildren() {
        return children;
    }

    @Override
    public void setChildren(Set<? extends SingleTableBase> children) {
        this.children = (Set<SingleTableBase>) children;
    }

    @Override
    @ManyToMany
    @JoinTable(name = "single_table_embeddable_map")
    @MapKeyColumn(name = "stem_map_key", nullable = false, length = 20)
    public Map<String, SingleTableBase> getMap() {
        return map;
    }

    @Override
    public void setMap(Map<String, ? extends SingleTableBase> map) {
        this.map = (Map<String, SingleTableBase>) map;
    }
}
