
package jpa.test.entities;

public interface Sub1<T extends Base> extends Base<T> {
    
    public IntIdEntity getRelation1();

    public void setRelation1(IntIdEntity relation1);

    public T getParent1();

    public void setParent1(T parent1);

    public Integer getSub1Value();

    public void setSub1Value(Integer sub1Value);
}
