
package jpa.test.entities;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Sub2Embeddable<T extends Base> {
    
    public Integer getSub2SomeValue();
    
    public void setSub2SomeValue(Integer sub2SomeValue);
    
    public T getSub2Parent();

    public void setSub2Parent(T sub2Parent);

    public List<? extends T> getSub2List();

    public void setSub2List(List<? extends T> sub2List);

    public Set<? extends T> getSub2Children();

    public void setSub2Children(Set<? extends T> sub2Children);

    public Map<String, ? extends T> getSub2Map();

    public void setSub2Map(Map<String, ? extends T> sub2Map);
    
}
