/*
 * ======================================================================================
 * FILE: NumberListAdapter.java
 * ======================================================================================
 * LOCATION: DMA06/src/main/java/ro/ase/ie/dma06/adapters/NumberListAdapter.java
 * PACKAGE:  ro.ase.ie.adapters        ← without `dma06`, like `data`
 * ROLE:     adapts List<Integer> to the two-column row (item_number.xml),
 *           for a ListView. It is the manual equivalent of ArrayAdapter.
 *
 * FILE STRUCTURE (4 parts):
 *   1. ViewHolder            — 4 lines: two references to TextView
 *   2. fields + constructor  — 8 lines: the list and the inflater
 *   3. bookkeeping           — 3 methods: getCount, getItem, getItemId
 *   4. getView               — 22 lines: creation OR reuse + filling
 *
 * WHAT YOU NEED TO KNOW BEFOREHAND:
 *   - BaseAdapter leaves 4 abstract methods; it implements the rest.
 *   - `convertView` is the row recycled by ListView; `null` = there is none free.
 *   - `setTag` / `getTag` glue the ViewHolder to the row. Without them, NPE on the first scroll.
 *   - `findViewById` is called once per row, not on every display.
 * ======================================================================================
 */

// ======================================================================================
// BLOCK 1 — THE PACKAGE AND THE IMPORTS
// ======================================================================================
// The declared package is ro.ase.ie.adapters, although the folder is ro/ase/ie/dma06/adapters/.
// The same situation as with NumberData: it compiles, but the imports must use the
// declared package.
//
// The imports fall into three groups:
//   android.*   — framework: Context, LayoutInflater, View, ViewGroup, BaseAdapter, TextView
//   java.util   — List
//   ro.ase.*    — R (the resources)
// ======================================================================================

package ro.ase.ie.adapters;   // the declared package does not contain `dma06`, although the folder does

import android.content.Context;      // needed only for LayoutInflater
import android.view.LayoutInflater;  // turns XML into View objects
import android.view.View;            // the row (inflated or recycled)
import android.view.ViewGroup;       // the parent of the row: the ListView
import android.widget.BaseAdapter;   // the base class for custom adapters
import android.widget.TextView;      // the two columns

import java.util.List;               // the interface for the list of numbers

import ro.ase.ie.dma06.R;            // the app's resources

// ======================================================================================
// THEORY BLOCK: BASEADAPTER AND THE VIEWHOLDER PATTERN
// ======================================================================================
/*
 * 1. WHAT BASEADAPTER IS
 *    -   An abstract class from android.widget that implements ListAdapter and
 *        SpinnerAdapter.
 *    -   It implements everything related to observers, view types and enabled state:
 *        isEmpty(), areAllItemsEnabled(), isEnabled(int), getItemViewType(int),
 *        getViewTypeCount(), hasStableIds(), registerDataSetObserver().
 *    -   It leaves EXACTLY four methods abstract:
 *          getCount()                                                  how many elements there are
 *          getItem(int position)                                       the object at the position
 *          getItemId(int position)                                     the stable id
 *          getView(int position, View convertView, ViewGroup parent)   the row
 *
 * 2. WHEN EACH METHOD IS CALLED
 *    getCount()   — on every layout of the list, when computing the scroll bar, after
 *                   notifyDataSetChanged. It is the first question ever asked.
 *    getItem()    — It is NOT called by ListView in order to draw. It is called by
 *                   AdapterView.getItemAtPosition(), by performItemClick() and by
 *                   your code (including from getView, as here).
 *    getItemId()  — on click (performItemClick receives the id), on getSelectedItemId().
 *                   With hasStableIds() == true, ListView keeps the selection between
 *                   reloads.
 *    getView()    — for every visible row, on every layout, on every scroll
 *                   step, after every notifyDataSetChanged.
 *
 * 3. THE VIEWHOLDER PATTERN
 *    The problem: findViewById walks the tree. On a row with 2 views it is cheap;
 *    on 10 rows × 2 views × every scroll, it starts to matter.
 *    The solution: look it up once, when the row is created, and glue the result to the row
 *    with setTag(). On reuse, you take it back with getTag().
 *
 *    Without a holder:                     With a holder:
 *    ┌──────────────────────────┐          ┌──────────────────────────┐
 *    │ row (View)               │          │ row (View)               │
 *    │  ├─ tvDecimal            │          │  ├─ tvDecimal            │
 *    │  └─ tvBinary             │          │  ├─ tvBinary             │
 *    │  (looked up on every     │          │  └─ tag → ViewHolder     │
 *    │   display)               │          │      ├─ tvDecimal ───────┘
 *    └──────────────────────────┘          │      └─ tvBinary
 *                                          └──────────────────────────┘
 *
 * 4. WHY `static`
 *    A non-static inner class holds an implicit reference to the outer instance
 *    (`this$0`). The ViewHolder is attached to a View, and the View can live longer
 *    than the adapter (ListView keeps it in the RecycleBin). `static` removes the reference.
 *    On top of that, a non-static holder cannot be built without an adapter — which
 *    complicates any test.
 *
 * 5. THE LIFE OF A ROW
 *    creation:   inflate → findViewById × 2 → new ViewHolder → setTag → fill
 *    reuse:      getTag → fill
 *    exit:       the row enters the RecycleBin, with its tag and all
 *
 * 6. THE OBJECT GRAPH
 *
 *    TwoColumnActivity
 *      └── NumberListAdapter            (a local field; lives as long as the activity)
 *            ├── List<Integer>          (a REFERENCE to the list from NumberData)
 *            │     └── 100 × Integer
 *            ├── LayoutInflater         (a single one, reused)
 *            └── getView() creates:
 *                  ViewHolder           (glued to the row with setTag)
 *                    ├── tvDecimal ──┐
 *                    └── tvBinary  ──┤
 *                                    ▼
 *                  View (the LinearLayout from item_number.xml)
 *                    ├── TextView tvDecimal
 *                    └── TextView tvBinary
 *
 *    The arrows go one way only: the activity holds the adapter, the adapter holds the
 *    list and creates holders, the holder holds the views. No arrow goes back —
 *    that is why the holder can be `static`.
 */

/** Step 2: fills a two-column row for a ListView. */
public class NumberListAdapter extends BaseAdapter {   // extends the skeleton, not ArrayAdapter

    // ==================================================================================
    // BLOCK 2 — THE INNER CLASS ViewHolder
    // ==================================================================================
    // Theory:  the box that holds the references to the row's views, so you stop looking them up.
    // Logic:   two fields, that is all. No methods, no logic, no constructor.
    // Risk:    if it is not `static`, it holds a reference to the adapter (`this$0`) and prolongs
    //          its life as long as the row is in the RecycleBin.
    // Ref:     course 4 (ListView and adapters).
    // ==================================================================================
    private static class ViewHolder {
        TextView tvDecimal;   // the reference to column 1
        TextView tvBinary;    // the reference to column 2
    }

    // Why it is an inner class: the two TextViews must travel
    // together. A holder is a box with two references, hung off the row with setTag.
    // Without it, you would have two separate variables and nowhere to keep them
    // between calls.
    //
    // Why `private`: only this adapter uses it. It appears in no public
    // signature. (In NumberRecyclerAdapter it is `static class` without private, because the
    // type appears in the adapter's public signature.)
    //
    // Why the fields are NOT `final`: they are assigned AFTER `new ViewHolder()`, in
    // getView. They cannot be final because they are not initialized in the constructor. Exactly
    // this is what the RecyclerView version fixes: it moves findViewById into the holder's
    // constructor and can make them final.

    // ==================================================================================
    // BLOCK 3 — THE FIELDS AND THE CONSTRUCTOR
    // ==================================================================================
    // Theory:  two final references: the list and the inflater.
    // Logic:   both are set once, in the constructor.
    // Risk:    if you copied the list (`new ArrayList<>(numbers)`), you would have two lists and
    //          changes from outside would no longer be visible.
    // Ref:     chapter 12.
    // ==================================================================================
    private final List<Integer> numbers;          // the data source, held by reference
    private final LayoutInflater layoutInflater;  // a single inflater, reused

    public NumberListAdapter(Context context, List<Integer> numbers) {
        this.layoutInflater = LayoutInflater.from(context);  // obtained once, not on every row
        this.numbers = numbers;                              // we do NOT copy the list
    }

    // `private final List<Integer> numbers` — the reference never changes.
    // The content of the list could change, but the adapter always points at the same list.
    //
    // `private final LayoutInflater layoutInflater` — LayoutInflater.from(context) does
    // an internal lookup. We do it ONCE, in the constructor, not on every row. On top of that,
    // getView no longer needs a Context.
    //
    // `this.numbers = numbers` — the list is not copied. The adapter receives the reference to
    // the list built by NumberData. If you modified the list from outside, the adapter would
    // see the change — but it would not redraw anything without notifyDataSetChanged().
    //
    // Why the constructor validates nothing: it has nothing to validate. A null `context` would
    // crash anyway at LayoutInflater.from, and a null list would crash on the first getCount().

    // ==================================================================================
    // BLOCK 4 — THE THREE BOOKKEEPING METHODS
    // ==================================================================================
    // Three short methods, each answering a question from ListView. None of them
    // draws anything.
    // ==================================================================================

    // ----------------------------------------------------------------------------------
    // METHOD 1 — getCount
    // ----------------------------------------------------------------------------------
    // Theory:  ListView's first question: how many rows are there?
    // Logic:   the size of the list.
    // Risk:    if you return 0, the list is empty; if you return more than size(),
    //          getView reaches an invalid position → IndexOutOfBoundsException.
    // Ref:     chapter 12.
    // ----------------------------------------------------------------------------------
    @Override
    public int getCount() {
        return numbers.size();      // how many rows: exactly the elements of the list
    }

    // ----------------------------------------------------------------------------------
    // METHOD 2 — getItem
    // ----------------------------------------------------------------------------------
    // Theory:  the object at the given position. It is NOT called by ListView for drawing.
    // Logic:   numbers.get(position), with the bounds check done by ArrayList.
    // Risk:    invalid position → IndexOutOfBoundsException; null element → NPE on
    //          unboxing in getView.
    // Ref:     chapter 12.
    // ----------------------------------------------------------------------------------
    @Override
    public Integer getItem(int position) {
        return numbers.get(position);   // the object at the given position
    }

    // ----------------------------------------------------------------------------------
    // METHOD 3 — getItemId
    // ----------------------------------------------------------------------------------
    // Theory:  the stable identity of the element, used on click and on selection.
    // Logic:   the numbers are unique, so the number itself is a valid id.
    // Risk:    in a list with duplicates, two rows would have the same id and the selection
    //          would jump between them.
    // Ref:     chapter 12.
    // ----------------------------------------------------------------------------------
    @Override
    public long getItemId(int position) {
        // Each number is unique, so the number itself can be the ID
        return numbers.get(position);   // Integer → int → long (widening)
    }

    // getCount() — the first thing ListView asks. It determines how many rows
    // exist and how long the scroll bar is. If you return 0, the list is empty.
    //
    // getItem(position) — the object at the given position. ListView calls it when someone
    // asks for the element at a position (getItemAtPosition), and the adapter calls it
    // itself in getView.
    //
    // getItemId(position) — the identity of the element. numbers.get(position) returns
    // Integer, which is unboxed to int and then widened to long (widening,
    // a safe implicit conversion). Because the numbers 1–100 are unique, the number
    // itself is a valid ID. In a list with duplicates (two movies with the same title)
    // you would need a separate id field.
    //
    // When getItemId is used: ListView calls it when it builds the arguments of a
    // click. The stable identity (hasStableIds() == true) would allow the widget to
    // keep the selection between reloads; here it is false by default, but the method must
    // be implemented anyway — it is abstract in BaseAdapter.
    //
    // @Override on all three — the methods come from Adapter / BaseAdapter. Without the annotation
    // they would still be overrides, but the compiler would not flag a wrong signature.
    //
    // Why getItem returns Integer and not int: the signature in Adapter is
    // `Object getItem(int)`. NumberListAdapter narrows it to Integer — allowed,
    // because Integer is a subtype of Object (return type covariance). The gain:
    // getView no longer needs a cast.
    //
    // THE TABLE OF THE FOUR METHODS:
    //
    //   Method          Who calls it                         When
    //   getCount()      ListView.onMeasure/layoutChildren    on every layout
    //   getItem(int)    AdapterView.getItemAtPosition        on demand / from getView
    //   getItemId(int)  AdapterView.getSelectedItemId        on click / selection
    //   getView(...)    AbsListView.obtainView               for every visible row

    // ==================================================================================
    // BLOCK 5 — getView, THE HEART OF THE ADAPTER
    // ==================================================================================
    // Theory:  the only method that produces a View. It has two branches: creation and reuse.
    // Logic:   1. if convertView is null → inflate + findViewById + setTag
    //          2. otherwise → getTag
    //          3. fill both TextViews
    //          4. return convertView
    // Risk:    without setTag, getTag returns null and the first line that touches the holder
    //          throws NullPointerException. The list works until the first scroll.
    // Ref:     chapter 12 (the section about recycling).
    // ==================================================================================
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;                       // declared before the if, used after it

        if (convertView == null) {
            // New row: inflate it and store its views
            convertView = layoutInflater.inflate(R.layout.item_number, parent, false);
            holder = new ViewHolder();                                      // the empty box
            holder.tvDecimal = convertView.findViewById(R.id.tvDecimal);    // lookup 1
            holder.tvBinary = convertView.findViewById(R.id.tvBinary);      // lookup 2
            convertView.setTag(holder);                                     // glues the box to the row
        } else {
            // Reused row: get its stored views
            holder = (ViewHolder) convertView.getTag();                     // takes the box back
        }

        int number = getItem(position);                                     // Integer → int
        holder.tvDecimal.setText(String.valueOf(number));                   // column 1: "42"
        holder.tvBinary.setText(Integer.toBinaryString(number));            // column 2: "101010"

        return convertView;                                                 // THE SAME row, filled
    }

    // ----------------------------------------------------------------------------------
    // THE SIGNATURE — what each parameter means
    // ----------------------------------------------------------------------------------
    //
    //   position      which row of the list (0…99). It is NOT the position on the screen.
    //                 On the first screen: 0…9. After scrolling: 10, 11, …
    //   convertView   a row RECYCLED by ListView, or null if there is none
    //                 free. It is a View, not a ViewHolder — hence setTag/getTag.
    //   parent        the ListView asking for the row. You do not add it to the tree yourself;
    //                 ListView does it after getView returns. You only need it
    //                 for inflate, so the correct LayoutParams gets computed.
    //
    // ----------------------------------------------------------------------------------
    // THE TWO BRANCHES
    // ----------------------------------------------------------------------------------
    //
    //   convertView == null   →  NEW row:        inflate + findViewById + setTag
    //   convertView != null   →  REUSED row:     getTag, with no lookup at all
    //
    // At startup, ListView asks for the first ~10 rows with convertView == null — it has
    // nothing to recycle. Then, on every scroll, the row that leaves the screen enters the
    // RecycleBin and is handed back on the next call:
    //
    //   getView(0,  null, listView)    → null → inflate + findViewById + setTag
    //   getView(1,  null, listView)    → null → inflate + findViewById + setTag
    //   ...
    //   getView(9,  null, listView)    → null → inflate + findViewById + setTag
    //         ── you scroll one position ──
    //   getView(10, row0, listView)    → != null → getTag + setText
    //
    // Why the first calls have convertView == null: because the RecycleBin is empty.
    // There is no row that has left the screen, because you have not scrolled yet.
    //
    // Why getView(10, row0, ...) and not getView(10, row9, ...): ListView takes
    // the row from the head of the recycling list, not necessarily the last one out. The exact
    // order depends on the RecycleBin implementation; for you, all that matters is that A
    // recycled row arrives, not which one.
    //
    // ----------------------------------------------------------------------------------
    // convertView == null / setTag / getTag, STEP BY STEP
    // ----------------------------------------------------------------------------------
    //
    // STEP 1 — the first call, convertView == null:
    //
    //   ListView has no free row
    //     → getView(0, null, parent)
    //         convertView = layoutInflater.inflate(item_number, parent, false)
    //             ├─ creates the LinearLayout from the XML
    //             ├─ creates the two TextViews
    //             └─ does NOT attach it to parent (third argument = false)
    //         holder = new ViewHolder()                    ← empty object, two null fields
    //         holder.tvDecimal = convertView.findViewById(R.id.tvDecimal)
    //         holder.tvBinary  = convertView.findViewById(R.id.tvBinary)
    //         convertView.setTag(holder)                   ← the row now holds the holder
    //         setText × 2
    //         return convertView
    //
    // STEP 2 — the second call for the same row, after scrolling:
    //
    //   row 0 leaves the screen
    //     → ListView removes it from the tree and puts it in the RecycleBin
    //     → getView(10, row0, parent)
    //         convertView = row0                           ← it is NOT null
    //         holder = (ViewHolder) convertView.getTag()   ← exactly the holder from step 1
    //         setText × 2                                  ← only the text changes
    //         return convertView
    //
    // STEP 3 — what happens to the tag on recycling: nothing. setTag is not reset by
    // ListView, it is not touched by removeView, it is not cleared by the RecycleBin. The row
    // goes into the pool with the holder glued on and comes back with it. That is why it
    // works: the tag belongs to the row, not to the position.
    //
    // What the holder contains between calls: the same two references to the same
    // TextViews. The row is not rebuilt, so the views do not change either.
    // Only the text inside them changes.
    //
    // Why setTag is called only on the null branch: because that is the only place the
    // holder is created. On the else branch, the holder already exists, glued to the row. If you
    // called setTag on the else branch too, you would create a new holder on every display —
    // exactly what you were trying to avoid.
    //
    // ----------------------------------------------------------------------------------
    // DANGER! — the exact NPE when setTag is missing
    // ----------------------------------------------------------------------------------
    //
    // If you remove the line `convertView.setTag(holder);`, the code compiles. The list
    // displays correctly at startup — the first 10 rows go through the null branch, where
    // the holder is built locally. On the first scroll, however:
    //
    //   holder = (ViewHolder) convertView.getTag();   // getTag() → null
    //                                                 // the cast (ViewHolder) null → null
    //   ...
    //   holder.tvDecimal.setText(...);                // ← this is where it crashes
    //
    // Why the cast does not throw: a cast applied to null returns null. (ViewHolder) null
    // is legal. ClassCastException appears only when the object exists and is not of the
    // requested type. That is why the exception does not come from the cast line, but from
    // the first line that TOUCHES the holder.
    //
    // The exact exception, on ART:
    //
    //   java.lang.NullPointerException: Attempt to read from field
    //   'android.widget.TextView ro.ase.ie.adapters.NumberListAdapter$ViewHolder.tvDecimal'
    //   on a null object reference
    //       at ro.ase.ie.adapters.NumberListAdapter.getView(NumberListAdapter.java:47)
    //
    // Why "read from field" and not "invoke virtual method": because the first operation
    // is reading the field holder.tvDecimal, not the setText call. If the holder
    // existed, but tvDecimal were null (for example you forgot a findViewById), then
    // you would get the variant with the method:
    //
    //   java.lang.NullPointerException: Attempt to invoke virtual method
    //   'void android.widget.TextView.setText(java.lang.CharSequence)'
    //   on a null object reference
    //
    // The wrong-tag variant: if you put convertView.setTag("row"), the cast would
    // throw immediately:
    //
    //   java.lang.ClassCastException: java.lang.String cannot be cast to
    //   ro.ase.ie.adapters.NumberListAdapter$ViewHolder
    //       at ro.ase.ie.adapters.NumberListAdapter.getView(NumberListAdapter.java:44)
    //
    // Without setTag: the list works until the first scroll. Then it crashes with
    // NullPointerException. It is the most common mistake with a ListView with a custom
    // adapter — and the only one that gets past the compiler, past the first screen and past
    // the demo done without scrolling.
    //
    // How to debug it in 30 seconds: if the list looks right when opened and
    // crashes on scroll, it is almost certainly a missing setTag or one placed on the wrong branch.
    //
    // ----------------------------------------------------------------------------------
    // inflate — why (layout, parent, false)
    // ----------------------------------------------------------------------------------
    //
    // Why parent: so that the correct LayoutParams is generated for the row. Without it,
    // inflate does not know which parameters to create, and match_parent from the XML cannot be
    // interpreted. LayoutParams is the class that tells the parent how to place the child;
    // LinearLayout has LinearLayout.LayoutParams, ListView has AbsListView.LayoutParams.
    //
    // Why false: it means do NOT attach now. ListView will attach the row when its
    // turn comes. With true, inflate would return the PARENT, not the row — and you would get a
    // ListView instead of a row, with a ClassCastException at findViewById.
    //
    // What happens if you put null instead of parent: inflate has nowhere to take
    // the LayoutParams from, so the row gets default LayoutParams. On a row with
    // match_parent height, the result would be a row of zero or wrong height.
    // You see it immediately: the rows overlap or disappear.
    //
    // findViewById twice — only on the new row. The lookup walks the
    // view tree; on a row with two TextViews it is cheap, but multiplied by 100
    // rows and by every scroll it becomes visible.
    //
    // setTag(holder) — View.setTag(Object) is a free field on any View. It has
    // nothing to do with display; it is just a place where you hang an object. Here we glue
    // the holder to its row.
    //
    // getTag() — it returns Object, so a cast is needed. The cast is NOT checked by
    // the compiler: if you put something else in the tag, ClassCastException at runtime; if
    // you forgot setTag, getTag() returns null and you get a NullPointerException at
    // the first line that touches the holder.
    //
    // ----------------------------------------------------------------------------------
    // THE FILLING PART
    // ----------------------------------------------------------------------------------
    //
    //   int number = getItem(position);                    // Integer → int (unboxing)
    //   holder.tvDecimal.setText(String.valueOf(number));  // "42"
    //   holder.tvBinary.setText(Integer.toBinaryString(number));  // "101010"
    //
    // int number = getItem(position) — getItem returns Integer, the assignment
    // unboxes it. If the list contained null, you would have a NullPointerException here.
    // It does not happen here: NumberData produces 1–100.
    //
    // String.valueOf(number) — equivalent to Integer.toString(number). More explicit
    // than number + "", which creates an extra StringBuilder.
    //
    // Integer.toBinaryString(number) — the binary representation without leading zeros and
    // without a prefix: 1 → "1", 100 → "1100100". That is why the second column is not
    // nicely aligned to the left, and item_number.xml right-aligns it and gives it a
    // monospaced font.
    //
    // return convertView — returns exactly the row received, now filled. You do not build
    // a new View on every call; that is the whole saving.
    //
    // Why not return holder.tvDecimal or something else: ListView needs the
    // whole row, not a child. The row is the LinearLayout with two TextViews.
    //
    // ----------------------------------------------------------------------------------
    // THE RECYCLING CYCLE, WITH WHAT convertView HOLDS AT EACH MOMENT
    // ----------------------------------------------------------------------------------
    //
    //   SCREEN (10 visible rows)                   RecycleBin (the ListView's pool)
    //   ┌───────────────────────────────┐
    //   │ row 0    position 0   tag=H0  │
    //   │ row 1    position 1   tag=H1  │
    //   │ row 2    position 2   tag=H2  │
    //   │ ...                           │
    //   │ row 9    position 9   tag=H9  │
    //   └───────────────────────────────┘
    //
    //   ① YOU SCROLL UP by one row
    //      row 0 leaves at the top
    //        → ListView.removeView(row0)
    //        → RecycleBin.add(row0)           ← row0 keeps tag=H0
    //      ListView asks for the row for position 10:
    //        getView(10, row0, parent)
    //          convertView = row0              ← a View object, NOT null
    //          getTag()    = H0                ← the holder created at position 0
    //          H0.tvDecimal.setText("11")
    //          H0.tvBinary.setText("1011")
    //          return row0
    //
    //   ② THE SCREEN NOW LOOKS LIKE
    //   ┌───────────────────────────────┐
    //   │ row 1    position 1   tag=H1  │
    //   │ row 2    position 2   tag=H2  │
    //   │ ...                           │
    //   │ row 9    position 9   tag=H9  │
    //   │ row 0    position 10  tag=H0  │   ← the same View, different text
    //   └───────────────────────────────┘
    //
    //   ③ YOU SCROLL BACK DOWN
    //      row 0 leaves at the bottom → RecycleBin
    //      getView(0, row0, parent)
    //          getTag() = H0
    //          setText("1"), setText("1")   ← what was there at the start is rewritten
    //
    // What to remember from the diagram: convertView is not "some row", it is a specific
    // View object, with its tag. The position does not travel with the row; the text does.
    // That is why setText is ALWAYS done, on both branches — if you skipped
    // the filling on the else branch, the row would show the old text.
    //
    // How many rows exist in total: roughly as many as fit on the screen, plus
    // one or two in the buffer. Not 100. That is the whole win: 10 View objects instead of 100,
    // no matter how far you scroll.
    //
    // When the pool empties: on setAdapter with a new adapter, on notifyDataSetChanged
    // (partially), on the destruction of the ListView. It does not empty on scroll — that is why
    // it works.
    //
    // ----------------------------------------------------------------------------------
    // WHAT WOULD HAPPEN IF YOU IGNORED RECYCLING
    // ----------------------------------------------------------------------------------
    //
    // The wrong version, for comparison — it is NOT in the repo:
    //
    //   View row = layoutInflater.inflate(R.layout.item_number, parent, false);
    //   // ... findViewById, setText ...
    //   return row;
    //
    // -   100 rows inflated for 100 elements, although ~10 fit on the screen;
    // -   memory grows with every scroll, because every row left in the tree
    //     holds a View and two TextViews;
    // -   ListView has nothing left to recycle, so convertView no longer arrives either;
    // -   the scroll becomes jerky, because every frame inflates XML.
    //
    // The code would be CORRECT — it would display the same numbers — but it would be slow.
    // Recycling is optimization, not correctness. At 100 rows the difference is small; at
    // 10,000 rows it is the difference between an app that works and one that does not.
    //
    // What inflate costs: parsing the XML is cached by Resources, but building the
    // View objects is not. For a row with two TextViews, we are talking microseconds.
    // Multiplied by 100 rows and by every scroll frame, it reaches milliseconds —
    // exactly the budget of a 60fps frame (16 ms).
    //
    // ==================================================================================
    // WHAT HAPPENS IF YOU FORGET A findViewById
    // ==================================================================================
    //
    //   holder.tvDecimal = convertView.findViewById(R.id.tvDecimal);
    //   // holder.tvBinary is missing
    //
    // It compiles. The list displays. The second column is empty on all rows. On
    // filling:
    //
    //   holder.tvBinary.setText(...)   →  NullPointerException:
    //     Attempt to invoke virtual method 'void android.widget.TextView.setText(...)'
    //     on a null object reference
    //
    // The difference from the missing setTag: there the holder is null, here the holder exists,
    // but one of its fields is null. The message says so: "invoke virtual method ... on a null
    // object reference" means the receiver of the call is null, that is tvBinary.
    //
    // ==================================================================================
    // WHY YOU CANNOT PUT DATA IN THE HOLDER
    // ==================================================================================
    //
    // You might be tempted to write:
    //
    //   holder.number = getItem(position);   // ← wrong
    //
    // and read the value on click. It does not work, because the holder is REUSED:
    // between bind and click, the row can be recycled for another position, and the value
    // in the holder would belong to another element. The holder holds VIEWS, not data. Data
    // goes into the views (setText), not into the holder.
    //
    // ==================================================================================
    // HOW IT COMPARES WITH ARRAYADAPTER
    // ==================================================================================
    //
    //   ArrayAdapter                      NumberListAdapter
    //   ├── getCount() → size()           ├── getCount() → size()
    //   ├── getItem() → list.get()        ├── getItem() → list.get()
    //   ├── getItemId() → position        ├── getItemId() → the number
    //   └── getView()                     └── getView()
    //         ├── inflate / reuse               ├── if (convertView == null)
    //         ├── findViewById(text1)           │     ├── inflate
    //         └── setText(toString())           │     ├── new ViewHolder
    //                                           │     ├── findViewById × 2
    //                                           │     └── setTag
    //                                           ├── else getTag
    //                                           └── setText × 2
    //
    // The same four methods, the same structure. The difference is that the second variant gives
    // you access to convertView and to the row's layout. All you gain is control; all
    // you pay is code.
    //
    // ==================================================================================
    // COMMON MISTAKES, IN ORDER OF FREQUENCY
    // ==================================================================================
    //
    //   1. setTag is missing → NPE on the first scroll.
    //   2. setTag placed on the else branch → a new holder is created on every display;
    //      it does not crash, but you lose the whole saving.
    //   3. findViewById called on every call, outside the if → it works, but
    //      the lookup happens 100 times more often than it should.
    //   4. inflate with `true` instead of `false` → ClassCastException or stuck-together rows.
    //   5. getCount() returns numbers.size() + 1 → IndexOutOfBoundsException on the last one.
    //   6. return holder.tvDecimal instead of convertView → ListView receives a TextView
    //      instead of the whole row; the second column disappears.
}
