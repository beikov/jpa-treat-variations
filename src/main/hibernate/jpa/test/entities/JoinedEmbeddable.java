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
public class JoinedEmbeddable implements BaseEmbeddable<JoinedBase>, Serializable {
    private static final long serialVersionUID = 1L;

    private JoinedBase parent;
    private List<JoinedBase> list = new ArrayList<>();
    private Set<JoinedBase> children = new HashSet<>();
    private Map<String, JoinedBase> map = new HashMap<>();

    public JoinedEmbeddable() {
    }

    public JoinedEmbeddable(JoinedBase parent) {
        this.parent = parent;
    }

    @Override
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "embeddableParent")
    public JoinedBase getParent() {
        return parent;
    }

    @Override
    public void setParent(JoinedBase parent) {
        this.parent = parent;
    }

    @Override
    @ManyToMany
    @OrderColumn(name = "list_idx", nullable = false)
    @JoinTable(name = "joined_embeddable_list")
    public List<JoinedBase> getList() {
        return list;
    }

    @Override
    public void setList(List<? extends JoinedBase> list) {
        this.list = (List<JoinedBase>) list;
    }

    @Override
    @OneToMany
    @JoinColumn(name = "embeddableParent")
    public Set<JoinedBase> getChildren() {
        return children;
    }

    @Override
    public void setChildren(Set<? extends JoinedBase> children) {
        this.children = (Set<JoinedBase>) children;
    }

    @Override
    @ManyToMany
    @JoinTable(name = "joined_embeddable_map")
    @MapKeyColumn(name = "jem_map_key", nullable = false, length = 20)
    public Map<String, JoinedBase> getMap() {
        return map;
    }

    @Override
    public void setMap(Map<String, ? extends JoinedBase> map) {
        this.map = (Map<String, JoinedBase>) map;
    }
}
