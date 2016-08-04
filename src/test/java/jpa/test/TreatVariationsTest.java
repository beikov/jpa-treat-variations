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
import jpa.test.entities.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TreatVariationsTest {
    
    private EntityManagerFactory emf;
    
    private final String strategy;
    
    public TreatVariationsTest(String strategy) {
        this.strategy = strategy;
    }
    
    @Parameterized.Parameters
    public static Object[] getParameters() {
        /*
         * Hibernate seems to only support treat in the FROM clause: https://hibernate.atlassian.net/browse/HHH-10988
         * Hibernate doesn't properly optimize unnecessary joins away: https://hibernate.atlassian.net/browse/HHH-10887
         */
        return new Object[] {
            "Joined", 
            "SingleTable", 
            /**
             * Eclipselink has no support for treat in conjunction with table per class
             */
            "TablePerClass"
        };
    }
    
    @Before
    public void setup() {
        emf = Persistence.createEntityManagerFactory("TestPU");
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        
        IntIdEntity i1 = new IntIdEntity("i1");
        em.persist(i1);
        
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
        
        persist(em, i1, tpc1, tpc2, tpc1Parent, tpc2Parent);
        
        tx.commit();
        em.close();
    }
    
    private void persist(
            EntityManager em,
            IntIdEntity i1,
            Sub1<? extends Base> s1,
            Sub2<? extends Base> s2,
            Sub1<? extends Base> s1Parent,
            Sub2<? extends Base> s2Parent) {
        
        
        em.persist(s1Parent);
        em.persist(s2Parent);
        em.persist(s1);
        em.persist(s2);
        
        s1Parent.setSub1Value(101);
        s1.setSub1Value(1);
        s1.setRelation1(i1);
        ((Sub1<Base<?>>) s1).setParent(s1Parent);
        ((Sub1<Base<?>>) s1).setParent1(s1Parent);
        ((List<Base<?>>) s1.getList()).add(s1Parent);
        ((List<Base<?>>) s1.getList()).add(s2);
        ((Map<String, Base<?>>) s1.getMap()).put(s1Parent.getName(), s1Parent);
        ((Map<String, Base<?>>) s1.getMap()).put(s2.getName(), s2);
        
        s2Parent.setSub2Value(102);
        s2.setSub2Value(2);
        s2.setRelation2(i1);
        ((Sub2<Base<?>>) s2).setParent(s2Parent);
        ((Sub2<Base<?>>) s2).setParent2(s2Parent);
        ((List<Base<?>>) s2.getList()).add(s2Parent);
        ((List<Base<?>>) s2.getList()).add(s1);
        ((Map<String, Base<?>>) s2.getMap()).put(s2Parent.getName(), s2Parent);
        ((Map<String, Base<?>>) s2.getMap()).put(s1.getName(), s1);
    }
    
    @After
    public void tearDown() {
        if (emf.isOpen()) {
            emf.close();
        }
    }
    
    @Test
    public void selectTreatedRoot() {
        // EclipseLink
        // - Joined        : issues 1 query, all successful
        // - SingleTable   : issues 1 query, all successful
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT TREAT(b AS " + strategy + "Sub1).sub1Value FROM " + strategy + "Base b", Integer.class);
        System.out.println("selectTreatedRoot-" + strategy);
        
        // 2 and not 4 because Base b is only treated for Sub1
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, 1);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void selectMultipleTreatedRoot() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because generates DTYPE = 1 AND DTYPE = 2
        // - SingleTable   : issues 1 query, FAILS because generates DTYPE = 1 AND DTYPE = 2
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT TREAT(b AS " + strategy + "Sub1).sub1Value, TREAT(b AS " + strategy + "Sub2).sub2Value FROM " + strategy + "Base b", Object[].class);
        System.out.println("selectMultipleTreatedRoot-" + strategy);
        
        // 4 because Base b is treated for Sub1 and Sub2
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { 1,    null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 2    });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    // Assuming b.parent results in INNER JOIN
    public void selectTreatedRelation() {
        // EclipseLink
        // - Joined        : issues 1 query, all successful
        // - SingleTable   : issues 1 query, all successful
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT TREAT(b.parent AS " + strategy + "Sub1).sub1Value FROM " + strategy + "Base b", Integer.class);
        System.out.println("selectTreatedRelation-" + strategy);
        
        Assert.assertEquals(1, bases.size());
        assertRemoved(bases, 101);
    }
    
    @Test
    // Assuming b.parent results in INNER JOIN
    public void selectMultipleTreatedRelation() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because generates DTYPE = 1 AND DTYPE = 2
        // - SingleTable   : issues 1 query, FAILS because generates DTYPE = 1 AND DTYPE = 2
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT TREAT(b.parent AS " + strategy + "Sub1).sub1Value, TREAT(b.parent AS " + strategy + "Sub2).sub2Value FROM " + strategy + "Base b", Object[].class);
        System.out.println("selectMultipleTreatedRelation-" + strategy);
        
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void treatJoinTreatedRoot() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because join for parent1 has inner join semantics
        // - SingleTable   : issues 1 query, FAILS because join for parent1 has inner join semantics
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT s1.sub1Value FROM " + strategy + "Base b LEFT JOIN TREAT(TREAT(b AS " + strategy + "Sub1).parent1 AS " + strategy + "Sub1) AS s1", Integer.class);
        System.out.println("treatJoinTreatedRoot-" + strategy);
        
        // 2 because of left join but not 4 because Base b is only treated for Sub1
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void treatJoinMultipleTreatedRoot() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because join for parent1 and parent2 have inner join semantics
        // - SingleTable   : issues 1 query, FAILS because join for parent1 and parent2 have inner join semantics
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT s1.sub1Value, s2.sub2Value FROM " + strategy + "Base b LEFT JOIN TREAT(TREAT(b AS " + strategy + "Sub1).parent1 AS " + strategy + "Sub1) AS s1 LEFT JOIN TREAT(TREAT(b AS " + strategy + "Sub2).parent2 AS " + strategy + "Sub2) AS s2", Object[].class);
        System.out.println("treatJoinMultipleTreatedRoot-" + strategy);
        
        // 4 because Base b is treated for Sub1 and Sub2 and uses left join
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    public void selectTreatJoinedRelation() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because join for parent does not filter for Sub1
        // - SingleTable   : issues 1 query, FAILS because of non-left-join aware filtering of parent for Sub1
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : issues 1 query, FAILS because inner joins on Sub1 for parent relation => should always use left from bottom up
        // - SingleTable   : issues 1 query, FAILS because of non-left-join aware filtering of parent for Sub1
        // - TablePerClass : issues 1 query, FAILS because it fails to apply Sub1 filter to parents properly
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT s1.sub1Value FROM " + strategy + "Base b LEFT JOIN TREAT(b.parent AS " + strategy + "Sub1) s1", Integer.class);
        System.out.println("selectTreatJoinedRelation-" + strategy);
        
        // 2 because of left join but not 4 because Base b is only treated for Sub1
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void selectMultipleTreatJoinedRelation() {
        // EclipseLink
        // - Joined        : issues 1 query, all successful
        // - SingleTable   : issues 1 query, FAILS because generates DTYPE = 1 AND DTYPE = 2
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : issues 1 query, FAILS because inner joins on Sub1 for parent relation => should always use left from bottom up: https://hibernate.atlassian.net/browse/HHH-9862
        // - SingleTable   : issues 1 query, all successful, but careful: https://hibernate.atlassian.net/browse/HHH-10768
        // - TablePerClass : issues 1 query, all successful, but careful: https://hibernate.atlassian.net/browse/HHH-10768
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT s1.sub1Value, s2.sub2Value FROM " + strategy + "Base b LEFT JOIN TREAT(b.parent AS " + strategy + "Sub1) s1 LEFT JOIN TREAT(b.parent AS " + strategy + "Sub2) s2", Object[].class);
        System.out.println("selectMultipleTreatJoinedRelation-" + strategy);
        
        // 4 because of left join
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    // TODO: treat other things than many to ones
    // TODO: paths over embeddables in treat
    // TODO: paths over embeddables after treat
    // TODO: treat in subqueries
    
    private <T> List<T> list(String query, Class<T> clazz) {
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
    
    private void assertRemoved(List<Object[]> list, Object[] expected) {
        Iterator<Object[]> iter = list.iterator();
        while (iter.hasNext()) {
            if (Arrays.deepEquals(expected, iter.next())) {
                iter.remove();
                return;
            }
        }
        
        Assert.fail(Arrays.deepToString(list.toArray()) + " does not contain expected entry: " + Arrays.deepToString(expected));
    }
    
    private void assertRemoved(List<? extends Object> list, Object expected) {
        if (list.remove(expected)) {
            return;
        }
        
        Assert.fail(list + " does not contain expected entry: " + expected);
    }
}
