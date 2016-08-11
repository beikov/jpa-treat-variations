package jpa.test;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class RootTreatTest extends AbstractTreatVariationsTest {

    public RootTreatTest(String strategy, String objectPrefix) {
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
    
}
