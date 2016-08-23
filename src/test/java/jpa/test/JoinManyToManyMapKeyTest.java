package jpa.test;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JoinManyToManyMapKeyTest extends AbstractTreatVariationsTest {

    public JoinManyToManyMapKeyTest(String strategy, String objectPrefix) {
        super(strategy, objectPrefix);
    }
    
    @Parameterized.Parameters
    public static Object[] getParameters() {
        return new Object[] {
            new Object[] { "Joined", "s" }, 
            new Object[] { "SingleTable", "st" }, 
            new Object[] { "TablePerClass", "tpc" }
        };
    }
    
    @Test
    public void treatJoinManyToManyMapKey() {
        // EclipseLink
        // - Joined        : not working, parsing key fails
        // - SingleTable   : not working, parsing key fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT s1.sub1Value FROM " + strategy + "Base b LEFT JOIN TREAT(KEY(b.map) AS " + strategy + "Sub1) s1", Integer.class);
        System.out.println("treatJoinManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // Left join on KEY(b.map) => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 1);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void treatJoinMultipleManyToManyMapKey() {
        // EclipseLink
        // - Joined        : not working, parsing key fails
        // - SingleTable   : not working, parsing key fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT s1.sub1Value, s2.sub2Value FROM " + strategy + "Base b LEFT JOIN TREAT(KEY(b.map) AS " + strategy + "Sub1) s1 LEFT JOIN TREAT(KEY(b.map) AS " + strategy + "Sub2) s2", Object[].class);
        System.out.println("treatJoinMultipleManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // Left join on KEY(b.map) => 4 instances
        // Left join on KEY(b.map) => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { 1,    null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 2    });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    public void treatJoinParentManyToManyMapKey() {
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
        List<Integer> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(KEY(b.map) AS " + strategy + "Sub1) s1) FROM " + strategy + "Base b", Integer.class);
        System.out.println("treatJoinParentManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // There are four map keys but only two are Sub1
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 1);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void treatJoinMultipleParentManyToManyMapKey() {
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
        List<Object[]> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(KEY(b.map) AS " + strategy + "Sub1) s1), (SELECT s2.sub2Value FROM TREAT(KEY(b.map) AS " + strategy + "Sub2) s2) FROM " + strategy + "Base b", Object[].class);
        System.out.println("treatJoinMultipleParentManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // There are four map keys, two are Sub1 and the other are Sub2
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { 1,    null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 2    });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    public void treatJoinEmbeddableManyToManyMapKey() {
        // EclipseLink
        // - Joined        : not working, parsing key fails
        // - SingleTable   : not working, parsing key fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT s1.sub1Value FROM " + strategy + "Base b LEFT JOIN TREAT(KEY(b.embeddable.map) AS " + strategy + "Sub1) s1", Integer.class);
        System.out.println("treatJoinEmbeddableManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // Left join on KEY(b.embeddable.map) => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 1);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void treatJoinMultipleEmbeddableManyToManyMapKey() {
        // EclipseLink
        // - Joined        : not working, parsing key fails
        // - SingleTable   : not working, parsing key fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT s1.sub1Value, s2.sub2Value FROM " + strategy + "Base b LEFT JOIN TREAT(KEY(b.embeddable.map) AS " + strategy + "Sub1) s1 LEFT JOIN TREAT(KEY(b.embeddable.map) AS " + strategy + "Sub2) s2", Object[].class);
        System.out.println("treatJoinMultipleEmbeddableManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // Left join on KEY(b.embeddable.map) => 4 instances
        // Left join on KEY(b.embeddable.map) => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { 1,    null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 2    });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    public void treatJoinParentEmbeddableManyToManyMapKey() {
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
        List<Integer> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(KEY(b.embeddable.map) AS " + strategy + "Sub1) s1) FROM " + strategy + "Base b", Integer.class);
        System.out.println("treatJoinParentEmbeddableManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // There are four map keys but only two are Sub1
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 1);
        assertRemoved(bases, 101);
    }
    
    @Test
    public void treatJoinMultipleParentEmbeddableManyToManyMapKey() {
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
        List<Object[]> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(KEY(b.embeddable.map) AS " + strategy + "Sub1) s1), (SELECT s2.sub2Value FROM TREAT(KEY(b.embeddable.map) AS " + strategy + "Sub2) s2) FROM " + strategy + "Base b", Object[].class);
        System.out.println("treatJoinMultipleParentEmbeddableManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // There are four map keys, two are Sub1 and the other are Sub2
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { 1,    null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 2    });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void joinTreatedRootManyToManyMapKey() {
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
        List<Integer> bases = list("SELECT s1.value FROM " + strategy + "Base b LEFT JOIN KEY(TREAT(b AS " + strategy + "Sub1).map1) s1", Integer.class);
        System.out.println("joinTreatedRootManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // Left join on KEY(b.map1) => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void joinMultipleTreatedRootManyToManyMapKey() {
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
        List<Object[]> bases = list("SELECT s1.value, s2.value FROM " + strategy + "Base b LEFT JOIN KEY(TREAT(b AS " + strategy + "Sub1).map1) s1 LEFT JOIN KEY(TREAT(b AS " + strategy + "Sub2).map2) s2", Object[].class);
        System.out.println("joinMultipleTreatedRootManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // Left join on KEY(b.map1) => 4 instances
        // Left join on KEY(b.map2) => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void joinTreatedParentRootManyToManyMapKey() {
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
        List<Integer> bases = list("SELECT (SELECT s1.sub1Value FROM KEY(TREAT(b AS " + strategy + "Sub1).map1) s1) FROM " + strategy + "Base b", Integer.class);
        System.out.println("joinTreatedParentRootManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // There are two map keys but only one is Sub1
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void joinMultipleTreatedParentRootManyToManyMapKey() {
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
        List<Object[]> bases = list("SELECT (SELECT s1.sub1Value FROM KEY(TREAT(b AS " + strategy + "Sub1).map1) s1), (SELECT s2.sub2Value FROM KEY(TREAT(b AS " + strategy + "Sub2).map2) s2) FROM " + strategy + "Base b", Object[].class);
        System.out.println("joinMultipleTreatedParentRootManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // There are two map keys, one is Sub1 and the other Sub2
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void joinTreatedRootEmbeddableManyToManyMapKey() {
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
        List<Integer> bases = list("SELECT s1.value FROM " + strategy + "Base b LEFT JOIN KEY(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Map) s1", Integer.class);
        System.out.println("joinTreatedRootEmbeddableManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // Left join on b.embeddable.sub1Parent => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void joinMultipleTreatedRootEmbeddableManyToManyMapKey() {
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
        List<Object[]> bases = list("SELECT s1.value, s2.value FROM " + strategy + "Base b LEFT JOIN KEY(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Map) s1 LEFT JOIN KEY(TREAT(b AS " + strategy + "Sub2).embeddable2.sub2Map) s2", Object[].class);
        System.out.println("joinMultipleTreatedRootEmbeddableManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // Left join on KEY(b.embeddable1.sub1Map) => 4 instances
        // Left join on KEY(b.embeddable2.sub2Map) => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void joinTreatedParentRootEmbeddableManyToManyMapKey() {
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
        List<Integer> bases = list("SELECT (SELECT s1.sub1Value FROM KEY(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Map) s1) FROM " + strategy + "Base b", Integer.class);
        System.out.println("joinTreatedParentRootEmbeddableManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // There are two map keys but only one is Sub1
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void joinMultipleTreatedParentRootEmbeddableManyToManyMapKey() {
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
        List<Object[]> bases = list("SELECT (SELECT s1.sub1Value FROM KEY(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Map) s1), (SELECT s2.sub2Value FROM KEY(TREAT(b AS " + strategy + "Sub2).embeddable2.sub2Map) s2) FROM " + strategy + "Base b", Object[].class);
        System.out.println("joinMultipleTreatedParentRootEmbeddableManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // There are two map keys, one is Sub1 and the other Sub2
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void treatJoinTreatedRootManyToManyMapKey() {
        // EclipseLink
        // - Joined        : not working, parsing key fails
        // - SingleTable   : not working, parsing key fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT s1.sub1Value FROM " + strategy + "Base b LEFT JOIN TREAT(KEY(TREAT(b AS " + strategy + "Sub1).map1) AS " + strategy + "Sub1) AS s1", Integer.class);
        System.out.println("treatJoinTreatedRootManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // Left join on KEY(b.map1) => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void treatJoinMultipleTreatedRootManyToManyMapKey() {
        // EclipseLink
        // - Joined        : not working, parsing key fails
        // - SingleTable   : not working, parsing key fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT s1.sub1Value, s2.sub2Value FROM " + strategy + "Base b LEFT JOIN TREAT(KEY(TREAT(b AS " + strategy + "Sub1).map1) AS " + strategy + "Sub1) AS s1 LEFT JOIN TREAT(KEY(TREAT(b AS " + strategy + "Sub2).map2) AS " + strategy + "Sub2) AS s2", Object[].class);
        System.out.println("treatJoinMultipleTreatedRooManyToManyMapKeyt-" + strategy);
        
        // From => 4 instances
        // Left join on KEY(b.map1) => 4 instances
        // Left join on KEY(b.map2) => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void treatJoinTreatedParentRootManyToManyMapKey() {
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
        List<Integer> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(KEY(TREAT(b AS " + strategy + "Sub1).map1) AS " + strategy + "Sub1) s1) FROM " + strategy + "Base b", Integer.class);
        System.out.println("treatJoinTreatedParentRootManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // There are two map keys but only one is Sub1
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void treatJoinMultipleTreatedParentRootManyToManyMapKey() {
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
        List<Object[]> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(KEY(TREAT(b AS " + strategy + "Sub1).map1) AS " + strategy + "Sub1) s1), (SELECT s2.sub2Value FROM TREAT(KEY(TREAT(b AS " + strategy + "Sub2).map2) AS " + strategy + "Sub2) s2) FROM " + strategy + "Base b", Object[].class);
        System.out.println("treatJoinMultipleTreatedParentRootManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // There are two map keys, one is Sub1 and the other Sub2
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void treatJoinTreatedRootEmbeddableManyToManyMapKey() {
        // EclipseLink
        // - Joined        : not working, parsing key fails
        // - SingleTable   : not working, parsing key fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Integer> bases = list("SELECT s1.sub1Value FROM " + strategy + "Base b LEFT JOIN TREAT(KEY(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Map) AS " + strategy + "Sub1) AS s1", Integer.class);
        System.out.println("treatJoinTreatedRootEmbeddableManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // Left join on KEY(b.embeddable1.sub1Map) => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void treatJoinMultipleTreatedRootEmbeddableManyToManyMapKey() {
        // EclipseLink
        // - Joined        : not working, parsing key fails
        // - SingleTable   : not working, parsing key fails
        // - TablePerClass : not working, strategy unsupported
        // Hibernate
        // - Joined        : not working, treated paths unsupported
        // - SingleTable   : not working, treated paths unsupported
        // - TablePerClass : not working, treated paths unsupported
        // DataNucleus
        // - Joined        : 
        // - SingleTable   : 
        // - TablePerClass : 
        List<Object[]> bases = list("SELECT s1.sub1Value, s2.sub2Value FROM " + strategy + "Base b LEFT JOIN TREAT(KEY(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Map) AS " + strategy + "Sub1) AS s1 LEFT JOIN TREAT(KEY(TREAT(b AS " + strategy + "Sub2).embeddable2.sub2Map) AS " + strategy + "Sub2) AS s2", Object[].class);
        System.out.println("treatJoinMultipleTreatedRootEmbeddableManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // Left join on KEY(b.embeddable1.sub1Map) => 4 instances
        // Left join on KEY(b.embeddable2.sub2Map) => 4 instances
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void treatJoinTreatedParentRootEmbeddableManyToManyMapKey() {
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
        List<Integer> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(KEY(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Map) AS " + strategy + "Sub1) s1) FROM " + strategy + "Base b", Integer.class);
        System.out.println("treatJoinTreatedParentRootEmbeddableManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // There are two map keys but only one is Sub1
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, null);
        assertRemoved(bases, 101);
    }
    
    @Test
    // NOTE: This is a special case that the JPA spec does not cover but is required to make TREAT complete
    public void treatJoinMultipleTreatedParentRootEmbeddableManyToManyMapKey() {
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
        List<Object[]> bases = list("SELECT (SELECT s1.sub1Value FROM TREAT(KEY(TREAT(b AS " + strategy + "Sub1).embeddable1.sub1Map) AS " + strategy + "Sub1) s1), (SELECT s2.sub2Value FROM TREAT(KEY(TREAT(b AS " + strategy + "Sub2).embeddable2.sub2Map) AS " + strategy + "Sub2) s2) FROM " + strategy + "Base b", Object[].class);
        System.out.println("treatJoinMultipleTreatedParentRootEmbeddableManyToManyMapKey-" + strategy);
        
        // From => 4 instances
        // There are two map keys, one is Sub1 and the other Sub2
        Assert.assertEquals(4, bases.size());
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { null, null });
        assertRemoved(bases, new Object[] { 101,  null });
        assertRemoved(bases, new Object[] { null, 102  });
    }
    
}
