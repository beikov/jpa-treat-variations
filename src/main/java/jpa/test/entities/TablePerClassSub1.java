package jpa.test.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

@Entity
public class TablePerClassSub1 extends TablePerClassBase implements Sub1<TablePerClassBase> {
    private static final long serialVersionUID = 1L;

    private IntIdEntity relation1;
    private TablePerClassBase parent1;
    private Integer sub1Value;
    private List<TablePerClassBase> list = new ArrayList<TablePerClassBase>();
    private Map<String, TablePerClassBase> map = new HashMap<String, TablePerClassBase>();

    public TablePerClassSub1() {
    }

    public TablePerClassSub1(String name) {
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
    public TablePerClassBase getParent1() {
        return parent1;
    }

    public void setParent1(TablePerClassBase parent1) {
        this.parent1 = parent1;
    }

    public Integer getSub1Value() {
        return sub1Value;
    }

    public void setSub1Value(Integer sub1Value) {
        this.sub1Value = sub1Value;
    }

    @OneToMany
    @OrderColumn(name = "list_idx", nullable = false)
    @JoinTable(name = "table_per_class_sub_1_list")
    public List<TablePerClassBase> getList() {
        return list;
    }

    public void setList(List<? extends TablePerClassBase> list) {
        this.list = (List<TablePerClassBase>) list;
    }
    
    @OneToMany
    @JoinTable(name = "table_per_class_sub_1_map")
    @MapKeyColumn(table = "table_per_class_sub_1_map", nullable = false, length = 20)
    public Map<String, TablePerClassBase> getMap() {
        return map;
    }

    public void setMap(Map<String, ? extends TablePerClassBase> map) {
        this.map = (Map<String, TablePerClassBase>) map;
    }
}
