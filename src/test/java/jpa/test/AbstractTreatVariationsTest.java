
package jpa.test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import jpa.test.entities.Base;
import jpa.test.entities.BaseEmbeddable;
import jpa.test.entities.IntIdEntity;
import jpa.test.entities.JoinedSub1;
import jpa.test.entities.JoinedSub2;
import jpa.test.entities.SingleTableSub1;
import jpa.test.entities.SingleTableSub2;
import jpa.test.entities.Sub1;
import jpa.test.entities.Sub1Embeddable;
import jpa.test.entities.Sub2;
import jpa.test.entities.Sub2Embeddable;
import jpa.test.entities.TablePerClassSub1;
import jpa.test.entities.TablePerClassSub2;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public abstract class AbstractTreatVariationsTest {
    
    protected EntityManagerFactory emf;
    
    protected final String strategy;
    protected final String objectPrefix;
    
    public AbstractTreatVariationsTest(String strategy, String objectPrefix) {
        this.strategy = strategy;
        this.objectPrefix = objectPrefix;
    }
    
    @Before
    public void setup() {
        emf = Persistence.createEntityManagerFactory("TestPU");
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        
        IntIdEntity i1 = new IntIdEntity("i1");
        em.persist(i1);
        persist(em, new IntIdEntity("s1"));
        persist(em, new IntIdEntity("s2"));
        persist(em, new IntIdEntity("s1.parent"));
        persist(em, new IntIdEntity("s2.parent"));
        persist(em, new IntIdEntity("st1"));
        persist(em, new IntIdEntity("st2"));
        persist(em, new IntIdEntity("st1.parent"));
        persist(em, new IntIdEntity("st2.parent"));
        persist(em, new IntIdEntity("tpc1"));
        persist(em, new IntIdEntity("tpc2"));
        persist(em, new IntIdEntity("tpc1.parent"));
        persist(em, new IntIdEntity("tpc2.parent"));
        
        /****************
         * Joined
         ***************/
        
        JoinedSub1 s1 = new JoinedSub1("s1");
        JoinedSub2 s2 = new JoinedSub2("s2");
        JoinedSub1 s1Parent = new JoinedSub1("s1.parent");
        JoinedSub2 s2Parent = new JoinedSub2("s2.parent");
        
        persist(em, i1, s1, s2, s1Parent, s2Parent);
        
        /****************
         * Single Table
         ***************/
        
        SingleTableSub1 st1 = new SingleTableSub1("st1");
        SingleTableSub2 st2 = new SingleTableSub2("st2");
        SingleTableSub1 st1Parent = new SingleTableSub1("st1.parent");
        SingleTableSub2 st2Parent = new SingleTableSub2("st2.parent");
        
        persist(em, i1, st1, st2, st1Parent, st2Parent);
        
        /****************
         * Table per Class
         ***************/
        
        TablePerClassSub1 tpc1 = new TablePerClassSub1("tpc1");
        TablePerClassSub2 tpc2 = new TablePerClassSub2("tpc2");
        TablePerClassSub1 tpc1Parent = new TablePerClassSub1("tpc1.parent");
        TablePerClassSub2 tpc2Parent = new TablePerClassSub2("tpc2.parent");
        
        // The Java compiler can't up-cast automatically, maybe a bug?
        //persist(em, i1, tpc1, tpc2, tpc1Parent, tpc2Parent);
        persist(em, i1, (Sub1) tpc1, (Sub2) tpc2, (Sub1) tpc1Parent, (Sub2) tpc2Parent);
        
        tx.commit();
        em.close();
    }
    
    private void persist(
            EntityManager em,
            IntIdEntity i1,
            Sub1<? extends Base<?, ?>, ? extends BaseEmbeddable<?>, ? extends Sub1Embeddable<?>> s1,
            Sub2<? extends Base<?, ?>, ? extends BaseEmbeddable<?>, ? extends Sub2Embeddable<?>> s2,
            Sub1<? extends Base<?, ?>, ? extends BaseEmbeddable<?>, ? extends Sub1Embeddable<?>> s1Parent,
            Sub2<? extends Base<?, ?>, ? extends BaseEmbeddable<?>, ? extends Sub2Embeddable<?>> s2Parent) {
        
        
        em.persist(s1Parent);
        em.persist(s2Parent);
        em.persist(s1);
        em.persist(s2);
        
        s1Parent.setValue(101);
        s1Parent.setSub1Value(101);
        s1Parent.getSub1Embeddable().setSomeValue(101);
        s1Parent.getEmbeddable1().setSub1SomeValue(101);
        s1.setValue(1);
        s1.setSub1Value(1);
        s1.getEmbeddable1().setSub1SomeValue(1);
        s1.setRelation1(i1);
        ((Sub1) s1).setParent(s1Parent);
        ((Sub1) s1).setParent1(s1Parent);
        ((BaseEmbeddable) s1.getEmbeddable()).setParent(s1Parent);
        ((Sub1Embeddable) s1.getEmbeddable1()).setSub1Parent(s1Parent);
        ((List<Base<?, ?>>) s1.getList()).add(s1Parent);
        ((List<Base<?, ?>>) s1.getList1()).add(s1Parent);
        ((List<Base<?, ?>>) s1.getEmbeddable().getList()).add(s1Parent);
        ((List<Base<?, ?>>) s1.getEmbeddable1().getSub1List()).add(s1Parent);
        ((List<Base<?, ?>>) s1Parent.getList()).add(s2);
        ((List<Base<?, ?>>) s1Parent.getList1()).add(s2);
        ((List<Base<?, ?>>) s1Parent.getEmbeddable().getList()).add(s2);
        ((List<Base<?, ?>>) s1Parent.getEmbeddable1().getSub1List()).add(s2);
        ((Map<String, Base<?, ?>>) s1.getMap()).put(s1Parent.getName(), s1Parent);
        ((Map<String, Base<?, ?>>) s1.getMap1()).put(s1Parent.getName(), s1Parent);
        ((Map<String, Base<?, ?>>) s1.getEmbeddable().getMap()).put(s1Parent.getName(), s1Parent);
        ((Map<String, Base<?, ?>>) s1.getEmbeddable1().getSub1Map()).put(s1Parent.getName(), s1Parent);
        ((Map<String, Base<?, ?>>) s1Parent.getMap()).put(s2.getName(), s2);
        ((Map<String, Base<?, ?>>) s1Parent.getMap1()).put(s2.getName(), s2);
        ((Map<String, Base<?, ?>>) s1Parent.getEmbeddable().getMap()).put(s2.getName(), s2);
        ((Map<String, Base<?, ?>>) s1Parent.getEmbeddable1().getSub1Map()).put(s2.getName(), s2);
        
        s2Parent.setValue(102);
        s2Parent.setSub2Value(102);
        s2Parent.getSub2Embeddable().setSomeValue(102);
        s2Parent.getEmbeddable2().setSub2SomeValue(102);
        s2.setValue(102);
        s2.setSub2Value(2);
        s2.getEmbeddable2().setSub2SomeValue(2);
        s2.setRelation2(i1);
        ((Sub2) s2).setParent(s2Parent);
        ((Sub2) s2).setParent2(s2Parent);
        ((BaseEmbeddable) s2.getEmbeddable()).setParent(s2Parent);
        ((Sub2Embeddable) s2.getEmbeddable2()).setSub2Parent(s2Parent);
        ((List<Base<?, ?>>) s2.getList()).add(s2Parent);
        ((List<Base<?, ?>>) s2.getList2()).add(s2Parent);
        ((List<Base<?, ?>>) s2.getEmbeddable().getList()).add(s2Parent);
        ((List<Base<?, ?>>) s2.getEmbeddable2().getSub2List()).add(s2Parent);
        ((List<Base<?, ?>>) s2Parent.getList()).add(s1);
        ((List<Base<?, ?>>) s2Parent.getList2()).add(s1);
        ((List<Base<?, ?>>) s2Parent.getEmbeddable().getList()).add(s1);
        ((List<Base<?, ?>>) s2Parent.getEmbeddable2().getSub2List()).add(s1);
        ((Map<String, Base<?, ?>>) s2.getMap()).put(s2Parent.getName(), s2Parent);
        ((Map<String, Base<?, ?>>) s2.getMap2()).put(s2Parent.getName(), s2Parent);
        ((Map<String, Base<?, ?>>) s2.getEmbeddable().getMap()).put(s2Parent.getName(), s2Parent);
        ((Map<String, Base<?, ?>>) s2.getEmbeddable2().getSub2Map()).put(s2Parent.getName(), s2Parent);
        ((Map<String, Base<?, ?>>) s2Parent.getMap()).put(s1.getName(), s1);
        ((Map<String, Base<?, ?>>) s2Parent.getMap2()).put(s1.getName(), s1);
        ((Map<String, Base<?, ?>>) s2Parent.getEmbeddable().getMap()).put(s1.getName(), s1);
        ((Map<String, Base<?, ?>>) s2Parent.getEmbeddable2().getSub2Map()).put(s1.getName(), s1);
    }
    
    private void persist(
            EntityManager em,
            IntIdEntity i1) {
        // Persist 2 name matching IntIdEntity
        em.persist(i1);
        em.persist(new IntIdEntity(i1.getName()));
    }
    
    @After
    public void tearDown() {
        if (emf.isOpen()) {
            emf.close();
        }
    }
    
    /************************************************************
     * Just some helper methods
     ************************************************************/
    
    protected <T> List<T> list(String query, Class<T> clazz) {
        EntityManager em = emf.createEntityManager();
        
        // EclipseLink issues 1 query, all successful
        // Hibernate issues 1 query, all successful
        // DataNucleus fails
        TypedQuery<T> q = em.createQuery(query, clazz);
        
        List<T> bases = q.getResultList();
        em.close();
        // Closing emf since eclipselink would do lazy loading even with closed entity manager!
        emf.close();
        return bases;
    }
    
    protected void assertRemoved(List<Object[]> list, Object[] expected) {
        Iterator<Object[]> iter = list.iterator();
        while (iter.hasNext()) {
            if (Arrays.deepEquals(expected, iter.next())) {
                iter.remove();
                return;
            }
        }
        
        Assert.fail(Arrays.deepToString(list.toArray()) + " does not contain expected entry: " + Arrays.deepToString(expected));
    }
    
    protected void assertRemoved(List<? extends Object> list, Object expected) {
        if (list.remove(expected)) {
            return;
        }
        
        Assert.fail(list + " does not contain expected entry: " + expected);
    }
    
}
