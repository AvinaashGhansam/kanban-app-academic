package com.metservice.kanban.model;

import static com.metservice.kanban.utils.DateUtils.parseIsoDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for WorkItem.
 *
 * <p>Phase behaviour is modelled as a directed graph G = (V, E) where V = {unphased, Backlog, In Progress, Testing,
 * Completed}. {@code advance} contributes the edges of the path graph unphased → Backlog → … → Completed,
 * {@code setDate} contributes jump edges (skipping vertices) and back edges (clearing a date), and Completed is the
 * only sink. Tests are selected for node, edge and edge-pair coverage of G, plus prime-path coverage of the loops in
 * {@code getPhaseOnDate}, {@code getLastComment} and {@code getLastBlockedComment}.
 */
class WorkItemTest {

    private static final String BACKLOG = "Backlog";
    private static final String IN_PROGRESS = "In Progress";
    private static final String TESTING = "Testing";
    private static final String COMPLETED = "Completed";
    private static final String UNKNOWN_PHASE = "Not A Phase";

    private static final LocalDate EPOCH = parseIsoDate("1970-01-01");
    private static final LocalDate MON_WEEK_1 = parseIsoDate("2013-03-04");
    private static final LocalDate WED_WEEK_1 = parseIsoDate("2013-03-06");
    private static final LocalDate MON_WEEK_2 = parseIsoDate("2013-03-11");
    private static final LocalDate MON_WEEK_3 = parseIsoDate("2013-03-18");
    private static final LocalDate MON_WEEK_4 = parseIsoDate("2013-03-25");

    private final WorkItemType type = new WorkItemType(BACKLOG, IN_PROGRESS, TESTING, COMPLETED);

    private WorkItem makeItem() {
        return new WorkItem(1, type);
    }

    private WorkItem makeItemWithPhaseDates(LocalDate... dates) {
        WorkItem item = makeItem();
        for (int i = 0; i < dates.length; i++) {
            item.setDate(type.getPhases().get(i), dates[i]);
        }
        return item;
    }

    private static WorkItemComment makeComment(String addedBy, String text, String addedOn) {
        WorkItemComment comment = mock(WorkItemComment.class);
        when(comment.getAddedBy()).thenReturn(addedBy);
        when(comment.getCommentText()).thenReturn(text);
        when(comment.getWhenAdded()).thenReturn(LocalDateTime.parse(addedOn));
        return comment;
    }

    @Test
    void constructor_NoPhaseGiven_ItemIsUnphased() {
        WorkItem item = makeItem();

        assertThat(item.getDatesByPhase()).isEmpty();
        assertThat(item.getLastPhaseDate()).isNull();
    }

    @Test
    void constructor_NoParentGiven_ItemIsTopLevel() {
        WorkItem item = makeItem();

        assertThat(item.getParentId()).isEqualTo(WorkItem.ROOT_WORK_ITEM_ID);
        assertThat(item.isTopLevel()).isTrue();
    }

    @Test
    void constructor_ParentGiven_ItemIsNotTopLevel() {
        WorkItem item = new WorkItem(2, 1, type);

        assertThat(item.getParentId()).isEqualTo(1);
        assertThat(item.isTopLevel()).isFalse();
    }

    @Test
    void constructor_Defaults_FieldsHaveDefaultValues() {
        WorkItem item = makeItem();

        assertThat(item.getName()).isEmpty();
        assertThat(item.getNotes()).isEmpty();
        assertThat(item.getAverageCaseEstimate()).isZero();
        assertThat(item.getWorstCaseEstimate()).isZero();
        assertThat(item.getImportance()).isZero();
        assertThat(item.isExcluded()).isFalse();
        assertThat(item.isBlocked()).isFalse();
        assertThat(item.isMustHave()).isFalse();
        assertThat(item.getColour().toString()).isEqualTo("#FFFFFF");
        assertThat(item.getComments()).isEmpty();
    }

    @Test
    void constructor_AdvanceToMiddlePhase_EveryPhaseOnPathIsDatedAtEpoch() {
        WorkItem item = new WorkItem(1, type, TESTING);

        assertThat(item.getCurrentPhase()).isEqualTo(TESTING);
        assertThat(item.getDatesByPhase())
                .containsOnlyKeys(BACKLOG, IN_PROGRESS, TESTING)
                .allSatisfy((phase, date) -> assertThat(date).isEqualTo(EPOCH));
    }

    @Test
    void constructor_AdvanceToLastPhase_ItemIsCompleted() {
        WorkItem item = new WorkItem(1, type, COMPLETED);

        assertThat(item.isCompleted()).isTrue();
    }

    @Test
    void constructor_AdvanceToUnknownPhase_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> new WorkItem(1, type, UNKNOWN_PHASE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("cannot advance; named phase does not exist: " + UNKNOWN_PHASE);
    }

    @Test
    void constructor_AdvanceToNullPhase_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> new WorkItem(1, type, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getCurrentPhase_Unphased_ThrowsIllegalStateException() {
        WorkItem item = makeItem();

        assertThatThrownBy(item::getCurrentPhase)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("work item is not yet in any phase");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        ", Backlog",
        "Backlog, In Progress",
        "In Progress, Testing",
        "Testing, Completed",
    })
    void advance_FromEachVertex_TraversesEdgeToNextPhase(String from, String to) {
        WorkItem item = from == null ? makeItem() : new WorkItem(1, type, from);

        item.advance(MON_WEEK_1);

        assertThat(item.getCurrentPhase()).isEqualTo(to);
        assertThat(item.getDate(to)).isEqualTo(MON_WEEK_1);
    }

    @Test
    void advance_WalkFromSourceToSink_VisitsEveryPhaseInOrder() {
        WorkItem item = makeItem();
        List<String> visited = new ArrayList<String>();

        while (!item.isCompleted()) {
            item.advance(MON_WEEK_1);
            visited.add(item.getCurrentPhase());
        }

        assertThat(visited).containsExactly(BACKLOG, IN_PROGRESS, TESTING, COMPLETED);
    }

    @Test
    void advance_FromSinkPhase_ThrowsIllegalStateException() {
        WorkItem item = new WorkItem(1, type, COMPLETED);

        assertThatThrownBy(() -> item.advance(MON_WEEK_1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("work item 1 () cannot advance: it is already in its final phase");
    }

    @Test
    void advance_TypeWithNoPhases_ThrowsIllegalStateException() {
        WorkItem item = new WorkItem(1, new WorkItemType());

        assertThatThrownBy(() -> item.advance(MON_WEEK_1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void advance_FromMiddlePhase_KeepsEarlierPhaseDates() {
        WorkItem item = makeItemWithPhaseDates(MON_WEEK_1, MON_WEEK_2);

        item.advance(MON_WEEK_3);

        assertThat(item.getDate(BACKLOG)).isEqualTo(MON_WEEK_1);
        assertThat(item.getDate(IN_PROGRESS)).isEqualTo(MON_WEEK_2);
        assertThat(item.getDate(TESTING)).isEqualTo(MON_WEEK_3);
    }

    @Test
    void setDate_LaterPhaseSetDirectly_JumpsOverIntermediatePhases() {
        WorkItem item = makeItem();

        item.setDate(TESTING, MON_WEEK_1);

        assertThat(item.getCurrentPhase()).isEqualTo(TESTING);
        assertThat(item.hasDate(BACKLOG)).isFalse();
        assertThat(item.hasDate(IN_PROGRESS)).isFalse();
    }

    @Test
    void setDate_LatestPhaseCleared_FallsBackToPreviousPhase() {
        WorkItem item = makeItemWithPhaseDates(MON_WEEK_1, MON_WEEK_2, MON_WEEK_3);

        item.setDate(TESTING, null);

        assertThat(item.getCurrentPhase()).isEqualTo(IN_PROGRESS);
        assertThat(item.hasDate(TESTING)).isFalse();
    }

    @Test
    void setDate_OnlyPhaseCleared_ReturnsToUnphased() {
        WorkItem item = makeItemWithPhaseDates(MON_WEEK_1);

        item.setDate(BACKLOG, null);

        assertThatThrownBy(item::getCurrentPhase).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void setDate_EarlierPhaseSetAfterLaterPhase_CurrentPhaseStaysAtLaterPhase() {
        WorkItem item = makeItem();
        item.setDate(TESTING, MON_WEEK_2);

        item.setDate(BACKLOG, MON_WEEK_1);

        assertThat(item.getCurrentPhase()).isEqualTo(TESTING);
    }

    @Test
    void setDateAsString_IsoDate_StoresParsedDate() {
        WorkItem item = makeItem();

        item.setDateAsString(BACKLOG, "2013-03-04");

        assertThat(item.getDate(BACKLOG)).isEqualTo(MON_WEEK_1);
    }

    @Test
    void getLastPhaseDate_SeveralPhasesDated_ReturnsCurrentPhaseDate() {
        WorkItem item = makeItemWithPhaseDates(MON_WEEK_1, MON_WEEK_2);

        assertThat(item.getLastPhaseDate()).isEqualTo(MON_WEEK_2);
    }

    @Test
    void isCompleted_Unphased_ReturnsFalse() {
        WorkItem item = makeItem();

        assertThat(item.isCompleted()).isFalse();
    }

    @Test
    void isCompleted_MiddlePhase_ReturnsFalse() {
        WorkItem item = new WorkItem(1, type, TESTING);

        assertThat(item.isCompleted()).isFalse();
    }

    @Test
    void getPhaseOnDate_TypeWithNoPhases_ReturnsNull() {
        WorkItem item = new WorkItem(1, new WorkItemType());

        assertThat(item.getPhaseOnDate(MON_WEEK_1)).isNull();
    }

    @Test
    void getPhaseOnDate_Unphased_ReturnsNull() {
        WorkItem item = makeItem();

        assertThat(item.getPhaseOnDate(MON_WEEK_1)).isNull();
    }

    @Test
    void getPhaseOnDate_BeforeFirstPhaseDate_ReturnsNull() {
        WorkItem item = makeItemWithPhaseDates(MON_WEEK_2, MON_WEEK_3);

        assertThat(item.getPhaseOnDate(MON_WEEK_1)).isNull();
    }

    @Test
    void getPhaseOnDate_OnPhaseDate_ReturnsThatPhase() {
        WorkItem item = makeItemWithPhaseDates(MON_WEEK_1, MON_WEEK_2, MON_WEEK_3);

        assertThat(item.getPhaseOnDate(MON_WEEK_2)).isEqualTo(IN_PROGRESS);
    }

    @Test
    void getPhaseOnDate_BetweenPhaseDates_ReturnsEarlierPhase() {
        WorkItem item = makeItemWithPhaseDates(MON_WEEK_1, MON_WEEK_2, MON_WEEK_3);

        assertThat(item.getPhaseOnDate(WED_WEEK_1)).isEqualTo(BACKLOG);
    }

    @Test
    void getPhaseOnDate_AfterLastPhaseDate_ReturnsLastDatedPhase() {
        WorkItem item = makeItemWithPhaseDates(MON_WEEK_1, MON_WEEK_2, MON_WEEK_3);

        assertThat(item.getPhaseOnDate(MON_WEEK_4)).isEqualTo(TESTING);
    }

    @Test
    void getPhaseOnDate_SkippedPhase_ReturnsLatestDatedPhase() {
        WorkItem item = makeItem();
        item.setDate(BACKLOG, MON_WEEK_1);
        item.setDate(TESTING, MON_WEEK_2);

        assertThat(item.getPhaseOnDate(MON_WEEK_3)).isEqualTo(TESTING);
    }

    @Test
    void getPhaseDurations_AllPhasesDated_ReturnsWorkingDaysBetweenConsecutivePhases() {
        WorkItem item = makeItemWithPhaseDates(MON_WEEK_1, MON_WEEK_2, MON_WEEK_4, MON_WEEK_4);

        Map<String, Integer> durations = item.getPhaseDurations();

        assertThat(durations).containsKeys(BACKLOG, IN_PROGRESS, TESTING, COMPLETED);
        assertThat(durations.get(BACKLOG)).isEqualTo(5);
        assertThat(durations.get(IN_PROGRESS)).isEqualTo(10);
        assertThat(durations.get(TESTING)).isZero();
    }

    @Test
    void getPhaseDurations_SkippedPhase_SkippedPhaseFilledWithLaterDateAndHasZeroDuration() {
        WorkItem item = makeItem();
        item.setDate(BACKLOG, MON_WEEK_1);
        item.setDate(TESTING, MON_WEEK_3);

        Map<String, Integer> durations = item.getPhaseDurations();

        assertThat(item.getDate(IN_PROGRESS)).isEqualTo(MON_WEEK_3);
        assertThat(durations.get(BACKLOG)).isEqualTo(10);
        assertThat(durations.get(IN_PROGRESS)).isZero();
    }

    @Test
    void getVariance_WorstAboveAverage_ReturnsSquaredDifference() {
        WorkItem item = makeItem();
        item.setAverageCaseEstimate(3);
        item.setWorstCaseEstimate(7);

        assertThat(item.getVariance()).isEqualTo(16);
    }

    @Test
    void getVariance_WorstBelowAverage_ReturnsPositiveSquaredDifference() {
        WorkItem item = makeItem();
        item.setAverageCaseEstimate(7);
        item.setWorstCaseEstimate(3);

        assertThat(item.getVariance()).isEqualTo(16);
    }

    @Test
    void getVariance_EstimatesEqual_ReturnsZero() {
        WorkItem item = makeItem();
        item.setAverageCaseEstimate(5);
        item.setWorstCaseEstimate(5);

        assertThat(item.getVariance()).isZero();
    }

    @Test
    void setWorkStreamsAsString_Null_SetsEmptyList() {
        WorkItem item = makeItem();

        item.setWorkStreamsAsString(null);

        assertThat(item.getWorkStreams()).isEmpty();
    }

    @Test
    void setWorkStreamsAsString_EntriesWithSpaces_TrimsEachEntry() {
        WorkItem item = makeItem();

        item.setWorkStreamsAsString(" alpha ,beta,  gamma");

        assertThat(item.getWorkStreams()).containsExactly("alpha", "beta", "gamma");
    }

    @Test
    void setWorkStreamsAsString_ConsecutiveCommas_SkipsEmptyEntries() {
        WorkItem item = makeItem();

        item.setWorkStreamsAsString("alpha,,beta");

        assertThat(item.getWorkStreams()).containsExactly("alpha", "beta");
    }

    @Disabled("documents bug WI-03")
    @Test
    void setWorkStreamsAsString_WhitespaceOnlyEntry_SkipsBlankEntry() {
        WorkItem item = makeItem();

        item.setWorkStreamsAsString("alpha, ,beta");

        assertThat(item.getWorkStreams()).containsExactly("alpha", "beta");
    }

    @Test
    void getWorkStreamsAsString_SeveralStreams_ReturnsCommaSeparatedString() {
        WorkItem item = makeItem();
        item.setWorkStreamsAsString("alpha, beta");

        assertThat(item.getWorkStreamsAsString()).isEqualTo("alpha,beta");
    }

    @Test
    void isInWorkStream_ChildItem_ReturnsTrue() {
        WorkItem item = new WorkItem(2, 1, type);
        item.setWorkStreamsAsString("alpha");

        assertThat(item.isInWorkStream("beta")).isTrue();
    }

    @Test
    void isInWorkStream_NullStream_ReturnsTrue() {
        WorkItem item = makeItem();
        item.setWorkStreamsAsString("alpha");

        assertThat(item.isInWorkStream(null)).isTrue();
    }

    @Test
    void isInWorkStream_EmptyStream_ReturnsTrue() {
        WorkItem item = makeItem();
        item.setWorkStreamsAsString("alpha");

        assertThat(item.isInWorkStream("")).isTrue();
    }

    @Test
    void isInWorkStream_StreamAssigned_ReturnsTrue() {
        WorkItem item = makeItem();
        item.setWorkStreamsAsString("alpha, beta");

        assertThat(item.isInWorkStream("beta")).isTrue();
    }

    @Test
    void isInWorkStream_StreamNotAssigned_ReturnsFalse() {
        WorkItem item = makeItem();
        item.setWorkStreamsAsString("alpha");

        assertThat(item.isInWorkStream("beta")).isFalse();
    }

    @Disabled("documents bug WI-02")
    @Test
    void isInWorkStream_WorkStreamsNeverSet_ReturnsFalse() {
        WorkItem item = makeItem();

        assertThat(item.isInWorkStream("alpha")).isFalse();
    }

    @Test
    void getLastComment_NoComments_ReturnsEmptyString() {
        WorkItem item = makeItem();

        assertThat(item.getLastComment()).isEmpty();
    }

    @Test
    void getLastComment_CommentsOutOfOrder_ReturnsMostRecentComment() {
        WorkItem item = makeItem();
        item.addComment(makeComment("ann", "second", "2013-03-02T09:00"));
        item.addComment(makeComment("bob", "third", "2013-03-03T09:00"));
        item.addComment(makeComment("cat", "first", "2013-03-01T09:00"));

        assertThat(item.getLastComment()).isEqualTo("third [bob @ 2013-03-03]");
    }

    @Test
    void getLastBlockedComment_NoBlockedComments_ReturnsEmptyString() {
        WorkItem item = makeItem();
        item.addComment(makeComment("ann", "looks good", "2013-03-01T09:00"));

        assertThat(item.getLastBlockedComment()).isEmpty();
    }

    @Test
    void getLastBlockedComment_MixedComments_ReturnsMostRecentBlockedComment() {
        WorkItem item = makeItem();
        item.addComment(makeComment("ann", "Blocked: waiting on API", "2013-03-02T09:00"));
        item.addComment(makeComment("bob", "unblocked soon", "2013-03-03T09:00"));
        item.addComment(makeComment("cat", "Blocked: no test data", "2013-03-01T09:00"));

        assertThat(item.getLastBlockedComment()).isEqualTo("Blocked: waiting on API [ann]");
    }

    @Test
    void getNotesAndBlock_NotBlocked_ReturnsNotes() {
        WorkItem item = makeItem();
        item.setNotes("some notes");
        item.addComment(makeComment("ann", "Blocked: waiting on API", "2013-03-02T09:00"));

        assertThat(item.getNotesAndBlock()).isEqualTo("some notes");
    }

    @Test
    void getNotesAndBlock_Blocked_AppendsLastBlockedComment() {
        WorkItem item = makeItem();
        item.setNotes("some notes");
        item.setBlocked(true);
        item.addComment(makeComment("ann", "Blocked: waiting on API", "2013-03-02T09:00"));

        assertThat(item.getNotesAndBlock()).isEqualTo("some notes\nBlocked: waiting on API [ann]");
    }

    @Test
    void getNotesAndBlock_BlockedWithNullNotes_ReturnsOnlyBlockedComment() {
        WorkItem item = makeItem();
        item.setNotes(null);
        item.setBlocked(true);
        item.addComment(makeComment("ann", "Blocked: waiting on API", "2013-03-02T09:00"));

        assertThat(item.getNotesAndBlock()).isEqualTo("\nBlocked: waiting on API [ann]");
    }

    @Test
    void stop_CalledTwice_TogglesBlockedBackToFalse() {
        WorkItem item = makeItem();

        item.stop();
        assertThat(item.isBlocked()).isTrue();

        item.stop();
        assertThat(item.isBlocked()).isFalse();
    }

    @Test
    void addComment_Null_ThrowsNullPointerException() {
        WorkItem item = makeItem();

        assertThatThrownBy(() -> item.addComment(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void getComments_CallerModifiesReturnedList_ThrowsUnsupportedOperationException() {
        WorkItem item = makeItem();

        assertThatThrownBy(() -> item.getComments().add(makeComment("ann", "x", "2013-03-01T09:00")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getCommentsInReverseOrder_SeveralComments_ReturnsReversedInsertionOrder() {
        WorkItem item = makeItem();
        WorkItemComment first = makeComment("ann", "first", "2013-03-01T09:00");
        WorkItemComment second = makeComment("bob", "second", "2013-03-02T09:00");
        item.addComment(first);
        item.addComment(second);

        assertThat(item.getCommentsInReverseOrder()).containsExactly(second, first);
    }

    @Test
    void resetCommentsAndReplaceWith_Null_ClearsComments() {
        WorkItem item = makeItem();
        item.addComment(makeComment("ann", "first", "2013-03-01T09:00"));

        item.resetCommentsAndReplaceWith(null);

        assertThat(item.getComments()).isEmpty();
    }

    @Test
    void getQuickOverview_AllFieldsSet_ReturnsFormattedSummary() {
        WorkItem item = makeItem();
        item.setImportance(3);
        item.setWorkStreamsAsString("alpha,beta");
        item.setNotes("some notes");

        assertThat(item.getQuickOverview())
                .isEqualTo("Importance : 3\nStreams :  alpha beta\nNotes : some notes\nLast comment : -\n");
    }

    @Test
    void getQuickOverview_NoStreamsAndNullNotes_ShowsNoItemMarkers() {
        WorkItem item = makeItem();
        item.setWorkStreamsAsString(null);
        item.setNotes(null);

        assertThat(item.getQuickOverview()).isEqualTo("Importance : 0\nStreams : -\nNotes : -\nLast comment : -\n");
    }

    @Disabled("documents bug WI-02")
    @Test
    void getQuickOverview_WorkStreamsNeverSet_ShowsNoItemMarker() {
        WorkItem item = makeItem();

        assertThat(item.getQuickOverview()).contains("Streams : -");
    }

    @Test
    void getTruncatedName_NullName_ReturnsNull() {
        WorkItem item = makeItem();
        item.setName(null);

        assertThat(item.getTruncatedName()).isNull();
    }

    @Test
    void getTruncatedName_ShorterThan40_ReturnsWholeName() {
        WorkItem item = makeItem();
        item.setName("a".repeat(39));

        assertThat(item.getTruncatedName()).hasSize(39);
    }

    @Test
    void getTruncatedName_Exactly40_ReturnsWholeName() {
        WorkItem item = makeItem();
        item.setName("a".repeat(40));

        assertThat(item.getTruncatedName()).isEqualTo("a".repeat(40));
    }

    @Test
    void getTruncatedName_LongerThan40_ReturnsFirst40Characters() {
        WorkItem item = makeItem();
        item.setName("a".repeat(40) + "b");

        assertThat(item.getTruncatedName()).isEqualTo("a".repeat(40));
    }

    @Test
    void setColour_ValidHex_ChangesColour() {
        WorkItem item = makeItem();

        item.setColour("00FF00");

        assertThat(item.getColour().toString()).isEqualTo("#00FF00");
    }

    @Test
    void setColour_Null_KeepsExistingColour() {
        WorkItem item = makeItem();

        item.setColour(null);

        assertThat(item.getColour().toString()).isEqualTo("#FFFFFF");
    }

    @Test
    void setColour_EmptyString_KeepsExistingColour() {
        WorkItem item = makeItem();

        item.setColour("");

        assertThat(item.getColour().toString()).isEqualTo("#FFFFFF");
    }

    @Test
    void setColour_InvalidHex_ThrowsIllegalArgumentException() {
        WorkItem item = makeItem();

        assertThatThrownBy(() -> item.setColour("ZZZ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withNewParent_NewParentId_CopyHasNewParentAndSameIdentity() {
        WorkItem item = makeItem();

        WorkItem copy = item.withNewParent(7);

        assertThat(copy).isNotSameAs(item).isEqualTo(item);
        assertThat(copy.getParentId()).isEqualTo(7);
        assertThat(copy.getType()).isSameAs(type);
    }

    @Test
    void withNewParent_FieldsSet_CopyKeepsCopiedFields() {
        WorkItem item = makeItemWithPhaseDates(MON_WEEK_1, MON_WEEK_2);
        item.setName("story");
        item.setAverageCaseEstimate(3);
        item.setImportance(5);
        item.setNotes("notes");
        item.setExcluded(true);
        item.setBlocked(true);
        item.setColour("00FF00");
        item.addComment(makeComment("ann", "first", "2013-03-01T09:00"));

        WorkItem copy = item.withNewParent(7);

        assertThat(copy.getName()).isEqualTo("story");
        assertThat(copy.getAverageCaseEstimate()).isEqualTo(3);
        assertThat(copy.getImportance()).isEqualTo(5);
        assertThat(copy.getNotes()).isEqualTo("notes");
        assertThat(copy.isExcluded()).isTrue();
        assertThat(copy.isBlocked()).isTrue();
        assertThat(copy.getColour().toString()).isEqualTo("#00FF00");
        assertThat(copy.getComments()).hasSize(1);
        assertThat(copy.getCurrentPhase()).isEqualTo(IN_PROGRESS);
        assertThat(copy.getDatesByPhase()).isEqualTo(item.getDatesByPhase());
    }

    @Test
    void withNewParent_CopyAdvanced_OriginalPhaseDatesUnaffected() {
        WorkItem item = makeItemWithPhaseDates(MON_WEEK_1);

        item.withNewParent(7).advance(MON_WEEK_2);

        assertThat(item.getCurrentPhase()).isEqualTo(BACKLOG);
        assertThat(item.hasDate(IN_PROGRESS)).isFalse();
    }

    @Disabled("documents bug WI-01")
    @Test
    void withNewParent_WorstCaseEstimateSet_CopyKeepsWorstCaseEstimate() {
        WorkItem item = makeItem();
        item.setWorstCaseEstimate(8);

        assertThat(item.withNewParent(7).getWorstCaseEstimate()).isEqualTo(8);
    }

    @Disabled("documents bug WI-01")
    @Test
    void withNewParent_MustHaveSet_CopyKeepsMustHave() {
        WorkItem item = makeItem();
        item.setMustHave(true);

        assertThat(item.withNewParent(7).isMustHave()).isTrue();
    }

    @Disabled("documents bug WI-01")
    @Test
    void withNewParent_WorkStreamsSet_CopyKeepsWorkStreams() {
        WorkItem item = makeItem();
        item.setWorkStreamsAsString("alpha,beta");

        assertThat(item.withNewParent(7).getWorkStreams()).containsExactly("alpha", "beta");
    }

    @Test
    void equals_SameIdDifferentFields_ReturnsTrue() {
        WorkItem first = makeItem();
        WorkItem second = new WorkItem(1, 9, type);
        second.setName("other");

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_DifferentId_ReturnsFalse() {
        assertThat(makeItem()).isNotEqualTo(new WorkItem(2, type));
    }

    @Test
    void equals_Null_ReturnsFalse() {
        assertThat(makeItem().equals(null)).isFalse();
    }

    @Test
    void equals_DifferentClass_ReturnsFalse() {
        assertThat(makeItem().equals("1")).isFalse();
    }

    @Test
    void hashCode_AnyItem_ReturnsId() {
        assertThat(new WorkItem(42, type).hashCode()).isEqualTo(42);
    }

    @Test
    void toString_NameSet_ReturnsIdAndName() {
        WorkItem item = makeItem();
        item.setName("story");

        assertThat(item.toString()).isEqualTo("work item 1 (story)");
    }

    @Test
    void lastPhaseDateComparator_ItemsInAnyOrder_SortsMostRecentFirst() {
        WorkItem oldest = new WorkItem(1, type);
        oldest.setDate(BACKLOG, MON_WEEK_1);
        WorkItem newest = new WorkItem(2, type);
        newest.setDate(BACKLOG, MON_WEEK_3);
        WorkItem middle = new WorkItem(3, type);
        middle.setDate(BACKLOG, MON_WEEK_2);
        List<WorkItem> items = new ArrayList<WorkItem>(Arrays.asList(oldest, newest, middle));

        items.sort(WorkItem.LAST_PHASE_DATE_COMPARATOR);

        assertThat(items).containsExactly(newest, middle, oldest);
    }
}
