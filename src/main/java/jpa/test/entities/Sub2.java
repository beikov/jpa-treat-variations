
package jpa.test.entities;

public interface Sub2<T extends Base> extends Base<T> {
    
    public IntIdEntity getRelation2();

    public void setRelation2(IntIdEntity relation1);

    public T getParent2();

    public void setParent2(T parent2);

    public Integer getSub2Value();

    public void setSub2Value(Integer sub2Value);
    
}
