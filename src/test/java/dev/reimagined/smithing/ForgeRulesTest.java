package dev.reimagined.smithing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ForgeRulesTest {
    @Test void qualityRequiresBothTrainingAndEquipmentAndNeverExceedsThree(){
        assertEquals(2,ForgeRules.quality(2,0,true));assertEquals(2,ForgeRules.quality(2,2,false));
        assertEquals(3,ForgeRules.quality(2,2,true));assertEquals(3,ForgeRules.quality(2,3,true));
        assertEquals(2,ForgeRules.quality(1,3,true));
    }
    @Test void treatmentsGiveDeclaredFiniteDurability(){
        assertEquals(275,ForgeRules.durability(250,1,false));assertEquals(300,ForgeRules.durability(250,1,true));
        assertEquals(312,ForgeRules.durability(250,2,false));assertEquals(337,ForgeRules.durability(250,2,true));
        assertEquals(350,ForgeRules.durability(250,3,false));assertEquals(375,ForgeRules.durability(250,3,true));
    }
    @Test void scalingNeverRepairsTheWearFractionOrBreaksAnIntactItem(){
        var random=new java.util.Random(7211);
        for(int i=0;i<10000;i++){
            int oldMax=1+random.nextInt(1000000),damage=random.nextInt(oldMax);
            int improved=ForgeRules.durability(oldMax,1+random.nextInt(3),random.nextBoolean());
            int result=ForgeRules.preserveWear(damage,oldMax,improved);
            assertTrue(result>=0 && result<improved);
            assertTrue((long)result*oldMax>=(long)damage*improved);
            assertTrue((long)result*oldMax<(long)damage*improved+oldMax);
        }
    }
    @Test void hugeDurabilityDoesNotOverflowOrCreateAFreeRepair(){
        assertEquals(Integer.MAX_VALUE,ForgeRules.durability(Integer.MAX_VALUE,3,true));
        assertEquals(Integer.MAX_VALUE-1,ForgeRules.preserveWear(Integer.MAX_VALUE-1,Integer.MAX_VALUE,Integer.MAX_VALUE));
        assertEquals(0,ForgeRules.preserveWear(0,1,1000));
        assertThrows(IllegalArgumentException.class,()->ForgeRules.preserveWear(250,250,350));
    }
}
