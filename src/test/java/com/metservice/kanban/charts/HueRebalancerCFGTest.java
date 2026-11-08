package com.metservice.kanban.charts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Input domain modeling tests for HueRebalancer.balance (Base Choice Coverage).
 * Test names follow Roy Osherove's convention:
 * UnitOfWork_StateUnderTest_ExpectedBehavior.
 */
class HueRebalancerCFGTest {

    private static final double TOLERANCE = 0.0001;

    /** Two segments, as used by the application: input halves map to 0.00–0.10 and 0.10–1.00. */
    private static final double[] THREE_SEGMENTS = {0.0, 0.2, 0.5, 1.0};
    private static final double[] ZERO_SEGMENT = {0.5};

    // Testing final return value calculation with three segments. This covers the CFG path through the balance method that calculates the final return value.
    @Test
    void balance_TestReturnCalculation_FinalReturnValueIsCorrect() {
        HueRebalancer rebalancer = new HueRebalancer(THREE_SEGMENTS);

        assertEquals(0.35, rebalancer.balance(0.5), TOLERANCE);
    }

    // Tests segmentStart ArrayIndexOutOfBoundsException with negative hue input. This covers the CFG path through the balance method that calculates the segmentStart and throws an exception for negative hue input.
    @Test
    void balance_NegativeHue_ThrowsArrayIndexOutOfBoundsException() {
        HueRebalancer rebalancer = new HueRebalancer(THREE_SEGMENTS);

        assertThrows(ArrayIndexOutOfBoundsException.class,() -> rebalancer.balance(-0.5));
    }

    // Tests segmentLength ArrayIndexOutOfBoundsException.
    @Test
    void balance_throwsOnSecondArrayReadWhenThereIsOnlyOneHue_ThrowsArrayIndexOutOfBoundsException() {
        HueRebalancer balancer = new HueRebalancer(ZERO_SEGMENT);
        
        assertThrows(ArrayIndexOutOfBoundsException.class,() -> balancer.balance(0.3));
    }
}
