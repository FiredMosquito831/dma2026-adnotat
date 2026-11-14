/*
 * ======================================================================================
 * FILE: PlaceCiocanul.java — THE TEST SCREEN
 * ======================================================================================
 * WHAT THIS FILE IS:
 *   A minimal activity, opened by the second FAB on the main screen. It has a
 *   single button, which displays a `Toast`. It reads and writes no `Movie`.
 *
 * WHY IT EXISTS:
 *   It is a test screen added in version 2: it verifies that the second FAB
 *   starts an activity and that the manifest declares it. It has no role in the data flow.
 *
 * WHAT IT DEPENDS ON:
 *   - `R.layout.activity_place_ciocanul` and two IDs: `main`, `floatingActionButton2`;
 *   - `R.string.app_name` (used wrongly — see the RISK in `onCreate`);
 *   - the entry in `AndroidManifest.xml`, without which it cannot be started.
 *
 * WHO DEPENDS ON IT:
 *   Only `MainActivity.initSecondListener()`, which launches it through `launcher`.
 *
 * WHAT IT DOES NOT DO:
 *   It returns no result: it does not call `setResult`, so on return the code
 *   is `RESULT_CANCELED` and the callback in MainActivity does nothing.
 *
 * WHAT BREAKS WITHOUT IT:
 *   `btnVlad` would start a non-existent class -> `ActivityNotFoundException` at runtime.
 * ======================================================================================
 */

// The screens' package. MainActivity and ItemActivity are here too.
package com.example.group1098.activities;

// Bundle: only in the signature of `onCreate`, as everywhere.
import android.os.Bundle;

// View: the type of the parameter in `onClick`.
import android.view.View;

// Toast: the short message displayed on the button press.
import android.widget.Toast;

// AppCompatActivity: the same base as the other two activities.
import androidx.appcompat.app.AppCompatActivity;

// Insets: the values of the system bars.
import androidx.core.graphics.Insets;

// ViewCompat: the insets listener, compatible on any version.
import androidx.core.view.ViewCompat;

// WindowCompat: the edge-to-edge variant that receives a `Window`, not an activity.
//   MainActivity uses `EdgeToEdge.enable(this)`; this form is used here.
import androidx.core.view.WindowCompat;

// WindowInsetsCompat: the description of the insets.
import androidx.core.view.WindowInsetsCompat;

// R: the layout and the IDs.
import com.example.group1098.R;

// FloatingActionButton: the screen's only button.
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// (two blank lines in the source — kept as they are)

/*
 * ======================================================================================
 * THEORY BLOCK: A THIRD SCREEN, AND TWO APIS FOR THE SAME THING
 * ======================================================================================
 *
 * 1. **One activity = one screen.** `PlaceCiocanul` is the app's third activity.
 *    It is declared in the manifest (38.8), started with an explicit `Intent` and closed with
 *    the system's Back button. That is all it takes for it to exist.
 *
 * 2. **Two edge-to-edge variants.** `MainActivity` uses `EdgeToEdge.enable(this)`,
 *    which receives the activity; here `WindowCompat.enableEdgeToEdge(getWindow())` is used,
 *    which receives the window. The effect is the same: the content is drawn under the
 *    system bars, and the padding is applied manually, from the insets. The two recipes coexist
 *    in the same project: a hygiene detail, not an error.
 *
 * 3. **Toast vs Snackbar.** A `Toast` appears over any screen, needs no parent
 *    and disappears by itself. A `Snackbar` needs a `CoordinatorLayout` and sits at the
 *    bottom of the screen. For a test message, a `Toast` is the minimal choice.
 *
 * 4. **What is missing for it to be a real screen:** a `RecyclerView` with an ID and an adapter, a
 *    correctly sized `ProgressBar`, a message from `strings.xml` instead of the hardcoded
 *    text and a `setResult` if the screen should return data.
 * ======================================================================================
 */
// The activity. No `implements`: this screen receives clicks from no list.
public class PlaceCiocanul extends AppCompatActivity {

    // The screen's button. The name `btn2` was chosen by the author; the ID in the XML is
    //   `floatingActionButton2`. The two do not have to match — the binding is done
    //   through `findViewById`, not by name.
    private FloatingActionButton btn2;

    // ==================================================================================
    // THE `string` FIELD — DECLARED, ASSIGNED, NEVER READ
    // ==================================================================================
    // RISK: the field is not used anywhere. It is “dead code”: no `getText`, no
    //   `Toast`, no `Log` reads it. The compiler does not complain, because
    //   the assignment is a valid side effect.
    // The name `string` is a weak choice too: it describes the TYPE, not the content.
    // ==================================================================================
    private String string;

    // ==================================================================================
    // METHOD: onCreate — THE WHOLE SCREEN, IN A SINGLE METHOD
    // ==================================================================================
    // WHAT IT DOES: it inflates the layout, applies the insets, binds the button to a Toast and
    //   assigns the `string` field (uselessly).
    // HOW: 1) super; 2) edge-to-edge; 3) setContentView; 4) insets; 5) findViewById;
    //   6) listener; 7) the wrong assignment of `string`.
    // RISK: unlike MainActivity, there is no `initControls()` and
    //   `initListeners()` here. With a single button, the separation is not worth it — but the order
    //   remains important: `findViewById` must come after `setContentView`.
    // REF: MainActivity (38.1) — the same scheme, with separate methods.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 1. The base class chain.
        super.onCreate(savedInstanceState);

        // 2. Edge-to-edge, the `Window` variant. `getWindow()` returns the activity's
        //    window; the method configures it to draw under the bars.
        // NOTE: it is the equivalent of `EdgeToEdge.enable(this)` in MainActivity,
        //    written with another API. Both do the same thing; the project mixes them.
        WindowCompat.enableEdgeToEdge(getWindow());

        // 3. The view tree from the new screen's layout.
        setContentView(R.layout.activity_place_ciocanul);

        // 4. The insets, on the `R.id.main` container in this layout (a RelativeLayout).
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {

            // 4a. The system bars.
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // 4b. Padding equal to the bars.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            // 4c. The received object is returned.
            return insets;
        });

        // 5. The button, by the ID from `activity_place_ciocanul.xml`.
        btn2 = findViewById(R.id.floatingActionButton2);

        // 6. The button's listener: a Toast. The whole behaviour of the screen.
        btn2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // `PlaceCiocanul.this` = the Context (in the anonymous class, `this` would be
                //   the listener). `Toast.LENGTH_LONG` = ~3.5 seconds.
                // RISK: the text is hardcoded in the code, not in `strings.xml`. Lint gives
                //   the “HardcodedText” warning, and the text cannot be translated.
                //   Before any submission, it must be replaced with a real message.
                Toast.makeText(PlaceCiocanul.this, "Caca", Toast.LENGTH_LONG).show();
            }
        });

        // 7. THE LAST LINE OF THE METHOD — and the most instructive one.
        // ==================================================================================
        // RISK: `String.valueOf(R.string.app_name)` does NOT return the app's name.
        // ==================================================================================
        // `R.string.app_name` is an `int`: the resource's numeric ID, generated in `R.java`.
        // `String.valueOf` has several overloads; with an `int` it picks the one that
        //   turns the number into digits. The result is a text of the form “2131689472”,
        //   not “Group1098”.
        //
        //   WRONG:   String.valueOf(R.string.app_name)      -> "2131689472"
        //   CORRECT: getString(R.string.app_name)           -> "Group1098"
        //   CORRECT: String.valueOf(getString(R.string.app_name))
        //
        // WHY IT IS NOT SEEN: the value is displayed nowhere. The `string` field is
        //   dead, so the bug has no visible effect — for now. The first attempt to
        //   put it into a `TextView` would display a number.
        // TO REMEMBER: `R.<type>.<name>` is ALWAYS an `int`. The text is obtained only through
        //   `getString(...)` / `getText(...)` / `getDrawable(...)`, that is, through `Resources`.
        string = String.valueOf(R.string.app_name);
    }
}
