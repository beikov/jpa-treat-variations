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
import org.junit.experimental.categories.Category;
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
    
    public TreatVariationsTest(String strategy) {
        this.strategy = strategy;
    }
    
    @Parameterized.Parameters
    public static Object[] getParameters() {
        return new Object[] {
            "Joined", 
            "SingleTable", 
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
        
        persist(em, i1, tpc1, tpc2, tpc1Parent, tpc2Parent);
        
        tx.commit();
        em.close();
    }
    
    private void persist(
            EntityManager em,
            IntIdEntity i1,
            Sub1<? extends Base<?, ?>, ? extends BaseEmbeddable<?>> s1,
            Sub2<? extends Base<?, ?>, ? extends BaseEmbeddable<?>> s2,
            Sub1<? extends Base<?, ?>, ? extends BaseEmbeddable<?>> s1Parent,
            Sub2<? extends Base<?, ?>, ? extends BaseEmbeddable<?>> s2Parent) {
        
        
        em.persist(s1Parent);
        em.persist(s2Parent);
        em.persist(s1);
        em.persist(s2);
        
        s1Parent.setSub1Value(101);
        s1Parent.getSub1Embeddable().setSomeValue(1001);
        s1.setSub1Value(1);
        s1.setRelation1(i1);
        ((Sub1) s1).setParent(s1Parent);
        ((Sub1) s1).setParent1(s1Parent);
        ((List<Base<?, ?>>) s1.getList()).add(s1Parent);
        ((List<Base<?, ?>>) s1.getList()).add(s2);
        ((Map<String, Base<?, ?>>) s1.getMap()).put(s1Parent.getName(), s1Parent);
        ((Map<String, Base<?, ?>>) s1.getMap()).put(s2.getName(), s2);
        
        s2Parent.setSub2Value(102);
        s2Parent.getSub2Embeddable().setSomeValue(1002);
        s2.setSub2Value(2);
        s2.setRelation2(i1);
        ((Sub2) s2).setParent(s2Parent);
        ((Sub2) s2).setParent2(s2Parent);
        ((List<Base<?, ?>>) s2.getList()).add(s2Parent);
        ((List<Base<?, ?>>) s2.getList()).add(s1);
        ((Map<String, Base<?, ?>>) s2.getMap()).put(s2Parent.getName(), s2Parent);
        ((Map<String, Base<?, ?>>) s2.getMap()).put(s1.getName(), s1);
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
    public void selectTreatedRoot() {
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
        System.out.println("selectTreatedRoot-" + strategy);
        
        // From => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 1);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void selectMultipleTreatedRoot() {
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
        System.out.println("selectMultipleTreatedRoot-" + strategy);
        
        // From => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { 1,    null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 2    });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    public void selectTreatedParentRoot() {
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
        System.out.println("selectTreatedParentRoot-" + strategy);
        
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
    public void selectMultipleTreatedParentRoot() {
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
        System.out.println("selectMultipleTreatedParentRoot-" + strategy);
        
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
    public void whereTreatedRoot() {
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
        List<Integer> bases = list("SELECT TREAT(b AS " + strategy + "Sub1).sub1Value FROM " + strategy + "Base b WHERE COALESCE(TREAT(b AS " + strategy + "Sub1).sub1Value, 0) < 100", Integer.class);
        System.out.println("whereTreatedRoot-" + strategy);
        
        // From => 4 instances
        // Where => 3 instances because 1 Sub1 has sub1Value 101, the other 1 and Sub2 use 0 because of coalesce
        Assert.assertEquals(3, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 1);
    }
    
    @Test
    public void whereMultipleTreatedRoot() {
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
        List<Integer> bases = list("SELECT COALESCE(TREAT(b AS " + strategy + "Sub1).sub1Value, TREAT(b AS " + strategy + "Sub2).sub2Value) FROM " + strategy + "Base b WHERE COALESCE(TREAT(b AS " + strategy + "Sub1).sub1Value, 0) < 100 AND COALESCE(TREAT(b AS " + strategy + "Sub2).sub2Value, 0) < 100", Integer.class);
        System.out.println("whereMultipleTreatedRoot-" + strategy);
        
        // From => 4 instances
        // Where => 2 instances because 1 Sub1 has sub1Value 101, the other 1 and Sub2 use 0 because of coalesce
        // and 1 Sub2 has sub2Value 102, the other 2 and Sub1 uses 0 because of coalesce
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, 1);
        assertRemoved(bases, 2);
    }
    
    /************************************************************
     * TREAT MANY-TO-ONE
     ************************************************************/
    
    @Test
    public void selectTreatedRelation() {
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
        System.out.println("selectTreatedRelation-" + strategy);
        
        // From => 4 instances
        // Inner join on b.parent => 2 instance
        Assert.assertEquals(2, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void selectMultipleTreatedRelation() {
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
        System.out.println("selectMultipleTreatedRelation-" + strategy);
        
        // From => 4 instances
        // Inner join on b.parent => 2 instance
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
        List<Integer> bases = list("SELECT TREAT(s1 AS " + strategy + "Sub1).sub1Value FROM " + strategy + "Base b LEFT JOIN TREAT(b AS " + strategy + "Sub1).parent1 s1", Integer.class);
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
        List<Object[]> bases = list("SELECT TREAT(s1 AS " + strategy + "Sub1).sub1Value, TREAT(s2 AS " + strategy + "Sub2).sub2Value FROM " + strategy + "Base b LEFT JOIN TREAT(b AS " + strategy + "Sub1).parent1 s1  LEFT JOIN TREAT(b AS " + strategy + "Sub2).parent2 s2", Object[].class);
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
    public void selectTreatJoinedRelation() {
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
        System.out.println("selectTreatJoinedRelation-" + strategy);
        
        // From => 4 instances
        // Left join on b.parent => 4 instance
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void selectMultipleTreatJoinedRelation() {
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
        System.out.println("selectMultipleTreatJoinedRelation-" + strategy);
        
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
    public void selectTreatJoinedParentRoot() {
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
        System.out.println("selectTreatJoinedParentRoot-" + strategy);
        
        // From => 4 instances
        // There are two parents but only one is Sub1
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void selectMultipleTreatJoinedParentRoot() {
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
        System.out.println("selectMultipleTreatedParentRoot-" + strategy);
        
        // From => 4 instances
        // There are two parents, one is Sub1 and the other Sub2
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    public void selectTreatJoinedEmbeddableRelation() {
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
        System.out.println("selectTreatJoinedEmbeddableRelation-" + strategy);
        
        // From => 4 instances
        // Left join on b.parent => 4 instance
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void selectMultipleTreatJoinedEmbeddableRelation() {
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
        System.out.println("selectMultipleTreatJoinedEmbeddableRelation-" + strategy);
        
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
    public void selectTreatedRelationEmbeddableValue() {
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
        System.out.println("selectTreatedRelationEmbeddableValue-" + strategy);
        
        // From => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 1001);
    }
    
    @Test
    public void selectMultipleTreatedRelationEmbeddableValue() {
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
        System.out.println("selectMultipleTreatedRelationEmbeddableValue-" + strategy);
        
        // From => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 1001, null });
        assertRemoved(bases, new Object[] { null, 1002 });
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
