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

/**
 * This is a collection of test cases for the treat operator.
 * 
 * It assumes some very specific behavior of the treat operator which might even differ from the JPA spec or intuition at first.
 * In addition to that, the following also tests for edge cases the JPA spec seemed to have forgotten to include.
 * 
 * There are 2 types of treat expressions
 *  - Root path treats e.g. "TREAT(rootAlias AS Subtype)"
 *  - Subpath treats e.g. "TREAT(rootAlias.subpath AS Subtype)"
 * 
 * Treat expressions can be used in different forms in different clauses
 *  - In SELECT/WHERE/ORDER BY all types of treat expressions must be followed by a subpath e.g. "TREAT(rootAlias AS Subtype).subpath"
 *  - In FROM a treat expression must be one of the following
 *    - Subpath treat e.g. "TREAT(rootAlias.subpath AS Subtype)"
 *    - Subpath treat with root path treat e.g. "TREAT(TREAT(rootAlias AS Subtype).subpath AS Subtype)"
 *    - Root path treat followed by subpath e.g. "TREAT(rootAlias AS Subtype).subpath"
 * 
 * Simplification translation strategy
 * 
 * S1. Nested treat expressions in non-FROM clauses can be implemented by replacing subexpressions with join nodes.
 * An expression like "TREAT(TREAT(rootAlias AS Subtype).subpath AS Subtype).subpath" will emmit "INNER JOIN TREAT(rootAlias AS Subtype).subpath p"
 * and the expression becomes "TREAT(p AS Subtype).subpath"
 * 
 * S2. Subpath treats in non-FROM clauses follow JPA navigation semantics and can be simplified by emmiting an inner join for the treated path 
 * and replacing the path with the join alias e.g. "TREAT(rootAlias.subpath AS Subtype).subpath" will emmit "INNER JOIN rootAlias.subpath p" 
 * and the expression becomes "TREAT(p AS Subtype).subpath"
 * 
 * S3. Since queries for types that have subtypes should produce concrete subtype values e.g. "SELECT o FROM Supertype o" returns [Subtype1, Subtype2, ...], subtype relations must be semantically left joined.
 * A treat join like "LEFT JOIN TREAT(root.subpath Subtype) p" can be simplified to "LEFT JOIN root.subpath p ON TYPE(p) = Subtype" and replacing every usage of "p" with "TREAT(p AS Subtype)"
 * 
 * S4. The join type for a relation join must semantically cascade for joining the type hierarchy of the relation, 
 * meaning that e.g. "LEFT JOIN TREAT(root.subpath Subtype)" must use left join semantics not only for joining the subpath relation(might be collection table) 
 * and the concrete subtype relation, but also for joining the supertype hierarchy relations.
 * The on clause predicates must be put on the appropriate relation joins on clauses so that the "illusion" of joining against a single table is kept(might require predicates to be transformed because "ON KEY(...) = ..." might get tricky)
 * 
 * S5. Due to this possible translation strategy the only new feature that needs deep implementation support is a root path treat.
 * When translating to SQL, the root path treat must be translated to the alias of the proper subtype relation.
 * 
 * Possible optimizations
 * 
 * O1. The optimization of avoiding joins for subtype relations can actually be seen independent of treat support when applying the translation strategy.
 * When encountering a top-level equality- or in-predicate of the form "TYPE(fromAlias) = Subtype" or "TYPE(fromAlias) IN (Subtype, Subtype2)" in a WHERE or ON clause, 
 * then the type hierarchy joins for the "fromAlias" relation can be reduced to only include the listed subtype relations.
 * Since the translation strategy mandates translating a treat join like "JOIN TREAT(rootAlias.subpath AS Subtype) p" to "JOIN rootAlias.subpath p ON TYPE(p) = Subtype",
 * joining other subrelations can be avoided in this case.
 * O2. Another possible optimization is to avoid joins for super types if no super type properties are used.
 * O3. If it can be proven, that for top-level predicates of the form "TREAT(root AS Subtype).subpath OPERATOR ..." the predicate is always false for a subtype T1, 
 * then the subtype mustn't be joined as instances of that type would be filtered out anyway.
 * 
 * 
 * In summary, treat joins can be reduced to normal joins with type restrictions and by replacing join aliases with the treated version.
 * Subpath treats can be replaced with root path treats by adding inner joins for paths. Treats in general do not cause subtype filtering,
 * only the type restrictions do.
 * 
 * Findings so far
 * 
 * Eclipselink
 *  - does not support map in embeddables(BUGREPORT!)
 *  - has no support for treat in conjunction with table per class
 *  - wrongly assumes it can filter when encountering a treat
 *  - does not support root path treat in FROM
 *
 * Hibernate
 *  - only supports treat in the FROM clause: https://hibernate.atlassian.net/browse/HHH-10988
 *  - doesn't properly optimize unnecessary joins away: https://hibernate.atlassian.net/browse/HHH-10887
 * 
 * Datanucleus 
 *  - does not seem to support the TablePerClass mappings: https://github.com/datanucleus/datanucleus-rdbms/issues/89
 * 
 * @author Christian Beikov
 */
@RunWith(Parameterized.class)
public class TreatVariationsTest {
    
    private EntityManagerFactory emf;
    
    private final String strategy;
    private final String objectPrefix;
    
    public TreatVariationsTest(String strategy, String objectPrefix) {
        this.strategy = strategy;
        this.objectPrefix = objectPrefix;
    }
    
    @Parameterized.Parameters
    public static Object[] getParameters() {
        return new Object[] {
            new Object[] { "Joined", "s" }, 
            new Object[] { "SingleTable", "st" }, 
            new Object[] { "TablePerClass", "tpc" }
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
     * TREAT ROOT
     ************************************************************/
    
    @Test
    public void selectTreatedRootBasic() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
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
        System.out.println("selectTreatedRootBasic-" + strategy);
        
        // From => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 1);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void selectMultipleTreatedRootBasic() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
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
        System.out.println("selectMultipleTreatedRootBasic-" + strategy);
        
        // From => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { 1,    null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 2    });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    public void selectTreatedParentRootBasic() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because query is completely wrong
        // - SingleTable   : issues 1 query, successful, but would fail because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT (SELECT SUM(TREAT(b AS " + strategy + "Sub1).sub1Value) FROM IntIdEntity i WHERE i.name = b.name) FROM " + strategy + "Base b", Integer.class);
        System.out.println("selectTreatedParentRootBasic-" + strategy);
        
        // From => 4 instances
        // There are 2 IntIdEntity instances per Base instance
        // For the 2 Sub1 instances, their sub1Value is doubled
        // For the 2 Sub2 instances, null is emmitted
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 2L);
        assertRemoved(bases, 202L);
    }
    
    @Test
    public void selectMultipleTreatedParentRootBasic() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because query is completely wrong
        // - SingleTable   : issues 1 query, successful, but would fail because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT (SELECT SUM(TREAT(b AS " + strategy + "Sub1).sub1Value) FROM IntIdEntity i WHERE i.name = b.name), (SELECT SUM(TREAT(b AS " + strategy + "Sub2).sub2Value) FROM IntIdEntity i WHERE i.name = b.name) FROM " + strategy + "Base b", Object[].class);
        System.out.println("selectMultipleTreatedParentRootBasic-" + strategy);
        
        // From => 4 instances
        // There are 2 IntIdEntity instances per Base instance
        // For the 2 Sub1 instances, their sub1Value is doubled
        // For the 2 Sub2 instances, null is emmitted
        // The second subquery is like the first but for Sub2
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { 2L,   null });
        assertRemoved(bases, new Object[] { 202L, null });
        assertRemoved(bases, new Object[] { null, 4L   });
        assertRemoved(bases, new Object[] { null, 204L });
    }
    
    @Test
    public void selectTreatedRootEmbeddableBasic() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treat embeddable path parsing fails
        // - SingleTable   : not working, treat embeddable path parsing fails
        // - TablePerClass : not working, treat embeddable path parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT TREAT(b AS " + strategy + "Sub1).sub1Embeddable.someValue FROM " + strategy + "Base b", Integer.class);
        System.out.println("selectTreatedRootEmbeddableBasic-" + strategy);
        
        // From => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void selectMultipleTreatedRootEmbeddableBasic() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treat embeddable path parsing fails
        // - SingleTable   : not working, treat embeddable path parsing fails
        // - TablePerClass : not working, treat embeddable path parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT TREAT(b AS " + strategy + "Sub1).sub1Embeddable.someValue, TREAT(b AS " + strategy + "Sub2).sub2Embeddable.someValue FROM " + strategy + "Base b", Object[].class);
        System.out.println("selectMultipleTreatedRootEmbeddableBasic-" + strategy);
        
        // From => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    public void selectTreatedParentRootEmbeddableBasic() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because query is completely wrong
        // - SingleTable   : issues 1 query, successful, but would fail because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT (SELECT SUM(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1SomeValue) FROM IntIdEntity i WHERE i.name = b.name) FROM " + strategy + "Base b", Integer.class);
        System.out.println("selectTreatedParentRootEmbeddableBasic-" + strategy);
        
        // From => 4 instances
        // There are 2 IntIdEntity instances per Base instance
        // For the 2 Sub1 instances, their sub1SomeValue is doubled
        // For the 2 Sub2 instances, null is emmitted
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 2L);
        assertRemoved(bases, 202L);
    }
    
    @Test
    public void selectMultipleTreatedParentRootEmbeddableBasic() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because query is completely wrong
        // - SingleTable   : issues 1 query, successful, but would fail because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT (SELECT SUM(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1SomeValue) FROM IntIdEntity i WHERE i.name = b.name), (SELECT SUM(TREAT(b AS " + strategy + "Sub2).embeddable2.sub2SomeValue) FROM IntIdEntity i WHERE i.name = b.name) FROM " + strategy + "Base b", Object[].class);
        System.out.println("selectMultipleTreatedParentRootEmbeddableBasic-" + strategy);
        
        // From => 4 instances
        // There are 2 IntIdEntity instances per Base instance
        // For the 2 Sub1 instances, their sub1SomeValue is doubled
        // For the 2 Sub2 instances, null is emmitted
        // The second subquery is like the first but for Sub2
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { 2L,   null });
        assertRemoved(bases, new Object[] { 202L, null });
        assertRemoved(bases, new Object[] { null, 4L   });
        assertRemoved(bases, new Object[] { null, 204L });
    }
    
    @Test
    public void whereTreatedRootBasic() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<String> bases = list("SELECT b.name FROM " + strategy + "Base b WHERE COALESCE(TREAT(b AS " + strategy + "Sub1).sub1Value, 0) < 100", String.class);
        System.out.println("whereTreatedRootBasic-" + strategy);
        
        // From => 4 instances
        // Where => 3 instances because 1 Sub1 has sub1Value 101, the other has 1 and Sub2s use 0 because of coalesce
        Assert.assertEquals(3, bases.size());
        assertRemoved(bases, objectPrefix + "1.parent");
        assertRemoved(bases, objectPrefix + "2.parent");
        assertRemoved(bases, objectPrefix + "1");
    }
    
    @Test
    public void whereMultipleTreatedRootBasic() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<String> bases = list("SELECT b.name FROM " + strategy + "Base b WHERE COALESCE(TREAT(b AS " + strategy + "Sub1).sub1Value, 0) < 100 AND COALESCE(TREAT(b AS " + strategy + "Sub2).sub2Value, 0) < 100", String.class);
        System.out.println("whereMultipleTreatedRootBasic-" + strategy);
        
        // From => 4 instances
        // Where => 2 instances because 1 Sub1 has sub1Value 101, the other has 1 and Sub2s use 0 because of coalesce
        // and 1 Sub2 has sub2Value 102, the other has 2 and Sub1 uses 0 because of coalesce
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, objectPrefix + "1");
        assertRemoved(bases, objectPrefix + "2");
    }
    
    @Test
    public void whereTreatedRootEmbeddableBasic() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<String> bases = list("SELECT b.name FROM " + strategy + "Base b WHERE COALESCE(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1SomeValue, 0) < 100", String.class);
        System.out.println("whereTreatedRootEmbeddableBasic-" + strategy);
        
        // From => 4 instances
        // Where => 3 instances because 1 Sub1 has sub1Value 101, the other has 1 and Sub2s use 0 because of coalesce
        Assert.assertEquals(3, bases.size());
        assertRemoved(bases, objectPrefix + "1.parent");
        assertRemoved(bases, objectPrefix + "2.parent");
        assertRemoved(bases, objectPrefix + "1");
    }
    
    @Test
    public void whereMultipleTreatedRootEmbeddableBasic() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<String> bases = list("SELECT b.name FROM " + strategy + "Base b WHERE COALESCE(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1SomeValue, 0) < 100 AND COALESCE(TREAT(b AS " + strategy + "Sub2).embeddable2.sub2SomeValue, 0) < 100", String.class);
        System.out.println("whereMultipleTreatedRootEmbeddableBasic-" + strategy);
        
        // From => 4 instances
        // Where => 2 instances because 1 Sub1 has sub1SomeValue 101, the other has 1 and Sub2s use 0 because of coalesce
        // and 1 Sub2 has sub2SomeValue 102, the other has 2 and Sub1 uses 0 because of coalesce
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, objectPrefix + "1");
        assertRemoved(bases, objectPrefix + "2");
    }
    
    /************************************************************
     * TREAT MANY-TO-ONE
     ************************************************************/
    
    @Test
    public void selectTreatedManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
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
        System.out.println("selectTreatedManyToOne-" + strategy);
        
        // From => 4 instances
        // Inner join on b.parent => 2 instance
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void selectMultipleTreatedManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
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
        System.out.println("selectMultipleTreatedManyToOne-" + strategy);
        
        // From => 4 instances
        // Inner join on b.parent => 2 instance
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    public void selectTreatedParentManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, all successful
        // - SingleTable   : issues 1 query, all successful
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT (SELECT SUM(TREAT(b.parent AS " + strategy + "Sub1).sub1Value) FROM IntIdEntity i WHERE i.name = b.name) FROM " + strategy + "Base b", Integer.class);
        System.out.println("selectTreatedParentManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents but only one is Sub1
        // The sub1Value is doubled
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 202L);
    }
    
    @Test
    public void selectMultipleTreatedParentManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because query is completely wrong
        // - SingleTable   : issues 1 query, FAILS because query is completely wrong
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT (SELECT SUM(TREAT(b.parent AS " + strategy + "Sub1).sub1Value) FROM IntIdEntity i WHERE i.name = b.name), (SELECT SUM(TREAT(b.parent AS " + strategy + "Sub2).sub2Value) FROM IntIdEntity i WHERE i.name = b.name) FROM " + strategy + "Base b", Object[].class);
        System.out.println("selectMultipleTreatedParentManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents, one is Sub1 and the other Sub2
        // The sub1Value and sub2Value is doubled
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 202L, null });
        assertRemoved(bases, new Object[] { null, 204L });
    }
    
    @Test
    public void selectTreatedEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT TREAT(b.embeddable.parent AS " + strategy + "Sub1).sub1Value FROM " + strategy + "Base b", Integer.class);
        System.out.println("selectTreatedEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // Inner join on b.parent => 2 instance
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void selectMultipleTreatedEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT TREAT(b.embeddable.parent AS " + strategy + "Sub1).sub1Value, TREAT(b.embeddable.parent AS " + strategy + "Sub2).sub2Value FROM " + strategy + "Base b", Object[].class);
        System.out.println("selectMultipleTreatedEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // Inner join on b.parent => 2 instance
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    public void selectTreatedParentEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, all successful
        // - SingleTable   : issues 1 query, all successful
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT (SELECT SUM(TREAT(b.embeddable.parent AS " + strategy + "Sub1).sub1Value) FROM IntIdEntity i WHERE i.name = b.name) FROM " + strategy + "Base b", Integer.class);
        System.out.println("selectTreatedParentEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents but only one is Sub1
        // The sub1Value is doubled
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 202L);
    }
    
    @Test
    public void selectMultipleTreatedParentEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because query is completely wrong
        // - SingleTable   : issues 1 query, FAILS because query is completely wrong
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT (SELECT SUM(TREAT(b.embeddable.parent AS " + strategy + "Sub1).sub1Value) FROM IntIdEntity i WHERE i.name = b.name), (SELECT SUM(TREAT(b.embeddable.parent AS " + strategy + "Sub2).sub2Value) FROM IntIdEntity i WHERE i.name = b.name) FROM " + strategy + "Base b", Object[].class);
        System.out.println("selectMultipleTreatedParentEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents, one is Sub1 and the other Sub2
        // The sub1Value and sub2Value is doubled
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 202L, null });
        assertRemoved(bases, new Object[] { null, 204L });
    }
    
    @Test
    public void selectTreatedEmbeddableManyToOneEmbeddable() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT TREAT(b.embeddable.parent AS " + strategy + "Sub1).embeddable1.sub1SomeValue FROM " + strategy + "Base b", Integer.class);
        System.out.println("selectTreatedEmbeddableManyToOneEmbeddable-" + strategy);
        
        // From => 4 instances
        // Inner join on b.parent => 2 instance
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void selectMultipleTreatedEmbeddableManyToOneEmbeddable() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT TREAT(b.embeddable.parent AS " + strategy + "Sub1).embeddable1.sub1SomeValue, TREAT(b.embeddable.parent AS " + strategy + "Sub2).embeddable2.sub2SomeValue FROM " + strategy + "Base b", Object[].class);
        System.out.println("selectMultipleTreatedEmbeddableManyToOneEmbeddable-" + strategy);
        
        // From => 4 instances
        // Inner join on b.parent => 2 instance
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    public void selectTreatedParentEmbeddableManyToOneEmbeddable() {
        // EclipseLink
        // - Joined        : issues 1 query, all successful
        // - SingleTable   : issues 1 query, all successful
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT (SELECT SUM(TREAT(b.embeddable.parent AS " + strategy + "Sub1).embeddable1.sub1SomeValue) FROM IntIdEntity i WHERE i.name = b.name) FROM " + strategy + "Base b", Integer.class);
        System.out.println("selectTreatedParentEmbeddableManyToOneEmbeddable-" + strategy);
        
        // From => 4 instances
        // There are two parents but only one is Sub1
        // The sub1Value is doubled
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 202L);
    }
    
    @Test
    public void selectMultipleTreatedParentEmbeddableManyToOneEmbeddable() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because query is completely wrong
        // - SingleTable   : issues 1 query, FAILS because query is completely wrong
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT (SELECT SUM(TREAT(b.embeddable.parent AS " + strategy + "Sub1).embeddable1.sub1SomeValue) FROM IntIdEntity i WHERE i.name = b.name), (SELECT SUM(TREAT(b.embeddable.parent AS " + strategy + "Sub2).embeddable2.sub2SomeValue) FROM IntIdEntity i WHERE i.name = b.name) FROM " + strategy + "Base b", Object[].class);
        System.out.println("selectMultipleTreatedParentEmbeddableManyToOneEmbeddable-" + strategy);
        
        // From => 4 instances
        // There are two parents, one is Sub1 and the other Sub2
        // The sub1Value and sub2Value is doubled
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 202L, null });
        assertRemoved(bases, new Object[] { null, 204L });
    }
    
    @Test
    public void selectTreatedRootManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT TREAT(TREAT(b AS " + strategy + "Sub1).parent1 AS " + strategy + "Sub1).sub1Value FROM " + strategy + "Base b", Integer.class);
        System.out.println("selectTreatedRootManyToOne-" + strategy);
        
        // From => 4 instances
        // Inner join on b.parent => 2 instance
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void selectMultipleTreatedRootManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT TREAT(TREAT(b AS " + strategy + "Sub1).parent1 AS " + strategy + "Sub1).sub1Value, TREAT(TREAT(b AS " + strategy + "Sub2).parent2 AS " + strategy + "Sub2).sub2Value FROM " + strategy + "Base b", Object[].class);
        System.out.println("selectMultipleTreatedRootManyToOne-" + strategy);
        
        // From => 4 instances
        // Inner join on b.parent => 2 instance
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    public void selectTreatedParentRootManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because query is completely wrong
        // - SingleTable   : issues 1 query, successful BUT query is completely wrong
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT (SELECT SUM(TREAT(TREAT(b AS " + strategy + "Sub1).parent1 AS " + strategy + "Sub1).sub1Value) FROM IntIdEntity i WHERE i.name = b.name) FROM " + strategy + "Base b", Integer.class);
        System.out.println("selectTreatedParentRootManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents but only one is Sub1
        // The sub1Value is doubled
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 202L);
    }
    
    @Test
    public void selectMultipleTreatedParentRootManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because query is completely wrong
        // - SingleTable   : issues 1 query, successful BUT query is completely wrong
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT (SELECT SUM(TREAT(TREAT(b AS " + strategy + "Sub1).parent1 AS " + strategy + "Sub1).sub1Value) FROM IntIdEntity i WHERE i.name = b.name), (SELECT SUM(TREAT(TREAT(b AS " + strategy + "Sub2).parent2 AS " + strategy + "Sub2).sub2Value) FROM IntIdEntity i WHERE i.name = b.name) FROM " + strategy + "Base b", Object[].class);
        System.out.println("selectMultipleTreatedParentRootManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents, one is Sub1 and the other Sub2
        // The sub1Value and sub2Value is doubled
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 202L, null });
        assertRemoved(bases, new Object[] { null, 204L });
    }
    
    @Test
    public void selectTreatedRootEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT TREAT(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Parent AS " + strategy + "Sub1).sub1Value FROM " + strategy + "Base b", Integer.class);
        System.out.println("selectTreatedRootEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // Inner join on b.parent => 2 instance
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void selectMultipleTreatedRootEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT TREAT(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Parent AS " + strategy + "Sub1).sub1Value, TREAT(TREAT(b AS " + strategy + "Sub2).embeddable2.sub2Parent AS " + strategy + "Sub2).sub2Value FROM " + strategy + "Base b", Object[].class);
        System.out.println("selectMultipleTreatedRootEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // Inner join on b.parent => 2 instance
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    public void selectTreatedParentRootEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because query is completely wrong
        // - SingleTable   : issues 1 query, successful BUT query is completely wrong
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT (SELECT SUM(TREAT(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Parent AS " + strategy + "Sub1).sub1Value) FROM IntIdEntity i WHERE i.name = b.name) FROM " + strategy + "Base b", Integer.class);
        System.out.println("selectTreatedParentRootEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents but only one is Sub1
        // The sub1Value is doubled
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 202L);
    }
    
    @Test
    public void selectMultipleTreatedParentRootEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because query is completely wrong
        // - SingleTable   : issues 1 query, successful BUT query is completely wrong
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT (SELECT SUM(TREAT(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Parent AS " + strategy + "Sub1).sub1Value) FROM IntIdEntity i WHERE i.name = b.name), (SELECT SUM(TREAT(TREAT(b AS " + strategy + "Sub2).embeddable2.sub2Parent AS " + strategy + "Sub2).sub2Value) FROM IntIdEntity i WHERE i.name = b.name) FROM " + strategy + "Base b", Object[].class);
        System.out.println("selectMultipleTreatedParentRootEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents, one is Sub1 and the other Sub2
        // The sub1Value and sub2Value is doubled
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 202L, null });
        assertRemoved(bases, new Object[] { null, 204L });
    }
    
    @Test
    public void selectTreatedRootEmbeddableManyToOneEmbeddable() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT TREAT(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Parent AS " + strategy + "Sub1).embeddable1.sub1SomeValue FROM " + strategy + "Base b", Integer.class);
        System.out.println("selectTreatedRootEmbeddableManyToOneEmbeddable-" + strategy);
        
        // From => 4 instances
        // Inner join on b.parent => 2 instance
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void selectMultipleTreatedRootEmbeddableManyToOneEmbeddable() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT TREAT(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Parent AS " + strategy + "Sub1).embeddable1.sub1SomeValue, TREAT(TREAT(b AS " + strategy + "Sub2).embeddable2.sub2Parent AS " + strategy + "Sub2).embeddable2.sub2SomeValue FROM " + strategy + "Base b", Object[].class);
        System.out.println("selectMultipleTreatedRootEmbeddableManyToOneEmbeddable-" + strategy);
        
        // From => 4 instances
        // Inner join on b.parent => 2 instance
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    public void selectTreatedParentRootEmbeddableManyToOneEmbeddable() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because query is completely wrong
        // - SingleTable   : issues 1 query, successful BUT query is completely wrong
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT (SELECT SUM(TREAT(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Parent AS " + strategy + "Sub1).embeddable1.sub1SomeValue) FROM IntIdEntity i WHERE i.name = b.name) FROM " + strategy + "Base b", Integer.class);
        System.out.println("selectTreatedParentRootEmbeddableManyToOneEmbeddable-" + strategy);
        
        // From => 4 instances
        // There are two parents but only one is Sub1
        // The sub1Value is doubled
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 202L);
    }
    
    @Test
    public void selectMultipleTreatedParentRootEmbeddableManyToOneEmbeddable() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because query is completely wrong
        // - SingleTable   : issues 1 query, successful BUT query is completely wrong
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT (SELECT SUM(TREAT(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Parent AS " + strategy + "Sub1).embeddable1.sub1SomeValue) FROM IntIdEntity i WHERE i.name = b.name), (SELECT SUM(TREAT(TREAT(b AS " + strategy + "Sub2).embeddable2.sub2Parent AS " + strategy + "Sub2).embeddable2.sub2SomeValue) FROM IntIdEntity i WHERE i.name = b.name) FROM " + strategy + "Base b", Object[].class);
        System.out.println("selectMultipleTreatedParentRootEmbeddableManyToOneEmbeddable-" + strategy);
        
        // From => 4 instances
        // There are two parents, one is Sub1 and the other Sub2
        // The sub1Value and sub2Value is doubled
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 202L, null });
        assertRemoved(bases, new Object[] { null, 204L });
    }
    
    // JOIN
    
    @Test
    public void treatJoinManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, all successful
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : issues 1 query, FAILS because inner joins on Sub1 for parent relation => should always use left join from bottom up
        // - SingleTable   : issues 1 query, all successful
        // - TablePerClass : issues 1 query, all successful
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT s1.sub1Value FROM " + strategy + "Base b LEFT JOIN TREAT(b.parent AS " + strategy + "Sub1) s1", Integer.class);
        System.out.println("treatJoinManyToOne-" + strategy);
        
        // From => 4 instances
        // Left join on b.parent => 4 instance
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void treatJoinMultipleManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, all successful
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : issues 1 query, FAILS because inner joins on Sub1 for parent relation => should always use left join from bottom up
        // - SingleTable   : issues 1 query, all successful
        // - TablePerClass : issues 1 query, all successful
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT s1.sub1Value, s2.sub2Value FROM " + strategy + "Base b LEFT JOIN TREAT(b.parent AS " + strategy + "Sub1) s1 LEFT JOIN TREAT(b.parent AS " + strategy + "Sub2) s2", Object[].class);
        System.out.println("treatJoinMultipleManyToOne-" + strategy);
        
        // From => 4 instances
        // Left join on b.parent1 => 4 instance
        // Left join on b.parent2 => 4 instance
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    public void treatJoinParentManyToOne() {
        // EclipseLink
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(b.parent AS " + strategy + "Sub1) s1) FROM " + strategy + "Base b", Integer.class);
        System.out.println("treatJoinParentManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents but only one is Sub1
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void treatJoinMultipleParentManyToOne() {
        // EclipseLink
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(b.parent AS " + strategy + "Sub1) s1), (SELECT s2.sub2Value FROM TREAT(b.parent AS " + strategy + "Sub2) s2) FROM " + strategy + "Base b", Object[].class);
        System.out.println("treatJoinMultipleParentManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents, one is Sub1 and the other Sub2
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    public void treatJoinEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, all successful
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : issues 1 query, FAILS because inner joins on Sub1 for parent relation => should always use left join from bottom up
        // - SingleTable   : issues 1 query, all successful
        // - TablePerClass : issues 1 query, all successful
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT s1.sub1Value FROM " + strategy + "Base b LEFT JOIN TREAT(b.embeddable.parent AS " + strategy + "Sub1) s1", Integer.class);
        System.out.println("treatJoinEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // Left join on b.parent => 4 instance
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void treatJoinMultipleEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, all successful
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : issues 1 query, FAILS because inner joins on Sub1 for parent relation => should always use left join from bottom up
        // - SingleTable   : issues 1 query, all successful
        // - TablePerClass : issues 1 query, all successful
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT s1.sub1Value, s2.sub2Value FROM " + strategy + "Base b LEFT JOIN TREAT(b.embeddable.parent AS " + strategy + "Sub1) s1 LEFT JOIN TREAT(b.embeddable.parent AS " + strategy + "Sub2) s2", Object[].class);
        System.out.println("treatJoinMultipleEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // Left join on b.parent1 => 4 instance
        // Left join on b.parent2 => 4 instance
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    public void treatJoinParentEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(b.embeddable.parent AS " + strategy + "Sub1) s1) FROM " + strategy + "Base b", Integer.class);
        System.out.println("treatJoinParentEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents but only one is Sub1
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void treatJoinMultipleParentEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(b.embeddable.parent AS " + strategy + "Sub1) s1), (SELECT s2.sub2Value FROM TREAT(b.embeddable.parent AS " + strategy + "Sub2) s2) FROM " + strategy + "Base b", Object[].class);
        System.out.println("treatJoinMultipleParentEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents, one is Sub1 and the other Sub2
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void joinTreatedRoot() {
        // EclipseLink
        // - Joined        : not working, join path parsing fails
        // - SingleTable   : not working, join path parsing fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT s1.value FROM " + strategy + "Base b LEFT JOIN TREAT(b AS " + strategy + "Sub1).parent1 s1", Integer.class);
        System.out.println("joinTreatedRoot-" + strategy);
        
        // From => 4 instances
        // Left join on b.parent1 => 4 instance
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void joinMultipleTreatedRoot() {
        // EclipseLink
        // - Joined        : not working, join path parsing fails
        // - SingleTable   : not working, join path parsing fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT s1.value, s2.value FROM " + strategy + "Base b LEFT JOIN TREAT(b AS " + strategy + "Sub1).parent1 s1 LEFT JOIN TREAT(b AS " + strategy + "Sub2).parent2 s2", Object[].class);
        System.out.println("joinMultipleTreatedRoot-" + strategy);
        
        // From => 4 instances
        // Left join on b.parent1 => 4 instance
        // Left join on b.parent2 => 4 instance
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void joinTreatedParentRootManyToOne() {
        // EclipseLink
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(b AS " + strategy + "Sub1).parent1 s1) FROM " + strategy + "Base b", Integer.class);
        System.out.println("joinTreatedParentRootManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents but only one is Sub1
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void joinMultipleTreatedParentRootManyToOne() {
        // EclipseLink
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(b AS " + strategy + "Sub1).parent1 s1), (SELECT s2.sub2Value FROM TREAT(b AS " + strategy + "Sub2).parent2 s2) FROM " + strategy + "Base b", Object[].class);
        System.out.println("joinMultipleTreatedParentRootManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents, one is Sub1 and the other Sub2
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void joinTreatedRootEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : not working, join path parsing fails
        // - SingleTable   : not working, join path parsing fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT s1.value FROM " + strategy + "Base b LEFT JOIN TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Parent s1", Integer.class);
        System.out.println("joinTreatedRootEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // Left join on b.parent1 => 4 instance
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void joinMultipleTreatedRootEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : not working, join path parsing fails
        // - SingleTable   : not working, join path parsing fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT s1.value, s2.value FROM " + strategy + "Base b LEFT JOIN TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Parent s1 LEFT JOIN TREAT(b AS " + strategy + "Sub2).embeddable2.sub2Parent s2", Object[].class);
        System.out.println("joinMultipleTreatedRootEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // Left join on b.parent1 => 4 instance
        // Left join on b.parent2 => 4 instance
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void joinTreatedParentRootEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Parent s1) FROM " + strategy + "Base b", Integer.class);
        System.out.println("joinTreatedParentRootEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents but only one is Sub1
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void joinMultipleTreatedParentRootEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Parent s1), (SELECT s2.sub2Value FROM TREAT(b AS " + strategy + "Sub2).embeddable2.sub2Parent s2) FROM " + strategy + "Base b", Object[].class);
        System.out.println("joinMultipleTreatedParentRootEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents, one is Sub1 and the other Sub2
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
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
        
        // From => 4 instances
        // Left join on b.parent1 => 4 instance
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
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
        
        // From => 4 instances
        // Left join on b.parent1 => 4 instance
        // Left join on b.parent2 => 4 instance
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void treatJoinTreatedParentRootManyToOne() {
        // EclipseLink
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(TREAT(b AS " + strategy + "Sub1).parent1 AS " + strategy + "Sub1) s1) FROM " + strategy + "Base b", Integer.class);
        System.out.println("treatJoinTreatedParentRootManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents but only one is Sub1
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void treatJoinMultipleTreatedParentRootManyToOne() {
        // EclipseLink
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(TREAT(b AS " + strategy + "Sub1).parent1 AS " + strategy + "Sub1) s1), (SELECT s2.sub2Value FROM TREAT(TREAT(b AS " + strategy + "Sub2).parent2 AS " + strategy + "Sub2) s2) FROM " + strategy + "Base b", Object[].class);
        System.out.println("treatJoinMultipleTreatedParentRootManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents, one is Sub1 and the other Sub2
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void treatJoinTreatedRootEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT s1.sub1Value FROM " + strategy + "Base b LEFT JOIN TREAT(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Parent AS " + strategy + "Sub1) AS s1", Integer.class);
        System.out.println("treatJoinTreatedRootEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // Left join on b.parent1 => 4 instance
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void treatJoinMultipleTreatedRootEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : issues 1 query, FAILS because filters subtype
        // - SingleTable   : issues 1 query, FAILS because filters subtype
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT s1.sub1Value, s2.sub2Value FROM " + strategy + "Base b LEFT JOIN TREAT(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Parent AS " + strategy + "Sub1) AS s1 LEFT JOIN TREAT(TREAT(b AS " + strategy + "Sub2).embeddable2.sub2Parent AS " + strategy + "Sub2) AS s2", Object[].class);
        System.out.println("treatJoinMultipleTreatedRootEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // Left join on b.parent1 => 4 instance
        // Left join on b.parent2 => 4 instance
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void treatJoinTreatedParentRootEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Parent AS " + strategy + "Sub1) s1) FROM " + strategy + "Base b", Integer.class);
        System.out.println("treatJoinTreatedParentRootEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents but only one is Sub1
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void treatJoinMultipleTreatedParentRootEmbeddableManyToOne() {
        // EclipseLink
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, subquery parsing fails
        // - SingleTable   : not working, subquery parsing fails
        // - TablePerClass : not working, subquery parsing fails
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Parent AS " + strategy + "Sub1) s1), (SELECT s2.sub2Value FROM TREAT(TREAT(b AS " + strategy + "Sub2).embeddable2.sub2Parent AS " + strategy + "Sub2) s2) FROM " + strategy + "Base b", Object[].class);
        System.out.println("treatJoinMultipleTreatedParentRootEmbeddableManyToOne-" + strategy);
        
        // From => 4 instances
        // There are two parents, one is Sub1 and the other Sub2
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    /************************************************************
     * TREAT ONE-TO-MANY LIST
     ************************************************************/
    
    // TODO: Add testcases
    /************************************************************
     * TREAT ONE-TO-MANY INVERSE SET
     ************************************************************/
    // TODO: Add testcases
    
    /************************************************************
     * TREAT ONE-TO-MANY MAP KEY
     ************************************************************/
    // TODO: Add testcases
    
    /************************************************************
     * TREAT ONE-TO-MANY MAP VALUE
     ************************************************************/
    // TODO: Add testcases
    
    
    /************************************************************
     * Just some helper methods
     ************************************************************/
    
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
