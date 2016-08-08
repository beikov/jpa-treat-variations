package jpa.test.entities;

import java.io.Serializable;
import javax.persistence.ConstraintMode;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Embeddable
public class TablePerClassEmbeddable implements BaseEmbeddable<TablePerClassBase>, Serializable {
    private static final long serialVersionUID = 1L;

    private TablePerClassBase parent;

    public TablePerClassEmbeddable() {
    }

    public TablePerClassEmbeddable(TablePerClassBase parent) {
        this.parent = parent;
    }

    @Override
    @ManyToOne(fetch = FetchType.LAZY)
    // We can't have a constraint in this case because we don't know the exact table this will refer to
    @JoinColumn(insertable = false, updatable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    public TablePerClassBase getParent() {
        return parent;
    }

    @Override
    public void setParent(TablePerClassBase parent) {
        this.parent = parent;
    }
}
