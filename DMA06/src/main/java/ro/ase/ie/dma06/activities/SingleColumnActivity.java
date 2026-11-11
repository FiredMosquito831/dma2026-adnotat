/*
 * ======================================================================================
 * FILE: SingleColumnActivity.java
 * ======================================================================================
 * LOCATION: DMA06/src/main/java/ro/ase/ie/dma06/activities/SingleColumnActivity.java
 * PACKAGE:  ro.ase.ie.dma06.activities
 * ROLE:     step 1. One ListView, one ArrayAdapter, one TextView per row.
 *           It writes no line of recycling code: ArrayAdapter does it in its place.
 *
 * FILE STRUCTURE (2 parts):
 *   1. imports     — 6 lines
 *   2. onCreate    — 10 lines, of which 4 build the adapter
 *
 * WHAT YOU NEED TO KNOW BEFOREHAND:
 *   - `android.R.layout.simple_list_item_1` is a layout from the SYSTEM, not from the project.
 *   - `ArrayAdapter` calls `toString()` on each element. For Integer = the number.
 *   - `setAdapter` is all you need: ListView asks for getCount and getView by itself.
 * ======================================================================================
 */

// ======================================================================================
// BLOCK 1 — THE PACKAGE AND THE IMPORTS
// ======================================================================================
// Here the package matches the folder (activities/). The imports come in three groups:
// android.* (framework), androidx.* (bundled library) and ro.ase.* (your code).
// The separation is not required by the compiler, but it makes obvious what comes from where.
// ======================================================================================

package ro.ase.ie.dma06.activities;      // the package of the screens

import android.os.Bundle;                // the parameter of onCreate
import android.widget.ArrayAdapter;      // the ready-made adapter from the SDK
import android.widget.ListView;          // the widget that displays the list

import androidx.appcompat.app.AppCompatActivity;   // the base activity

import ro.ase.ie.data.NumberData;        // the declared package, not the folder
import ro.ase.ie.dma06.R;                // the app's resources

// ======================================================================================
// THEORY BLOCK: LISTVIEW + ARRAYADAPTER
// ======================================================================================
/*
 * 1. WHAT LISTVIEW DOES
 *    -   It is an AdapterView: a widget that does not know what it displays, but knows how to display.
 *    -   It asks an Adapter: "how many elements do you have?" (getCount) and "give me the row for
 *        position N" (getView).
 *    -   It keeps only the visible rows in the view tree. The rest do not exist.
 *    -   It has an internal RecycleBin: the rows that leave the screen are kept for
 *        reuse, by type (getItemViewType).
 *
 * 2. WHAT ARRAYADAPTER DOES
 *    -   It implements getCount, getItem, getItemId and getView.
 *    -   getView: if convertView is null, it inflates `resource`; otherwise it reuses it.
 *    -   Then it takes the element at the position and calls `toString()` on it, which it puts
 *        into a TextView.
 *    -   So: a single text per row. That is all.
 *
 * 3. WHAT `resource` MUST CONTAIN
 *    -   A TextView. Either the root itself (the simple_list_item_1 case), or a child
 *        with the id android.R.id.text1.
 *    -   If it is missing, ArrayAdapter throws a RuntimeException on the first row:
 *        "You must supply a resource ID for a TextView".
 *
 * 4. WHERE IT STOPS
 *    -   At the second TextView. ArrayAdapter has nowhere to put the second value.
 *    -   At per-column formatting. toString() gives a single string; you cannot right-align
 *        the second column with a monospaced font.
 *    -   At models without an overridden toString(): the list shows Movie@a1b2c3.
 *
 * 5. RECYCLING IS HIDDEN, NOT MISSING
 *    -   ArrayAdapter receives `convertView` and reuses it, exactly like a custom
 *        adapter. It is just that you do not see the parameter and cannot attach a ViewHolder.
 *    -   For a single TextView, the missing ViewHolder costs nothing: there is nothing to look up.
 *
 * 6. WHAT CODE RUNS FOR EACH ROW
 *    ListView.obtainView(position)
 *      └─ ArrayAdapter.getView(position, convertView, parent)
 *           ├─ convertView == null ?
 *           │     ├─ yes → inflater.inflate(simple_list_item_1, parent, false)
 *           │     └─ no  → view = convertView
 *           ├─ text = (TextView) view        ← mFieldId == 0, the root is a TextView
 *           └─ text.setText(getItem(position).toString())
 *
 * DIAGRAM:
 *
 *   ListView ──getCount()──▶ ArrayAdapter ──▶ List.size()
 *            ──getView(n)──▶ ArrayAdapter ──▶ convertView? inflate : reuse
 *                                          ──▶ getItem(n).toString()
 *                                          ──▶ textView.setText(...)
 */

/**
 * Step 1: one column.
 * ArrayAdapter is enough because each row shows one text value.
 */
public class SingleColumnActivity extends AppCompatActivity {   // an activity, with no state

    // ==================================================================================
    // METHOD 1 — onCreate
    // ==================================================================================
    // Theory:  the lifecycle; here the whole screen is built.
    // Logic:   1. super  2. setContentView  3. findViewById  4. build the adapter
    //          5. setAdapter
    // Risk:    if you forget setAdapter, the screen stays empty, with no error at all.
    // Ref:     chapter 12 (ListView vs RecyclerView).
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);                       // 1. the parent's lifecycle
        setContentView(R.layout.activity_single_column);          // 2. the layout root IS the ListView

        ListView lvNumbers = findViewById(R.id.lvNumbers);        // 3. the only view in the layout

        // ArrayAdapter calls toString() on each number and shows it in a TextView
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, NumberData.getNumbers());
        //      ↑ Context   ↑ layout from the SYSTEM          ↑ List<Integer> with 100 elements
        lvNumbers.setAdapter(adapter);                            // 4. binds the list to the widget
    }

    // ArrayAdapter<Integer> — the generic must match the type of the list. The list is
    // List<Integer>, so the adapter is ArrayAdapter<Integer>. If you put
    // ArrayAdapter<String>, you would need a List<String> — and you would lose the numeric type.
    //
    // new ArrayAdapter<>(...) with the diamond — the type is inferred from the variable on the left.
    //
    // `this` as the first argument — it is the Context. The adapter needs it to obtain
    // the LayoutInflater with which it inflates the row. In NumberListAdapter you will see exactly
    // the same thing, only explicit: LayoutInflater.from(context).
    //
    // android.R.layout.simple_list_item_1 — the leading "android." is essential.
    // R.layout.simple_list_item_1 does not exist in the project; android.R is the framework's
    // resource package. The file is shipped by the system, not by you.
    //
    // NumberData.getNumbers() as the third argument — it is called once, in
    // onCreate. The result is a list of 100 elements, held by the adapter through
    // a reference. If you called getNumbers() again, you would have a new list, which the
    // adapter does not see.
    //
    // lvNumbers.setAdapter(adapter) — the last line and the most important one. Before it,
    // the ListView is empty and does not know that data exists. On the call, ListView registers a
    // DataSetObserver on the adapter and asks for a layout; only then do getCount() and getView() start.
    //
    // The order between findViewById and setAdapter does not matter — you can build the adapter
    // before finding the list. What matters is that both happen after setContentView,
    // otherwise findViewById returns null.
    //
    // ==================================================================================
    // WHAT ARRAYADAPTER DOES WITH convertView
    // ==================================================================================
    //
    //   ArrayAdapter.getView(position, convertView, parent)
    //     ├─ convertView == null ?
    //     │     ├─ yes → view = inflater.inflate(mResource, parent, false)
    //     │     └─ no  → view = convertView
    //     ├─ mFieldId == 0 ?
    //     │     ├─ yes → text = (TextView) view          ← the root is a TextView
    //     │     └─ no  → text = view.findViewById(mFieldId)
    //     └─ text.setText(getItem(position).toString())
    //
    // Recycling is IDENTICAL to the one in NumberListAdapter. The difference is that you do not see
    // the convertView parameter and cannot hang a ViewHolder off the row. For a single
    // TextView, that costs nothing — there is nothing to look up.
    //
    // ==================================================================================
    // WHAT HAPPENS IF YOU FORGET setAdapter
    // ==================================================================================
    //
    //   ListView without an adapter
    //     ├─ getCount() is never called
    //     ├─ getView() is never called
    //     ├─ the screen: white, with no rows
    //     └─ no exception, no message in Logcat
    //
    // It is the same class of error as a RecyclerView without a LayoutManager: a valid component,
    // in a valid state, that has nothing to display. It is not a bug of the framework; it is an
    // incomplete configuration.
    //
    // ==================================================================================
    // WHY THREE LINES ARE ENOUGH HERE AND ARE NOT ENOUGH AT STEP 2
    // ==================================================================================
    //
    //   Step 1: 1 column  → 1 TextView  → ArrayAdapter knows how
    //   Step 2: 2 columns → 2 TextViews → ArrayAdapter does NOT know how
    //   Step 3: 2 columns → 2 TextViews → RecyclerView.Adapter knows how
    //
    // The rule is not "ArrayAdapter is for simple lists". The rule is: ArrayAdapter knows
    // EXACTLY ONE text per row. As long as your row is one text, it is the best
    // tool. When the row has structure, you move to BaseAdapter.
}
