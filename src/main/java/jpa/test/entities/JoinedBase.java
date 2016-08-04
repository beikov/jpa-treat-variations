package jpa.test.entities;

import java.io.Serializable;
import java.util.*;
import javax.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class JoinedBase implements Serializable, Base<JoinedBase> {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private JoinedBase parent;
    private List<JoinedBase> list = new ArrayList<JoinedBase>();
    private Set<JoinedBase> children = new HashSet<JoinedBase>();
    private Map<String, JoinedBase> map = new HashMap<String, JoinedBase>();

    public JoinedBase() {
    }

    public JoinedBase(String name) {
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
    public JoinedBase getParent() {
        return parent;
    }

    public void setParent(JoinedBase parent) {
        this.parent = parent;
    }

    @OneToMany
    @OrderColumn(name = "list_idx", nullable = false)
    @JoinTable(name = "joined_list")
    public List<JoinedBase> getList() {
        return list;
    }

    public void setList(List<? extends JoinedBase> list) {
        this.list = (List<JoinedBase>) list;
    }

    @OneToMany(mappedBy = "parent")
    public Set<JoinedBase> getChildren() {
        return children;
    }

    public void setChildren(Set<? extends JoinedBase> children) {
        this.children = (Set<JoinedBase>) children;
    }

    @OneToMany
    @JoinTable(name = "joined_map")
    @MapKeyColumn(table = "joined_map", nullable = false, length = 20)
    public Map<String, JoinedBase> getMap() {
        return map;
    }

    public void setMap(Map<String, ? extends JoinedBase> map) {
        this.map = (Map<String, JoinedBase>) map;
    }
}
