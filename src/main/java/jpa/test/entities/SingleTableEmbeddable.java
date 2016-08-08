package jpa.test.entities;

import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Embeddable
public class SingleTableEmbeddable implements BaseEmbeddable<SingleTableBase>, Serializable {
    private static final long serialVersionUID = 1L;

    private SingleTableBase parent;

    public SingleTableEmbeddable() {
    }

    public SingleTableEmbeddable(SingleTableBase parent) {
        this.parent = parent;
    }

    @Override
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(insertable = false, updatable = false)
    public SingleTableBase getParent() {
        return parent;
    }

    @Override
    public void setParent(SingleTableBase parent) {
        this.parent = parent;
    }
}
