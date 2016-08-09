package jpa.test.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.ConstraintMode;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

@Entity
@Table(name = "table_per_class_sub_2")
@AssociationOverrides({
    @AssociationOverride(
            name = "embeddable.list",
            joinTable = @JoinTable(name = "table_per_class_embeddable_sub_2_list", inverseForeignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    ),
    @AssociationOverride(
            name = "embeddable.map",
            joinTable = @JoinTable(name = "table_per_class_embeddable_sub_2_map", inverseForeignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    )
})
public class TablePerClassSub2 extends TablePerClassBase implements Sub2<TablePerClassBase, TablePerClassEmbeddable, TablePerClassEmbeddableSub2> {
    private static final long serialVersionUID = 1L;

    private IntIdEntity relation2;
    private TablePerClassBase parent2;
    private Integer sub2Value;
    private IntValueEmbeddable sub2Embeddable = new IntValueEmbeddable();
    private List<TablePerClassBase> list = new ArrayList<>();
    private Map<String, TablePerClassBase> map = new HashMap<>();
    private TablePerClassEmbeddableSub2 embeddable2 = new TablePerClassEmbeddableSub2();
    private List<TablePerClassBase> list2 = new ArrayList<>();
    private Set<TablePerClassBase> children2 = new HashSet<>();
    private Map<String, TablePerClassBase> map2 = new HashMap<>();

    public TablePerClassSub2() {
    }

    public TablePerClassSub2(String name) {
        super(name);
    }

    @Override
    @ManyToOne(fetch = FetchType.LAZY)
    public IntIdEntity getRelation2() {
        return relation2;
    }

    @Override
    public void setRelation2(IntIdEntity relation2) {
        this.relation2 = relation2;
    }
    
    @Override
    @ManyToOne(fetch = FetchType.LAZY)
    // We can't have a constraint in this case because we don't know the exact table this will refer to
    @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    public TablePerClassBase getParent2() {
        return parent2;
    }

    @Override
    public void setParent2(TablePerClassBase parent2) {
        this.parent2 = parent2;
    }

    @Override
    public Integer getSub2Value() {
        return sub2Value;
    }

    @Override
    public void setSub2Value(Integer sub2Value) {
        this.sub2Value = sub2Value;
    }

    @Override
    @Embedded
    public IntValueEmbeddable getSub2Embeddable() {
        return sub2Embeddable;
    }

    @Override
    public void setSub2Embeddable(IntValueEmbeddable sub2Embeddable) {
        this.sub2Embeddable = sub2Embeddable;
    }

    @Override
    @Embedded
    public TablePerClassEmbeddableSub2 getEmbeddable2() {
        return embeddable2;
    }

    @Override
    public void setEmbeddable2(TablePerClassEmbeddableSub2 embeddable2) {
        this.embeddable2 = embeddable2;
    }

    @Override
    @ManyToMany
    @OrderColumn(name = "list_idx", nullable = false)
    // We can't have a constraint in this case because we don't know the exact table this will refer to
    @JoinTable(name = "table_per_class_sub_2_list", inverseForeignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    public List<TablePerClassBase> getList() {
        return list;
    }

    @Override
    public void setList(List<? extends TablePerClassBase> list) {
        this.list = (List<TablePerClassBase>) list;
    }
    
    @Override
    @ManyToMany
    // We can't have a constraint in this case because we don't know the exact table this will refer to
    @JoinTable(name = "table_per_class_sub_2_map", inverseForeignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @MapKeyColumn(name = "tpcs2m_table_per_class_sub_2_map", nullable = false, length = 20)
    public Map<String, TablePerClassBase> getMap() {
        return map;
    }

    @Override
    public void setMap(Map<String, ? extends TablePerClassBase> map) {
        this.map = (Map<String, TablePerClassBase>) map;
    }

    @Override
    @ManyToMany
    @OrderColumn(name = "list_idx", nullable = false)
    // We can't have a constraint in this case because we don't know the exact table this will refer to
    @JoinTable(name = "table_per_class_sub_2_list_2", inverseForeignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    public List<TablePerClassBase> getList2() {
        return list2;
    }

    @Override
    public void setList2(List<? extends TablePerClassBase> list2) {
        this.list2 = (List<TablePerClassBase>) list2;
    }

    @Override
    @OneToMany(mappedBy = "parent2", targetEntity = TablePerClassSub2.class)
    public Set<TablePerClassBase> getChildren2() {
        return children2;
    }

    @Override
    public void setChildren2(Set<? extends TablePerClassBase> children2) {
        this.children2 = (Set<TablePerClassBase>) children2;
    }
    
    @Override
    @ManyToMany
    // We can't have a constraint in this case because we don't know the exact table this will refer to
    @JoinTable(name = "table_per_class_sub_2_map_2", inverseForeignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @MapKeyColumn(name = "tpcs2m2_map_key", nullable = false, length = 20)
    public Map<String, TablePerClassBase> getMap2() {
        return map2;
    }

    @Override
    public void setMap2(Map<String, ? extends TablePerClassBase> map2) {
        this.map2 = (Map<String, TablePerClassBase>) map2;
    }
}
