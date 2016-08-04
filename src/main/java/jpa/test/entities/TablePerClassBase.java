package jpa.test.entities;

import java.io.Serializable;
import java.util.*;
import javax.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class TablePerClassBase implements Serializable, Base<TablePerClassBase> {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private TablePerClassBase parent;
    private Set<TablePerClassBase> children = new HashSet<TablePerClassBase>();

    public TablePerClassBase() {
    }

    public TablePerClassBase(String name) {
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
    public TablePerClassBase getParent() {
        return parent;
    }

    public void setParent(TablePerClassBase parent) {
        this.parent = parent;
    }

    @OneToMany(mappedBy = "parent")
    public Set<TablePerClassBase> getChildren() {
        return children;
    }

    public void setChildren(Set<? extends TablePerClassBase> children) {
        this.children = (Set<TablePerClassBase>) children;
    }
}
