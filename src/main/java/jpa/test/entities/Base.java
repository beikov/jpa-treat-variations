
package jpa.test.entities;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Base<T extends Base, B extends BaseEmbeddable<T>> {
    
    public Long getId();

    public void setId(Long id);

    public String getName();

    public void setName(String name);

    public Integer getValue();

    public void setValue(Integer value);

    public T getParent();

    public void setParent(T parent);

    public B getEmbeddable();

    public void setEmbeddable(B embeddable);

    public List<? extends T> getList();

    public void setList(List<? extends T> list);

    public Set<? extends T> getChildren();

    public void setChildren(Set<? extends T> children);

    public Map<String, ? extends T> getMap();

    public void setMap(Map<String, ? extends T> map);
}
