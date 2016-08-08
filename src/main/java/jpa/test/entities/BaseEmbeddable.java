
package jpa.test.entities;

public interface BaseEmbeddable<T extends Base> {
    
    public T getParent();

    public void setParent(T parent);
    
}
