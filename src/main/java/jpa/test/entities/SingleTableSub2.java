package jpa.test.entities;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

@Entity
public class SingleTableSub2 extends SingleTableBase implements Sub2<SingleTableBase> {
    private static final long serialVersionUID = 1L;

    private IntIdEntity relation2;
    private SingleTableBase parent2;
    private Integer sub2Value;

    public SingleTableSub2() {
    }

    public SingleTableSub2(String name) {
        super(name);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    public IntIdEntity getRelation2() {
        return relation2;
    }

    public void setRelation2(IntIdEntity relation2) {
        this.relation2 = relation2;
    }
    @ManyToOne(fetch = FetchType.LAZY)
    public SingleTableBase getParent2() {
        return parent2;
    }

    public void setParent2(SingleTableBase parent2) {
        this.parent2 = parent2;
    }

    public Integer getSub2Value() {
        return sub2Value;
    }

    public void setSub2Value(Integer sub2Value) {
        this.sub2Value = sub2Value;
    }
}
