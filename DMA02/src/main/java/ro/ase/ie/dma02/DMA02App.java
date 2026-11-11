/*
 * ======================================================================================
 * DMA02App.java — THE `Application` CLASS OF THE DMA02 PROJECT
 * ======================================================================================
 * WHAT IT IS:
 *   The class that becomes the single object of the Android process. The framework
 *   instantiates it once, at process start, BEFORE any activity.
 *
 * WHY IT EXISTS:
 *   1. It subscribes an observer to the lifecycle of ALL activities
 *      (`registerActivityLifecycleCallbacks`).
 *   2. It enables Material You globally (`DynamicColors.applyToActivitiesIfAvailable`).
 *   3. It writes the first Logcat message, as proof that global initialization ran.
 *
 * WHAT BREAKS WITHOUT IT:
 *   - without `android:name=".DMA02App"` in the manifest, the class is NOT instantiated:
 *     no error, no message — just silence in Logcat;
 *   - without `implements ActivityLifecycleCallbacks`, you receive no event at all;
 *   - without `DynamicColors`, the app stays on the fixed palette from `themes.xml`.
 *
 * THE ORDER, IN SHORT:
 *   new process → Application.attachBaseContext() → ContentProvider.onCreate()
 *              → DMA02App.onCreate() → MainActivity.onCreate() → onStart() → onResume()
 *
 * REF: android.app.Application (Android 36), the class documentation:
 *      "The Application class, or your subclass of the Application class, is
 *       instantiated before any other class when the process for your
 *       application/package is created."
 * ======================================================================================
 */

package ro.ase.ie.dma02;                    // the application package; identical to `namespace` in build.gradle.kts

import android.app.Activity;                // the type received by every callback: the observed activity
import android.app.Application;             // the base class — the single object of the process
import android.os.Bundle;                   // the saved state, received by two of the callbacks
import android.util.Log;                    // writing to Logcat

import androidx.annotation.NonNull;         // lint contract: the parameter must not be null
import androidx.annotation.Nullable;        // lint contract: the parameter may be null

import com.google.android.material.color.DynamicColors;   // Material You: colors extracted from the wallpaper

/*
 * ======================================================================================
 * THEORY BLOCK: `Application` — the single object of the process
 * ======================================================================================
 * 1. ONE PROCESS = ONE `Application`
 *    Android starts the app in a new process (by default). At the start of the process,
 *    `ActivityThread` creates EXACTLY ONE `Application` object. All activities,
 *    services and receivers in that process share it.
 *    Consequence: the fields of `Application` are "global" to the process, not to an
 *    activity. They are not reset on rotation and not recreated on navigation.
 *
 * 2. `Application` IS NOT AN ACTIVITY
 *    It has no window, no layout, it does not appear on screen. It is only an
 *    initialization point and a place for global state. That is why its `onCreate()`
 *    has no `setContentView` and receives no `Bundle`: it has no state to restore.
 *
 * 3. `onCreate()` — A SINGLE CALL, BEFORE ANY ACTIVITY
 *    The framework documentation: "Called when the application is starting, before any
 *    activity, service, or receiver objects (excluding content providers) have been
 *    created." So: content providers may be created before it, nothing else.
 *
 * 4. `ActivityLifecycleCallbacks` — THE OBSERVER PATTERN
 *    `Application` keeps a list of observers. Every lifecycle method of an activity
 *    calls, inside itself, `Application.dispatchActivityX(...)`, which iterates the
 *    list. By registering `this`, `DMA02App` receives events from ALL activities,
 *    without writing anything in them.
 *
 * 5. `DynamicColors` — MATERIAL YOU, GLOBAL
 *    `applyToActivitiesIfAvailable(this)` registers its own observer and applies a
 *    theme overlay to every activity, before `onCreate`. One single call, an effect
 *    across the whole app.
 *
 * ASCII: WHO IS CREATED FIRST
 *
 *   Zygote ──fork()──► new process
 *                         │
 *                         ├─ 1. Application.attachBaseContext()   (not in the code)
 *                         ├─ 2. ContentProvider.onCreate()        (if any)
 *                         ├─ 3. DMA02App.onCreate()               ◄── HERE
 *                         │       ├─ super.onCreate()
 *                         │       ├─ registerActivityLifecycleCallbacks(this)
 *                         │       ├─ DynamicColors.applyToActivitiesIfAvailable(this)
 *                         │       └─ Log.w(TAG, "onCreate")
 *                         └─ 4. MainActivity.onCreate()           ◄── only now
 * ======================================================================================
 */
public class DMA02App
        extends Application                                       // (1) DMA02App IS an Application
        implements Application.ActivityLifecycleCallbacks {       // (2) and IS the lifecycle observer

    // The Logcat tag. `getSimpleName()` returns "DMA02App", without the package.
    // `getName()` would have given "ro.ase.ie.dma02.DMA02App" — too long for a column.
    // It is not `static`: Application has a single instance per process, so an
    // instance field is enough. In MainActivity it is `static final`, because there
    // several instances may exist (on rotation) and the tag must belong to the class.
    private final String TAG = DMA02App.class.getSimpleName();

    // ==================================================================================
    // METHOD 1/8: onCreate() — THE ENTRY POINT OF THE PROCESS
    // ==================================================================================
    // THEORY: `Application.onCreate()` is called once, at process start, before any
    //         activity. It is the right place for global initializations:
    //         subscriptions, themes, libraries, DI.
    // LOGIC:
    //   1. `super.onCreate()` — the base class contract.
    //   2. `registerActivityLifecycleCallbacks(this)` — subscribing the observer.
    //   3. `DynamicColors.applyToActivitiesIfAvailable(this)` — the global theme.
    //   4. `Log.w(...)` — the proof that initialization has finished.
    // REF: android.app.Application#onCreate (Android 36); the class javadoc.
    // RISK: the order 2 → 3 → 4 is not accidental. The subscription and the theme must
    //       be done BEFORE the first activity is created; if you move them into
    //       MainActivity, they run too late for the first activity.
    @Override
    public void onCreate() {
        super.onCreate();                                  // step 0: the base class contract (empty here, but kept)
        registerActivityLifecycleCallbacks(this);          // step 1: "this" becomes the observer of all activities
        DynamicColors.applyToActivitiesIfAvailable(this);  // step 2: Material You, global, if available

        Log.w(TAG, "onCreate");                            // step 3: the first line in Logcat — before any activity
    }

    // ==================================================================================
    // CALLBACK 1/7: onActivityCreated(Activity, Bundle)
    // ==================================================================================
    // THEORY: it fires when an activity calls `super.onCreate(...)`.
    //         `bundle` is exactly `savedInstanceState`: `null` on first creation,
    //         non-null on recreation (rotation, return after a kill).
    // WHEN: inside `Activity.onCreate`, after `mFragments.dispatchCreate()`
    //       and before `mCalled = true`.
    // LOGIC: writes a single line in Logcat, with the activity's class name.
    // REF: Activity.java (android-36), line 1883 — `dispatchActivityCreated(...)`.
    // RISK: at this moment `setContentView` has NOT run yet (if the activity calls
    //       `super` first, as MainActivity does). So `findViewById` would return null:
    //       do not touch the views here.
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        Log.w(TAG, "onActivityCreated" + activity.getClass().getSimpleName());   // e.g. "onActivityCreatedMainActivity"
    }                                                                            // the space between text and name is missing

    // ==================================================================================
    // CALLBACK 2/7: onActivityDestroyed(Activity) — position 2 in the file, not in life
    // ==================================================================================
    // THEORY: it fires when the activity is definitively destroyed: Back, rotation,
    //         `finish()`. It is the counterpart of `onDestroy`.
    // WHEN: inside `Activity.onDestroy`, after the framework's cleanup.
    // LOGIC: writes a single line in Logcat.
    // REF: Activity.java (android-36), line 2971 — `dispatchActivityDestroyed()`.
    // RISK: it is NOT guaranteed. If the system kills the process for lack of memory,
    //       `onDestroy` does not run and the callback never comes. Do not save data here.
    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        Log.w(TAG, "onActivityDestroyed" + activity.getClass().getSimpleName());
    }

    // ==================================================================================
    // CALLBACK 3/7: onActivityPaused(Activity)
    // ==================================================================================
    // THEORY: it fires when the activity loses focus: another activity comes over it,
    //         a dialog, the share sheet, Home. The activity may still be visible.
    // WHEN: at the start of `Activity.onPause`, BEFORE the rest of the method body.
    // LOGIC: writes a single line in Logcat.
    // REF: Activity.java (android-36), line 2662 — `dispatchActivityPaused()`.
    // RISK: `onPause` blocks the transition to the next activity. Whatever you do here
    //       delays the next screen: keep the method short.
    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        Log.w(TAG, "onActivityPaused" + activity.getClass().getSimpleName());
    }

    // ==================================================================================
    // CALLBACK 4/7: onActivityResumed(Activity)
    // ==================================================================================
    // THEORY: it fires when the activity becomes interactive: it is in the foreground
    //         and receives input. It is the counterpart of `onResume`.
    // WHEN: at the start of `Activity.onResume`, before the rest of the body.
    // LOGIC: writes a single line in Logcat.
    // REF: Activity.java (android-36), line 2228 — `dispatchActivityResumed()`.
    // RISK: here you may start animations and listeners; do not do heavy work, because
    //       it delays the first interactive frame.
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        Log.w(TAG, "onActivityResumed" + activity.getClass().getSimpleName());
    }

    // ==================================================================================
    // CALLBACK 5/7: onActivitySaveInstanceState(Activity, Bundle)
    // ==================================================================================
    // THEORY: it fires when the system asks the activity to save its state, so that it
    //         can rebuild it after the process is killed. `bundle` is exactly the
    //         object that will reach `onCreate` on return.
    // WHEN: inside `Activity.onSaveInstanceState`. From Android 9 (API 28), after
    //       `onStop`; before that, it could come before `onStop`.
    // LOGIC: none — the method is empty on purpose.
    // REF: Activity.java (android-36), line 2565 — `dispatchActivitySaveInstanceState(...)`.
    // RISK: the Bundle travels through Binder; large data gives `TransactionTooLargeException`.
    //       DMA02 has no state to save, so the method stays empty.
    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    // ==================================================================================
    // CALLBACK 6/7: onActivityStarted(Activity)
    // ==================================================================================
    // THEORY: it fires when the activity becomes visible, but not necessarily
    //         interactive. It is the counterpart of `onStart`.
    // WHEN: inside `Activity.onStart`, after `mFragments.doLoaderStart()`.
    // LOGIC: writes a single line in Logcat.
    // REF: Activity.java (android-36), line 2154 — `dispatchActivityStarted()`.
    // RISK: here the activity is visible, but does not receive input yet. Do not start
    //       animations that depend on focus.
    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        Log.w(TAG, "onActivityStarted" + activity.getClass().getSimpleName());
    }

    // ==================================================================================
    // CALLBACK 7/7: onActivityStopped(Activity)
    // ==================================================================================
    // THEORY: it fires when the activity is no longer visible: Home, another
    //         full-screen activity, screen off. It is the counterpart of `onStop`.
    // WHEN: inside `Activity.onStop`, after `mActivityTransitionState.onStop`.
    // LOGIC: writes a single line in Logcat.
    // REF: Activity.java (android-36), line 2893 — `dispatchActivityStopped()`.
    // RISK: the activity may be destroyed right after, without a guaranteed `onDestroy`.
    //       Do not keep here resources that must be released no matter what.
    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        Log.w(TAG, "onActivityStopped" + activity.getClass().getSimpleName());
    }
}   // closes the DMA02App class
