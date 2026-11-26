package com.metservice.kanban.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for DefaultWorkItemTree.
 *
 * <p>The tree is modelled as a rooted directed graph G = (V, E) where V is the set of work items plus the implicit
 * root (id 0) and E holds one edge parent → child per work item. The fixture is a three-level tree:
 *
 * <pre>
 * root (0)
 * ├── feature 1 [alpha]
 * │   ├── story 3
 * │   │   └── task 6
 * │   └── story 4
 * └── feature 2 [beta]
 *     └── story 5
 * </pre>
 *
 * Tests are selected for node and edge coverage of G (root, inner node, leaf), for the mutations that add, remove
 * and redirect edges ({@code addWorkItem}, {@code delete}, {@code reparent}), and for the sibling orderings produced
 * by {@code move} and {@code reorder} (front, middle, end).
 */
class DefaultWorkItemTreeTest {

    private static final String BACKLOG = "Backlog";
    private static final String IN_PROGRESS = "In Progress";
    private static final String COMPLETED = "Completed";

    private static final String ALPHA = "alpha";
    private static final String BETA = "beta";

    private static final int ROOT_ID = WorkItem.ROOT_WORK_ITEM_ID;
    private static final int UNKNOWN_ID = 99;

    private final WorkItemType featureType = new WorkItemType(BACKLOG, IN_PROGRESS, COMPLETED);
    private final WorkItemType storyType = new WorkItemType(BACKLOG, IN_PROGRESS, COMPLETED);
    private final WorkItemType taskType = new WorkItemType(BACKLOG, IN_PROGRESS, COMPLETED);

    private DefaultWorkItemTree tree;
    private WorkItem feature1;
    private WorkItem feature2;
    private WorkItem story3;
    private WorkItem story4;
    private WorkItem story5;
    private WorkItem task6;

    @BeforeEach
    void buildThreeLevelTree() {
        feature1 = makeItem(1, ROOT_ID, featureType, ALPHA);
        feature2 = makeItem(2, ROOT_ID, featureType, BETA);
        story3 = makeItem(3, 1, storyType, ALPHA);
        story4 = makeItem(4, 1, storyType, ALPHA);
        story5 = makeItem(5, 2, storyType, BETA);
        task6 = makeItem(6, 3, taskType, ALPHA);

        tree = new DefaultWorkItemTree();
        tree.addWorkItems(feature1, feature2, story3, story4, story5, task6);
    }

    private static WorkItem makeItem(int id, int parentId, WorkItemType type, String workStreams) {
        WorkItem item = new WorkItem(id, parentId, type, BACKLOG);
        item.setWorkStreamsAsString(workStreams);
        return item;
    }

    /** A tree whose only items are top-level features with the given ids, added in the given order. */
    private DefaultWorkItemTree makeFlatTree(int... ids) {
        DefaultWorkItemTree flatTree = new DefaultWorkItemTree();
        for (int id : ids) {
            flatTree.addWorkItem(makeItem(id, ROOT_ID, featureType, ALPHA));
        }
        return flatTree;
    }

    private static List<Integer> idsOf(List<WorkItem> items) {
        return items.stream().map(WorkItem::getId).toList();
    }

    @Test
    void getWorkItem_ItemAdded_ReturnsItem() {
        assertThat(tree.getWorkItem(3)).isSameAs(story3);
    }

    @Test
    void getWorkItem_UnknownId_ReturnsNull() {
        assertThat(tree.getWorkItem(UNKNOWN_ID)).isNull();
    }

    @Test
    void getWorkItem_EmptyTree_ReturnsNull() {
        assertThat(new DefaultWorkItemTree().getWorkItem(1)).isNull();
    }

    @Test
    void addWorkItems_SeveralItems_AddsEveryItem() {
        DefaultWorkItemTree newTree = new DefaultWorkItemTree();

        newTree.addWorkItems(feature1, feature2);

        assertThat(newTree.getWorkItemList()).containsExactly(feature1, feature2);
    }

    @Test
    void getWorkItemsById_ItemsAdded_MapsEveryIdToItsItem() {
        assertThat(tree.getWorkItemsById())
                .containsOnlyKeys(1, 2, 3, 4, 5, 6)
                .containsEntry(1, feature1)
                .containsEntry(6, task6);
    }

    @Disabled("documents bug DWIT-06")
    @Test
    void addWorkItem_IdAlreadyInTree_ReplacesExistingItem() {
        WorkItem replacement = makeItem(4, 1, storyType, ALPHA);

        tree.addWorkItem(replacement);

        assertThat(idsOf(tree.getChildren(1))).containsExactly(3, 4);
    }

    @Test
    void getParent_ChildItem_ReturnsParent() {
        assertThat(tree.getParent(story3)).isSameAs(feature1);
    }

    @Test
    void getParent_TopLevelItem_ReturnsNull() {
        assertThat(tree.getParent(feature1)).isNull();
    }

    @Test
    void getChildren_EmptyTree_ReturnsEmptyList() {
        assertThat(new DefaultWorkItemTree().getChildren(ROOT_ID)).isEmpty();
    }

    @Test
    void getChildren_RootId_ReturnsTopLevelItemsInInsertionOrder() {
        assertThat(tree.getChildren(ROOT_ID)).containsExactly(feature1, feature2);
    }

    @Test
    void getChildren_ParentWithChildren_ReturnsOnlyDirectChildrenInInsertionOrder() {
        assertThat(tree.getChildren(1)).containsExactly(story3, story4);
    }

    @Test
    void getChildren_LeafItem_ReturnsEmptyList() {
        assertThat(tree.getChildren(6)).isEmpty();
    }

    @Test
    void getChildren_UnknownParentId_ReturnsEmptyList() {
        assertThat(tree.getChildren(UNKNOWN_ID)).isEmpty();
    }

    @Test
    void getChildren_CallerModifiesReturnedList_TreeIsUnaffected() {
        tree.getChildren(1).clear();

        assertThat(tree.getChildren(1)).containsExactly(story3, story4);
    }

    @Test
    void getWorkItemList_EmptyTree_ReturnsEmptyList() {
        assertThat(new DefaultWorkItemTree().getWorkItemList()).isEmpty();
    }

    @Test
    void getWorkItemList_ThreeLevelTree_ReturnsItemsDepthFirst() {
        assertThat(tree.getWorkItemList()).containsExactly(feature1, story3, task6, story4, feature2, story5);
    }

    @Test
    void getWorkItemList_ChildrenAddedBeforeParents_ReturnsItemsDepthFirst() {
        DefaultWorkItemTree newTree = new DefaultWorkItemTree();

        newTree.addWorkItems(task6, story3, feature1);

        assertThat(newTree.getWorkItemList()).containsExactly(feature1, story3, task6);
    }

    @Test
    void getWorkItemsOfType_TypeWithItems_ReturnsOnlyItemsOfThatTypeDepthFirst() {
        assertThat(tree.getWorkItemsOfType(storyType, null)).containsExactly(story3, story4, story5);
    }

    @Test
    void getWorkItemsOfType_TypeWithNoItems_ReturnsEmptyList() {
        WorkItemType unusedType = new WorkItemType(BACKLOG, COMPLETED);

        assertThat(tree.getWorkItemsOfType(unusedType, null)).isEmpty();
    }

    @ParameterizedTest(name = "work stream \"{0}\"")
    @NullAndEmptySource
    void getWorkItemsOfType_NoWorkStreamGiven_ReturnsItemsInEveryWorkStream(String workStream) {
        assertThat(tree.getWorkItemsOfType(featureType, workStream)).containsExactly(feature1, feature2);
    }

    @Test
    void getWorkItemsOfType_WorkStreamGiven_ReturnsOnlyTopLevelItemsInThatWorkStream() {
        assertThat(tree.getWorkItemsOfType(featureType, BETA)).containsExactly(feature2);
    }

    @Test
    void getWorkItemsOfType_WorkStreamWithNoItems_ReturnsEmptyList() {
        assertThat(tree.getWorkItemsOfType(featureType, "gamma")).isEmpty();
    }

    @Test
    void getWorkItemsOfType_WorkStreamGivenForChildType_ReturnsChildrenRegardlessOfWorkStream() {
        assertThat(tree.getWorkItemsOfType(storyType, BETA)).containsExactly(story3, story4, story5);
    }

    @Test
    void getChildrenWithType_ChildrenOfMixedTypes_ReturnsOnlyChildrenOfRequestedType() {
        WorkItem task7 = makeItem(7, 1, taskType, ALPHA);
        tree.addWorkItem(task7);

        assertThat(tree.getChildrenWithType(1, storyType, null)).containsExactly(story3, story4);
        assertThat(tree.getChildrenWithType(1, taskType, null)).containsExactly(task7);
    }

    @Test
    void getChildrenWithType_ChildTypeHeldOnlyByGrandchildren_ReturnsEmptyList() {
        assertThat(tree.getChildrenWithType(1, taskType, null)).isEmpty();
    }

    @Test
    void getChildrenWithType_WorkStreamGivenForRoot_ReturnsOnlyTopLevelItemsInThatWorkStream() {
        assertThat(tree.getChildrenWithType(ROOT_ID, featureType, ALPHA)).containsExactly(feature1);
    }

    @Test
    void getChildrenWithType_UnknownParentId_ReturnsEmptyList() {
        assertThat(tree.getChildrenWithType(UNKNOWN_ID, storyType, null)).isEmpty();
    }

    @Test
    void getTopLevelAncestor_TopLevelItem_ReturnsItself() {
        assertThat(tree.getTopLevelAncestor(feature1)).isSameAs(feature1);
    }

    @Test
    void getTopLevelAncestor_ChildItem_ReturnsParent() {
        assertThat(tree.getTopLevelAncestor(story5)).isSameAs(feature2);
    }

    @Test
    void getTopLevelAncestor_GrandchildItem_ReturnsTopLevelItem() {
        assertThat(tree.getTopLevelAncestor(task6)).isSameAs(feature1);
    }

    @Test
    void getNewId_EmptyTree_ReturnsOne() {
        assertThat(new DefaultWorkItemTree().getNewId()).isEqualTo(1);
    }

    @Test
    void getNewId_LargestIdIsLeafDeepInTree_ReturnsLargestIdPlusOne() {
        assertThat(tree.getNewId()).isEqualTo(7);
    }

    @Test
    void getNewId_IdsHaveGaps_ReturnsLargestIdPlusOne() {
        DefaultWorkItemTree flatTree = makeFlatTree(2, 9, 4);

        assertThat(flatTree.getNewId()).isEqualTo(10);
    }

    @Test
    void getNewId_SmallerIdDeleted_ReturnsLargestIdPlusOne() {
        tree.delete(4);

        assertThat(tree.getNewId()).isEqualTo(7);
    }

    @Test
    void getNewId_LargestIdDeleted_ReturnsLargestRemainingIdPlusOne() {
        tree.delete(6);

        assertThat(tree.getNewId()).isEqualTo(6);
    }

    @Disabled("documents bug DWIT-07")
    @Test
    void getNewId_LargestIdBelongsToItemWithMissingParent_ReturnsLargestIdPlusOne() {
        tree.addWorkItem(makeItem(8, UNKNOWN_ID, storyType, ALPHA));

        assertThat(tree.getNewId()).isEqualTo(9);
    }

    @Test
    void delete_LeafItem_RemovesItemFromTree() {
        tree.delete(4);

        assertThat(tree.getWorkItem(4)).isNull();
        assertThat(tree.getWorkItemsById()).doesNotContainKey(4);
        assertThat(tree.getWorkItemList()).containsExactly(feature1, story3, task6, feature2, story5);
    }

    @Test
    void delete_FirstOfSeveralSiblings_RemainingSiblingsKeepOrder() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3);

        flatTree.delete(1);

        assertThat(idsOf(flatTree.getChildren(ROOT_ID))).containsExactly(2, 3);
    }

    @Test
    void delete_OnlyItemInTree_TreeIsEmpty() {
        DefaultWorkItemTree flatTree = makeFlatTree(1);

        flatTree.delete(1);

        assertThat(flatTree.getWorkItemList()).isEmpty();
        assertThat(flatTree.getWorkItemsById()).isEmpty();
    }

    @Test
    void delete_ParentAfterItsChildrenAreDeleted_RemovesParent() {
        tree.delete(5);

        tree.delete(2);

        assertThat(tree.getChildren(ROOT_ID)).containsExactly(feature1);
    }

    @Test
    void delete_UnknownId_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> tree.delete(UNKNOWN_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unknown work item id: " + UNKNOWN_ID);
    }

    @Test
    void delete_AlreadyDeletedId_ThrowsIllegalArgumentException() {
        tree.delete(4);

        assertThatThrownBy(() -> tree.delete(4)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_ItemWithChildren_ThrowsIllegalStateException() {
        assertThatThrownBy(() -> tree.delete(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cannot delete work item with children");
    }

    @Test
    void delete_ItemWithChildren_TreeIsUnchanged() {
        assertThatThrownBy(() -> tree.delete(1)).isInstanceOf(IllegalStateException.class);

        assertThat(tree.getWorkItemList()).containsExactly(feature1, story3, task6, story4, feature2, story5);
    }

    @Test
    void reparent_LeafMovedToAnotherParent_ItemBecomesLastChildOfNewParent() {
        tree.reparent(4, 2);

        assertThat(idsOf(tree.getChildren(1))).containsExactly(3);
        assertThat(idsOf(tree.getChildren(2))).containsExactly(5, 4);
    }

    @Test
    void reparent_LeafMovedToAnotherParent_ItemReportsNewParent() {
        tree.reparent(4, 2);

        assertThat(tree.getWorkItem(4).getParentId()).isEqualTo(2);
        assertThat(tree.getParent(tree.getWorkItem(4))).isSameAs(feature2);
    }

    @Test
    void reparent_ItemWithChildren_ChildrenStayAttachedToItem() {
        tree.reparent(3, 2);

        assertThat(tree.getChildren(3)).containsExactly(task6);
        assertThat(idsOf(tree.getWorkItemList())).containsExactly(1, 4, 2, 5, 3, 6);
    }

    @Test
    void reparent_ChildMovedToRoot_ItemBecomesLastTopLevelItem() {
        tree.reparent(5, ROOT_ID);

        assertThat(idsOf(tree.getChildren(ROOT_ID))).containsExactly(1, 2, 5);
        assertThat(tree.getWorkItem(5).isTopLevel()).isTrue();
    }

    @Test
    void reparent_TopLevelItemMovedUnderAnotherItem_ItemIsNoLongerTopLevel() {
        tree.reparent(2, 1);

        assertThat(idsOf(tree.getChildren(ROOT_ID))).containsExactly(1);
        assertThat(idsOf(tree.getChildren(1))).containsExactly(3, 4, 2);
    }

    @Test
    void reparent_SameParent_ItemMovesToEndOfSiblings() {
        tree.reparent(3, 1);

        assertThat(idsOf(tree.getChildren(1))).containsExactly(4, 3);
    }

    @Test
    void reparent_AnyItem_NumberOfItemsIsUnchanged() {
        tree.reparent(4, 2);

        assertThat(tree.getWorkItemList()).hasSize(6);
        assertThat(tree.getWorkItemsById()).hasSize(6);
    }

    @Test
    void reparent_FieldsSet_ItemKeepsNameEstimateImportanceNotesAndPhase() {
        story4.setName("story four");
        story4.setAverageCaseEstimate(5);
        story4.setImportance(3);
        story4.setNotes("some notes");
        story4.setExcluded(true);
        story4.setColour("00FF00");

        tree.reparent(4, 2);

        WorkItem reparented = tree.getWorkItem(4);
        assertThat(reparented.getName()).isEqualTo("story four");
        assertThat(reparented.getAverageCaseEstimate()).isEqualTo(5);
        assertThat(reparented.getImportance()).isEqualTo(3);
        assertThat(reparented.getNotes()).isEqualTo("some notes");
        assertThat(reparented.isExcluded()).isTrue();
        assertThat(reparented.getColour().toString()).isEqualTo("#00FF00");
        assertThat(reparented.getType()).isSameAs(storyType);
        assertThat(reparented.getCurrentPhase()).isEqualTo(BACKLOG);
    }

    @Disabled("documents bug DWIT-01")
    @Test
    void reparent_WorstCaseEstimateSet_ItemKeepsWorstCaseEstimate() {
        story4.setWorstCaseEstimate(8);

        tree.reparent(4, 2);

        assertThat(tree.getWorkItem(4).getWorstCaseEstimate()).isEqualTo(8);
    }

    @Disabled("documents bug DWIT-01")
    @Test
    void reparent_MustHaveSet_ItemKeepsMustHave() {
        story4.setMustHave(true);

        tree.reparent(4, 2);

        assertThat(tree.getWorkItem(4).isMustHave()).isTrue();
    }

    @Disabled("documents bug DWIT-01")
    @Test
    void reparent_WorkStreamsSet_ItemKeepsWorkStreams() {
        tree.reparent(4, 2);

        assertThat(tree.getWorkItem(4).getWorkStreams()).containsExactly(ALPHA);
    }

    @Disabled("documents bug DWIT-02")
    @Test
    void reparent_ChildMovedToRoot_ItemIsFoundByItsWorkStream() {
        tree.reparent(5, ROOT_ID);

        assertThat(idsOf(tree.getWorkItemsOfType(storyType, BETA))).contains(5);
    }

    @Disabled("documents bug DWIT-03")
    @Test
    void reparent_UnknownId_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> tree.reparent(UNKNOWN_ID, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Disabled("documents bug DWIT-04")
    @Test
    void reparent_UnknownNewParentId_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> tree.reparent(4, UNKNOWN_ID)).isInstanceOf(IllegalArgumentException.class);
    }

    @Disabled("documents bug DWIT-04")
    @Test
    void reparent_UnknownNewParentId_ItemStaysInWorkItemList() {
        try {
            tree.reparent(4, UNKNOWN_ID);
        } catch (IllegalArgumentException expected) {
            // rejecting the unknown parent is the desired behaviour
        }

        assertThat(idsOf(tree.getWorkItemList())).contains(4);
    }

    @Disabled("documents bug DWIT-05")
    @Test
    void reparent_NewParentIsItemItself_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> tree.reparent(3, 3)).isInstanceOf(IllegalArgumentException.class);
    }

    @Disabled("documents bug DWIT-05")
    @Test
    void reparent_NewParentIsOwnDescendant_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> tree.reparent(1, 6)).isInstanceOf(IllegalArgumentException.class);
    }

    @Disabled("documents bug DWIT-05")
    @Test
    void reparent_NewParentIsOwnDescendant_SubtreeStaysInWorkItemList() {
        try {
            tree.reparent(1, 6);
        } catch (IllegalArgumentException expected) {
            // rejecting the cycle is the desired behaviour
        }

        assertThat(idsOf(tree.getWorkItemList())).containsExactly(1, 3, 6, 4, 2, 5);
    }

    @Test
    void move_AfterLaterTarget_PlacesItemDirectlyAfterTarget() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3, 4);

        flatTree.move(flatTree.getWorkItem(1), flatTree.getWorkItem(3), true);

        assertThat(idsOf(flatTree.getChildren(ROOT_ID))).containsExactly(2, 3, 1, 4);
    }

    @Test
    void move_BeforeEarlierTarget_PlacesItemDirectlyBeforeTarget() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3, 4);

        flatTree.move(flatTree.getWorkItem(4), flatTree.getWorkItem(2), false);

        assertThat(idsOf(flatTree.getChildren(ROOT_ID))).containsExactly(1, 4, 2, 3);
    }

    @Test
    void move_AfterLastSibling_PlacesItemAtEnd() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3);

        flatTree.move(flatTree.getWorkItem(1), flatTree.getWorkItem(3), true);

        assertThat(idsOf(flatTree.getChildren(ROOT_ID))).containsExactly(2, 3, 1);
    }

    @Test
    void move_BeforeFirstSibling_PlacesItemAtFront() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3);

        flatTree.move(flatTree.getWorkItem(3), flatTree.getWorkItem(1), false);

        assertThat(idsOf(flatTree.getChildren(ROOT_ID))).containsExactly(3, 1, 2);
    }

    @Test
    void move_ItemAlreadyDirectlyAfterTarget_OrderIsUnchanged() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3);

        flatTree.move(flatTree.getWorkItem(2), flatTree.getWorkItem(1), true);

        assertThat(idsOf(flatTree.getChildren(ROOT_ID))).containsExactly(1, 2, 3);
    }

    @Test
    void move_NullTargetAfter_PlacesItemAtFront() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3);

        flatTree.move(flatTree.getWorkItem(3), null, true);

        assertThat(idsOf(flatTree.getChildren(ROOT_ID))).containsExactly(3, 1, 2);
    }

    @Test
    void move_NullTargetBefore_PlacesItemAtEnd() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3);

        flatTree.move(flatTree.getWorkItem(1), null, false);

        assertThat(idsOf(flatTree.getChildren(ROOT_ID))).containsExactly(2, 3, 1);
    }

    @Test
    void move_ChildItems_OtherLevelsOfTreeAreUnaffected() {
        tree.move(story4, story3, false);

        assertThat(tree.getWorkItemList()).containsExactly(feature1, story4, story3, task6, feature2, story5);
    }

    @Disabled("documents bug DWIT-08")
    @ParameterizedTest(name = "after = {0}")
    @ValueSource(booleans = {true, false})
    void move_TargetIsItemItself_OrderIsUnchanged(boolean after) {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3);

        flatTree.move(flatTree.getWorkItem(2), flatTree.getWorkItem(2), after);

        assertThat(idsOf(flatTree.getChildren(ROOT_ID))).containsExactly(1, 2, 3);
    }

    @Disabled("documents bug DWIT-08")
    @Test
    void move_TargetHasDifferentParent_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> tree.move(story4, story5, true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reorder_ItemListedFirst_MovesItemToFront() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3);
        List<WorkItem> newOrder =
                Arrays.asList(flatTree.getWorkItem(3), flatTree.getWorkItem(1), flatTree.getWorkItem(2));

        flatTree.reorder(flatTree.getWorkItem(3), newOrder);

        assertThat(idsOf(flatTree.getChildren(ROOT_ID))).containsExactly(3, 1, 2);
    }

    @Test
    void reorder_ItemListedInMiddle_MovesItemAfterItsNewPredecessor() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3);
        List<WorkItem> newOrder =
                Arrays.asList(flatTree.getWorkItem(2), flatTree.getWorkItem(1), flatTree.getWorkItem(3));

        flatTree.reorder(flatTree.getWorkItem(1), newOrder);

        assertThat(idsOf(flatTree.getChildren(ROOT_ID))).containsExactly(2, 1, 3);
    }

    @Test
    void reorder_ItemListedLast_MovesItemToEnd() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3);
        List<WorkItem> newOrder =
                Arrays.asList(flatTree.getWorkItem(2), flatTree.getWorkItem(3), flatTree.getWorkItem(1));

        flatTree.reorder(flatTree.getWorkItem(1), newOrder);

        assertThat(idsOf(flatTree.getChildren(ROOT_ID))).containsExactly(2, 3, 1);
    }

    @Test
    void reorder_ListMatchesCurrentOrder_OrderIsUnchanged() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3);

        flatTree.reorder(flatTree.getWorkItem(2), flatTree.getChildren(ROOT_ID));

        assertThat(idsOf(flatTree.getChildren(ROOT_ID))).containsExactly(1, 2, 3);
    }

    @Test
    void reorder_SiblingInAnotherPhaseLeftOutOfList_SiblingIsIgnoredByStaleCheck() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3);
        flatTree.getWorkItem(2).setDateAsString(IN_PROGRESS, "2013-03-04");
        List<WorkItem> newOrder = Arrays.asList(flatTree.getWorkItem(3), flatTree.getWorkItem(1));

        flatTree.reorder(flatTree.getWorkItem(3), newOrder);

        assertThat(idsOf(flatTree.getChildren(ROOT_ID))).containsExactly(3, 1, 2);
    }

    @Test
    void reorder_ListMissingSiblingAddedSinceListWasBuilt_ThrowsIllegalStateException() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3);
        List<WorkItem> staleOrder = Arrays.asList(flatTree.getWorkItem(2), flatTree.getWorkItem(1));

        assertThatThrownBy(() -> flatTree.reorder(flatTree.getWorkItem(2), staleOrder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Tree was changed by someone else.");
    }

    @Test
    void reorder_ListContainsSiblingDeletedSinceListWasBuilt_ThrowsIllegalStateException() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3);
        List<WorkItem> staleOrder = flatTree.getChildren(ROOT_ID);
        flatTree.delete(3);

        assertThatThrownBy(() -> flatTree.reorder(flatTree.getWorkItem(1), staleOrder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Tree was changed by someone else.");
    }

    @Test
    void reorder_OtherSiblingsListedInDifferentOrder_ThrowsIllegalStateException() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3);
        List<WorkItem> staleOrder =
                Arrays.asList(flatTree.getWorkItem(1), flatTree.getWorkItem(3), flatTree.getWorkItem(2));

        assertThatThrownBy(() -> flatTree.reorder(flatTree.getWorkItem(1), staleOrder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Tree was changed by someone else.");
    }

    @Test
    void reorder_StaleList_OrderIsUnchanged() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3);
        List<WorkItem> staleOrder = Arrays.asList(flatTree.getWorkItem(2), flatTree.getWorkItem(1));

        assertThatThrownBy(() -> flatTree.reorder(flatTree.getWorkItem(2), staleOrder))
                .isInstanceOf(IllegalStateException.class);

        assertThat(idsOf(flatTree.getChildren(ROOT_ID))).containsExactly(1, 2, 3);
    }

    @Disabled("documents bug DWIT-09")
    @Test
    void reorder_ItemMissingFromList_ThrowsIllegalArgumentException() {
        DefaultWorkItemTree flatTree = makeFlatTree(1, 2, 3);
        List<WorkItem> orderWithoutItem = Arrays.asList(flatTree.getWorkItem(1), flatTree.getWorkItem(2));

        assertThatThrownBy(() -> flatTree.reorder(flatTree.getWorkItem(3), orderWithoutItem))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getParentAlternatives_TopLevelItem_ReturnsOnlyRoot() {
        assertThat(tree.getParentAlternatives(feature1))
                .singleElement()
                .satisfies(root -> assertThat(root.getId()).isEqualTo(ROOT_ID));
    }

    @Test
    void getParentAlternatives_ChildOfTopLevelItem_ReturnsEveryTopLevelItem() {
        assertThat(tree.getParentAlternatives(story3)).containsExactly(feature1, feature2);
    }

    @Test
    void getParentAlternatives_GrandchildItem_ReturnsParentAndItsSiblings() {
        assertThat(tree.getParentAlternatives(task6)).containsExactly(story3, story4);
    }

    @Test
    void getRoot_AnyTree_ReturnsPlaceholderNamedTopLevel() {
        WorkItem root = tree.getRoot();

        assertThat(root.getId()).isEqualTo(ROOT_ID);
        assertThat(root.getParentId()).isEqualTo(-1);
        assertThat(root.getName()).isEqualTo("Top level");
    }
}
