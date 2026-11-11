/*
 * ======================================================================================
 * DMA02App.java — CLASA `Application` A PROIECTULUI DMA02
 * ======================================================================================
 * CE ESTE:
 *   Clasa care devine obiectul unic al procesului Android. Framework-ul o instanțiază
 *   o singură dată, la pornirea procesului, ÎNAINTE de orice activitate.
 *
 * DE CE EXISTĂ:
 *   1. Abonează un observator la ciclul de viață al TUTUROR activităților
 *      (`registerActivityLifecycleCallbacks`).
 *   2. Activează global Material You (`DynamicColors.applyToActivitiesIfAvailable`).
 *   3. Scrie primul mesaj din Logcat, ca dovadă că inițializarea globală a rulat.
 *
 * CE SE STRICĂ FĂRĂ EL:
 *   - fără `android:name=".DMA02App"` în manifest, clasa NU este instanțiată:
 *     nicio eroare, niciun mesaj — doar tăcere în Logcat;
 *   - fără `implements ActivityLifecycleCallbacks`, nu primești niciun eveniment;
 *   - fără `DynamicColors`, aplicația rămâne pe paleta fixă din `themes.xml`.
 *
 * ORDINEA PE SCURT:
 *   proces nou → Application.attachBaseContext() → ContentProvider.onCreate()
 *              → DMA02App.onCreate() → MainActivity.onCreate() → onStart() → onResume()
 *
 * REF: android.app.Application (Android 36), documentația clasei:
 *      „The Application class, or your subclass of the Application class, is
 *       instantiated before any other class when the process for your
 *       application/package is created."
 * ======================================================================================
 */

package ro.ase.ie.dma02;                    // pachetul aplicației; identic cu `namespace` din build.gradle.kts

import android.app.Activity;                // tipul primit de fiecare callback: activitatea observată
import android.app.Application;             // clasa de bază — obiectul unic al procesului
import android.os.Bundle;                   // starea salvată, primită de două dintre callback-uri
import android.util.Log;                    // scrierea în Logcat

import androidx.annotation.NonNull;         // contract pentru lint: parametrul nu are voie să fie null
import androidx.annotation.Nullable;        // contract pentru lint: parametrul poate fi null

import com.google.android.material.color.DynamicColors;   // Material You: culori extrase din wallpaper

/*
 * ======================================================================================
 * THEORY BLOCK: `Application` — obiectul unic al procesului
 * ======================================================================================
 * 1. UN PROCES = UN `Application`
 *    Android pornește aplicația într-un proces nou (implicit). La începutul procesului,
 *    `ActivityThread` creează EXACT UN obiect `Application`. Toate activitățile,
 *    serviciile și receiver-ele din acel proces îl împart.
 *    Consecință: câmpurile din `Application` sunt „globale" pentru proces, nu pentru o
 *    activitate. Nu se resetează la rotire și nu se recreează la navigare.
 *
 * 2. `Application` NU ESTE O ACTIVITATE
 *    Nu are fereastră, nu are layout, nu apare pe ecran. Este doar un punct de
 *    inițializare și un loc de stat global. De aceea `onCreate()` lui nu are
 *    `setContentView` și nu primește `Bundle`: nu are stare de restaurat.
 *
 * 3. `onCreate()` — UN SINGUR APEL, ÎNAINTE DE ORICE ACTIVITATE
 *    Documentația framework-ului: „Called when the application is starting, before any
 *    activity, service, or receiver objects (excluding content providers) have been
 *    created." Deci: providerii de conținut se pot crea înaintea lui, restul nu.
 *
 * 4. `ActivityLifecycleCallbacks` — ȘABLONUL OBSERVATOR
 *    `Application` ține o listă de observatori. Fiecare metodă de ciclu de viață a
 *    unei activități apelează, în interiorul ei, `Application.dispatchActivityX(...)`,
 *    care iterează lista. Înregistrând `this`, `DMA02App` primește evenimente de la
 *    TOATE activitățile, fără să scrie nimic în ele.
 *
 * 5. `DynamicColors` — MATERIAL YOU GLOBAL
 *    `applyToActivitiesIfAvailable(this)` înregistrează propriul observator și aplică
 *    un theme overlay fiecărei activități, înainte de `onCreate`. Un singur apel,
 *    efect în toată aplicația.
 *
 * ASCII: CINE SE CREEAZĂ PRIMUL
 *
 *   Zygote ──fork()──► proces nou
 *                         │
 *                         ├─ 1. Application.attachBaseContext()   (nu apare în cod)
 *                         ├─ 2. ContentProvider.onCreate()        (dacă există)
 *                         ├─ 3. DMA02App.onCreate()               ◄── AICI
 *                         │       ├─ super.onCreate()
 *                         │       ├─ registerActivityLifecycleCallbacks(this)
 *                         │       ├─ DynamicColors.applyToActivitiesIfAvailable(this)
 *                         │       └─ Log.w(TAG, "onCreate")
 *                         └─ 4. MainActivity.onCreate()           ◄── abia acum
 * ======================================================================================
 */
public class DMA02App
        extends Application                                       // (1) DMA02App ESTE un Application
        implements Application.ActivityLifecycleCallbacks {       // (2) și ESTE observatorul de ciclu de viață

    // TAG-ul din Logcat. `getSimpleName()` întoarce "DMA02App", fără pachet.
    // `getName()` ar fi dat "ro.ase.ie.dma02.DMA02App" — prea lung pentru o coloană.
    // Nu e `static`: Application are o singură instanță per proces, deci un câmp de
    // instanță e suficient. În MainActivity e `static final`, pentru că acolo pot
    // exista mai multe instanțe (la rotire) și tag-ul trebuie să fie al clasei.
    private final String TAG = DMA02App.class.getSimpleName();

    // ==================================================================================
    // METODA 1/8: onCreate() — PUNCTUL DE INTRARE AL PROCESULUI
    // ==================================================================================
    // THEORY: `Application.onCreate()` e chemat o singură dată, la pornirea procesului,
    //         înaintea oricărei activități. Este locul corect pentru inițializări
    //         globale: abonări, teme, biblioteci, DI.
    // LOGIC:
    //   1. `super.onCreate()` — contractul clasei de bază.
    //   2. `registerActivityLifecycleCallbacks(this)` — abonarea observatorului.
    //   3. `DynamicColors.applyToActivitiesIfAvailable(this)` — tema globală.
    //   4. `Log.w(...)` — dovada că inițializarea s-a terminat.
    // REF: android.app.Application#onCreate (Android 36); javadoc-ul clasei.
    // RISK: ordinea 2 → 3 → 4 nu e întâmplătoare. Abonarea și tema trebuie făcute
    //       ÎNAINTE ca prima activitate să fie creată; dacă le muți în MainActivity,
    //       rulează prea târziu pentru prima activitate.
    @Override
    public void onCreate() {
        super.onCreate();                                  // pas 0: contractul clasei de bază (aici e gol, dar se păstrează)
        registerActivityLifecycleCallbacks(this);          // pas 1: „this" devine observatorul tuturor activităților
        DynamicColors.applyToActivitiesIfAvailable(this);  // pas 2: Material You, global, dacă e disponibil

        Log.w(TAG, "onCreate");                            // pas 3: primul rând din Logcat — înaintea oricărei activități
    }

    // ==================================================================================
    // CALLBACK 1/7: onActivityCreated(Activity, Bundle)
    // ==================================================================================
    // THEORY: se declanșează când o activitate apelează `super.onCreate(...)`.
    //         `bundle` este exact `savedInstanceState`: `null` la prima creare,
    //         non-null la recreare (rotire, revenire după kill).
    // CÂND: în interiorul lui `Activity.onCreate`, după `mFragments.dispatchCreate()`
    //       și înainte de `mCalled = true`.
    // LOGIC: scrie un singur rând în Logcat, cu numele clasei activității.
    // REF: Activity.java (android-36), linia 1883 — `dispatchActivityCreated(...)`.
    // RISK: în acest moment `setContentView` NU a rulat încă (dacă activitatea apelează
    //       `super` prima, cum face MainActivity). Deci `findViewById` ar întoarce null:
    //       nu atinge view-urile aici.
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        Log.w(TAG, "onActivityCreated" + activity.getClass().getSimpleName());   // ex.: "onActivityCreatedMainActivity"
    }                                                                            // lipsește spațiul dintre text și nume

    // ==================================================================================
    // CALLBACK 2/7: onActivityDestroyed(Activity) — poziția 2 în fișier, nu în viață
    // ==================================================================================
    // THEORY: se declanșează când activitatea este distrusă definitiv: Back, rotire,
    //         `finish()`. Este perechea lui `onDestroy`.
    // CÂND: în interiorul lui `Activity.onDestroy`, după curățenia framework-ului.
    // LOGIC: scrie un singur rând în Logcat.
    // REF: Activity.java (android-36), linia 2971 — `dispatchActivityDestroyed()`.
    // RISK: NU e garantat. Dacă sistemul omoară procesul din lipsă de memorie,
    //       `onDestroy` nu rulează și callback-ul nu vine. Nu pune aici salvarea de date.
    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        Log.w(TAG, "onActivityDestroyed" + activity.getClass().getSimpleName());
    }

    // ==================================================================================
    // CALLBACK 3/7: onActivityPaused(Activity)
    // ==================================================================================
    // THEORY: se declanșează când activitatea își pierde focusul: altă activitate vine
    //         peste ea, dialog, share sheet, Home. Activitatea poate fi încă vizibilă.
    // CÂND: la începutul lui `Activity.onPause`, ÎNAINTE de restul corpului metodei.
    // LOGIC: scrie un singur rând în Logcat.
    // REF: Activity.java (android-36), linia 2662 — `dispatchActivityPaused()`.
    // RISK: `onPause` blochează tranziția către activitatea următoare. Orice lucrezi
    //       aici întârzie ecranul următor: păstrează metoda scurtă.
    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        Log.w(TAG, "onActivityPaused" + activity.getClass().getSimpleName());
    }

    // ==================================================================================
    // CALLBACK 4/7: onActivityResumed(Activity)
    // ==================================================================================
    // THEORY: se declanșează când activitatea devine interactivă: e în prim-plan și
    //         primește input. Este perechea lui `onResume`.
    // CÂND: la începutul lui `Activity.onResume`, înainte de restul corpului.
    // LOGIC: scrie un singur rând în Logcat.
    // REF: Activity.java (android-36), linia 2228 — `dispatchActivityResumed()`.
    // RISK: aici poți porni animații și listeners; nu face treabă grea, pentru că
    //       întârzie primul frame interactiv.
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        Log.w(TAG, "onActivityResumed" + activity.getClass().getSimpleName());
    }

    // ==================================================================================
    // CALLBACK 5/7: onActivitySaveInstanceState(Activity, Bundle)
    // ==================================================================================
    // THEORY: se declanșează când sistemul cere activității să-și salveze starea, ca
    //         să o poată reconstrui după ce procesul e omorât. `bundle` este exact
    //         obiectul care va ajunge în `onCreate` la revenire.
    // CÂND: în interiorul lui `Activity.onSaveInstanceState`. Din Android 9 (API 28),
    //       după `onStop`; înainte, putea fi înainte de `onStop`.
    // LOGIC: niciunul — metoda e goală intenționat.
    // REF: Activity.java (android-36), linia 2565 — `dispatchActivitySaveInstanceState(...)`.
    // RISK: Bundle-ul trece prin Binder; datele mari dau `TransactionTooLargeException`.
    //       DMA02 nu are stare de salvat, deci metoda rămâne goală.
    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    // ==================================================================================
    // CALLBACK 6/7: onActivityStarted(Activity)
    // ==================================================================================
    // THEORY: se declanșează când activitatea devine vizibilă, dar nu neapărat
    //         interactivă. Este perechea lui `onStart`.
    // CÂND: în interiorul lui `Activity.onStart`, după `mFragments.doLoaderStart()`.
    // LOGIC: scrie un singur rând în Logcat.
    // REF: Activity.java (android-36), linia 2154 — `dispatchActivityStarted()`.
    // RISK: aici activitatea e vizibilă, dar nu primește încă input. Nu porni
    //       animații care depind de focus.
    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        Log.w(TAG, "onActivityStarted" + activity.getClass().getSimpleName());
    }

    // ==================================================================================
    // CALLBACK 7/7: onActivityStopped(Activity)
    // ==================================================================================
    // THEORY: se declanșează când activitatea nu mai este vizibilă: Home, altă
    //         activitate full-screen, ecran stins. Este perechea lui `onStop`.
    // CÂND: în interiorul lui `Activity.onStop`, după `mActivityTransitionState.onStop`.
    // LOGIC: scrie un singur rând în Logcat.
    // REF: Activity.java (android-36), linia 2893 — `dispatchActivityStopped()`.
    // RISK: activitatea poate fi distrusă imediat după, fără `onDestroy` garantat.
    //       Nu păstra aici resurse care trebuie eliberate neapărat.
    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        Log.w(TAG, "onActivityStopped" + activity.getClass().getSimpleName());
    }
}   // închide clasa DMA02App
