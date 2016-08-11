package jpa.test;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JoinOneToManyInverseSetTest extends AbstractTreatVariationsTest {

    public JoinOneToManyInverseSetTest(String strategy, String objectPrefix) {
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
    
    // TODO: Add testcases
    
}
