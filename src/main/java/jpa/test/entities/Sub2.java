
package jpa.test.entities;

public interface Sub2<T extends Base, B extends BaseEmbeddable<T>> extends Base<T, B> {
    
    public IntIdEntity getRelation2();

    public void setRelation2(IntIdEntity relation1);

    public T getParent2();

    public void setParent2(T parent2);

    public Integer getSub2Value();

    public void setSub2Value(Integer sub2Value);

    public IntValueEmbeddable getSub2Embeddable();

    public void setSub2Embeddable(IntValueEmbeddable sub2Embeddable);
    
}
