/*
 * ======================================================================================
 * FILE: NumberRecyclerAdapter.java
 * ======================================================================================
 * LOCATION: DMA06/src/main/java/ro/ase/ie/dma06/adapters/NumberRecyclerAdapter.java
 * PACKAGE:  ro.ase.ie.adapters
 * ROLE:     adapts List<Integer> to THE SAME two-column row (item_number.xml),
 *           but for a RecyclerView. The ViewHolder is mandatory, and getView is
 *           split into two methods.
 *
 * FILE STRUCTURE (5 parts):
 *   1. ViewHolder            — 10 lines: final fields + constructor with findViewById
 *   2. field + constructor   — 5 lines: only the list, no Context
 *   3. onCreateViewHolder    — 6 lines: creates the row and the holder
 *   4. onBindViewHolder      — 6 lines: fills the holder
 *   5. getItemCount          — 3 lines: how many elements there are
 *
 * WHAT YOU NEED TO KNOW BEFOREHAND:
 *   - Three mandatory abstract methods: onCreateViewHolder, onBindViewHolder,
 *     getItemCount. There is no getView.
 *   - The generic parameter <NumberViewHolder> removes all the casts.
 *   - `super(itemView)` is mandatory in the holder's constructor.
 *   - There is no `convertView`: RecyclerView decides by itself which holder to reuse.
 * ======================================================================================
 */

// ======================================================================================
// BLOCK 1 — THE PACKAGE AND THE IMPORTS
// ======================================================================================
// Compared with NumberListAdapter, Context, BaseAdapter and the inflater as a field have
// disappeared; NonNull and RecyclerView have appeared. The `android.view.View` import
// remains: it is the type of the parameter in the holder's constructor.
// ======================================================================================

package ro.ase.ie.adapters;   // the same package as NumberListAdapter

import android.view.LayoutInflater;   // for inflate, in onCreateViewHolder
import android.view.View;             // the holder's itemView
import android.view.ViewGroup;        // the parent in onCreateViewHolder
import android.widget.TextView;       // the two columns

import androidx.annotation.NonNull;                 // contract: a parameter / result that is not null
import androidx.recyclerview.widget.RecyclerView;  // the base class and its ViewHolder

import java.util.List;

import ro.ase.ie.dma06.R;

// ======================================================================================
// THEORY BLOCK: RECYCLERVIEW.ADAPTER
// ======================================================================================
/*
 * 1. WHAT IT IS
 *    -   The abstract class that binds the data to RecyclerView.
 *    -   It has THREE abstract methods:
 *          onCreateViewHolder(ViewGroup parent, int viewType) → ViewHolder
 *          onBindViewHolder(ViewHolder holder, int position) → void
 *          getItemCount()                                    → int
 *    -   All the others have default implementations.
 *
 * 2. WHY IT HAS NO getView
 *    -   Because RecyclerView never asks for a View. It asks for a ViewHolder.
 *    -   A ViewHolder is a box with a View inside (itemView) plus references to
 *        its children. RecyclerView holds holders, not views.
 *    -   Why: so it can reuse a holder WITHOUT knowing what is in it. For that it
 *        needs a stable type (the holder), not some View.
 *
 * 3. WHY THREE METHODS AND NOT ONE
 *    -   Creation (inflate + findViewById) is expensive and rare.
 *    -   Filling (setText) is cheap and frequent.
 *    -   By separating them, RecyclerView can call only the second, thousands of times,
 *        without paying for the first.
 *    -   getItemCount is the third because RecyclerView must know how many positions
 *        exist BEFORE asking for any holder.
 *
 * 4. WHAT HAPPENS WITH THE OTHER METHODS
 *    getItemId(int)             → NO_ID (-1) by default; enabled with setHasStableIds(true)
 *    getItemViewType(int)       → 0 by default; for rows of different types
 *    onViewRecycled(holder)     → empty; good for cancelling an image download
 *    onViewAttachedToWindow     → empty; the holder enters the screen
 *    onViewDetachedFromWindow   → empty; the holder leaves the screen
 *    onAttachedToRecyclerView   → empty; the adapter is bound to a RecyclerView
 *    onDetachedFromRecyclerView → empty; the adapter is unbound
 *
 * 5. WHO CALLS WHAT
 *    RecyclerView + LayoutManager decide:
 *      - how many holders fit on the screen
 *      - which holder is reused for which position
 *      - when a new holder is needed
 *    The adapter only answers: "here is a new holder" / "I have filled the holder".
 *
 * 6. WHY IT IS AN ABSTRACT CLASS AND NOT AN INTERFACE
 *    Because it has state: the observer list (AdapterDataObservable), the reference to
 *    RecyclerView and the default implementations of the optional methods. An interface
 *    cannot hold state.
 *
 * DIAGRAM OF THE THREE METHODS:
 *
 *   RecyclerView                         Adapter
 *   ── getItemCount() ──────────────────▶ 100
 *   ── "I need a holder" ───────────────▶ onCreateViewHolder(parent, 0)
 *                                            └─ inflate + new NumberViewHolder
 *   ── "fill it for position 7" ────────▶ onBindViewHolder(holder, 7)
 *                                            └─ setText × 2
 *   ── "the holder left the screen" ────▶ (it goes into the pool; the adapter is not asked)
 */

/**
 * Step 3: fills the same row for a RecyclerView.
 * The ViewHolder is required here, and getView is split into two methods.
 */
public class NumberRecyclerAdapter
        extends RecyclerView.Adapter<NumberRecyclerAdapter.NumberViewHolder> {
        //                       ↑ the holder type, as a generic parameter

    // ==================================================================================
    // BLOCK 2 — THE INNER CLASS NumberViewHolder
    // ==================================================================================
    // Theory:  mandatory; RecyclerView.Adapter is parameterized with it.
    // Logic:   the constructor receives itemView, calls super(itemView) and looks up the children.
    // Risk:    without `super(itemView)`, RecyclerView has nothing to recycle — the holder has
    //          no associated View.
    // Ref:     chapter 12.
    // ==================================================================================
    static class NumberViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDecimal;   // final: assigned in the constructor, never changes again
        final TextView tvBinary;    // final: assigned in the constructor, never changes again

        NumberViewHolder(@NonNull View itemView) {
            super(itemView);                                        // mandatory: saves the row
            tvDecimal = itemView.findViewById(R.id.tvDecimal);      // lookup 1, once per holder
            tvBinary = itemView.findViewById(R.id.tvBinary);        // lookup 2, once per holder
        }
    }

    // The generic parameter — RecyclerView.Adapter<NumberRecyclerAdapter.NumberViewHolder>
    // declares WHAT TYPE OF HOLDER the adapter produces. From here on, all the methods
    // work with NumberViewHolder, not with RecyclerView.ViewHolder: onCreateViewHolder
    // returns it, onBindViewHolder receives it. NO cast anywhere — that is the
    // gain over getTag().
    //
    // Why NumberRecyclerAdapter.NumberViewHolder and not just NumberViewHolder: in the
    // `extends` declaration you are not yet inside the class, so the name of the inner
    // class must be qualified. In the rest of the file you can write just NumberViewHolder.
    //
    // super(itemView) — mandatory. RecyclerView.ViewHolder has no constructor without
    // parameters, so the compiler forces you to call it. It saves the row in itemView.
    //
    // findViewById in the holder's constructor — this is the CREATION half. It runs
    // once per holder, not on every display.
    //
    // The final fields — they can be final because they are assigned in the constructor. In
    // NumberListAdapter they could not, because they were assigned after `new ViewHolder()`.
    // This small difference is exactly why RecyclerView moved the lookup
    // into the constructor.
    //
    // static class NumberViewHolder — static again, for the same two reasons: no
    // implicit reference to the adapter and no dependency on an adapter instance.
    // Here it is `static class` without `private`: the type appears in the class's public
    // signature (RecyclerView.Adapter<...NumberViewHolder>), so there is no point hiding it.
    // In NumberListAdapter the holder is `private static class`, because it appears in
    // no public signature.
    //
    // @NonNull on itemView — contract: RecyclerView never gives you null. The annotation
    // does not change the runtime; it is read by lint and by Android Studio.

    // ==================================================================================
    // BLOCK 3 — THE FIELD AND THE CONSTRUCTOR
    // ==================================================================================
    // Theory:  it receives only the list; the Context is taken from `parent` on creation.
    // Logic:   this.numbers = numbers.
    // Risk:    if you also asked for a Context, you would have an extra parameter that is not
    //          used anywhere in the class.
    // Ref:     chapter 12.
    // ==================================================================================
    private final List<Integer> numbers;   // the data source, by reference

    public NumberRecyclerAdapter(List<Integer> numbers) {
        this.numbers = numbers;   // we do NOT copy the list
    }

    // The adapter's constructor receives only the list — unlike
    // NumberListAdapter, it does not need a Context. The context is taken when needed from
    // parent.getContext(), in onCreateViewHolder.
    //
    // Why it has no LayoutInflater as a field: because it can take one from `parent` on
    // every creation, and creations are rare. A field would be a premature optimization —
    // and would require a Context in the constructor.

    // ==================================================================================
    // BLOCK 4 — onCreateViewHolder, THE CREATION HALF
    // ==================================================================================
    // Theory:  creates a NEW holder: inflates the row and builds the box.
    // Logic:   1. LayoutInflater.from(parent.getContext())
    //          2. inflate(item_number, parent, false)
    //          3. new NumberViewHolder(view)
    // Risk:    if you forget `false`, inflate returns the parent, not the row.
    // Ref:     chapter 12.
    // ==================================================================================
    @NonNull
    @Override
    public NumberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Runs only when a new row is needed
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_number, parent, false);   // THE SAME layout as step 2
        return new NumberViewHolder(view);                       // the holder, not the view
    }

    // When it runs: only when RecyclerView needs a NEW holder — the recycling pool
    // is empty. At startup, for the first visible rows; then less and less
    // often.
    //
    // `parent` — the RecyclerView. The Context for the inflater is taken from it, and it
    // supplies the LayoutParams.
    //
    // `viewType` — here it is always 0, because we do not override getItemViewType. If you
    // had two kinds of rows (header + item), you would return 0/1 from getItemViewType
    // and switch here, to inflate the right layout.
    //
    // LayoutInflater.from(parent.getContext()) — a layoutInflater field is no longer
    // needed, because `parent` carries the context with it.
    //
    // .inflate(R.layout.item_number, parent, false) — exactly the same call as with ListView:
    // the same layout, the same parameters, the same false.
    //
    // return new NumberViewHolder(view) — returns the HOLDER, not the view. RecyclerView
    // needs the holder so it knows what it created and can recycle it.
    //
    // @NonNull — contract: parent is not null, the result is not null. Lint checks it.
    //
    // Why the method does not return View: because RecyclerView does not work with Views.
    // It works with holders, which know what children they contain. A returned View would
    // have to be re-inspected on every reuse — exactly what ViewHolder avoids.

    // ==================================================================================
    // BLOCK 5 — onBindViewHolder AND getItemCount
    // ==================================================================================
    // Theory:  fills an existing holder with the data at the given position.
    // Logic:   1. numbers.get(position)  2. setText × 2
    // Risk:    if you do a `findViewById` here, you have lost the whole gain of the holder.
    // Ref:     chapter 12.
    // ==================================================================================
    @Override
    public void onBindViewHolder(@NonNull NumberViewHolder holder, int position) {
        // Runs each time a row shows a number
        int number = numbers.get(position);                          // Integer → int
        holder.tvDecimal.setText(String.valueOf(number));            // column 1
        holder.tvBinary.setText(Integer.toBinaryString(number));     // column 2
    }

    // ----------------------------------------------------------------------------------
    // METHOD 3 — getItemCount
    // ----------------------------------------------------------------------------------
    // Theory:  replaces getCount() from BaseAdapter.
    // Logic:   the size of the list.
    // Risk:    if it lies, onBindViewHolder receives an invalid position.
    // Ref:     chapter 12.
    // ----------------------------------------------------------------------------------
    @Override
    public int getItemCount() {
        return numbers.size();   // 100
    }

    // When onBindViewHolder runs: every time a row SHOWS an element —
    // on the first display, on every reuse after a scroll, after notifyItemChanged,
    // after an animation.
    //
    // There is no convertView — the holder arrives already created. There is no
    // `if (convertView == null)`, because the decision has already been made by RecyclerView.
    //
    // There is no findViewById — the references are in the holder, taken at creation.
    //
    // numbers.get(position) directly — the adapter has no getItem(position). If you need
    // it (for example for a click), you add it as a method of your own.
    //
    // getItemCount() replaces getCount() — the same role: how many elements the list has.
    //
    // If getItemCount() lies: if it returns more than numbers.size(),
    // onBindViewHolder reaches numbers.get(position) with an invalid position →
    // IndexOutOfBoundsException. If it returns less, the last elements simply
    // do not appear.
    //
    // Why onBindViewHolder returns void: because the holder is already at RecyclerView.
    // Nothing needs to be returned — the method only modifies an object that RecyclerView
    // already has. In getView, the return was necessary because ListView had no other
    // way to find out which view to add.
    //
    // What happens if you tap on a row: nothing. There is no listener. In a real
    // app, you would put an OnClickListener on holder.itemView right in onBindViewHolder —
    // and you would take care to use holder.getAdapterPosition(), not `position`, because
    // the position can change between the bind and the click.
    //
    // ==================================================================================
    // GETVIEW()  vs  onCreateViewHolder() + onBindViewHolder() — THE SAME PICTURE, SPLIT
    // ==================================================================================
    //
    // ┌─────────────────────────────────────────────────────────────────────────────────┐
    // │                          LISTVIEW: A SINGLE METHOD                              │
    // │                                                                                 │
    // │   getView(position, convertView, parent)                                        │
    // │   ├─────────────────────────────────────────────────────────────────────────┐   │
    // │   │ PART A — CREATION                                     only if null     │   │
    // │   │   if (convertView == null) {                                           │   │
    // │   │       convertView = inflate(...)          ← XML → objects              │   │
    // │   │       holder = new ViewHolder()            ← the empty box              │   │
    // │   │       holder.tvDecimal = findViewById(...) ← lookup                     │   │
    // │   │       holder.tvBinary  = findViewById(...) ← lookup                     │   │
    // │   │       convertView.setTag(holder)           ← glues the box              │   │
    // │   │   }                                                                    │   │
    // │   ├─────────────────────────────────────────────────────────────────────────┤   │
    // │   │ PART B — FILLING                                 on EVERY call          │   │
    // │   │   else { holder = getTag() }              ← takes the box              │   │
    // │   │   holder.tvDecimal.setText(...)                                        │   │
    // │   │   holder.tvBinary.setText(...)                                         │   │
    // │   │   return convertView                                                   │   │
    // │   └─────────────────────────────────────────────────────────────────────────┘   │
    // │                                                                                 │
    // │   ↑ the two parts are in the SAME method, separated only by an `if`             │
    // └─────────────────────────────────────────────────────────────────────────────────┘
    //
    //                                     ↓  the split  ↓
    //
    // ┌─────────────────────────────────────────────────────────────────────────────────┐
    // │                       RECYCLERVIEW: TWO SEPARATE METHODS                        │
    // │                                                                                 │
    // │   onCreateViewHolder(parent, viewType)      onBindViewHolder(holder, position)  │
    // │   ├──────────────────────────────────┐      ├──────────────────────────────────┤
    // │   │ PART A — CREATION                │      │ PART B — FILLING                 │
    // │   │   View view = inflate(...)       │      │   int number = numbers.get(pos)  │
    // │   │   return new NumberViewHolder(   │      │   holder.tvDecimal.setText(...)  │
    // │   │       view)                      │      │   holder.tvBinary.setText(...)   │
    // │   │       ├─ super(itemView)         │      │   (void — returns nothing)       │
    // │   │       ├─ findViewById × 2        │      │                                  │
    // │   │       └─ final fields            │      │                                  │
    // │   └──────────────────────────────────┘      └──────────────────────────────────┘
    // │                                                                                 │
    // │   WHO DECIDES WHICH ONE IS CALLED:                                              │
    // │     empty pool          → onCreateViewHolder + onBindViewHolder                 │
    // │     pool has a holder   → only onBindViewHolder                                 │
    // │                                                                                 │
    // │   WHO TAKES THE PLACE OF convertView: the internal pool (RecycledViewPool)      │
    // │   WHO TAKES THE PLACE OF setTag/getTag: the adapter's generic type              │
    // └─────────────────────────────────────────────────────────────────────────────────┘
    //
    // Read the diagram from top to bottom, then from bottom to top. Top: one method, two
    // branches, an `if` that separates them. Bottom: two methods, each with a single branch,
    // and the decision moved into the framework. THE SAME WORK, DIVIDED DIFFERENTLY.
    //
    // WHAT IS GAINED CONCRETELY:
    //   refilling without creation  the pool gives a ready-made holder; only onBindViewHolder
    //   animations                  RecyclerView knows which holder moved
    //   partial updates             notifyItemChanged(5) rebinds only position 5
    //   typed code                  the holder is a parameter, not an Object from a tag
    //   testability                 you can test onBindViewHolder with a holder built by hand
    //
    // WHAT IS LOST: two methods instead of one, one extra generic parameter,
    // a mandatory super(itemView). For a list of 100 numbers, the gain is small.
    // For a list of 10,000 images, it is the difference between 60fps and 20fps.
    //
    // ==================================================================================
    // WHAT HAPPENS IF YOU FORGET super(itemView)
    // ==================================================================================
    //
    //   NumberViewHolder(@NonNull View itemView) {
    //       // super(itemView);   ← removed
    //       tvDecimal = itemView.findViewById(R.id.tvDecimal);
    //   }
    //
    // It does not compile: RecyclerView.ViewHolder has no constructor without parameters, and
    // the compiler tries to call super() implicitly. The error is at compile time, not at
    // runtime — so you catch it immediately:
    //
    //   error: constructor ViewHolder in class ViewHolder cannot be applied to given types;
    //     required: View
    //     found: no arguments
    //
    // If you called super(null), it would compile, but itemView would be null and RecyclerView
    // would crash on the first layout, when it tries to add the holder to the tree.
    //
    // ==================================================================================
    // RECYCLEDVIEWPOOL — WHAT IT HOLDS AND WHEN IT EMPTIES
    // ==================================================================================
    //
    //   RecycledViewPool
    //     ├── SparseArray<ScrapData> per viewType
    //     │     ├── viewType 0 → list of holders (max 5 by default)
    //     │     └── viewType 1 → list of holders
    //     └── each holder holds itemView + the references from the constructor
    //
    // By default, the pool keeps a maximum of 5 holders per type. The rest are left to
    // be collected. For a list with 10 visible rows, 5 in the pool are enough.
    //
    // It empties on setAdapter with another adapter, on setRecycledViewPool(null) and on
    // the destruction of the RecyclerView. It does not empty on scroll.
    //
    // ==================================================================================
    // HOW TO ADD A CLICK ON A ROW
    // ==================================================================================
    //
    //   @Override
    //   public void onBindViewHolder(@NonNull NumberViewHolder holder, int position) {
    //       int number = numbers.get(position);
    //       holder.tvDecimal.setText(String.valueOf(number));
    //       holder.tvBinary.setText(Integer.toBinaryString(number));
    //       holder.itemView.setOnClickListener(v ->
    //               Toast.makeText(v.getContext(),
    //                       "Position " + holder.getAdapterPosition(),
    //                       Toast.LENGTH_SHORT).show());
    //   }
    //
    // Why getAdapterPosition() and not `position`: between the moment of the bind and the
    // moment of the click, the list can change (an element deleted, one added). `position`
    // is the value from the bind, which can be stale; getAdapterPosition() returns the
    // current position. In the repo code there is no click, so there is no problem either.
    //
    // ==================================================================================
    // WHY THE HOLDER DOES NOT NEED THE ADAPTER
    // ==================================================================================
    //
    // A ViewHolder holds itemView and two children. It needs no list, no position or
    // adapter in order to exist. That is why it can be `static`: it depends on no
    // instance of the outer class. All the information arrives as a parameter.
    //
    // In NumberListAdapter, the holder is built in getView and filled immediately, in the same
    // place. Here it is built in onCreateViewHolder and filled in another method, with data
    // received as a parameter. The design difference shows in the constructor's signature.
}
