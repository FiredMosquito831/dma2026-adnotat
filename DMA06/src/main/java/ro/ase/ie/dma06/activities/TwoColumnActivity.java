/*
 * ======================================================================================
 * FILE: TwoColumnActivity.java
 * ======================================================================================
 * LOCATION: DMA06/src/main/java/ro/ase/ie/dma06/activities/TwoColumnActivity.java
 * PACKAGE:  ro.ase.ie.dma06.activities
 * ROLE:     step 2. The same ListView as step 1, but with a custom adapter that
 *           fills two columns.
 *
 * FILE STRUCTURE (2 parts):
 *   1. imports     — 7 lines
 *   2. onCreate    — 8 lines: layout, findViewById, adapter, setAdapter
 *
 * WHAT YOU NEED TO KNOW BEFOREHAND:
 *   - The id `lvNumbers` is THE SAME as in step 1, but in another layout. No conflict.
 *   - The activity does not know what is in a row: it does not touch tvDecimal / tvBinary.
 *   - The difference from step 1 is a single class: NumberListAdapter.
 * ======================================================================================
 */

// ======================================================================================
// BLOCK 1 — THE PACKAGE AND THE IMPORTS
// ======================================================================================
// Compared with SingleColumnActivity, the `android.widget.ArrayAdapter` import is gone and
// `ro.ase.ie.adapters.NumberListAdapter` has appeared. The rest is identical.
// ======================================================================================

package ro.ase.ie.dma06.activities;      // the package of the screens

import android.os.Bundle;                // the parameter of onCreate
import android.widget.ListView;          // THE SAME widget as step 1

import androidx.appcompat.app.AppCompatActivity;

import ro.ase.ie.adapters.NumberListAdapter;   // the custom adapter, from the `adapters` package
import ro.ase.ie.data.NumberData;             // the same data as in step 1
import ro.ase.ie.dma06.R;

// ======================================================================================
// THEORY BLOCK: FROM ARRAYADAPTER TO BASEADAPTER
// ======================================================================================
/*
 * 1. WHAT STAYS IDENTICAL
 *    -   The widget: ListView.
 *    -   The data: NumberData.getNumbers().
 *    -   The cycle: onCreate → setContentView → findViewById → new Adapter → setAdapter.
 *    -   Recycling: ListView still does it; it is just that now you write it.
 *
 * 2. WHAT CHANGES
 *    -   The adapter: ArrayAdapter (SDK) → NumberListAdapter (yours).
 *    -   The row layout: android.R.layout.simple_list_item_1 → item_number.xml.
 *    -   The number of columns: 1 → 2.
 *    -   Who calls findViewById: the SDK → your code, once per row.
 *
 * 3. WHAT THE ACTIVITY MUST DO EXTRA
 *    -   Nothing, apart from passing `this` (Context) to the custom adapter.
 *    -   ArrayAdapter took the Context through the constructor; NumberListAdapter likewise.
 *    -   So the difference shows only in the type of the variable.
 *
 * 4. WHAT THE ACTIVITY MUST NOT DO
 *    -   Know the ids inside the row (tvDecimal, tvBinary).
 *    -   Know how many columns the row has.
 *    -   Touch convertView, ViewHolder or recycling.
 *    Any such line would be a leak of responsibility: the activity would have to be
 *    changed when the row changes.
 *
 * DIAGRAM OF RESPONSIBILITIES:
 *
 *   TwoColumnActivity                 NumberListAdapter
 *   ├── setContentView(layout)        ├── getCount()   → 100
 *   ├── findViewById(lvNumbers)       ├── getItem(n)
 *   ├── new NumberListAdapter(        ├── getItemId(n)
 *   │       this,                     └── getView(n, convertView, parent)
 *   │       NumberData.getNumbers())        ├── inflate / getTag
 *   └── setAdapter(adapter)                 ├── findViewById × 2
 *                                           └── setText × 2
 *
 * 5. WHY THE IDS DO NOT CLASH
 *    R.id.lvNumbers is a single int constant, generated once for all layouts.
 *    The value is the same, and findViewById looks for it in the tree attached to the
 *    current activity. If you had two ListViews with the same id IN THE SAME layout, the
 *    first one found would be returned — and the second would be inaccessible.
 */

/**
 * Step 2: two columns in a ListView.
 * A custom adapter fills each column of the row layout.
 */
public class TwoColumnActivity extends AppCompatActivity {

    // ==================================================================================
    // METHOD 1 — onCreate
    // ==================================================================================
    // Theory:  the same lifecycle; only the adapter class changes.
    // Logic:   1. super  2. setContentView  3. findViewById  4. new NumberListAdapter
    //          5. setAdapter
    // Risk:    `this` must be a Context; if you passed something else, LayoutInflater.from
    //          would throw.
    // Ref:     chapter 12.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);                    // 1. the lifecycle
        setContentView(R.layout.activity_two_column);          // 2. includes the header + ListView

        ListView lvNumbers = findViewById(R.id.lvNumbers);     // 3. THE SAME id as in step 1

        NumberListAdapter adapter = new NumberListAdapter(this, NumberData.getNumbers());
        //                                ↑ Context                  ↑ List<Integer> with 100
        lvNumbers.setAdapter(adapter);                         // 4. binds the list to the widget
    }

    // The same id, another layout — lvNumbers appears both in activity_single_column.xml and in
    // activity_two_column.xml. It is not a problem: ids are unique PER LAYOUT, not globally.
    // findViewById looks only in the current layout's tree, so it finds the ListView
    // of the current screen.
    //
    // `this` as the Context — the adapter needs it for the LayoutInflater. Here it is
    // the only place where the activity touches the inflate detail.
    //
    // The activity does not know what is in a row — there is no findViewById(R.id.tvDecimal)
    // here. The two columns are the adapter's business. If you rename tvDecimal in the
    // XML, only NumberListAdapter changes.
    //
    // Comparison with step 1: the same structure, four lines, only the adapter class
    // different. The lesson: the activity hardly changes; the adapter is where everything
    // happens.
    //
    // What would happen if you passed `null` as the Context: LayoutInflater.from(null) throws
    // NullPointerException in the adapter's constructor, so at
    // new NumberListAdapter(null, ...). The error appears early, in onCreate, not on the first
    // row — good for debugging.
    //
    // ==================================================================================
    // WHY THE ACTIVITY DOES NOT NEED `implements`
    // ==================================================================================
    //
    // No interface is needed because the activity does not receive events from the
    // adapter. There is no "onItemBound" or "onRowCreated". The adapter is a passive object:
    // ListView asks, it answers. The activity only builds it and hands it over.
    //
    // If you wanted to react to a click on a row, you would have two options:
    //   1. lvNumbers.setOnItemClickListener(...)  → in the activity, with the position
    //   2. a listener put on the row, in getView  → in the adapter
    // Neither is needed here: the rows are not clickable.
    //
    // ==================================================================================
    // WHAT WOULD HAPPEN IF YOU PUT THE ADAPTER IN A STATIC FIELD
    // ==================================================================================
    //
    //   private static NumberListAdapter adapter;   // ← wrong
    //
    // The adapter holds the list and, through the holders, the views. A static field would
    // keep the adapter alive after the activity is destroyed, and the views in the holders
    // would hold the old tree. The result: a memory leak that grows with every
    // rotation. The adapter is always an instance field (or a local variable, as
    // here) — never static.
}
