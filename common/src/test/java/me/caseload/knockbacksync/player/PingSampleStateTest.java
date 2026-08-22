package me.caseload.knockbacksync.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PingSampleStateTest {
    @Test
    void preservesFallbackPreviousPingAndJitterSemantics() {
        PingSampleState state = new PingSampleState();

        assertEquals(91.0, state.getPingOr(91.0));
        assertEquals(91.0, state.getPreviousPingOr(91.0));

        state.record(40.0);
        assertEquals(40.0, state.getPing());
        assertNull(state.getPreviousPing());
        assertEquals(91.0, state.getPreviousPingOr(91.0));

        state.record(50.0);
        state.record(60.0);
        state.record(70.0);
        assertEquals(70.0, state.getPing());
        assertEquals(60.0, state.getPreviousPing());
        assertTrue(state.getJitter() > 0.0);

        state.reset();
        assertNull(state.getPing());
        assertNull(state.getPreviousPing());
        assertEquals(0.0, state.getJitter());
        assertEquals(91.0, state.getPingOr(91.0));
    }
}
