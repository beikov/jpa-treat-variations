package jpa.test.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

@Entity
public class SingleTableSub2 extends SingleTableBase implements Sub2<SingleTableBase, SingleTableEmbeddable, SingleTableEmbeddableSub2> {
    private static final long serialVersionUID = 1L;

    private IntIdEntity relation2;
    private SingleTableBase parent2;
    private Integer sub2Value;
    private IntValueEmbeddable sub2Embeddable = new IntValueEmbeddable();
    private SingleTableEmbeddableSub2 embeddable2 = new SingleTableEmbeddableSub2();
    private List<SingleTableBase> list2 = new ArrayList<>();
    private Set<SingleTableSub2> children2 = new HashSet<>();
    private Map<String, SingleTableBase> map2 = new HashMap<>();

    public SingleTableSub2() {
    }

    public SingleTableSub2(String name) {
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
    public SingleTableBase getParent2() {
        return parent2;
    }

    @Override
    public void setParent2(SingleTableBase parent2) {
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
    @AttributeOverride(name = "someValue", column = @Column(name = "someValue1"))
    public IntValueEmbeddable getSub2Embeddable() {
        return sub2Embeddable;
    }

    @Override
    public void setSub2Embeddable(IntValueEmbeddable sub2Embeddable) {
        this.sub2Embeddable = sub2Embeddable;
    }

    @Override
    @Embedded
    public SingleTableEmbeddableSub2 getEmbeddable2() {
        return embeddable2;
    }

    @Override
    public void setEmbeddable2(SingleTableEmbeddableSub2 embeddable2) {
        this.embeddable2 = embeddable2;
    }

    @Override
    @ManyToMany
    @OrderColumn(name = "list_idx", nullable = false)
    @JoinTable(name = "single_table_list_2")
    public List<SingleTableBase> getList2() {
        return list2;
    }

    @Override
    public void setList2(List<? extends SingleTableBase> list2) {
        this.list2 = (List<SingleTableBase>) list2;
    }

    @Override
    @OneToMany(mappedBy = "parent2")
    public Set<SingleTableSub2> getChildren2() {
        return children2;
    }

    @Override
    public void setChildren2(Set<? extends SingleTableBase> children2) {
        this.children2 = (Set<SingleTableSub2>) children2;
    }

    @Override
    @ManyToMany
    @JoinTable(name = "single_table_map_2")
    @MapKeyColumn(name = "stm2_map_key", nullable = false, length = 20)
    public Map<String, SingleTableBase> getMap2() {
        return map2;
    }

    @Override
    public void setMap2(Map<String, ? extends SingleTableBase> map2) {
        this.map2 = (Map<String, SingleTableBase>) map2;
    }
}
