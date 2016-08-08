package jpa.test.entities;

import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Embeddable
public class JoinedEmbeddable implements BaseEmbeddable<JoinedBase>, Serializable {
    private static final long serialVersionUID = 1L;

    private JoinedBase parent;

    public JoinedEmbeddable() {
    }

    public JoinedEmbeddable(JoinedBase parent) {
        this.parent = parent;
    }

    @Override
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(insertable = false, updatable = false)
    public JoinedBase getParent() {
        return parent;
    }

    @Override
    public void setParent(JoinedBase parent) {
        this.parent = parent;
    }
}
