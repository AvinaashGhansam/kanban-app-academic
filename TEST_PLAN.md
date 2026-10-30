# Test Plan

A 7-week plan for three team members to write a focused, high-value test suite for this Kanban app. We are **not**
trying to test everything. We aim at the code where the business logic lives, where bugs are most likely, and where a
test teaches us the most.

Stack: JUnit 6 (Jupiter), Mockito 5, AssertJ 3, Hamcrest 3. See the README for setup.

## 1. What the code base looks like

About 7,600 lines of Java across 76 classes in `com.metservice.kanban`:

| Area | Packages / classes | Lines | Worth testing? |
|---|---|---|---|
| Domain model | `model.WorkItem`, `WorkItemType`, `DefaultWorkItemTree`, `TreeNode`, `TreeIterator`, `WorkItemTypeCollection` | ~1,000 | **Yes.** This is the core logic: phases, dates, parent/child tree. |
| Board building | `model.KanbanBoardBuilder`, `KanbanBoard`, `KanbanBoardRow`, `KanbanCell`, `KanbanBoardColumnList` | ~560 | **Yes.** It's algorithmic and has clear inputs and outputs. |
| Metrics / charts data | `charts.burnup.BurnUpDataModel`, `ProjectedDatasetPopulator`, `EstimatesBurnDownDataModel`, `charts.cumulativeflow.CumulativeFlowChartMatrix`, `charts.cycletime.CycleTimeColumn`, `charts.ChartUtils`, `charts.HueRebalancer` | ~640 | **Yes.** It's pure calculation, and several bugs are hiding here. |
| Estimates | `model.EstimatesProject`, `EstimatesBudgetEntry`, `EstimatesDao` (static helpers) | ~370 | **Yes.** It handles money/budget math and is easy to check by hand. |
| Utilities | `utils.WorkingDayUtils`, `DateUtils`, `Day`, `model.HtmlColour` | ~290 | **Yes.** These are small and pure, which makes them good warm-up targets. Several are marked `//TODO needs unit tests`. |
| Persistence | `csv.*`, `KanbanPropertiesFile`, `KanbanCsvFile`, `KanbanJournalFile`, `KanbanCommentsFile` | ~800 | **Partly.** Test parsing and round trips, using `@TempDir` for files. |
| Project / service | `model.DefaultKanbanProject`, `KanbanService`, `web.KanbanPersistence` | ~800 | **Partly.** These are good places to practise Mockito and integration tests. |
| Web controllers | `web.*Controller` (`KanbanBoardController` alone is 1,255 lines) | ~1,700 | **No.** It's mostly request plumbing on Spring 3.1 and expensive to test. Out of scope. |
| JSP tags, chart image rendering | `jsptags.*`, `*ChartGenerator`, `*ChartBuilder`, `KanbanDrawingSupplier` | ~800 | **No.** These are presentation code with little logic. Out of scope. |

**Target:** the classes marked "Yes" plus the selected "Partly" classes. That's roughly 40% of the code, and it holds
nearly all of the logic.

## 2. Who does what

Each person owns one area for the whole course. That way everyone gets depth in one area plus a fair share of
unit, edge-case, mocking and integration work.

### Member 1: Domain model (work items and the tree)

| Test class to create | Methods to focus on | What to test |
|---|---|---|
| `model/WorkItemTypeTest` | `hasPhaseAfter`, `getPhaseAfter`, `isPhaseBefore`, `isPhaseAfter`, `getBacklogPhase`, `getWallPhases`, `getCompletedPhase`, `containsPhase` | First, middle and last phase. `null` phase. An unknown phase. A type with exactly 2 phases (`getWallPhases` should be empty). |
| `model/WorkItemTest` | constructors, `advance`, `setDate`, `getCurrentPhase`, `getPhaseOnDate`, `isCompleted`, `getPhaseDurations`, `getVariance`, `setWorkStreamsAsString`, `isInWorkStream`, `getLastComment`, `getLastBlockedComment`, `getNotesAndBlock`, `getTruncatedName`, `setColour`, `withNewParent`, `equals`/`hashCode` | Advancing through every phase, then past the last one (`IllegalStateException`). The constructor with an unknown phase. `getCurrentPhase` before any phase. `getPhaseOnDate` before, on and after phase dates. The work-stream string with spaces and empty entries. Names shorter than, exactly at, and longer than 40 characters. **Known bugs 1 & 2.** |
| `model/DefaultWorkItemTreeTest` | `addWorkItem`, `getChildren`, `getWorkItemList` (depth-first order), `getWorkItemsOfType`, `getChildrenWithType`, `getTopLevelAncestor`, `getNewId`, `delete`, `reparent`, `move`, `reorder`, `getParentAlternatives` | An empty tree. `getNewId` on an empty tree and after a delete. Deleting an unknown id or a parent that has children (should throw). `move` before/after/with a `null` target. `reorder` with a stale list ("changed by someone else"). `reparent` keeps all fields (**known bug 1**). |
| `model/TreeNodeTest` and `model/WorkItemTypeCollectionTest` | `TreeNode.create`, `depthFirstIterator`, `getChildren`; `WorkItemTypeCollection.getByName`, `getTreeNode`, `size` | Depth-first order on a 3-level tree. Iterator `remove()` throws. Mixed value classes throw `ClassCastException`. **Known bugs 3 & 5.** |
| `model/DefaultKanbanProjectTest` (week 5, Mockito) | `addWorkItem`, `advance`, `stop`, `move`, `reorder`, `addJournalItem`, `deleteJournalItem`, `getWorkStreams` | Mock `WorkItemTree` and `KanbanPersistence`. Verify `addWorkItem` asks the tree for a new id and advances the item into its first phase. Verify the journal id is max + 1. Verify `getWorkStreams` is case-insensitive and has no duplicates. |

### Member 2: Utilities, persistence, and estimates

| Test class to create | Methods to focus on | What to test |
|---|---|---|
| `utils/WorkingDayUtilsTest` | `isWorkingDay`, `getWorkingDaysBetween`, `nextWorkingDay` | Weekdays, Saturdays and Sundays. A listed public holiday (e.g. `2011-04-25`). Start equal to end (0). Start after end (0). A range that spans a weekend plus a holiday. `nextWorkingDay(null)`. Good candidate for `@ParameterizedTest`. |
| `utils/DateUtilsTest` and `utils/DayTest` | `parseIsoDate`, `formatIsoDate`, `parseConventionalNewZealandDate`, `formatConventionalNewZealandDate`, `parseDate`; `Day.compareTo`, `equals`, `toString` | Round trips. Invalid strings. An empty or `null` string falls back to the default in `parseDate`. Leap day `2012-02-29`. |
| `model/HtmlColourTest` | both constructors, `toString` | With and without `#`. Lower/upper case. Wrong length or non-hex characters (`IllegalArgumentException`). `java.awt.Color` conversion. |
| `csv/CsvRowTest` and `csv/CsvColumnNamesTest` | `getInt`, `getString`, `getBoolean`, `getDate`, `getColumnIndex`, `hasColumn` | A missing column returns the defaults. An empty cell returns `null`. A non-numeric int throws. An unknown column in `getColumnIndex` throws. |
| `csv/DefaultWorkItemParserTest` and `csv/KanbanCsvRoundTripTest` | `parseWorkItem`; `KanbanCsvWriter.write` → `KanbanCsvReader.read` | Old `size` column is used when the average-case estimate is missing (backwards compatibility). An empty colour defaults to white. **Write → read gives back an equal item for every field and phase date.** A bad row's error names the row number. |
| `csv/KanbanCsvFileTest` (week 5, `@TempDir`) | `read`, `write`, `cleanUpTemproaryFiles` | A missing file returns an empty list. A write followed by a read round-trips. At most 25 `.temp` files are kept. |
| `KanbanPropertiesFileTest` | `getWorkItemTypes`, `getPhases`, `getParentWorkItemType`, `isChildWorkItemType`, `getPhaseWIPLimit`, `setColumnWipLimit`, `renameColumn` | Build from a `StringReader` using `src/main/resources/sample-kanban.properties`. A WIP list shorter than the phase list. Renaming a column updates both the phase list and `boards.wall`. Renaming an unknown column throws. |
| `model/EstimatesProjectTest` | `getBudgetEntries`, `getCostSoFar`, `getCostPerPointSoFar`, `setKanbanProject` | Hand-calculate the cumulative average cost and the worst-case cost (`sqrt` of the summed variance × cost per point). The over-budget flags at, just below and just above the budget. No completed points gives 0. `setKanbanProject(null)` clears both lists. |
| `EstimatesDaoTest` | `getCostDailyMap`, `getCostDailyStr` (static) | Round trip. An empty or `null` string. A malformed entry is skipped and doesn't break the others. Output is sorted by date. |

### Member 3: Board building and metrics

| Test class to create | Methods to focus on | What to test |
|---|---|---|
| `model/KanbanBoardColumnListTest` | `containsPhase`, `getIndexOfPhase`, `filter` (with and without a comparator) | An unknown phase throws. `filter` drops items whose phase isn't on the board and sorts when given a comparator. |
| `model/KanbanCellTest` and `model/KanbanBoardRowTest` | `setWorkItem`, `isEmptyCell`, `clone`; `canAdd`, `insert`, `merge`, `hasItemOfType`, `listOfCellsOfType` | Adding to an occupied cell throws. Merging rows from a different column list throws. A clone is independent of the original. |
| `model/KanbanBoardTest` | `insert`, `merge`, `stack`, `getCell`, `getItemsInColumn` | `insert` makes a new row when the column is full. `merge` extends to the longer board. `stack` appends rows. **Known bug 8.** |
| `model/KanbanBoardBuilderTest` | `build`, `buildKanbanBacklog` | Use the `feature` → `story` hierarchy from the sample properties. Stories line up under their feature. Items not on the board are hidden. Filtering by work stream. The above/below/top neighbours of each backlog cell. Compare `board.toString()` against an expected ASCII grid. |
| `charts/ChartUtilsTest` and `charts/HueRebalancerTest` | `getFirstDate`, `getWorkingDays`, `nextWorkingDayAfter`; `balance` | No items gives `null`. Working days across a weekend. Hue at 0, at segment boundaries, at 1.0, and negative (**known bug 7**). |
| `charts/burnup/BurnUpDataModelTest` | `getBacklogSizeOnDate`, `getInProgressSizeOnDate`, `getCompletedSizeOnDate`, `getTotalSizeOnDate`, `getWorkingDays` | Several items in different phases on a fixed date. Excluded items aren't counted. A date before any item exists. **Known bug 6.** |
| `charts/burnup/ProjectedDatasetPopulatorTest` | `getProjectedEndDate` | Steady progress gives the hand-computed end date. No progress gives `null`. **Known bug 4.** |
| `charts/burnup/EstimatesBurnDownDataModelTest` | `getBudgetEntries`, `getRemainingFeaturePointForBudget`, `getProjectedBudgetConsumed` | Cumulative budget entries. Remaining points drop only for items completed on or before the entry date. No budget data or no completed work gives the `budget * 5` fallback. |
| `charts/cumulativeflow/CumulativeFlowChartMatrixTest` and `charts/cycletime/CycleTimeColumnTest` | `registerWorkItem`, `getQuantityOfItemsOnDateAndPhase`, `getOrderedListOfDates`, `getData`; `buildCycleTimeColumnFromWorkItem` | Counts per day and phase. Weekends skipped. Dates limited to start/end. Excluded items ignored. Cycle-time fragments are working days between phases, and skipped phases are handled. |

## 3. Known and suspected bugs

These came out of the code review. Bugs 1–4 and 7 were **confirmed** by a throwaway test run on 2026-10-29. Bugs 5, 6
and 8 come from reading the code and still need a test to confirm. Write a test that shows each bug. Then either fix
the code (and keep the test as a regression test) or mark the test `@Disabled("documents bug #N")`. Agree on one
approach as a team.

| # | Where | Bug | Owner |
|---|---|---|---|
| 1 | `WorkItem.withNewParent` (used by `DefaultWorkItemTree.reparent`) | The copy loses `worstCaseEstimate`, `mustHave` and `workStreams`. Reparenting an item silently resets them. **Confirmed.** | M1 |
| 2 | `WorkItem.getQuickOverview`, `WorkItem.isInWorkStream` | A `WorkItem` built in code without `setWorkStreamsAsString` has `workStreams == null`, so both methods throw `NullPointerException`. It also happens after bug 1. **Confirmed.** | M1 |
| 3 | `TreeNode.getNumberOfNodes` → `WorkItemTypeCollection.size()` | It always returns `0`, so `isEmpty()` is `true` even though iterating yields 2 types. **Confirmed.** | M1 |
| 4 | `ProjectedDatasetPopulator.getProjectedEndDate` | When only one working day has passed, `0 / 0 = NaN`, which gets past the `<= 0` guard. You get a date instead of `null`. **Confirmed.** | M3 |
| 5 | `WorkItemTypeCollection.getTreeNode` | It only searches the root and its direct children. A type 3 levels deep returns `null`, so `DefaultKanbanProject.getChildType` throws `NullPointerException`. *From reading.* | M1 |
| 6 | `BurnUpDataModel` constructors | `startDate` comes from the unfiltered list, so excluded items can move the chart's start date. *From reading.* | M3 |
| 7 | `HueRebalancer.balance` | A negative hue gives a negative result (`-0.25` → `-0.25`) instead of wrapping into `[0, 1)`. **Confirmed.** | M3 |
| 8 | `KanbanBoard.getItemsInColumn` | The result is cached the first time it's called and isn't recalculated after `insert`/`merge`/`stack`. *From reading.* | M3 |

## 4. Seven-week schedule

| Week | Everyone | Member 1 | Member 2 | Member 3 |
|---|---|---|---|---|
| 1 | Agree on conventions (§5). Build a shared `TestData` helper in `src/test/java/com/metservice/kanban/testsupport` with factory methods for `WorkItemType` and `WorkItem`. Everyone writes 1–2 tests to check the setup. | `WorkItemTypeTest` | `HtmlColourTest`, `DayTest` | `KanbanBoardColumnListTest` |
| 2 | Core unit tests: happy paths. | `WorkItemTest` (phases, dates, advance) | `WorkingDayUtilsTest`, `DateUtilsTest` | `KanbanCellTest`, `KanbanBoardRowTest`, `KanbanBoardTest` |
| 3 | Core unit tests continued. Review each other's code via PRs. | `DefaultWorkItemTreeTest` | `CsvRowTest`, `DefaultWorkItemParserTest`, CSV round trip | `ChartUtilsTest`, `BurnUpDataModelTest`, `ProjectedDatasetPopulatorTest` |
| 4 | **Edge cases and black-box design:** boundary values, equivalence classes, exceptions. Write the tests for the bugs in §3. | `TreeNodeTest`, `WorkItemTypeCollectionTest`, bugs 1/2/3/5 | `EstimatesProjectTest`, `EstimatesDaoTest` | `KanbanBoardBuilderTest`, bugs 4/6/7/8 |
| 5 | **Test doubles and integration:** Mockito, `@TempDir`. | `DefaultKanbanProjectTest` (mocks) | `KanbanCsvFileTest`, `KanbanPropertiesFileTest` | `EstimatesBurnDownDataModelTest`, `CumulativeFlowChartMatrixTest`, `CycleTimeColumnTest` |
| 6 | **Measure:** add a coverage report (JaCoCo) and look for untested branches in the classes you own. Optionally run mutation testing (PIT) on 2–3 core classes and strengthen tests that let mutants survive. | Fill gaps | Fill gaps | Fill gaps |
| 7 | Clean up: remove duplicated setup, fix naming, make sure `mvn test` is green and fast. Write the final report: coverage numbers, bugs found, lessons learned. | Report section | Report section | Report section |

Stretch goal (any week, if ahead): an integration test of `KanbanService.createProject` → `getKanbanProject` against a
`@TempDir` home that uses `sample-kanban.properties`.

## 5. Conventions and pitfalls

- **Layout:** mirror the main package, e.g. `src/test/java/com/metservice/kanban/model/WorkItemTest.java`. Surefire
  runs classes named `*Test`.
- **Naming:** `methodName_condition_expectedResult`, or `@DisplayName("...")`. Use one behaviour per test.
- **Structure:** Arrange / Act / Assert. Prefer AssertJ (`assertThat(x).isEqualTo(y)`,
  `assertThatThrownBy(...)`). Use `@ParameterizedTest` for tables of inputs.
- **Fixed dates only:** use `DateUtils.parseIsoDate("2011-04-20")` and never "today". Some methods call `new LocalDate()`
  internally (`WorkItem.getWorkingDaysOnCurrentPhase`, `getPhaseDurations`, and `BurnUpDataModel` when the end date is
  `null`). Only test them in ways that don't depend on the current date, or pass explicit dates where the API allows it.
- **Public holidays:** `WorkingDayUtils` only knows New Zealand holidays from 2010–2012. Pick 2011 dates when you want
  to exercise holidays, and dates in other years when you don't.
- **Reuse `WorkItemType` instances:** `WorkItemType.equals` compares object identity. Two types built with the same
  phases are *not* equal, so share one instance across the items in a test.
- **Files:** never write to the real Kanban home. Use JUnit's `@TempDir`.
- **Out of scope:** web controllers, JSP tags, chart image generation, and the JavaScript/CSS front end.
