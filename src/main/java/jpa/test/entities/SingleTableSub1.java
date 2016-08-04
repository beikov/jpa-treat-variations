package jpa.test.entities;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

@Entity
public class SingleTableSub1 extends SingleTableBase implements Sub1<SingleTableBase> {
    private static final long serialVersionUID = 1L;

    private IntIdEntity relation1;
    private SingleTableBase parent1;
    private Integer sub1Value;

    public SingleTableSub1() {
    }

    public SingleTableSub1(String name) {
        super(name);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    public IntIdEntity getRelation1() {
        return relation1;
    }

    public void setRelation1(IntIdEntity relation1) {
        this.relation1 = relation1;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    public SingleTableBase getParent1() {
        return parent1;
    }

    public void setParent1(SingleTableBase parent1) {
        this.parent1 = parent1;
    }

    public Integer getSub1Value() {
        return sub1Value;
    }

    public void setSub1Value(Integer sub1Value) {
        this.sub1Value = sub1Value;
    }
}
