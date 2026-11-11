/*
 * ======================================================================================
 * FILE: MainActivity.java
 * ======================================================================================
 * ROLE:
 *   The main screen of the DMA03 app. A single `onCreate` that ties together five
 *   components: the list, the context menu, the drawer, the top bar and the Back button.
 *
 * WHAT IT DEMONSTRATES:
 *   1. DrawerLayout + NavigationView   — side menu sliding over the content.
 *   2. MaterialToolbar                 — title, hamburger icon, actions menu.
 *   3. PopupMenu                       — context menu anchored to a row.
 *   4. OnBackPressedCallback           — intercepting the Back button.
 *
 * DEPENDS ON:
 *   - androidx.activity           → EdgeToEdge, OnBackPressedCallback
 *   - com.google.android.material → MaterialToolbar, NavigationView
 *   - androidx.drawerlayout       → DrawerLayout (brought in transitively by `material`)
 *
 * WHAT IT DOES NOT DO:
 *   - it does not start SecondActivity (there is no `startActivity` anywhere in the file);
 *   - it saves nothing in `savedInstanceState`;
 *   - it has no hand-written adapter — the list is fed from `@array/items`.
 *
 * REF: ch. 14 — DMA03: navigation, menus and dialogs.
 * ======================================================================================
 */

package ro.ase.ie.dma03;

// ======================================================================================
// IMPORT 1: android.content.Intent — IMPORTED BUT UNUSED
// ======================================================================================
// WHAT IT IS: the description of an action or of a screen to start.
// WHY IT IS HERE: the only way to open SecondActivity would be
//   `startActivity(new Intent(this, SecondActivity.class))`.
// WHY IT IS A PROBLEM: there is no `new Intent(...)` anywhere in the file, no
//   `startActivity`. The import is the ONLY trace of the missing link.
// HOW TO CHECK: Ctrl+F for "Intent" across the whole project → a single result, this line.
// ======================================================================================
import android.content.Intent;

// ======================================================================================
// IMPORT 2: android.os.Bundle — the state saved on recreation
// ======================================================================================
// WHAT IT IS: a key-value dictionary that the system keeps between instances.
// WHY IT IS HERE: the signature of `onCreate(Bundle)` requires it.
// WHY IT IS USELESS HERE: the code never reads `savedInstanceState`. On rotation,
//   everything returns to the initial state: the title "Inbox", "Inbox" checked, the list at the top.
// ======================================================================================
import android.os.Bundle;

// ======================================================================================
// IMPORT 3: android.widget.ListView — the list of 20 rows
// ======================================================================================
// WHAT IT IS: a `ViewGroup` that displays a scrollable vertical list.
// WHY IT IS HERE: the main screen is, in practice, a list with a context menu.
// WHY NOT `RecyclerView`: 20 static rows, no animations, no complicated recycled
//   views. `ListView` is enough and requires zero adapter code.
// ======================================================================================
import android.widget.ListView;

// ======================================================================================
// IMPORT 4: android.widget.PopupMenu — the menu anchored to a row
// ======================================================================================
// WHAT IT IS: a floating menu, attached to a specific view (the anchor).
// WHY IT IS HERE: a long press on a row must open Open / Rename / Delete
//   next to that row, not in the corner of the screen.
// WHY NOT `androidx.appcompat.widget.PopupMenu`: it would have worked, but the platform
//   variant is enough. The difference only shows in the background style and the ripple.
// ======================================================================================
import android.widget.PopupMenu;

// ======================================================================================
// IMPORT 5: android.widget.Toast — the short message, with no action
// ======================================================================================
// WHAT IT IS: a text that appears and disappears on its own, without blocking the screen.
// WHY IT IS HERE: all the menu branches are demonstrations — a `Toast` is enough.
// WHY NOT `Snackbar`: `Snackbar` needs a parent view and an extra `.show()`
//   call. For a confirmation with no action, `Toast` is more direct.
// ======================================================================================
import android.widget.Toast;

// ======================================================================================
// IMPORTS 6-13: androidx.* — the activity infrastructure
// ======================================================================================
// EdgeToEdge              → drawing under the system bars (API 21+).
// OnBackPressedCallback   → the modern mechanism for the Back button.
// AppCompatActivity       → the base activity, with AppCompat support.
// Insets                  → the rectangles of the system bars, in pixels.
// GravityCompat           → START/END, safe for RTL.
// ViewCompat              → the insets listener, compatible on all API levels.
// WindowInsetsCompat      → the insets types (systemBars, ime, displayCutout).
// DrawerLayout            → the drawer container (comes in transitively through `material`).
// ======================================================================================
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

// ======================================================================================
// IMPORTS 14-15: Material — the bar and the drawer
// ======================================================================================
// MaterialToolbar  → `Toolbar` with Material 3 styling applied automatically.
// NavigationView   → the drawer contents: menu, icons, check marks, its own insets.
// ======================================================================================
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

/*
 * ======================================================================================
 * THEORY BLOCK: ACTIVITY AND THE LIFECYCLE
 * ======================================================================================
 *
 * 1. WHAT AN ACTIVITY IS
 *    -   An Android component with its own window. The system creates it, shows it,
 *        hides it and destroys it by its own rules.
 *    -   You do not instantiate it with `new MainActivity()`. The system starts it,
 *        based on an `Intent` or on the launcher icon.
 *    -   It has a lifecycle with states: CREATED → STARTED → RESUMED → PAUSED →
 *        STOPPED → DESTROYED. Between states, the system calls callback methods.
 *
 * 2. `onCreate` — THE FIRST CALLBACK, ONCE PER INSTANCE
 *    -   This is where the interface is built: `setContentView`, `findViewById`, listeners.
 *    -   It is called at startup and at every recreation (rotation, theme change,
 *        language change). On recreation it receives `savedInstanceState` with the data
 *        saved previously — here it is `null` the first time and is always ignored.
 *    -   It is not the place for long operations: the block runs on the main thread,
 *        and the system waits for it to finish before drawing the first frame.
 *
 * 3. THE MANDATORY ORDER IN `onCreate`
 *    -   `super.onCreate(...)` FIRST. The parent initializes the infrastructure
 *        (including the `OnBackPressedDispatcher` used below).
 *    -   `setContentView(...)` BEFORE any `findViewById`. Otherwise you get
 *        `null`, not an exception — and the `NullPointerException` appears later, on a
 *        line with no apparent connection.
 *    -   `findViewById` and listeners AFTER `setContentView`.
 *
 * 4. WHY A SINGLE `onCreate` IS A DIDACTIC PROBLEM, NOT A TECHNICAL ONE
 *    -   It works perfectly. But seven responsibilities in a 100-line method
 *        are hard to read and even harder to test.
 *    -   In a real application: private methods `setupToolbar()`, `setupDrawer()`,
 *        `setupList()`. Here they are left inline so that you see the real order of the calls.
 *
 * 5. WHAT "CONTEXT" MEANS
 *    -   `this` from an activity is a `Context` (more precisely a `ContextThemeWrapper`):
 *        it gives access to resources, to the app theme and to the system services.
 *    -   Widgets require a THEMED context. `getApplicationContext()` does not have the
 *        activity theme — that is why `new Button(SecondActivity.this)` and not
 *        `new Button(getApplicationContext())`.
 *
 * REF: ch. 15 — DMA02: Application and lifecycle; the documentation of
 *      `android.app.Activity` and `androidx.activity.ComponentActivity`.
 * ======================================================================================
 */

public class MainActivity extends AppCompatActivity {

    // ==================================================================================
    // FIELD 1: drawerLayout
    // ==================================================================================
    // WHAT IT IS: the reference to the `DrawerLayout` from the XML.
    // WHERE IT IS USED: in three places — the drawer listener (`closeDrawer`),
    //   the Back callback (`isDrawerOpen` + `closeDrawer`) and the hamburger (`openDrawer`).
    // WHY IT IS A FIELD AND NOT A LOCAL: all three uses are in `onCreate`, so
    //   a local would have worked too. It is a field out of habit, so that it is at hand if
    //   you add new methods.
    // `private` — nothing outside needs it.
    // ==================================================================================
    private DrawerLayout drawerLayout;

    // ==================================================================================
    // FIELD 2: listView
    // ==================================================================================
    // WHAT IT IS: the reference to the list from the XML.
    // WHERE IT IS USED: in the long-press listener, to read the text of the pressed
    //   row (`getItemAtPosition(position)`).
    // WHY IT IS A FIELD: the long-press lambda runs AFTER `onCreate` has finished.
    //   A local variable would be captured in the closure (a lambda captures only
    //   effectively final variables), but a field is clearer and lives as long as
    //   the activity does.
    // ==================================================================================
    private ListView listView;

    /*
     * ----------------------------------------------------------------------------------
     * METHOD: onCreate(Bundle savedInstanceState)
     * ----------------------------------------------------------------------------------
     * THEORY:
     *   The creation callback of the activity. It runs once per instance, on the main
     *   thread, before the first frame is drawn. This is where the whole view tree is
     *   built and all the listeners are bound.
     *
     * LOGIC (the real order in the code):
     *   1. `super.onCreate`                — initializes the parent (including the Back dispatcher)
     *   2. `EdgeToEdge.enable`             — the window extends under the system bars
     *   3. `setContentView`                — inflates `activity_main.xml`
     *   4. insets on `R.id.main`           — padding so you don't sit under the clock
     *   5. `listView` + long press         — `PopupMenu` anchored to the row
     *   6. `drawerLayout`/`toolbar`/`nav`  — the three components found by id
     *   7. drawer listener                 — changes the title, closes the drawer
     *   8. Back callback                   — closes the drawer or delegates
     *   9. hamburger                       — opens the drawer
     *  10. toolbar menu                    — Search / Share / Settings
     *
     * REF:
     *   ch. 14 (sections 14.1–14.6), the documentation of `Activity#onCreate`.
     *
     * RISK:
     *   - `findViewById` before `setContentView` → `null`, then an NPE in a listener.
     *   - `super.onCreate` omitted → `SuperNotCalledException` at runtime.
     *   - listeners moved before `setContentView` → they receive `null` views.
     * ----------------------------------------------------------------------------------
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // STEP 1: super.onCreate — starts the parent's lifecycle.
        // Without it, `getOnBackPressedDispatcher()` below would receive an unconfigured
        // dispatcher. `@Override` is also a check: if the signature is wrong,
        // the compiler warns you.
        super.onCreate(savedInstanceState);

        // STEP 2: EdgeToEdge — the window extends under the status bar and under the
        // navigation bar. It calls `WindowCompat.setDecorFitsSystemWindows(window, false)`.
        // Without it, the app would have opaque system bars and you would not need the
        // insets block below. With it, you DO need it — otherwise the first row in the
        // list goes under the clock.
        EdgeToEdge.enable(this);

        // STEP 3: setContentView — `LayoutInflater` reads the XML, creates the
        // Java objects for every tag and attaches them to the window.
        // From this moment, `findViewById` can find something. Before, the tree does not exist.
        // `R.layout` is the layout resource (the whole file); `R.id` is an identifier
        // inside a view. Two different namespaces, generated in the same `R.java`.
        setContentView(R.layout.activity_main);

        // STEP 4: insets — when the system calculates how much the bars occupy, it applies
        // to the view with `id=main` a padding equal to them. The content moves into the
        // safe area, the background stays stretched across the whole screen.
        //
        // `ViewCompat.setOnApplyWindowInsetsListener` registers a compatible listener:
        // on old APIs it uses the old methods, on new APIs the new ones.
        // The listener runs immediately after `setContentView` and then on every
        // change (rotation, keyboard, bars appearing/disappearing). It does not run once
        // — that is why the code inside it must be idempotent.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // `Type.systemBars()` = status bar + navigation bar + caption bar.
            // It does not include the keyboard (`Type.ime()`) nor the cutout (`Type.displayCutout()`).
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // `setPadding` OVERWRITES the existing padding. If the `CoordinatorLayout`
            // had `android:padding="8dp"` in the XML, those 8dp would disappear at the first
            // insets pass. The correct way would be `v.setPadding(base + insets.left, ...)`, with the base
            // values saved in a field. Here the XML has no padding, so it does not show.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            // `return insets` — NOT `WindowInsetsCompat.CONSUMED`. The insets go further,
            // to the children. If you consumed them, `AppBarLayout` would no longer know about them
            // and `liftOnScroll` would receive wrong values. You consume only when you have fully
            // handled the area yourself and want to stop the propagation.
            return insets;
        });

        // STEP 5: the list. The method is generic (`<T extends View> T findViewById(int)`),
        // so the compiler infers the type from the assignment; no cast is needed.
        // The search is done on the activity, that is, across the whole window tree. If you had
        // two views with the same id in different layouts, you would get the first one found —
        // that is why ids must be unique within a layout.
        listView = findViewById(R.id.listView);

        // `setOnItemLongClickListener` registers a listener for a long press
        // on ANY row of the list. There is no "per-row" listener: `ListView` has a single
        // one, for all the rows, and it tells you in the parameters which row was pressed.
        //
        // Lambda with four parameters for `AdapterView.OnItemLongClickListener`:
        //   parent   : AdapterView<?> — the `ListView` itself (not used here)
        //   view     : View           — the pressed ROW; it becomes the anchor of the menu
        //   position : int            — the index of the row in the list, 0-based
        //   id       : long           — the id of the item; for `ArrayAdapter` it is
        //                               equal to `position`. The type is `long` because
        //                               `AdapterView` can also work with ids from
        //                               databases (`SQLite` uses `long`).
        //
        // There is NO `setOnItemClickListener` anywhere in the file. A simple tap on a row
        // does nothing. Only the long press reacts. It is a didactic choice:
        // the project demonstrates the context menu, not navigation.
        //
        // What happens on a simple tap, concretely: `ListView` receives `ACTION_DOWN`,
        // starts a `CheckForLongPress` with the system timeout (~500 ms), and on
        // `ACTION_UP` without a click listener nothing happens. The row does not even
        // stay highlighted — `ListView` in `CHOICE_MODE_NONE` mode keeps no selection.
        //
        // Why the listener is on the list and not on the adapter: the adapter produces views,
        // not events. `ListView` is the one that knows the position and the pressed row.
        //
        // ------------------------------------------------------------------------------
        // METHOD (lambda): onItemLongClick(parent, view, position, id)
        // THEORY: callback for a long press on a list row.
        // LOGIC:  1. builds a `PopupMenu` anchored to `view`;
        //         2. inflates `list_item_menu`;
        //         3. registers the item listener;
        //         4. `show()` and `return true`.
        // REF:    26.1.9–26.1.11
        // RISK:   without `return true`, the normal click fires as well.
        // ------------------------------------------------------------------------------
        // Long press shows the popup menu
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            // `new PopupMenu(this, view)` — `this` is the context (the activity, for the
            // theme), `view` is the anchor. `PopupWindow` calculates the position starting from
            // the view's rectangle and places it below if it fits, or above
            // if it does not. That is why the menu appears attached to the row, not in the corner of the screen.
            //
            // Why NOT `listView` as the anchor: `ListView` is a large rectangle, as big as the whole
            // screen. The menu would appear at its edge — that is, at the top, next to the bar — and would
            // cover the pressed row. Why NOT `toolbar`: it would look like an overflow menu
            // of the bar, not like a context menu.
            PopupMenu popup = new PopupMenu(this, view);   // anchored to the pressed row

            // `getMenuInflater()` comes from `Context` (the activity) and returns a
            // `MenuInflater` — a parser for `<menu>`, not for layouts.
            // `inflate(resource, menu)` — the second argument is the DESTINATION `Menu`,
            // obtained with `popup.getMenu()`. It is not like `setContentView`, where you pass only
            // the resource. Why `R.menu.list_item_menu` and not `top_app_bar`: they are two
            // different menus, for two different contexts. `list_item_menu` has
            // "Open / Rename / Delete" — actions on a row. `top_app_bar` has
            // "Search / Share / Settings" — actions on the screen.
            //
            // What happens if you forget `inflate`: the menu is empty. `popup.show()` displays
            // a rectangle with no items, and the listener is never called.
            popup.getMenuInflater().inflate(R.menu.list_item_menu, popup.getMenu());

            // `setOnMenuItemClickListener` is called BEFORE `popup.show()`;
            // if you register it afterwards, the menu is already displayed and the clicks in the first
            // second are lost.
            popup.setOnMenuItemClickListener(menuItem -> {
                // `getItemId()` — the id from the XML (`R.id.action_open` etc.). Not the position:
                // if you reorder the items in the XML, the ids stay the same, the positions do not.
                int itemId = menuItem.getItemId();

                // `position` is captured from the outer lambda. It is a method parameter,
                // hence effectively final — Java allows capturing it. If you tried to
                // modify it in the meantime (`position++`), the compiler would refuse.
                //
                // `getItemAtPosition(position)` returns a `CharSequence` — the text from
                // `@array/items`, because `ListView` built its own
                // `ArrayAdapter` from the `android:entries` attribute in the XML. You wrote
                // no adapter in the code.
                //
                // `.toString()` is redundant for a `String`, but harmless. It becomes
                // necessary the moment you put objects in the list: there `toString()`
                // is the display contract.
                String selected = listView.getItemAtPosition(position).toString();

                // `return true` = "I handled the click". The menu closes and
                // the event no longer circulates. `return false` = "I don't care" —
                // the system tries the default handler.
                //
                // What exactly happens with `return false` on a `PopupMenu`: `PopupMenu`
                // closes the menu anyway, on click. The value propagates to
                // `MenuBuilder.performItemAction`, which, if it is not consumed, continues
                // with the item's default action — for example an `Intent` set on
                // the item. Here the items have no `Intent`, so `false` changes nothing
                // visible. But the contract remains: `true` = handled.
                if (itemId == R.id.action_open) {
                    // `Toast.makeText(...).show()` — two calls. `makeText`
                    // BUILDS, `show()` DISPLAYS. Without `.show()`, nothing happens,
                    // with no error. It is the most frequent mistake at the beginning.
                    // `LENGTH_SHORT` ≈ 2 seconds; `LENGTH_LONG` ≈ 3.5 seconds. The values
                    // are hints, not guarantees: the manufacturer can adjust the duration.
                    Toast.makeText(this, "Open: " + selected, Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.action_rename) {
                    Toast.makeText(this, "Rename: " + selected, Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.action_delete) {
                    Toast.makeText(this, "Delete: " + selected, Toast.LENGTH_SHORT).show();
                    return true;
                }
                // The final `return false` is dead code — all three ids are
                // covered above. It stays correct: if you add a fourth item
                // in `list_item_menu.xml` and forget to handle it, the click falls nicely into
                // the default handler instead of doing something unpredictable.
                return false;
            });

            // `popup.show()` — up to here you have only configured the menu. Only now does it appear
            // on screen. The order is: build → inflate → register listener → display.
            popup.show();

            // `return true` from the long-press listener — tells the list "the long
            // press was handled". If you returned `false`, the system would consider the
            // long press unhandled and could also trigger a normal click after
            // it. The result: the menu appears and, at the same time, the click action runs.
            //
            // The mechanism behind it: `AdapterView` keeps an internal flag
            // (`mHasPerformedLongPress`). The listener sets it only if it returns
            // `true`. At `ACTION_UP`, if the flag is set, the click is ignored. If not,
            // `performClick()` runs normally.
            //
            // The order matters: `popup.show()` BEFORE `return true`. The reverse is not
            // possible — `return` ends the lambda.
            return true;   // true = long press handled, so the normal click doesn't also fire
        });


        // STEP 6: binding the three components. All three ids must exist
        // in `activity_main.xml`, otherwise you get `null` and a `NullPointerException`
        // on the first call. The order of the three lines does not matter — they are independent.
        // All that matters is that they come after `setContentView`.
        //
        // `toolbar` and `navigationView` are LOCAL, `drawerLayout` is a FIELD.
        // A real inconsistency in the code: all three are used only in `onCreate`.
        // It works with all three local; it works with all three as fields. Pick one
        // variant and stick to it.
        drawerLayout = findViewById(R.id.drawerLayout);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        NavigationView navigationView = findViewById(R.id.navigationView);


        // STEP 7: the drawer listener.
        //
        // What each branch does: it changes the TITLE of the top bar. The contents of the list
        // stay the same — the 20 "Item N" entries. The app is a demonstration of
        // navigation, not of data.
        //
        // `toolbar.setTitle("Inbox")` overwrites `app:title="Inbox"` from the XML.
        // The value in the XML is only the initial one; after the first click, the title comes from the code.
        //
        // `nav_settings` is treated differently — a `Toast`, not a title. It is the only item
        // in the "Other" submenu, so it makes no sense for it to change the screen title.
        //
        // Why `item.getItemId()` and not `item.getTitle()`: the title is visible text,
        // which can be translated or changed. The id is stable. The comparison with `R.id.*`
        // is the only one that survives a language change.
        //
        // What happens if no `if` matches: the listener continues,
        // calls `item.setChecked(true)` and closes the drawer. The unknown item
        // stays checked, but the title does not change. It is not an error — it is a default
        // behaviour.
        //
        // ------------------------------------------------------------------------------
        // METHOD (lambda): onMenuItemClick(item) — the drawer selection
        // THEORY: callback for an item in the `NavigationView`.
        // LOGIC:  1. reads the id; 2. updates the title; 3. sets the check mark;
        //         4. closes the drawer; 5. `return true`.
        // REF:    26.1.13
        // RISK:   without `closeDrawer`, the drawer stays over the screen.
        // ------------------------------------------------------------------------------
        // Drawer item clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inbox) {
                toolbar.setTitle("Inbox");
            } else if (id == R.id.nav_sent) {
                toolbar.setTitle("Sent");
            } else if (id == R.id.nav_trash) {
                toolbar.setTitle("Trash");
            } else if (id == R.id.nav_settings) {
                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
            }

            // `item.setChecked(true)` — the menu has `android:checkableBehavior="single"`
            // (see 26.6), so it behaves like a group of radio buttons: a single
            // checked item. The explicit call guarantees the state after the drawer
            // closes and after it is reopened.
            //
            // Why `setChecked` is needed explicitly if the menu is already "single":
            // `checkableBehavior` makes the group exclusive, but it does not mean that
            // `NavigationView` marks the item automatically on click. The code does not
            // rely on the default behaviour — it sets it itself. It is a defensive
            // choice, a good one.
            item.setChecked(true);

            // `closeDrawer(GravityCompat.START)` — closes the drawer from the `start` side.
            // It must match `android:layout_gravity="start"` from the XML. If
            // the XML says `start` and the code says `end`, the drawer stays open.
            //
            // Why `setNavigationItemSelectedListener` does NOT close the drawer itself:
            // `NavigationView` is a menu container. It has no reference to
            // `DrawerLayout` and does not know that one exists. They are neighbours in the tree, not
            // parent-child. The listener is only a callback: "item X was pressed".
            // What you do with the information — change the title, close the drawer, start a
            // screen — is your decision, in your code.
            //
            // The practical consequence: if you forget `closeDrawer`, the drawer stays over
            // the screen after the selection. The user sees the changed title but does not see
            // the content. It is the first thing missing from a hasty implementation.
            drawerLayout.closeDrawer(GravityCompat.START);

            // `return true` — tells `NavigationView` that the click was handled. If you
            // return `false`, the item is not marked visually as selected. The documented contract
            // of the old variant (`OnNavigationItemSelectedListener`) is explicit:
            // "true to display the item as the selected item". The new variant
            // (`MenuItem.OnMenuItemClickListener`) propagates the value into `MenuBuilder`,
            // and `NavigationView` uses it the same way.
            //
            // Why the code still calls `setChecked(true)`: because it relies on the
            // contract, not on the implementation. If `NavigationView` changed, the check mark
            // would remain correct.
            return true;
        });

        // STEP 8: the Back button. Why not `@Override public void onBackPressed()`:
        // the method was marked deprecated starting with API 33, and `OnBackPressedDispatcher`
        // is the official mechanism. On top of that, the dispatcher allows SEVERAL
        // callbacks in a chain, each with its own priority — a single override
        // cannot do that.
        //
        // `new OnBackPressedCallback(true)` — the argument is the initial state:
        // `true` = active. A disabled callback receives no events.
        //
        // `addCallback(this, ...)` — the first argument is the `LifecycleOwner` (the activity).
        // The dispatcher keeps the callback only while the activity is in `STARTED`.
        // At `onStop`, the callback is removed automatically; at `onStart`, it comes back. You do not
        // have to remove it manually. The variant without a `LifecycleOwner` leaves the callback in
        // the list until you remove it — if you forget, it holds a reference to the activity.
        //
        // The logic, step by step:
        //   1. Back was pressed.
        //   2. Is the drawer open? → close it. Done. The user stays on the screen.
        //   3. Is the drawer closed? → `setEnabled(false)`, then re-send the event.
        //
        // Why `setEnabled(false)` before re-sending: without it,
        // `onBackPressed()` would enter the SAME callback again, which would see the
        // drawer closed and re-send the event — infinite recursion,
        // `StackOverflowError`. By disabling yourself, you let the next handler
        // in the chain (the default one, which closes the activity) receive the event.
        //
        // The exact mechanism: `OnBackPressedDispatcher` keeps the callbacks in an
        // ordered collection. `onBackPressed()` walks it from the last added to
        // the first and stops at the first ACTIVE callback that handles the event.
        // If your callback re-enters without disabling itself, the walk finds it
        // again. `setEnabled(false)` removes it from the collection of active
        // callbacks, so the walk moves on.
        //
        // What the "default handler" is: `ComponentActivity` registers a fallback
        // that calls `super.onBackPressed()` — that is, `finish()` on the activity. If
        // it did not exist, Back would be ignored.
        //
        // Why not `finish()` directly: `finish()` would close the activity, but it would skip
        // any other callback in the chain (for example a `NavController` that should
        // handle Back first). Delegating keeps the chain intact.
        //
        // ------------------------------------------------------------------------------
        // METHOD: handleOnBackPressed()
        // THEORY: the callback that intercepts Back before the system.
        // LOGIC:  1. if the drawer is open, close it;
        //         2. otherwise disable yourself and delegate the event.
        // REF:    26.1.14
        // RISK:   without `setEnabled(false)`, infinite recursion.
        // ------------------------------------------------------------------------------
        // Back button closes the drawer first if it's open
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });


        // STEP 9: the hamburger. The icon on the left of the bar (the three lines) opens
        // the drawer. The icon already exists in the XML — `app:navigationIcon="@drawable/ic_menu"`.
        // Without a listener, it is purely decorative: it is visible, it can be pressed, nothing happens.
        // The listener is what turns it into a button.
        //
        // `setNavigationOnClickListener` — a single listener for the toolbar's navigation
        // icon. It receives no position or id: it is a single button.
        //
        // Why not `ActionBarDrawerToggle`: the classic variant binds the icon, the drawer
        // and accessibility automatically, but it requires the toolbar to be set as
        // the `ActionBar` (`setSupportActionBar`). Here the toolbar is used as an ordinary
        // view, so the binding is done manually. It is less code, but also less
        // automation — for example, the icon does not animate into an arrow when the drawer is
        // open, and the accessibility description is not updated automatically.
        //
        // `GravityCompat.START` — the same direction as `layout_gravity` in the XML.
        // The fourth occurrence in the file.
        // Hamburger (navigation) icon
        toolbar.setNavigationOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // STEP 10: the toolbar menu. The menu is already inflated —
        // `app:menu="@menu/top_app_bar"` from the XML. Here you only react to clicks.
        // If you had used `setSupportActionBar(toolbar)`, you would have overridden
        // `onCreateOptionsMenu` and `onOptionsItemSelected`; with the toolbar as a plain
        // view, this is the right hook.
        //
        // `action_settings` has `app:showAsAction="never"` — that is why it ends up in the
        // "⋮" menu (overflow), not on the bar. `action_search` and `action_share` have
        // `ifRoom` — they appear directly on the bar if they fit.
        //
        // The comment `// open settings` marks an empty spot. The branch displays a
        // `Toast`, but the intention was to open a settings screen. It is a disguised
        // TODO.
        //
        // The final `return false` is dead code — all three ids are covered
        // above. It stays correct: if you add a fourth item to the menu and forget
        // to handle it, the click falls nicely into the default handler instead of throwing
        // an exception.
        //
        // What happens with `return false` on an overflow item: `Toolbar`
        // propagates the value to `MenuBuilder`, which tries the item's default
        // action (an `Intent`, an `ActionProvider`). Here there is neither,
        // so the menu closes and that is all.
        //
        // ------------------------------------------------------------------------------
        // METHOD (lambda): onMenuItemClick(item) — the bar menu
        // THEORY: callback for Search / Share / Settings.
        // LOGIC:  1. reads the id; 2. shows a `Toast`; 3. `return true`.
        // REF:    26.1.16
        // RISK:   `return false` on an unknown id lets the event circulate.
        // ------------------------------------------------------------------------------
        // Search, share and overflow menu items
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.action_search) {
                Toast.makeText(this, "Search", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.action_share) {
                Toast.makeText(this, "Share", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.action_settings) {
                // open settings
                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }
}
