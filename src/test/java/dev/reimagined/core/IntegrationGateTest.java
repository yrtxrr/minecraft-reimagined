package dev.reimagined.core;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IntegrationGateTest {
    @Test void absentModNeverResolvesAdapterClasses() {
        assertFalse(IntegrationGate.run("missing",Set.of(),() -> false,() -> {throw new AssertionError("Absent class resolved");}));
    }
    @Test void disabledAdapterNeverTestsPresenceOrLoadsClasses() {
        assertFalse(IntegrationGate.run("present",Set.of("present"),() -> {throw new AssertionError();},() -> {throw new AssertionError();}));
    }
    @Test void enabledPresentAdapterRunsExactlyOnce() {
        var calls=new AtomicInteger();
        assertTrue(IntegrationGate.run("present",Set.of(),() -> true,() -> calls::incrementAndGet));
        assertEquals(1,calls.get());
    }
}
