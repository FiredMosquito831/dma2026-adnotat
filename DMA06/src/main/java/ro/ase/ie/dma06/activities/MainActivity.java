/*
 * ======================================================================================
 * FILE: MainActivity.java
 * ======================================================================================
 * LOCATION: DMA06/src/main/java/ro/ase/ie/dma06/activities/MainActivity.java
 * PACKAGE:  ro.ase.ie.dma06.activities
 * ROLE:     the app menu. Three buttons, three activities. It touches no list,
 *           no adapter, no data. All it does is start the right screen.
 *
 * FILE STRUCTURE (3 parts):
 *   1. imports          — 6 lines
 *   2. onCreate         — 8 lines: inflates the layout, then three identical calls
 *   3. openOnClick      — 4 lines: findViewById + listener
 *
 * WHAT YOU NEED TO KNOW BEFOREHAND:
 *   - `startActivity` returns nothing; the screens do not send a result back.
 *   - `Class<?>` is a class type, not an instance. `SingleColumnActivity.class` is the object.
 *   - `this` inside a lambda is MainActivity, not the lambda. That is why `new Intent(this, ...)` works.
 *
 * FLOW:
 *   MainActivity  ──btnStep1──▶  SingleColumnActivity    (ListView + ArrayAdapter)
 *                 ──btnStep2──▶  TwoColumnActivity      (ListView + NumberListAdapter)
 *                 ──btnStep3──▶  RecyclerViewActivity    (RecyclerView + NumberRecyclerAdapter)
 * ======================================================================================
 */

// ======================================================================================
// BLOCK 1 — THE PACKAGE AND THE IMPORTS
// ======================================================================================
// The package is what gives the class its full name: ro.ase.ie.dma06.activities.MainActivity.
// It must match the folder here (activities/), unlike the `data` and `adapters` packages,
// where it does not match — see 28.3.
// ======================================================================================

package ro.ase.ie.dma06.activities;                     // the package of the screens; all 4 activities live here

import android.content.Intent;                          // the object that describes "I am starting activity X"
import android.os.Bundle;                               // the saved state; not used here
import android.view.View;                               // the type returned by findViewById
import androidx.appcompat.app.AppCompatActivity;        // the base activity, with the AppCompat theme
import ro.ase.ie.dma06.R;                               // the class generated from resources

// `import android.view.View` although you build no View: it is the type of the local
// variable in openOnClick. findViewById has been generic since API 26: <T extends View> T findViewById(int).
// Here the compiler has nothing to infer T from, because the result is assigned to a
// variable declared explicitly as `View`. So `View` appears in the code and needs an import.
//
// Why AppCompatActivity and not Activity: it brings AppCompatDelegate, which applies the
// Material theme on any Android version and swaps the widgets at inflate time for the
// compatible variants (AppCompatTextView, AppCompatButton).
//
// `R` is generated, not written. The import carries the prefix from `namespace` (ro.ase.ie.dma06),
// not from the folder. If you change the namespace in build.gradle.kts, the import changes
// by itself on the next build — but you must know where it comes from.

// ======================================================================================
// THEORY BLOCK: THREE SCREENS, ONE MENU
// ======================================================================================
/*
 * 1. WHAT AN ACTIVITY IS
 *    -   A component with its own window. It has its own lifecycle and its own
 *        view tree.
 *    -   It is declared in AndroidManifest.xml. Without the declaration, startActivity throws
 *        ActivityNotFoundException at runtime, not at compile time.
 *
 * 2. HOW TO START ANOTHER ONE
 *    -   `startActivity(new Intent(context, Clasa.class))`.
 *    -   The explicit Intent (with a class) is the only safe way inside the same app.
 *    -   The current activity is NOT destroyed: it is stopped and left on the stack (back stack).
 *
 * 3. THE STACK, AT EVERY STEP
 *    [MainActivity]                       ← at launch
 *    [MainActivity, SingleColumnActivity] ← after clicking button 1
 *    [MainActivity]                       ← after Back
 *    MainActivity is NOT recreated on return: it receives onRestart → onStart → onResume.
 *    That is why the menu's onCreate runs only once per instance.
 *
 * 4. WHY A HELPER
 *    The three buttons differ in exactly two things: the id and the class. The rest is
 *    identical. A parameterized helper removes six duplicated lines and makes obvious
 *    what varies.
 *
 * DIAGRAM:
 *
 *   click btnStep1
 *     └─ OnClickListener.onClick(v)
 *          └─ startActivity(new Intent(this, SingleColumnActivity.class))
 *               ├─ ActivityManager receives the request
 *               ├─ PackageManager checks: does .activities.SingleColumnActivity exist?
 *               ├─ MainActivity.onPause()           ← stays on the stack
 *               ├─ SingleColumnActivity.onCreate()  ← NEW instance
 *               └─ SingleColumnActivity.onResume()  ← visible, receives input
 */

/** Opens each step of the demo. */
public class MainActivity extends AppCompatActivity {   // the only activity with a LAUNCHER intent-filter

    // ==================================================================================
    // METHOD 1 — onCreate
    // ==================================================================================
    // Theory:  the first method of the lifecycle; it runs once per instance.
    // Logic:   1. super.onCreate  2. setContentView  3. three openOnClick calls
    // Risk:    `super.onCreate` must come first; the AppCompat delegate needs it
    //          before the window is touched.
    // Ref:     chapter 15 (Application and lifecycle).
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);             // 1. starts the parent's lifecycle
        setContentView(R.layout.activity_main);         // 2. inflates activity_main.xml into the window

        openOnClick(R.id.btnStep1, SingleColumnActivity.class);    // 3. button 1 → step 1
        openOnClick(R.id.btnStep2, TwoColumnActivity.class);       //    button 2 → step 2
        openOnClick(R.id.btnStep3, RecyclerViewActivity.class);    //    button 3 → step 3
    }

    // `@Override` — the method exists in Activity. The annotation does not change behaviour, but
    // it tells the compiler to check the signature. If you wrote onCreate(Bundle b, int x),
    // without @Override you would have a new method, which is never called.
    //
    // `protected`, not `public` — Activity.onCreate is protected. The method is called by
    // the framework, not by your code.
    //
    // `savedInstanceState` — null on the first start, a Bundle when the activity is recreated
    // (rotation, theme change, return after it was destroyed in the background). It is not
    // used here: the menu has no state.
    //
    // The order of the three lines is not negotiable. setContentView needs the window
    // created by super.onCreate. openOnClick needs the tree built by
    // setContentView, because findViewById searches inside it. Reversed, the first two throw
    // exceptions, the third returns null.

    // ==================================================================================
    // METHOD 2 — openOnClick
    // ==================================================================================
    // Theory:  a helper that receives the button id and the class to start.
    // Logic:   1. findViewById  2. setOnClickListener  3. startActivity(new Intent(...))
    // Risk:    `findViewById` returns `View`, not `Button`; if the id does not exist in the layout
    //          it returns null and `setOnClickListener` throws NullPointerException.
    // Ref:     chapter 04 (MainActivity - list).
    // ==================================================================================
    private void openOnClick(int buttonId, Class<?> activityClass) {
        View button = findViewById(buttonId);           // finds the button in the tree inflated at step 2
        button.setOnClickListener(v -> startActivity(new Intent(this, activityClass)));
        //                              ↑ lambda: `this` = MainActivity, so it is a valid Context
    }

    // Why a helper and not three identical blocks: the only things that differ between
    // the three buttons are the id and the class. The rest is the same code. If you added a
    // step 4, you would add one line, not seven.
    //
    // `Class<?>` — the second parameter of the Intent(Context, Class<?>) constructor is
    // the class of the component to start. `<?>` means "any class"; here all
    // three are activities, but the type does not enforce it. You could write
    // `Class<? extends Activity>` and you would get an extra check at compile time.
    //
    // `this` inside the lambda — inside a lambda expression, `this` remains the instance that surrounds
    // the lambda, that is MainActivity. Inside an anonymous class (new View.OnClickListener() {...}),
    // `this` would be the instance of the anonymous class, which is NOT a Context, and you would have to write
    // MainActivity.this. The lambda is shorter here exactly for this reason.
    //
    // `v -> startActivity(...)` — the parameter v is the view that received the click, that is
    // the button. It is not used: we already know which button it is, because the listener is installed on it.
    //
    // `startActivity` and not `launcher`: the screens of the three steps return nothing. You do not
    // need registerForActivityResult, because you are not waiting for a result.
    //
    // `View button` and not `MaterialButton`: the helper works with any view that has
    // setOnClickListener. findViewById returns the declared type; here it is View, because
    // the helper needs nothing specific to a button.
    //
    // ==================================================================================
    // WHAT HAPPENS ON BACK
    // ==================================================================================
    //
    //   SingleColumnActivity.onPause()
    //     → SingleColumnActivity.onStop()
    //     → SingleColumnActivity.onDestroy()        ← the instance dies
    //     → MainActivity.onRestart()
    //     → MainActivity.onStart()
    //     → MainActivity.onResume()                 ← the menu returns, WITHOUT onCreate
    //
    // Why it matters: the menu's onCreate does not run a second time, so neither
    // setContentView nor openOnClick repeat. The buttons keep their
    // listeners because the view tree was not destroyed.
    //
    // What would happen if MainActivity had been destroyed in the meantime (low memory):
    // on return, onCreate runs again, with a non-null savedInstanceState. It does not
    // matter here, because the menu has no state to restore.
    //
    // ==================================================================================
    // WHAT THIS FILE DOES NOT DO
    // ==================================================================================
    //
    //   -   It creates no adapter.
    //   -   It reads no list.
    //   -   It does not know what a ViewHolder is.
    //   -   It keeps no reference to the activities it starts.
    //
    // The last line matters: after startActivity, MainActivity has no connection
    // with the screen it started. It cannot read data from it, cannot find out when it closes,
    // cannot receive anything back. If you needed a result, you would use
    // registerForActivityResult — but none of the three steps returns anything.
}
