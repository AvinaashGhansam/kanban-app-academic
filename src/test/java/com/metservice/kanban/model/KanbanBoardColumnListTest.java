package com.metservice.kanban.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class KanbanBoardColumnListTest {

    @Test
    void containsPhaseReturnsTrueWhenPhaseExists() {
        KanbanBoardColumn todo = mock(KanbanBoardColumn.class);
        KanbanBoardColumn doing = mock(KanbanBoardColumn.class);

        when(todo.getPhase()).thenReturn("Todo");
        when(doing.getPhase()).thenReturn("Doing");

        KanbanBoardColumnList list = new KanbanBoardColumnList(todo, doing);

        assertTrue(list.containsPhase("Todo"));
        assertTrue(list.containsPhase("Doing"));
    }

    @Test
    void containsPhaseReturnsFalseWhenPhaseDoesNotExist() {
        KanbanBoardColumn todo = mock(KanbanBoardColumn.class);

        when(todo.getPhase()).thenReturn("Todo");

        KanbanBoardColumnList list = new KanbanBoardColumnList(todo);

        assertFalse(list.containsPhase("Done"));
    }

    @Test
    void getIndexOfPhaseReturnsCorrectIndex() {
        KanbanBoardColumn todo = mock(KanbanBoardColumn.class);
        KanbanBoardColumn doing = mock(KanbanBoardColumn.class);
        KanbanBoardColumn done = mock(KanbanBoardColumn.class);

        when(todo.getPhase()).thenReturn("Todo");
        when(doing.getPhase()).thenReturn("Doing");
        when(done.getPhase()).thenReturn("Done");

        KanbanBoardColumnList list = new KanbanBoardColumnList(todo, doing, done);

        assertAll(
                () -> assertEquals(0, list.getIndexOfPhase("Todo")),
                () -> assertEquals(1, list.getIndexOfPhase("Doing")),
                () -> assertEquals(2, list.getIndexOfPhase("Done")));
    }

    @Test
    void getIndexOfPhaseThrowsForUnknownPhase() {
        KanbanBoardColumn todo = mock(KanbanBoardColumn.class);

        when(todo.getPhase()).thenReturn("Todo");

        KanbanBoardColumnList list = new KanbanBoardColumnList(todo);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> list.getIndexOfPhase("Unknown"));

        assertEquals("phase not in list: Unknown", exception.getMessage());
    }

    @Test
    void filterRemovesItemsWhosePhaseIsNotOnBoard() {
        KanbanBoardColumn todo = mock(KanbanBoardColumn.class);
        KanbanBoardColumn doing = mock(KanbanBoardColumn.class);

        when(todo.getPhase()).thenReturn("Todo");
        when(doing.getPhase()).thenReturn("Doing");

        WorkItem todoItem = mock(WorkItem.class);
        WorkItem doingItem = mock(WorkItem.class);
        WorkItem otherItem = mock(WorkItem.class);

        when(todoItem.getCurrentPhase()).thenReturn("Todo");
        when(doingItem.getCurrentPhase()).thenReturn("Doing");
        when(otherItem.getCurrentPhase()).thenReturn("Archived");

        KanbanBoardColumnList board = new KanbanBoardColumnList(todo, doing);

        List<WorkItem> result = board.filter(List.of(todoItem, doingItem, otherItem));

        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertTrue(result.contains(todoItem)),
                () -> assertTrue(result.contains(doingItem)),
                () -> assertFalse(result.contains(otherItem)));
    }

    @Test
    void filterSortsUsingComparatorWhenProvided() {
        KanbanBoardColumn todo = mock(KanbanBoardColumn.class);

        when(todo.getPhase()).thenReturn("Todo");

        WorkItem firstItem = mock(WorkItem.class, "firstItem");
        WorkItem secondItem = mock(WorkItem.class, "secondItem");

        when(firstItem.getCurrentPhase()).thenReturn("Todo");
        when(secondItem.getCurrentPhase()).thenReturn("Todo");

        Comparator<WorkItem> comparator = Comparator.comparingInt(item -> item == firstItem ? 2 : 1);

        KanbanBoardColumnList board = new KanbanBoardColumnList(todo);

        List<WorkItem> result = board.filter(List.of(firstItem, secondItem), comparator);

        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertSame(secondItem, result.get(0)),
                () -> assertSame(firstItem, result.get(1)));
    }
}
