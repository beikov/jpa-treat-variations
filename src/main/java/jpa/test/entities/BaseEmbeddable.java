
package jpa.test.entities;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BaseEmbeddable<T extends Base> {
    
    public T getParent();

    public void setParent(T parent);

    public List<? extends T> getList();

    public void setList(List<? extends T> list);

    public Set<? extends T> getChildren();

    public void setChildren(Set<? extends T> children);

    public Map<? extends T, ? extends T> getMap();

    public void setMap(Map<? extends T, ? extends T> map);
    
}
