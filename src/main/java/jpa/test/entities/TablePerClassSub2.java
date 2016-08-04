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
public class TablePerClassSub2 extends TablePerClassBase implements Sub2<TablePerClassBase> {
    private static final long serialVersionUID = 1L;

    private IntIdEntity relation2;
    private TablePerClassBase parent2;
    private Integer sub2Value;
    private List<TablePerClassBase> list = new ArrayList<TablePerClassBase>();
    private Map<String, TablePerClassBase> map = new HashMap<String, TablePerClassBase>();

    public TablePerClassSub2() {
    }

    public TablePerClassSub2(String name) {
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
    public TablePerClassBase getParent2() {
        return parent2;
    }

    public void setParent2(TablePerClassBase parent2) {
        this.parent2 = parent2;
    }

    public Integer getSub2Value() {
        return sub2Value;
    }

    public void setSub2Value(Integer sub2Value) {
        this.sub2Value = sub2Value;
    }

    @OneToMany
    @OrderColumn(name = "list_idx", nullable = false)
    @JoinTable(name = "table_per_class_sub_2_list")
    public List<TablePerClassBase> getList() {
        return list;
    }

    public void setList(List<? extends TablePerClassBase> list) {
        this.list = (List<TablePerClassBase>) list;
    }
    
    @OneToMany
    @JoinTable(name = "table_per_class_sub_2_map")
    @MapKeyColumn(table = "table_per_class_sub_2_map", nullable = false, length = 20)
    public Map<String, TablePerClassBase> getMap() {
        return map;
    }

    public void setMap(Map<String, ? extends TablePerClassBase> map) {
        this.map = (Map<String, TablePerClassBase>) map;
    }
}
