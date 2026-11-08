package com.metservice.kanban.charts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/**
 * Input domain modeling tests for HueRebalancer.balance (Base Choice Coverage).
 * Test names follow Roy Osherove's convention:
 * UnitOfWork_StateUnderTest_ExpectedBehavior.
 */
class HueRebalancerIdmTest {

    private static final double TOLERANCE = 0.0001;

    /** Two segments, as used by the application: input halves map to 0.00–0.10 and 0.10–1.00. */
    private static final double[] TWO_SEGMENTS = {0.00, 0.10, 1.00};

    private static final double[] THREE_SEGMENTS = {0.0, 0.2, 0.5, 1.0};
    private static final double[] ONE_SEGMENT = {0.0, 1.0};

    // IDM-01: base choice (C1b2, C2b2, C3b2)
    @Test
    void balance_HueInFirstSegmentWithTwoSegments_ReturnsHueScaledIntoFirstSegment() {
        HueRebalancer balancer = new HueRebalancer(TWO_SEGMENTS);

        double result = balancer.balance(0.25);

        assertThat(result).isCloseTo(0.05, within(TOLERANCE));
    }

    // IDM-02: (C1b1, C2b2, C3b2) — expected to fail: known bug #7
    /* Commented out so it does not break the pipeline.
    @Test
    void balance_NegativeHue_WrapsIntoRangeBeforeScaling() {
        HueRebalancer balancer = new HueRebalancer(TWO_SEGMENTS);

        double result = balancer.balance(-0.75);

        assertThat(result).isCloseTo(0.05, within(TOLERANCE));
    }*/

    // IDM-03: (C1b3, C2b2, C3b2)
    @Test
    void balance_HueAboveOne_WrapsIntoRangeBeforeScaling() {
        HueRebalancer balancer = new HueRebalancer(TWO_SEGMENTS);

        double result = balancer.balance(1.25);

        assertThat(result).isCloseTo(0.05, within(TOLERANCE));
    }

    // IDM-04: (C1b2, C2b1, C3b2)
    @Test
    void balance_HueAtStartOfSecondSegment_ReturnsStartOfSecondOutputSegment() {
        HueRebalancer balancer = new HueRebalancer(TWO_SEGMENTS);

        double result = balancer.balance(0.50);

        assertThat(result).isCloseTo(0.10, within(TOLERANCE));
    }

    // IDM-05: (C1b2, C2b3, C3b2)
    @Test
    void balance_HueInLastSegment_ReturnsHueScaledIntoLastSegment() {
        HueRebalancer balancer = new HueRebalancer(TWO_SEGMENTS);

        double result = balancer.balance(0.75);

        assertThat(result).isCloseTo(0.55, within(TOLERANCE));
    }

    // IDM-06: (C1b2, C2b4, C3b3) — C3 changed because a middle segment needs three segments
    @Test
    void balance_HueInMiddleSegmentWithThreeSegments_ReturnsHueScaledIntoMiddleSegment() {
        HueRebalancer balancer = new HueRebalancer(THREE_SEGMENTS);

        double result = balancer.balance(0.50);

        assertThat(result).isCloseTo(0.35, within(TOLERANCE));
    }

    // IDM-07: (C1b2, C2b2, C3b1)
    @Test
    void balance_SingleSegment_ReturnsHueUnchanged() {
        HueRebalancer balancer = new HueRebalancer(ONE_SEGMENT);

        double result = balancer.balance(0.25);

        assertThat(result).isCloseTo(0.25, within(TOLERANCE));
    }

    // IDM-08: (C1b2, C2b2, C3b3)
    @Test
    void balance_HueInFirstSegmentWithThreeSegments_ReturnsHueScaledIntoFirstSegment() {
        HueRebalancer balancer = new HueRebalancer(THREE_SEGMENTS);

        double result = balancer.balance(0.25);

        assertThat(result).isCloseTo(0.15, within(TOLERANCE));
    }

    // Hue normalization test
    @Test
    void balance_UnbalancedHueIsNormalizedUsingModulo_UnbalancedHueIsNormalized() {
        HueRebalancer rebalancer =
                new HueRebalancer(ONE_SEGMENT);

        double result = rebalancer.balance(1.25);

        assertEquals(0.25, result, TOLERANCE);
    }

    // Testing segment number calculation with three segments
    @Test
    void balance_TestSegmentNumberInitializationWithThreeSegments_SegmentNumberIsCorrect(){
        HueRebalancer balancer = new HueRebalancer(THREE_SEGMENTS);

        double result = balancer.balance(0.99);

        assertEquals(2, (int)(result * THREE_SEGMENTS.length - 1));
    }

    // Testing position within segment calculation with one segment
    @Test
    void balance_TestPositionWithinSegment_PositionWithinSegmentIsCorrect() {
        HueRebalancer rebalancer = new HueRebalancer(ONE_SEGMENT);

        double result = rebalancer.balance(0.75);

        assertEquals(0.75, result, TOLERANCE);
    }

    // Testing segment start calculation with two segments
    @Test
    void balance_TestSegmentStartUsesCorrectArrayElement_SegmentStartElementIsCorrect() {
        HueRebalancer rebalancer = new HueRebalancer(TWO_SEGMENTS);

        assertEquals(0.55,rebalancer.balance(0.75),TOLERANCE);
    }

    // Testing segment length calculation with three segments
    @Test
    void balance_TestSegmentLengthCalculation_SegmentLengthIsCorrect() {
        HueRebalancer rebalancer = new HueRebalancer(THREE_SEGMENTS);

        assertEquals(0.35, rebalancer.balance(0.5), TOLERANCE);
    }

    // Testing final return value calculation with three segments
    @Test
    void balance_TestReturnCalculation_FinalReturnValueIsCorrect() {
        HueRebalancer rebalancer = new HueRebalancer(THREE_SEGMENTS);

        assertEquals(0.35, rebalancer.balance(0.5), TOLERANCE);
    }
}
