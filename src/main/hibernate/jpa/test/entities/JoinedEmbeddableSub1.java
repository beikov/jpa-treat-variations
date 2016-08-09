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
public class JoinedEmbeddableSub1 implements Sub1Embeddable<JoinedBase>, Serializable {
    private static final long serialVersionUID = 1L;

    private Integer sub1SomeValue;
    private JoinedBase sub1Parent;
    private List<JoinedBase> sub1List = new ArrayList<>();
    private Set<JoinedSub1> sub1Children = new HashSet<>();
    private Map<String, JoinedBase> sub1Map = new HashMap<>();

    public JoinedEmbeddableSub1() {
    }

    public JoinedEmbeddableSub1(JoinedBase sub1Parent) {
        this.sub1Parent = sub1Parent;
    }

    @Override
    public Integer getSub1SomeValue() {
        return sub1SomeValue;
    }

    @Override
    public void setSub1SomeValue(Integer sub1SomeValue) {
        this.sub1SomeValue = sub1SomeValue;
    }

    @Override
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "embeddableSub1Parent")
    public JoinedBase getSub1Parent() {
        return sub1Parent;
    }

    @Override
    public void setSub1Parent(JoinedBase sub1Parent) {
        this.sub1Parent = sub1Parent;
    }

    @Override
    @ManyToMany
    @OrderColumn(name = "list_idx", nullable = false)
    @JoinTable(name = "joined_embeddable_1_list")
    public List<JoinedBase> getSub1List() {
        return sub1List;
    }

    @Override
    public void setSub1List(List<? extends JoinedBase> sub1List) {
        this.sub1List = (List<JoinedBase>) sub1List;
    }

    @Override
    @OneToMany
    @JoinColumn(name = "embeddableSub1Parent")
    public Set<JoinedSub1> getSub1Children() {
        return sub1Children;
    }

    @Override
    public void setSub1Children(Set<? extends JoinedBase> sub1Children) {
        this.sub1Children = (Set<JoinedSub1>) sub1Children;
    }

    @Override
    @ManyToMany
    @JoinTable(name = "joined_embeddable_1_map")
    @MapKeyColumn(name = "jes1m_map_key", nullable = false, length = 20)
    public Map<String, JoinedBase> getSub1Map() {
        return sub1Map;
    }

    @Override
    public void setSub1Map(Map<String, ? extends JoinedBase> sub1Map) {
        this.sub1Map = (Map<String, JoinedBase>) sub1Map;
    }

}
