/*
 * ======================================================================================
 * FILE: SecondActivity.java
 * ======================================================================================
 * ROLE:
 *   A lab screen for three things that do not fit in MainActivity:
 *     1. a confirmation dialog built with `AlertDialog.Builder`;
 *     2. a `Snackbar` with an undo action;
 *     3. a view created from code (`new Button`) and added to the tree.
 *
 * WHAT IT DEMONSTRATES:
 *   1. AlertDialog.Builder   — a chain of setters + `show()`.
 *   2. Snackbar.make(...)    — transient feedback with an action button.
 *   3. addButton()           — `new Button(ctx)`, `setId`, `LayoutParams`, `addView`.
 *
 * DEPENDS ON:
 *   - android.app.AlertDialog        → the PLATFORM variant (not AppCompat)
 *   - com.google.android.material    → Snackbar
 *   - android:onClick="btnClick"     → the link from the XML, through reflection
 *
 * WHAT IT DOES NOT DO:
 *   - it is not started from MainActivity (no `Intent` in the whole project);
 *   - it does not save the added buttons across rotation;
 *   - it validates nothing — any click on "Do it!" adds a button.
 *
 * REF: ch. 14 (the section on dialogs), the documentation of `AlertDialog.Builder`,
 *      `Snackbar`, `ViewGroup.LayoutParams`.
 * ======================================================================================
 */

package ro.ase.ie.dma03;

// ======================================================================================
// IMPORT 1: android.app.AlertDialog — CAREFUL, the PLATFORM variant
// ======================================================================================
// WHAT IT IS: the standard dialog of the Android system.
// WHY IT IS A DEBATABLE CHOICE: there is also `androidx.appcompat.app.AlertDialog`.
//   The platform variant uses `android.R.attr.alertDialogTheme` of the current
//   theme; the AppCompat variant forces the library's theme and style. With
//   `Theme.Material3.DayNight.NoActionBar`, the visual difference may be small or
//   nil, but the AppCompat variant is the one that respects the app theme in all
//   cases. Functionally they are identical.
// WHY IT MATTERS ANYWAY: if the app theme were a pure AppCompat one (without
//   Material3), `android.app.AlertDialog` would fall back to the system style — buttons
//   coloured differently, another font, another shadow.
// ======================================================================================
import android.app.AlertDialog;

// ======================================================================================
// IMPORT 2: android.content.DialogInterface — the interface of the dialog buttons
// ======================================================================================
// WHAT IT IS: the interface that contains the dialog's `OnClickListener`.
// WHY IT IS IMPORTED SEPARATELY: `OnClickListener` is an interface NESTED inside it:
//   `DialogInterface.OnClickListener`. Without the import, you would have to write the full name.
// ======================================================================================
import android.content.DialogInterface;

// ======================================================================================
// IMPORT 3: android.os.Bundle — the state saved on recreation
// ======================================================================================
// The signature of `onCreate(Bundle)` requires it. The code never reads it.
// ======================================================================================
import android.os.Bundle;

// ======================================================================================
// IMPORTS 4-5: android.util.DisplayMetrics + android.util.TypedValue
// ======================================================================================
// USED ONLY IN `addButton`, for the 50dp → pixels conversion.
// `DisplayMetrics` = the screen density (how many pixels one dp has).
// `TypedValue.applyDimension` = the conversion itself, for any unit.
// THE RESULT IS DEAD CODE (see 26.2.7). The imports stay because the line that uses
// them exists.
// ======================================================================================
import android.util.DisplayMetrics;
import android.util.TypedValue;

// ======================================================================================
// IMPORTS 6-7: android.view.View + android.view.ViewGroup
// ======================================================================================
// `View` — the type of the parameter in `btnClick(View view)`. The signature is imposed by
//   `android:onClick` in the XML.
// `ViewGroup` — the base class for `LayoutParams`. `ViewGroup.LayoutParams` is
//   the type required by `setLayoutParams`; `LinearLayout.LayoutParams` would be the subtype
//   with `weight`.
// ======================================================================================
import android.view.View;
import android.view.ViewGroup;

// ======================================================================================
// IMPORTS 8-10: android.widget.Button + LinearLayout + Toast
// ======================================================================================
// `Button`        — the widget created from code.
// `LinearLayout`  — the container into which the buttons are added.
// `Toast`         — the cancellation message from the negative button.
// ======================================================================================
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

// ======================================================================================
// IMPORTS 11-15: androidx.* — the activity infrastructure
// ======================================================================================
// Identical to the ones in MainActivity: EdgeToEdge, AppCompatActivity, Insets,
// ViewCompat, WindowInsetsCompat. The insets block is copied word for word.
// ======================================================================================
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// ======================================================================================
// IMPORT 16: com.google.android.material.snackbar.Snackbar
// ======================================================================================
// WHAT IT IS: the feedback bar at the bottom of the screen, with text + an optional action.
// WHY IT COMES FROM MATERIAL: `Snackbar` is not part of the platform. That is why
//   `SecondActivity` needs `libs.material` in the dependencies — just like
//   `MainActivity` for the toolbar.
// WHAT WOULD HAPPEN WITH `Toast`: you would lose the action button. `Toast` has no
//   `setAction`, cannot be closed by the user and has no indefinite duration.
//   For "undo the last action", `Snackbar` is the only option in the platform.
// ======================================================================================
import com.google.android.material.snackbar.Snackbar;

/*
 * ======================================================================================
 * THEORY BLOCK: THREE KINDS OF FEEDBACK AND A VIEW BUILT FROM CODE
 * ======================================================================================
 *
 * 1. DIALOG — BLOCKS, DEMANDS A DECISION
 *    -   `AlertDialog` appears over the screen, in a separate window. The user
 *        must answer before continuing.
 *    -   It is built with `AlertDialog.Builder`: an object that gathers the configuration
 *        (title, message, buttons) and only at `show()` creates the dialog.
 *    -   Every setter returns THE SAME builder → a chain of calls.
 *
 * 2. TOAST — PASSES BY, DEMANDS NOTHING
 *    -   Short text, no action, no layout. It appears and disappears on its own.
 *    -   It does not need a parent view: it is drawn in its own window.
 *
 * 3. SNACKBAR — PASSES BY, BUT CAN BE UNDONE
 *    -   A bar at the bottom of the screen, with text + an optional action button.
 *    -   UNLIKE TOAST, it needs a VIEW: it is inserted into the view tree,
 *        not into a separate window. Hence the second argument
 *        in `make(...)`.
 *    -   With a `CoordinatorLayout` parent, it can be swiped away and
 *        can push a FAB upwards. With a `LinearLayout`, it works, but
 *        without those bonuses.
 *
 * 4. A VIEW BUILT FROM CODE
 *    -   Every widget has a constructor that requires a THEMED `Context`.
 *    -   A view created from code has no id, no `LayoutParams` and is not in the tree.
 *        Three separate operations:
 *          a) `setId(...)`            → so that it can be found with `findViewById`;
 *          b) `setLayoutParams(...)`  → so that the parent knows how to measure it;
 *          c) `parent.addView(...)`   → so that it enters the tree and is drawn.
 *    -   The order matters: `addView` without `LayoutParams` works (the parent
 *        generates some defaults), but `findViewById` without `setId` finds nothing.
 *
 * 5. WHY A VIEW ADDED FROM CODE DISAPPEARS ON ROTATION
 *    -   The view tree is rebuilt from the XML at every recreation.
 *    -   The added button lives only in the old instance of the activity.
 *    -   To survive, it would have to be saved in `savedInstanceState` (a list of
 *        ids/texts) and rebuilt in `onCreate`.
 *
 * REF: ch. 14 (dialogs and Snackbar), the documentation of `AlertDialog.Builder`,
 *      `Snackbar`, `ViewGroup.addView`.
 * ======================================================================================
 */

public class SecondActivity extends AppCompatActivity {

    // ==================================================================================
    // FIELD: inflatedLayout
    // ==================================================================================
    // WHAT IT IS: the root from `activity_second.xml` — a vertical `LinearLayout`.
    // WHY IT IS A FIELD AND NOT A LOCAL: it is used in THREE methods — `onCreate` (initialization),
    //   `btnClick` (Snackbar) and `addButton` (`addView`).
    // WITHOUT `private` — an inconsistency compared to `MainActivity`, where the fields are
    //   `private`. It breaks nothing (we are in the same package), but the convention is `private`.
    // THE NAME IS MISLEADING: it is not a layout "inflated" at runtime, it is the layout from the XML.
    //   The inflation was done by `setContentView`. The name suggests the intention — a container
    //   into which views are added — not the mechanism.
    // ==================================================================================
    LinearLayout inflatedLayout;

    /*
     * ----------------------------------------------------------------------------------
     * METHOD: onCreate(Bundle savedInstanceState)
     * ----------------------------------------------------------------------------------
     * THEORY:
     *   The creation callback. This is where `activity_second.xml` is inflated and the
     *   reference to the container into which the buttons will be added is obtained.
     *
     * LOGIC:
     *   1. `super.onCreate` — the parent.
     *   2. `EdgeToEdge.enable` — drawing under the bars.
     *   3. `setContentView` — inflates the layout.
     *   4. insets on `R.id.main` — padding for the system bars.
     *   5. `inflatedLayout = findViewById(R.id.main)` — the reference to the container.
     *
     * REF: ch. 14, the documentation of `Activity#onCreate`.
     *
     * RISK:
     *   - `inflatedLayout` initialized before `setContentView` → `null`, then
     *     `NullPointerException` on the first `addView`.
     * ----------------------------------------------------------------------------------
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_second);

        // The insets block is IDENTICAL to the one in MainActivity — copied. Here
        // `R.id.main` is on the root `LinearLayout`, not on a `CoordinatorLayout`.
        // It works the same way: padding on the root, the children move into the safe area.
        //
        // Why it is copied and not extracted into a utility function: in a lab
        // project, copying is acceptable. In a real application, you would have a function
        // `applyInsets(View root)` in a utility class — or you would use
        // `ViewCompat.setOnApplyWindowInsetsListener` once, in a base
        // class.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // The LAST line — after `setContentView`. If it came before, it would receive `null`
        // and the first `addView` would throw `NullPointerException`.
        //
        // What `findViewById(R.id.main)` does here: it searches the tree of
        // `activity_second.xml`. The id `main` also exists in `activity_main.xml`, but
        // the two layouts never coexist in the same tree — each
        // activity has its own window. It is not a collision.
        inflatedLayout = findViewById(R.id.main);
    }

    /*
     * ----------------------------------------------------------------------------------
     * METHOD: btnClick(View view)
     * ----------------------------------------------------------------------------------
     * THEORY:
     *   A public method, with a fixed signature, called through REFLECTION by the platform when
     *   the button with `android:onClick="btnClick"` from the XML is pressed. There is no
     *   `setOnClickListener` anywhere in the code — the whole link is in the XML.
     *
     * LOGIC:
     *   1. Creates an `AlertDialog.Builder` with the activity context.
     *   2. Chains: title → message → cancelable → positive button → negative button.
     *   3. In the positive button: `addButton()` returns the id of the new button.
     *   4. Shows a `Snackbar` with the "Cancel" action, which deletes the button by id.
     *   5. In the negative button: only a `Toast`.
     *   6. `builder.show()` — only here does the dialog appear on screen.
     *
     * REF:
     *   ch. 14 (the section on dialogs), the documentation of `android:onClick`,
     *   `AlertDialog.Builder`, `Snackbar.make`.
     *
     * RISK:
     *   - the method made `private` → it compiles, but on click:
     *     `IllegalStateException: Could not find method btnClick(View) ...`
     *   - the parameter changed from `View` to another type → the same exception, at runtime.
     *   - `builder.show()` forgotten → the dialog is built and never displayed.
     * ----------------------------------------------------------------------------------
     */
    public void btnClick(View view) {
        // The signature is IMPOSED by the XML. In `activity_second.xml`, the button has
        // `android:onClick="btnClick"`. At runtime, Android looks, through reflection, for a
        // method `public void btnClick(View)` in the activity context.
        //
        // THE CONSEQUENCE: if you make it `private`, or change its parameter to `Object`,
        // or rename it without modifying the XML, COMPILATION PASSES. The error appears
        // on click: `IllegalStateException: Could not find method btnClick(View) in a
        // parent or ancestor Context`. One of the few places in Android where
        // the mistake is not visible at build time.
        //
        // `view` is the pressed button. It is not used here — the code does not need to know
        // which button it was. But the parameter is mandatory: reflection looks for exactly
        // the signature with a `View`.
        //
        // Why `public` and not `protected`: reflection looks for public methods.
        // `protected` or package-private are not found from the `View` that makes
        // the call (another package).
        //
        // THE MODERN ALTERNATIVE: `setOnClickListener` in `onCreate`. It is safer
        // (the compiler checks), but more verbose. `android:onClick` is accepted
        // for demonstrations; Google considers it outdated for new code.

        // `AlertDialog.Builder` — builds the dialog piece by piece. Every
        // setter returns THE SAME builder, that is why they can be chained. The chain
        // ends with `setNegativeButton`, and `builder.show()` at the end of the method
        // displays the dialog.
        //
        // Why the chain works: `setTitle`, `setMessage`, `setCancelable`,
        // `setPositiveButton`, `setNegativeButton` all have `return this;` in
        // their implementation. It is not magic — it is the "fluent builder" convention.
        //
        // `SecondActivity.this` and not `this`: inside a listener, `this` would
        // mean the listener. `SecondActivity.this` is the syntax for the instance of the
        // outer class. Here we are still in the body of the method, so `this` would have worked —
        // but the explicit form is a good habit, because in two steps you will be in
        // `onClick`.
        AlertDialog.Builder builder = new AlertDialog.Builder(SecondActivity.this);
        builder.setTitle("Add new control ...")
                .setMessage("Are you sure you want to add a new control?")

                // `setCancelable(true)` is the default value. Written explicitly,
                // it documents the behaviour: it can be closed with Back or with a tap
                // outside. The consequence: if the user closes it that way, NO button
                // runs — neither "Do it!" nor "Cancel". If you need a guaranteed
                // action, you need `setOnCancelListener`.
                //
                // With `setCancelable(false)`, the dialog can no longer be closed except
                // through the two buttons. For a critical confirmation that is the correct
                // choice; for an ordinary one, it annoys the user.
                .setCancelable(true)

                // The positive button is the main action — it appears on the right, coloured.
                // The text "Do it!" is explicit; "OK" would be vague.
                //
                // `which` is `DialogInterface.BUTTON_POSITIVE` here. It is not used —
                // each button has its own listener. If you had a single listener
                // for all the buttons, `which` would be the only way to tell them apart.
                //
                // Why the listener is an anonymous class and not a lambda:
                // `DialogInterface.OnClickListener` has a single abstract method,
                // so a lambda would work too (`(dialog, which) -> { ... }`). The code
                // chooses the old form, from the Java 7 generation. Both compile; a lambda
                // is shorter.
                .setPositiveButton("Do it!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Creates the button BEFORE the Snackbar, so that you have something to delete.
                        // The returned id is the ONLY link to the new button: you have no
                        // reference to the object, only a number.
                        int id = addButton();

                        // `Snackbar.make(context, view, text, duration)` — four arguments:
                        //   context  : SecondActivity.this — for the theme and resources
                        //   view     : inflatedLayout — the reference view
                        //   text     : "Cancel previous action."
                        //   duration : Snackbar.LENGTH_LONG ≈ 2.75 s
                        //
                        // WHY the second argument must be a VIEW: `Snackbar`
                        // is not a separate window like the `Toast`. It is an ordinary view,
                        // added to the existing tree. To know where to add it,
                        // it needs a starting point: it starts from the given view
                        // and walks up through the parents until it finds a `CoordinatorLayout` or
                        // `android.R.id.content`. Without a `View`, it has nowhere to
                        // start from — that is why there is no
                        // `make(Context, String, int)` overload.
                        //
                        // What exactly happens with `inflatedLayout`: it is a plain
                        // `LinearLayout`. `Snackbar` walks up from it to `android.R.id.content`
                        // (the window container) and adds its bar there. The result:
                        // the bar appears at the bottom, over the content.
                        //
                        // What you would gain with a `CoordinatorLayout`: `SwipeDismissBehavior`
                        // (you swipe to close it) and the coordination with a
                        // `FloatingActionButton` (the FAB moves up while the
                        // Snackbar is visible). Here there is no FAB, so the difference is
                        // only the swipe.
                        Snackbar.make(SecondActivity.this, inflatedLayout, "Cancel previous action.", Snackbar.LENGTH_LONG)

                                // `setAction("Cancel", listener)` — adds the button on the
                                // right. The text is short on purpose: an "Undo last
                                // action" would crowd the message on small screens.
                                //
                                // The text and the label are both "Cancel" — an unfortunate
                                // naming choice. The message "Cancel previous action."
                                // says what happened; the button "Cancel" should
                                // be "Undo", because it UNDOES the addition, not cancels
                                // the Snackbar. Functionally, the button deletes the added button.
                                .setAction("Cancel", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // `id` is captured from the outer lambda. It is an
                                        // `int` local variable, assigned a single
                                        // time → effectively final → it can be captured.
                                        // If you wrote `id++` between the declaration and
                                        // the use, the compiler would refuse the capture.
                                        //
                                        // `inflatedLayout.findViewById(id)` — works
                                        // because the button received an explicit id with
                                        // `setId(counter)`. Without `setId`, `findViewById`
                                        // would return `null` and `removeView(null)` would
                                        // throw `NullPointerException`.
                                        Button viewById = inflatedLayout.findViewById(id);

                                        // `removeView(viewById)` — removes the view from the
                                        // container. The button disappears from the screen; the object
                                        // stays in memory until the next garbage
                                        // collection.
                                        inflatedLayout.removeView(viewById);
                                    }
                                }).show();
                        // `.show()` at the end — without it, the Snackbar is built and
                        // never displayed. The same trap as with `Toast`.
                        //
                        // The moment of display: the Snackbar is created INSIDE the `onClick` of the
                        // positive button, so it appears after the dialog has closed.
                        // If you created it before `builder.show()`, it would appear under
                        // the dialog.
                        //
                        // The `LENGTH_LONG` duration: ~2.75 seconds (the values in
                        // `SnackbarManager`: 1,500 ms for `SHORT`, 2,750 ms for
                        // `LONG`). Enough for the user to press "Cancel".
                        // `LENGTH_INDEFINITE` would keep the bar until an action or a
                        // swipe — too aggressive for a confirmation.

                    }
                })

                // The negative button — cancel. Only a `Toast`, no state change.
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(SecondActivity.this, "Object creation cancelled", Toast.LENGTH_LONG).show();
                    }
                });

        // `builder.show()` — creates the dialog and displays it. Equivalent to
        // `builder.create().show()`. It is called ONCE, after the chain of
        // setters has ended.
        //
        // Why `show()` is separate and not in the chain: because the chain ends with `;`
        // after `setNegativeButton`, and `show()` is a method of the builder, not a
        // setter. `builder.setTitle(...).show()` would have worked too, but the code chooses the
        // explicit form.
        //
        // What happens if you call `show()` twice: two dialogs are created,
        // both displayed. The second appears over the first. It is not an exception — just a visual
        // bug.
        //
        // What happens if you call a setter after `show()`: it has no effect on the
        // displayed dialog. The builder copies the values into the dialog at `create()`;
        // after that, the setters modify only the builder, not the live dialog.
        builder.show();
    }


    // ==================================================================================
    // FIELD: counter
    // ==================================================================================
    // DECLARED BETWEEN METHODS — legal in Java, unusual as style.
    // TWO ROLES AT ONCE: id generator and source for the label.
    // WHY NOT `static`: it would be shared between instances. On recreation, the new activity
    //   would continue from the old value — ids that do not repeat, but also a counter
    //   that grows to infinity. Here `counter` is per instance, so it resets on
    //   rotation.
    // ==================================================================================
    int counter = 0;

    /*
     * ----------------------------------------------------------------------------------
     * METHOD: addButton()
     * ----------------------------------------------------------------------------------
     * THEORY:
     *   A view built from code is an ordinary Java object until you add it to a
     *   parent. It needs three things to be useful:
     *     1. a THEMED CONTEXT (the constructor);
     *     2. an ID (so that it can be found again);
     *     3. LayoutParams (so that the parent knows how to measure it).
     *
     * LOGIC:
     *   1. Converts 50dp to pixels (the result is ignored — dead code).
     *   2. Creates `new Button(SecondActivity.this)`.
     *   3. Sets the text with pre-increment: "Button_1", "Button_2", ...
     *   4. Sets the id = counter (1, 2, 3, ...).
     *   5. Creates `ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)`.
     *   6. `addView(newBtn)` — adds it at the end of the `LinearLayout`.
     *   7. Returns the id, so that the caller can delete the button.
     *
     * REF:
     *   ch. 14 (the section on dynamic views), the documentation of `View#setId`,
     *   `ViewGroup.LayoutParams`, `ViewGroup#addView`.
     *
     * RISK:
     *   - application context instead of activity → you lose the theme;
     *   - `setId` forgotten → the button cannot be found and deleted;
     *   - `counter` not saved → on rotation the buttons disappear and the ids repeat.
     * ----------------------------------------------------------------------------------
     */
    public int addButton()
    {
        // DEAD CODE. `hPixels` is computed and NEVER used. The intention was probably
        // `newBtn.setHeight(hPixels)` or a `LayoutParams` with a 50dp height.
        // The compiler gives a warning (`variable hPixels is never used`), not an error.
        //
        // What it does anyway, useful to know: `TypedValue.applyDimension(COMPLEX_UNIT_DIP,
        // 50, metrics)` converts 50dp into physical pixels, using the screen
        // density. At 3x, it returns 150. It is the correct conversion when you need
        // pixels in a calculation — for example for animations or for `setPadding`.
        //
        // Why not `50 * getResources().getDisplayMetrics().density`: it is equivalent,
        // but `applyDimension` also handles `SP`, `PT`, `MM`, `IN` with the same
        // signature. One function, all the units.
        //
        // Why `DisplayMetrics` is obtained only once:
        // `getResources().getDisplayMetrics()` returns the object of the current
        // configuration. Called several times, it returns the same object. It is not an expensive
        // operation, but it is pointless to repeat it.
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int hPixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50,
                displayMetrics);

        // `new Button(Context)` — the constructor requires a THEMED CONTEXT.
        // `SecondActivity.this` has the `Theme.DMA03` theme. If you passed
        // `getApplicationContext()`, the button would lose the theme and, on some Material
        // attributes, would throw an exception at inflate.
        //
        // Why `new Button(ctx)` and not `LayoutInflater.from(ctx).inflate(...)`:
        // for a simple button, the constructor is more direct. The inflater makes sense
        // when you want a whole XML layout, with styles and children.
        Button newBtn = new Button(SecondActivity.this);

        // `++counter` (pre-increment) — increments first, then uses. The first
        // button is `Button_1`, and `counter` becomes 1. With `counter++` (post-increment),
        // the first would be `Button_0` and you would have a button with id 0.
        newBtn.setText("Button_" + (++counter));

        // `setId(counter)` — ids 1, 2, 3... The resource ids are in the
        // `0x7f......` range, so they do not collide. `View.NO_ID` is `-1`, so 0 would have been
        // an unfortunate value — one more reason for the pre-increment.
        //
        // WITHOUT `setId`, the button exists but is impossible to find with `findViewById`.
        // It would appear on screen and you could no longer delete it.
        //
        // Why small ids are safe: `findViewById` searches by the integer
        // value. An id of 1 does not collide with `R.id.button` (which is `0x7f0b....`).
        // The only requirement is that it be unique in the subtree you search in.
        //
        // THE MODERN ALTERNATIVE: `View.generateViewId()` returns a unique, guaranteed id.
        // It would be safer if you had several sources of ids. Here `counter` is
        // enough.
        newBtn.setId(counter);

        // `ViewGroup.LayoutParams` — the base class. `LinearLayout` accepts it. If you
        // need `layout_weight`, you must use `LinearLayout.LayoutParams` — the base
        // class does not have that field.
        //
        // `MATCH_PARENT` on the width — the button fills the container, like the original button
        // from the XML. `WRAP_CONTENT` on the height — as much as the text needs.
        //
        // What the two constants mean: `MATCH_PARENT` = `-1` (fills the parent),
        // `WRAP_CONTENT` = `-2` (as much as the content). They are negative values on purpose,
        // so that they are not confused with real dimensions in pixels.
        //
        // WITHOUT `setLayoutParams`, `addView` applies the container's default
        // values — also `MATCH_PARENT`/`WRAP_CONTENT` for `LinearLayout`,
        // so practically the same result. Explicit is safer when the container
        // changes.
        //
        // What happens if you pass `LinearLayout.LayoutParams`: it works even
        // better — the parent no longer has to convert.
        // `ViewGroup.LayoutParams` is accepted because
        // `LinearLayout.generateLayoutParams(ViewGroup.LayoutParams)` knows how to
        // wrap it.
        ViewGroup.LayoutParams layoutParams =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        newBtn.setLayoutParams(layoutParams);

        // `addView` — adds at the end of the vertical `LinearLayout`, so below the
        // existing button. The second button appears below the first, the third below the second.
        //
        // `addView` has two effects: it attaches the view to the tree and triggers a
        // layout request (`requestLayout`). The button appears immediately, without you doing
        // anything else.
        inflatedLayout.addView(newBtn);

        // `return counter` — the id just assigned. The caller keeps it in `id` and
        // uses it in the Snackbar action.
        //
        // What happens on rotation: `counter` resets to 0, and the added buttons
        // disappear — they are not saved in `savedInstanceState`. On rotation,
        // the activity is recreated, `onCreate` runs again, and
        // `activity_second.xml` once again has a single button.
        //
        // Why the ids reset too: if you added buttons again after rotation,
        // the first would again receive id 1. It collides with nothing, because the old
        // buttons disappeared together with the tree.
        return counter;
    }
}
