/*
 * ======================================================================================
 * FILE: RecyclerViewActivity.java
 * ======================================================================================
 * LOCATION: DMA06/src/main/java/ro/ase/ie/dma06/activities/RecyclerViewActivity.java
 * PACKAGE:  ro.ase.ie.dma06.activities
 * ROLE:     step 3. The same row, the same data, but with a RecyclerView. Here you see
 *           what ListView kept inside: LayoutManager, ItemDecoration, ItemAnimator.
 *
 * FILE STRUCTURE (2 parts):
 *   1. imports     — 10 lines, of which 3 from androidx.recyclerview
 *   2. onCreate    — 12 lines: layout, RecyclerView, LayoutManager, decoration, adapter
 *
 * WHAT YOU NEED TO KNOW BEFOREHAND:
 *   - Without setLayoutManager, RecyclerView displays NOTHING and gives no error.
 *   - DividerItemDecoration takes the orientation as an `int`, not as an object.
 *   - The adapter does not receive a Context: it takes it from `parent` on creation.
 * ======================================================================================
 */

// ======================================================================================
// BLOCK 1 — THE PACKAGE AND THE IMPORTS
// ======================================================================================
// Three new imports compared with step 2, all from androidx.recyclerview:
//   RecyclerView            the widget
//   LinearLayoutManager     the arrangement
//   DividerItemDecoration   the separator
// ======================================================================================

package ro.ase.ie.dma06.activities;      // the package of the screens

import android.os.Bundle;                // the parameter of onCreate

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;   // the line between rows
import androidx.recyclerview.widget.LinearLayoutManager;    // the vertical arrangement
import androidx.recyclerview.widget.RecyclerView;

import ro.ase.ie.adapters.NumberRecyclerAdapter;
import ro.ase.ie.data.NumberData;
import ro.ase.ie.dma06.R;

// ======================================================================================
// THEORY BLOCK: THE FOUR PIECES OF A RECYCLERVIEW
// ======================================================================================
/*
 * 1. ADAPTER — THE CONTENT
 *    What is displayed. It creates holders and fills them. It does not know where they sit.
 *
 * 2. LAYOUTMANAGER — THE ARRANGEMENT
 *    Where they sit. How many rows fit, how tall each one is, how the scroll is computed.
 *    Without it, RecyclerView has nobody to tell it what to draw.
 *
 * 3. ITEMDECORATION — THE DECORATION
 *    What is drawn over or under the rows: separators, margins, shadows.
 *    Unlike ListView, there is no `android:divider` in the XML.
 *
 * 4. ITEMANIMATOR — THE ANIMATION
 *    How the rows move when the data changes. It comes by default (DefaultItemAnimator);
 *    you do not configure it here.
 *
 * THE SEPARATION:
 *
 *   ┌──────────────────────────────────────────────────────────────────────────────┐
 *   │                            RecyclerView                                      │
 *   │                                                                              │
 *   │   ┌──────────────┐   ┌──────────────────┐   ┌──────────────┐   ┌──────────┐  │
 *   │   │   Adapter    │   │  LayoutManager   │   │ItemDecoration│   │ Animator │  │
 *   │   │  (content)   │   │  (arrangement)   │   │(separators)  │   │(movement)│  │
 *   │   └──────┬───────┘   └────────┬─────────┘   └──────┬───────┘   └────┬─────┘  │
 *   │          │                    │                    │                │        │
 *   │   what is seen          where it is seen       what is seen    how it        │
 *   │   in each row           on the screen          between rows    moves         │
 *   └──────────────────────────────────────────────────────────────────────────────┘
 *
 *   ListView had all four crammed into a single class. That is why you could not
 *   change the arrangement without changing the widget.
 *
 * THE PRACTICAL CONSEQUENCE:
 *   -   You swap LinearLayoutManager for GridLayoutManager(2) → a grid, with no
 *       change in the adapter.
 *   -   You add an ItemDecoration → separators, without touching the adapter.
 *   -   You swap the adapter → other content, without touching the arrangement.
 *
 * WHY "FOUR" AND NOT THREE:
 *   ItemAnimator exists and works even if you do not configure it. A RecyclerView without
 *   setItemAnimator has a DefaultItemAnimator, which animates additions, deletions and
 *   moves. If you remove it with setItemAnimator(null), the animations disappear, but the
 *   list works.
 */

/**
 * Step 3: the same two columns in a RecyclerView.
 * RecyclerView needs a LayoutManager, and dividers are added separately.
 */
public class RecyclerViewActivity extends AppCompatActivity {

    // ==================================================================================
    // METHOD 1 — onCreate
    // ==================================================================================
    // Theory:  the lifecycle; here the four pieces are assembled.
    // Logic:   1. super  2. setContentView  3. findViewById
    //          4. LayoutManager + setLayoutManager
    //          5. DividerItemDecoration + addItemDecoration
    //          6. adapter + setAdapter
    // Risk:    without setLayoutManager, the screen stays empty, with no exception.
    // Ref:     chapter 12 and chapter 23 (the ListView → RecyclerView migration).
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);                          // 1. the lifecycle
        setContentView(R.layout.activity_recycler_view);             // 2. header + RecyclerView

        RecyclerView rvNumbers = findViewById(R.id.rvNumbers);       // 3. the only RecyclerView

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);   // 4. vertical arrangement
        rvNumbers.setLayoutManager(layoutManager);                          //    ← MANDATORY
        rvNumbers.addItemDecoration(
                new DividerItemDecoration(this, layoutManager.getOrientation()));  // 5. separator
        //                                ↑ Context   ↑ the orientation, as an int

        NumberRecyclerAdapter adapter = new NumberRecyclerAdapter(NumberData.getNumbers());
        rvNumbers.setAdapter(adapter);                               // 6. the content
    }

    // The order of the three settings: setLayoutManager → addItemDecoration → setAdapter.
    // The order is not fatal between them, but setLayoutManager MUST come before
    // any layout. If you forget it, RecyclerView draws nothing, no matter how many
    // decorations and adapters you add.
    //
    // LinearLayoutManager(this) — the single-argument constructor means vertical.
    // The full variant: LinearLayoutManager(context, orientation, reverseLayout).
    // Here orientation would be RecyclerView.VERTICAL (1), and reverseLayout false.
    //
    // layoutManager.getOrientation() — returns RecyclerView.VERTICAL, that is 1.
    // DividerItemDecoration expects its own constant, DividerItemDecoration.VERTICAL,
    // which is also 1. They are two constants different in meaning, but with the same values
    // (0 = horizontal, 1 = vertical in both classes), so the line works.
    //
    // DANGER! The compiler cannot check it: both are int. If you switched the
    // LayoutManager to horizontal, getOrientation() would return 0, which is also
    // DividerItemDecoration.HORIZONTAL — so the code stays correct. The match is real,
    // not accidental, but it relies on numeric values, not on a common type.
    //
    // addItemDecoration before setAdapter — the order between them does not matter.
    // The decoration is applied at drawing time, not when the data is set.
    //
    // Why rvNumbers is declared RecyclerView and not View: because you need
    // setLayoutManager, addItemDecoration and setAdapter — all methods of the class.
    // findViewById is generic, so it infers the type from the variable.
    //
    // Why notifyDataSetChanged() is not called: the data is fixed, and setAdapter
    // triggers the first layout by itself. notifyDataSetChanged() would be redundant and
    // would cause a pointless refill.

    // ==================================================================================
    // WHAT THE LAYOUTMANAGER DOES ON EVERY FRAME
    // ==================================================================================
    //
    //   RecyclerView.onLayout()
    //     └─ LayoutManager.onLayoutChildren(recycler, state)
    //          ├─ computes how many children fit in the available space
    //          ├─ asks the pool for holders for the visible positions
    //          ├─ calls adapter.onBindViewHolder(holder, position) for each one
    //          ├─ measures and positions each row (layoutDecorated)
    //          └─ throws the holders that left the screen into the pool
    //
    // GridLayoutManager — new GridLayoutManager(this, 2) arranges in two columns.
    // The adapter stays unchanged, item_number.xml stays unchanged.
    //
    // StaggeredGridLayoutManager — columns with different heights, like a Pinterest.
    // Likewise, with no changes in the adapter.
    //
    // setReverseLayout(true) — the list grows from the bottom up, like a chat. Also from the
    // LayoutManager.
    //
    // ==================================================================================
    // WHAT HAPPENS WITHOUT setLayoutManager
    // ==================================================================================
    //
    //   Nothing. And that is exactly the problem.
    //
    //   RecyclerView without a LayoutManager
    //     ├─ setAdapter(adapter)        → success, no exception
    //     ├─ addItemDecoration(...)     → success, no exception
    //     ├─ onMeasure()                → measures 0 × 0
    //     ├─ onLayout()                 → Log.e: "No layout manager attached; skipping layout"
    //     └─ the screen                 → empty
    //
    //   The exact message from Logcat:
    //     E/RecyclerView: No layout manager attached; skipping layout
    //
    //   Why it does not throw an exception: because RecyclerView is designed to work even
    //   without content. An empty RecyclerView is a valid state (for example before the
    //   data is loaded). The framework cannot know whether you forgot to set the manager
    //   or whether you intentionally have no data yet.
    //
    //   Why it is the ugliest error in the chapter: it compiles, it runs, it does not crash, but
    //   the screen is empty. All the other mistakes in the chapter show up immediately; this one
    //   shows up only if you know to look in Logcat.
    //
    //   The diagnostic order for an empty RecyclerView:
    //     1) do you have setLayoutManager?
    //     2) do you have setAdapter?
    //     3) does getItemCount() return more than 0?
    //     4) does the layout root have a non-zero height?
    //   In 90% of cases, the answer is to the first question.

    // ==================================================================================
    // THE SEPARATION DIAGRAM: CONTENT vs ARRANGEMENT
    // ==================================================================================
    //
    // ┌─────────────────────────────────────────────────────────────────────────────────┐
    // │              WHAT IS DISPLAYED      vs.        HOW IT IS ARRANGED              │
    // │                                                                                 │
    // │   ADAPTER (content)                      LAYOUTMANAGER (arrangement)            │
    // │   ├── getItemCount() → 100                ├── how many rows fit                 │
    // │   ├── onCreateViewHolder → the row        ├── where each row sits               │
    // │   ├── onBindViewHolder → the text         ├── how tall each one is              │
    // │   └── getItemViewType → the type          ├── how the scroll is computed         │
    // │                                           └── which holder is reused            │
    // │                                                                                 │
    // │   It does not know:                       It does not know:                     │
    // │   ├── how many rows fit                   ├── what is in a row                  │
    // │   ├── where the row sits                  ├── what data exists                   │
    // │   └── how it scrolls                      └── how many elements there are        │
    // │                                                                                 │
    // │   ─────────────────────────────────────────────────────────────────────────     │
    // │                                                                                 │
    // │   THE SAME DATA + THE SAME ADAPTER:                                             │
    // │                                                                                 │
    // │   LinearLayoutManager      GridLayoutManager(2)        StaggeredGrid(2)          │
    // │   ┌───┐                    ┌───┬───┐                   ┌───┬───┐                 │
    // │   │ 1 │                    │ 1 │ 2 │                   │ 1 │ 2 │                 │
    // │   ├───┤                    ├───┼───┤                   │   ├───┤                 │
    // │   │ 2 │                    │ 3 │ 4 │                   │ 3 │ 4 │                 │
    // │   ├───┤                    ├───┼───┤                   ├───┼───┤                 │
    // │   │ 3 │                    │ 5 │ 6 │                   │ 5 │ 6 │                 │
    // │   └───┘                    └───┴───┘                   └───┴───┘                 │
    // │                                                                                 │
    // │   zero changes in the adapter, zero changes in item_number.xml                  │
    // └─────────────────────────────────────────────────────────────────────────────────┘
    //
    // Why the separation matters at the exam: the question "what must we set on a
    // RecyclerView for it to display something" has the answer "adapter + LayoutManager". If
    // you answer only "adapter", you have lost half the marks — and, more importantly,
    // you have lost exactly the difference from ListView.
    //
    // What "content" and "arrangement" mean in practice: the content is what is seen in
    // each row (the decimal and the binary number). The arrangement is where each
    // row is seen (one under another, in two columns, reversed). They are two independent
    // questions, so two independent objects.

    // ==================================================================================
    // DIVIDERITEMDECORATION — THE SEPARATOR
    // ==================================================================================
    //
    // ListView has android:divider in the XML. RecyclerView has no such thing. The lines
    // between rows are drawn by an ItemDecoration, and DividerItemDecoration is the
    // ready-made decoration from the library.
    //
    // How it works, in three steps:
    //   1. RecyclerView draws the rows (through the LayoutManager)
    //   2. ItemDecoration.onDraw()      → draws UNDER the content (before the children)
    //   3. ItemDecoration.onDrawOver()  → draws OVER the content (after the children)
    //
    // DividerItemDecoration uses onDraw and draws a line at the bottom edge of
    // each row, except the last one.
    //
    // layoutManager.getOrientation() — tells the decoration whether the lines are horizontal
    // or vertical. For a vertical list, the separator is a horizontal line between the
    // rows. For a horizontal list, it would be a vertical line between the columns.
    //
    // What happens if you put the wrong orientation: the separator is drawn on the other
    // axis — that is vertical lines over the rows, or overlapping horizontal lines. It does
    // not crash, it just looks wrong. It is the kind of bug you see at first glance.
    //
    // Why it is not in the XML: because RecyclerView has no separator attributes of its own.
    // Everything related to drawing is done from code, through ItemDecoration. That is the
    // price of the separation: more code, more control.
    //
    // ==================================================================================
    // LISTVIEW vs RECYCLERVIEW — THE DIFFERENCES IN THIS FILE
    // ==================================================================================
    //
    //   ListView (steps 1 and 2)            RecyclerView (step 3)
    //   ├── internal, fixed arrangement     ├── explicit setLayoutManager(...)
    //   ├── android:divider in the theme    ├── addItemDecoration(...) in code
    //   ├── limited animations              ├── default ItemAnimator
    //   ├── adapter with getView            ├── adapter with 3 methods
    //   └── internal recycling              └── RecycledViewPool, also internal
    //
    // Three extra configuration lines, three extra capabilities. Nothing in this file
    // exists in steps 1 and 2 — and exactly that is the lesson of the chapter.
    //
    // ==================================================================================
    // WHAT HAPPENS IF YOU PUT THE DECORATION AFTER setAdapter
    // ==================================================================================
    //
    //   It works. addItemDecoration does not depend on the adapter; the decoration is applied
    //   at the drawing phase, which happens after the layout anyway. The recommended order
    //   (manager → decoration → adapter) is a reading convention, not a requirement.
    //
    // What IS a requirement: setLayoutManager before RecyclerView receives the first
    // layout. If you set it after the view has already been measured, RecyclerView calls
    // requestLayout() — that is why, in practice, you set it in onCreate, before
    // anything else.
    //
    // ==================================================================================
    // WHY `this` AS THE FIRST ARGUMENT TO LinearLayoutManager AND TO DividerItemDecoration
    // ==================================================================================
    //
    // Both need a Context:
    //   - LinearLayoutManager uses it for Resources (dimensions, density);
    //   - DividerItemDecoration uses it to obtain the separator drawable
    //     from the current theme.
    //
    // It is not the same Context as for the adapter: the adapter receives none, because it
    // takes it from `parent` in onCreateViewHolder. Here, in onCreate, you do not yet have a
    // `parent`, so `this` is the only source.
}
