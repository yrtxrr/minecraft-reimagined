package dev.reimagined.research;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JournalPageBudgetTest {
    @Test void everyFullBudgetPartitionStaysStrictlyBelowOneHundredPages() {
        for(int a=0;a<=256;a++)for(int b=0;b<=256-a;b++) {
            int c=256-a-b,p=JournalPageBudget.entriesPerPage(a,b,c);
            int total=14+Math.max(1,(a+p-1)/p)+Math.max(1,(b+p-1)/p)+Math.max(1,(c+p-1)/p);
            assertTrue(total<100,"Full discovery budget must fit the vanilla book even with empty catalogues");
        }
    }
    @Test void ordinaryJournalsKeepExistingThreeEntryLayout() {
        assertEquals(3,JournalPageBudget.entriesPerPage(2,4,3));
        assertEquals(3,JournalPageBudget.entriesPerPage(0,0,0));
    }
}
