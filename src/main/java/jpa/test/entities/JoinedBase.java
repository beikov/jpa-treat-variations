package jpa.test.entities;

import java.io.Serializable;
import java.util.*;
import javax.persistence.*;

@Entity
@Table(name = "joined_base")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class JoinedBase implements Serializable, Base<JoinedBase, JoinedEmbeddable> {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Integer value;
    private JoinedBase parent;
    private JoinedEmbeddable embeddable = new JoinedEmbeddable();
    private List<JoinedBase> list = new ArrayList<>();
    private Set<JoinedBase> children = new HashSet<>();
    private Map<JoinedBase, JoinedBase> map = new HashMap<>();

    public JoinedBase() {
    }

    public JoinedBase(String name) {
        this.name = name;
    }

    @Id
    @Override
    @GeneratedValue
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public void setValue(Integer value) {
        this.value = value;
    }

    @Override
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    public JoinedBase getParent() {
        return parent;
    }

    @Override
    public void setParent(JoinedBase parent) {
        this.parent = parent;
    }

    @Override
    @Embedded
    public JoinedEmbeddable getEmbeddable() {
        return embeddable;
    }

    @Override
    public void setEmbeddable(JoinedEmbeddable embeddable) {
        this.embeddable = embeddable;
    }

    @Override
    @ManyToMany
    @OrderColumn(name = "list_idx", nullable = false)
    @JoinTable(name = "joined_list")
    public List<JoinedBase> getList() {
        return list;
    }

    @Override
    public void setList(List<? extends JoinedBase> list) {
        this.list = (List<JoinedBase>) list;
    }

    @Override
    @OneToMany(mappedBy = "parent")
    public Set<JoinedBase> getChildren() {
        return children;
    }

    @Override
    public void setChildren(Set<? extends JoinedBase> children) {
        this.children = (Set<JoinedBase>) children;
    }

    @Override
    @ManyToMany
    @JoinTable(name = "joined_map")
    @MapKeyColumn(name = "jm_map_key", nullable = false, length = 20)
    public Map<JoinedBase, JoinedBase> getMap() {
        return map;
    }

    @Override
    public void setMap(Map<? extends JoinedBase, ? extends JoinedBase> map) {
        this.map = (Map<JoinedBase, JoinedBase>) map;
    }
}
