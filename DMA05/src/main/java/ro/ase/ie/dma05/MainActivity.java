/*
 * ======================================================================================
 * FIȘIER: MainActivity.java
 * ======================================================================================
 * LOCAȚIE:  DMA05/src/main/java/ro/ase/ie/dma05/MainActivity.java
 * PACHET:   ro.ase.ie.dma05
 * ROL:      singura activitate a aplicației. Nu are model, nu are date, nu are rețea.
 *           Construiește arborele de view-uri din activity_main.xml și înregistrează
 *           șapte listeneri, fiecare cu un Toast ca efect.
 *
 * STRUCTURA FIȘIERULUI (4 părți):
 *   1. importuri                  — 15 linii, dintre care unul generat
 *   2. câmpul `binding`           — 1 linie
 *   3. onCreate                   — 64 de linii, tot comportamentul
 *   4. închiderea clasei          — 2 linii
 *
 * CE TREBUIE SĂ ȘTII ÎNAINTE:
 *   - `ActivityMainBinding` NU există în sursă. O generează Gradle la build.
 *   - `inflate` construiește arborele; `setContentView` îl pune pe ecran. Doi pași.
 *   - Fiecare `binding.<nume>` este o citire de câmp, nu o căutare în arbore.
 *   - Șapte listeneri, cinci interfețe, două stiluri (lambda / clasă anonimă).
 * ======================================================================================
 */

/*
 * ======================================================================================
 * THEORY BLOCK 1: VIEW BINDING
 * ======================================================================================
 *
 * 1. CE ESTE
 *    O clasă Java generată de Android Gradle Plugin, câte una pentru fiecare fișier
 *    de layout din res/layout/. Nu o scrii tu, nu o modifici, nu o versionezi.
 *
 * 2. CÂND SE GENEREAZĂ
 *    La BUILD, nu la runtime. AGP rulează un task de generare înainte de compilarea
 *    Java, scrie fișierele în build/generated/... și le pune pe classpath.
 *    Consecința 1: dacă adaugi un android:id și nu rulezi build-ul, câmpul nu există.
 *    Consecința 2: clasa nu apare în src/, deci nu o vei găsi căutând în proiect.
 *
 * 3. DIN CE SE GENEREAZĂ
 *    Din res/layout/activity_main.xml. Fiecare android:id="@+id/..." devine un câmp
 *    public final cu tipul tag-ului:
 *      <Button android:id="@+id/button">    ->  public final Button button;
 *      <SeekBar android:id="@+id/seekBar">  ->  public final SeekBar seekBar;
 *
 * 4. REGULA DE NUME
 *    activity_main.xml  ->  ActivityMainBinding
 *    activity_item.xml  ->  ActivityItemBinding
 *    item_movie.xml     ->  ItemMovieBinding
 *    underscore + literă mică -> CamelCase; sufixul Binding se adaugă mereu.
 *
 * 5. CONDIȚIA DE ACTIVARE
 *    build.gradle.kts:
 *      android { buildFeatures { viewBinding = true } }
 *    Fără ea, clasa nu se generează și importul nu compilează.
 *
 * 6. CE CONȚINE
 *    - un câmp public final per android:id
 *    - un câmp privat rootView + metoda getRoot()
 *    - două metode statice inflate(...)
 *    - o metodă statică bind(View)
 *
 * 7. CE NU FACE
 *    - nu e DataBinding: fără expresii @{...} în XML
 *    - nu înlocuiește findViewById pentru ID-uri primite la runtime
 *    - nu generează câmpuri pentru view-uri fără android:id
 *
 * PIPELINE-UL DE GENERARE:
 *
 *   activity_main.xml
 *        │
 *        ├── aapt2 ──────────────▶ R.java (R.id.button, R.layout.activity_main)
 *        │
 *        └── AGP ViewBinding ────▶ ActivityMainBinding.java
 *                                      │
 *                                      ▼
 *                                  javac (compilează ambele)
 *                                      │
 *                                      ▼
 *                                  APK (clasa e în dex, ca orice clasă)
 */

/*
 * ======================================================================================
 * THEORY BLOCK 2: CICLUL DE VIAȚĂ AL UNEI ACTIVITĂȚI
 * ======================================================================================
 *
 * 1. STĂRILE, ÎN ORDINE
 *    onCreate()   → instanța e construită; aici se umflă layout-ul și se leagă listenerii
 *    onStart()    → activitatea devine vizibilă, dar nu neapărat interactivă
 *    onResume()   → activitatea e în prim-plan și primește input
 *    onPause()    → pierde focusul; salvezi ce e ieftin, oprești animații
 *    onStop()     → nu mai e vizibilă; eliberezi resurse grele
 *    onDestroy()  → instanța moare; aici s-ar anula listeneri globali
 *    onRestart()  → după onStop, înainte de onStart, când revii în activitate
 *
 * 2. CE GARANTEAZĂ onCreate
 *    - Window-ul există deja (creat de ActivityThread înainte de apel).
 *    - `setContentView` poate fi chemat după `super.onCreate`, fără să arunce.
 *    - `findViewById` întoarce null până după `setContentView`.
 *    - `savedInstanceState` e `null` la prima pornire și un Bundle la recreare.
 *
 * 3. DE CE `super.onCreate` PRIMUL
 *    AppCompatActivity delegă ciclul de viață către AppCompatDelegate. Dacă apelezi
 *    `setContentView` înainte de `super.onCreate`, delegate-ul nu are încă `Context`-ul
 *    și tema, iar inflate-ul aruncă. Regula e mecanică: super primul, restul după.
 *
 * 4. RECREAREA LA ROTIRE
 *    ROTIRE → onPause → onStop → onSaveInstanceState → onDestroy
 *           → onCreate(savedInstanceState ≠ null) → onStart → onResume
 *    Instanța e NOUĂ. Câmpul `binding` e reasignat. Listener-ii se reînregistrează.
 *    Starea widget-urilor cu `android:id` se restaurează automat de către framework.
 *
 * 5. CE SE ÎNTÂMPLĂ CU `binding` LA RECREARE
 *    Vechiul binding rămâne atașat de arborele vechi, care e distrus. Ambele devin
 *    eligibile pentru garbage collection. Nu există scurgere pentru că nu păstrezi
 *    o referință statică și nu asculti nimic global.
 *
 * DIAGRAMA STĂRILOR:
 *
 *   PORNIRE
 *     │
 *     ▼
 *   onCreate ──▶ onStart ──▶ onResume ──▶ [RUNNING]
 *                                            │
 *                                            ▼
 *                                         onPause
 *                                            │
 *                                            ▼
 *                                         onStop ──▶ onDestroy   (dacă nu revine)
 *                                            │
 *                                            ▼
 *                                        onRestart ──▶ onStart ──▶ onResume
 */

/*
 * ======================================================================================
 * THEORY BLOCK 3: WINDOW INSETS ȘI EDGE-TO-EDGE
 * ======================================================================================
 *
 * 1. IERARHIA FERESTREI
 *    Activity
 *      └── Window (PhoneWindow)
 *            └── DecorView (FrameLayout)
 *                  ├── (opțional) action bar
 *                  └── android.R.id.content (FrameLayout)
 *                        └── ce ai pus tu cu setContentView
 *
 *    Barele de sistem (status, navigare) NU sunt copii ale DecorView: sunt ferestre
 *    separate, desenate de sistem deasupra lui. De aceea conținutul poate ajunge
 *    „sub" ele când fereastra e edge-to-edge.
 *
 *    `setContentView(view)` nu pune view-ul direct în fereastră: îl adaugă în
 *    `android.R.id.content`, un FrameLayout care umple tot spațiul rămas.
 *
 * 2. CE SUNT INSETS
 *    Patru valori (left, top, right, bottom), în pixeli, care spun cât din marginea
 *    ecranului e acoperită de ceva ce nu trebuie să acoperi cu conținut:
 *      - statusBars()      → bara de sus (ceas, baterie, notificări)
 *      - navigationBars()  → bara de jos (butoane sau gest bar)
 *      - captionBar()      → bara de titlu în multi-window
 *      - ime()             → tastatura
 *      - displayCutout()   → notch / gaură în ecran
 *      - systemGestures()  → zonele gesturilor de sistem
 *    `Type.systemBars()` = statusBars | navigationBars | captionBar.
 *
 * 3. EDGE-TO-EDGE
 *    Implicit, `decorFitsSystemWindows = true`: sistemul micșorează fereastra ca să
 *    încapă între bare. Conținutul nu ajunge niciodată sub bară.
 *    `EdgeToEdge.enable(this)` setează `decorFitsSystemWindows = false`: fereastra
 *    se întinde pe tot ecranul, iar tu primești insets-urile și decizi ce faci cu ele.
 *    Avantaj: conținutul poate desena sub bară (imagini, hărți, video).
 *    Cost: trebuie să aplici tu padding-ul, altfel primul widget stă sub ceas.
 *
 * 4. CUM CIRCULĂ INSETS-URILE
 *    Sistem → DecorView → content → rădăcină → copii
 *    Fiecare view poate:
 *      - să ignore (returnează insets-urile primite)
 *      - să consume (returnează CONSUMED) → dispatch-ul se oprește
 *      - să modifice (returnează insets-uri noi) → copiii primesc valorile modificate
 *    Listener-ul din DMA05 returnează `insets`, deci nu consumă nimic.
 *
 * 5. DE CE PADDING PE RĂDĂCINĂ
 *    Padding-ul pe rădăcină mută TOȚI copiii în zona sigură, cu o singură operație.
 *    Padding-ul pe un copil mută doar acel copil. Iar dacă rădăcina nu are padding,
 *    primul copil stă sub bara de status, indiferent ce padding are al doilea.
 *
 * 6. CE NU ACOPERĂ `Type.systemBars()`
 *    Tastatura. Pentru IME ai nevoie de `Type.ime()` și, de obicei, de un al doilea
 *    listener sau de o combinație în același listener. DMA05 nu o face, pentru că
 *    toate câmpurile sunt în jumătatea de sus a ecranului.
 *
 * DIAGRAMA DISPATCH-ULUI:
 *
 *   WindowInsets { systemBars: (0, 24, 0, 48), ime: (0, 0, 0, 0) }
 *        │
 *        ▼
 *   DecorView ──▶ content ──▶ LinearLayout (root)
 *                                  │  listener: setPadding(0, 24, 0, 48)
 *                                  │  return insets  ← nu consumă
 *                                  ▼
 *                            Button, Switch, … primesc aceleași insets,
 *                            dar nu au listener, deci le ignoră
 */

// ======================================================================================
// PACHETUL ȘI IMPORTURILE
// ======================================================================================
// Primul rând al oricărui fișier Java: pachetul. Aici nu e doar organizare — e și
// sursa prefixului pentru clasa generată: `ro.ase.ie.dma05` + `.databinding`.
// ======================================================================================

package ro.ase.ie.dma05;                              // pachetul modulului; vine din `namespace` în build.gradle.kts

// --------------------------------------------------------------------------------------
// GRUPUL 1 — API-ul Android clasic (android.*)
// Sunt clase din framework-ul care există pe dispozitiv, nu din biblioteci împachetate.
// --------------------------------------------------------------------------------------

import android.os.Bundle;                             // parametrul lui onCreate: starea salvată la rotire
import android.view.View;                             // tipul parametrului din onClick(View v) și al lui v din lambda de insets
import android.widget.RadioButton;                    // tipul returnat de group.findViewById(checkedId)
import android.widget.RadioGroup;                     // tipul parametrului `group` din OnCheckedChangeListener
import android.widget.SeekBar;                        // interfața cu TREI metode — singura care cere clasă anonimă
import android.widget.Toast;                          // feedback-ul tuturor celor șapte listeneri

// --------------------------------------------------------------------------------------
// GRUPUL 2 — AndroidX (androidx.*)
// Biblioteci împachetate în APK, cu versiune fixată în build.gradle.kts.
// --------------------------------------------------------------------------------------

import androidx.activity.EdgeToEdge;                  // desenare sub barele de sistem (activity-ktx 1.8+)
import androidx.annotation.NonNull;                   // adnotare de contract, nu schimbă runtime-ul
import androidx.appcompat.app.AppCompatActivity;      // clasa de bază: Activity + AppCompat + delegate
import androidx.core.graphics.Insets;                 // cele patru valori de padding, în pixeli
import androidx.core.view.ViewCompat;                 // înregistrarea listener-ului de insets, compatibil pe orice API
import androidx.core.view.WindowInsetsCompat;         // Type.systemBars() și obiectul `insets`

// --------------------------------------------------------------------------------------
// GRUPUL 3 — codul GENERAT (ro.ase.ie.dma05.databinding)
// Acesta e singurul import care nu are un fișier corespondent în src/.
// --------------------------------------------------------------------------------------

import ro.ase.ie.dma05.databinding.ActivityMainBinding;   // ← clasa GENERATĂ la build din activity_main.xml

// --------------------------------------------------------------------------------------
// DE CE LIPSESC Button, Switch, CheckBox, ToggleButton, RatingBar, EditText, ImageView
// --------------------------------------------------------------------------------------
// În fișier nu apare niciodată numele acestor tipuri. Le accesezi doar ca `binding.button`,
// `binding.switch1` etc. — compilatorul știe tipul din câmpul generat, tu nu îl mai scrii.
// ViewBinding elimină și importurile, nu doar findViewById.
//
// `View` e importat deși nu instanțiezi unul: e tipul parametrului din onClick(View v)
// și al parametrului `v` din lambda de insets. `RadioButton` e importat pentru că îl
// declari ca tip al variabilei locale `selected`. `SeekBar` e importat pentru că numele
// apare explicit în `new SeekBar.OnSeekBarChangeListener()`.

// ======================================================================================
// CLASA ȘI CÂMPUL
// ======================================================================================
// O singură clasă, un singur câmp. Câmpul e cheia întregului fișier: el ține arborele
// viu după ce onCreate se termină.
// ======================================================================================

public class MainActivity extends AppCompatActivity {   // AppCompatActivity = Activity + AppCompat + delegate

    ActivityMainBinding binding;                         // câmp, nu variabilă locală; null până la inflate

    // ----------------------------------------------------------------------------------
    // DE CE CÂMP ȘI NU VARIABILĂ LOCALĂ
    // ----------------------------------------------------------------------------------
    // În DMA05 ar funcționa și local, pentru că totul se întâmplă în onCreate. Dar tiparul
    // e câmp pentru că în orice aplicație reală binding-ul e atins și din onSaveInstanceState,
    // și din metode ajutătoare, și din listeneri. `ActivityMainBinding binding;` fără
    // `private` e package-private — funcționează, dar `private` ar fi stilul corect.
    //
    // Înainte de `inflate`, câmpul e `null`. Nu e „gol", e referință nulă. Orice
    // `binding.button` înainte de linia cu `inflate` dă NullPointerException.
    //
    // De ce nu e `static`: binding-ul aparține instanței de activitate. La rotire,
    // onCreate rulează din nou pe o instanță nouă și reasignează câmpul. Un câmp static
    // ar ține arborele vechi în viață și ar scurge memoria activității distruse.
    //
    // De ce nu e `final`: pentru că `inflate` se apelează în onCreate, nu în constructor.
    // ----------------------------------------------------------------------------------

    // ----------------------------------------------------------------------------------
    // METODA: onCreate
    // ----------------------------------------------------------------------------------
    // CÂND SE APELEAZĂ: o singură dată per instanță de activitate, după ce Window-ul
    //   a fost creat și înainte ca arborele să fie desenat.
    // CE PRIMEȘTE: `savedInstanceState` — un Bundle cu starea salvată la rotire, sau
    //   `null` la prima pornire. În DMA05 nu e citit niciodată: starea widget-urilor
    //   se restaurează automat, pentru că toate au `android:id`.
    // CE TREBUIE SĂ FACĂ: să apeleze `super.onCreate` PRIMUL. Altfel, delegate-ul
    //   AppCompat nu e inițializat și `setContentView` aruncă.
    // ----------------------------------------------------------------------------------
    @Override                                             // suprascriem metoda din AppCompatActivity
    protected void onCreate(Bundle savedInstanceState) {  // `protected`, ca în clasa de bază
        super.onCreate(savedInstanceState);               // 1. ciclul de viață al părintelui — obligatoriu primul

        // ------------------------------------------------------------------------------
        // PASUL 2 — CONSTRUIREA ARBORELUI
        // ------------------------------------------------------------------------------
        // `ActivityMainBinding.inflate(...)` face TREI lucruri, într-o singură expresie:
        //   a. cere LayoutInflater-ului să parseze res/layout/activity_main.xml
        //   b. creează obiectele Java pentru fiecare tag (LinearLayout, Button, …)
        //   c. parcurge arborele și umple câmpurile clasei generate, după `android:id`
        // Rezultatul e un obiect ActivityMainBinding pe care îl păstrăm în câmp.
        //
        // getLayoutInflater() întoarce LayoutInflater-ul activității, deja legat de
        // Context-ul și de tema ei. De aceea nu scrii LayoutInflater.from(this) — e
        // același obiect, doar obținut pe altă cale.
        //
        // Varianta cu un argument apelează intern `inflate(inflater, null, false)`:
        // arborele se construiește complet, dar rămâne NEATAȘAT. Nu are încă un părinte
        // și nu e desenat. Asta permite `setContentView(binding.getRoot())` să îl
        // atașeze exact o dată, la locul potrivit. Dacă `inflate` l-ar atașa singur,
        // `setContentView` ar arunca IllegalStateException: The specified child already
        // has a parent.
        // ------------------------------------------------------------------------------
        binding = ActivityMainBinding.inflate(getLayoutInflater());   // 2. XML → obiecte Java + câmpuri umplute

        /*
         * DIAGRAMA 1 — cum se construiește arborele și cum se umplu câmpurile:
         *
         * activity_main.xml (text)                          obiecte Java (heap)
         * ┌────────────────────────────────┐
         * │ <LinearLayout id="@+id/main">  │
         * │   ├─ <Button id="@+id/button"> │   LayoutInflater
         * │   ├─ <ImageView id="…">        │   ───────────────▶   creează câte un obiect
         * │   ├─ <Switch id="@+id/switch1">│                      per tag, cu atributele
         * │   ├─ <EditText id="…"> × 5     │                      citite din XML
         * │   ├─ <CheckBox id="…">         │
         * │   ├─ <RadioGroup id="…">       │
         * │   │    ├─ <RadioButton id="…"> │
         * │   │    └─ <RadioButton id="…"> │
         * │   ├─ <ToggleButton id="…">     │
         * │   ├─ <ProgressBar id="…">      │
         * │   ├─ <SeekBar id="…">          │
         * │   └─ <RatingBar id="…">        │
         * └────────────────────────────────┘
         *                                              ┌──────────────────────────────────┐
         *                                              │ LinearLayout        (id = main)  │
         *                                              │   ├─ Button         (id = button)│
         *                                              │   ├─ ImageView                   │
         *                                              │   ├─ Switch         (id = switch1)│
         *                                              │   ├─ EditText × 5                │
         *                                              │   ├─ CheckBox                    │
         *                                              │   ├─ RadioGroup                  │
         *                                              │   │    ├─ RadioButton            │
         *                                              │   │    └─ RadioButton            │
         *                                              │   ├─ ToggleButton                │
         *                                              │   ├─ ProgressBar                 │
         *                                              │   ├─ SeekBar                     │
         *                                              │   └─ RatingBar                   │
         *                                              └────────────────┬─────────────────┘
         *                                                               │
         *                                             bind(root) parcurge arborele
         *                                             și, pentru fiecare android:id,
         *                                             scrie referința în câmpul corespunzător
         *                                                               │
         *                                              ┌────────────────▼─────────────────┐
         *                                              │ ActivityMainBinding              │
         *                                              │   rootView : LinearLayout        │
         *                                              │   main     : LinearLayout ───────┼──▶ același obiect
         *                                              │   button   : Button ─────────────┼──▶ același obiect
         *                                              │   switch1  : Switch ─────────────┼──▶ același obiect
         *                                              │   … 17 câmpuri                   │
         *                                              └──────────────────────────────────┘
         *
         * DIAGRAMA 2 — cum se rezolvă `binding.button` până la view-ul cu id="button":
         *
         * XML:   android:id="@+id/button"
         *               │
         *               ├──────────────────────────────┬─────────────────────────────────────┐
         *               │ aapt2 (BUILD)                │ AGP ViewBinding (BUILD)             │
         *               ▼                              ▼                                     │
         *       R.id.button = 0x7f0800a3       public final Button button;                 │
         *       (int ilustrativ, stabil în APK) (un câmp în clasa generată)                │
         *               │                              │                                     │
         *               └──────────────┬───────────────┘                                     │
         *                              ▼                                                     │
         *               bind(root) la inflate:                                               │
         *                 button = ViewBindings.findChildViewById(root, R.id.button)         │
         *                 if (button == null) throw NullPointerException("Missing …")        │
         *                              │                                                     │
         *                              ▼                                                     │
         *               binding.button ───────────────────────────────────────────────────────┘
         *                 = getfield, O(1), fără nicio căutare în arbore
         */

        // ------------------------------------------------------------------------------
        // PASUL 3 — EDGE-TO-EDGE ȘI ATAȘAREA
        // ------------------------------------------------------------------------------
        // Ordinea contează: întâi edge-to-edge, apoi atașarea, apoi listener-ul de insets.
        //
        // EdgeToEdge.enable(this) setează decorFitsSystemWindows = false, face barele
        // de sistem transparente și aplică scrim-uri (SystemBarStyle.auto) pentru contrast.
        // Rezultatul: conținutul se desenează sub bară, iar tu ești responsabil să îl
        // împingi în zona sigură — exact ce face listener-ul de mai jos.
        //
        // setContentView(binding.getRoot()) înlocuiește setContentView(R.layout.activity_main).
        // getRoot() întoarce LinearLayout-ul rădăcină — exact view-ul care are
        // android:id="@+id/main" în XML.
        //
        // CAPCANA NR. 1: dacă lași și setContentView(R.layout.activity_main) după linia
        // cu binding-ul, Android construiește UN AL DOILEA ARBORE, complet separat. Pe
        // ecran ajunge al doilea; `binding` arată spre primul. Cei șapte listeneri se
        // leagă pe view-uri pe care nu le vede nimeni, iar aplicația pare „moartă" —
        // niciun Toast nu apare. Se întâmplă des la migrarea de la findViewById.
        // ------------------------------------------------------------------------------
        EdgeToEdge.enable(this);                      // 3. fereastra se întinde SUB barele de sistem
        setContentView(binding.getRoot());            // 4. arborele construit ajunge pe ecran

        /*
         * DIAGRAMA 3 — fluxul în doi pași, inflate vs setContentView:
         *
         * PASUL 1 — CONSTRUIREA                         PASUL 2 — ATAȘAREA
         * binding = ActivityMainBinding                 setContentView(binding.getRoot())
         *           .inflate(getLayoutInflater())
         *         │                                             │
         *         ▼                                             ▼
         *   activity_main.xml                            DecorView (fereastra)
         *         │                                             │
         *         ▼                                             ├─ android.R.id.content (FrameLayout)
         *   arbore de View-uri                                  │     │
         *   (neatașat, invizibil,                               │     ▼
         *    fără părinte)                                      │  LinearLayout (root)
         *         │                                             │     ├─ Button
         *         ▼                                             │     ├─ ImageView
         *   ActivityMainBinding                                 │     └─ … copiii
         *   (câmpuri umplute)                                   │
         *                                                       ▼
         *                                              arborele e pe ecran,
         *                                              pregătit pentru measure/layout
         *
         * Între cele două linii: arborele există în heap, cu toate atributele citite,
         * dar nu are părinte și nu e desenat. Un findViewById pe activitate ar returna
         * null, iar binding.button.getWidth() ar returna 0.
         */

        // ------------------------------------------------------------------------------
        // LISTENER: View.OnApplyWindowInsetsListener
        // ------------------------------------------------------------------------------
        // CE OBSERVĂ: insets-urile ferestrei — cât de mult din marginea ecranului e
        //   acoperită de barele de sistem (status, navigare, caption).
        // CÂND SE DECLANȘEAZĂ: la fiecare dispatch de insets, adică la prima așezare
        //   în pagină, la rotire, la schimbarea dimensiunii ferestrei, la deschiderea
        //   tastaturii (dacă ai cere Type.ime()) și la intrarea/ieșirea din multi-window.
        // STIL: lambda — interfața are O SINGURĂ metodă: onApplyWindowInsets(View, WindowInsetsCompat).
        // PARAMETRI: `v` = view-ul pe care e înregistrat listener-ul (rădăcina);
        //            `insets` = WindowInsetsCompat, obiectul cu toate tipurile de insets.
        // RETUR: insets-urile care merg mai departe la copii. `return insets` = nu consuma.
        // ------------------------------------------------------------------------------
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            // a. scoate cele patru dimensiuni ale barelor de sistem, în pixeli
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // b. aplică-le ca padding pe RĂDĂCINĂ: toți copiii se mută în zona sigură
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            // c. lasă insets-urile să circule mai departe, la copii
            return insets;
        });

        /*
         * DIAGRAMA 4 — padding pe rădăcină vs padding pe copil:
         *
         * VARIANTA CORECTĂ: padding pe rădăcină
         * ┌───────────────────────────────────────────────┐
         * │ ███ status bar ███████████████████████████████ │  ← se desenează PESTE conținut
         * │ ┌───────────────────────────────────────────┐ │
         * │ │ LinearLayout (root)                       │ │
         * │ │   paddingTop    = systemBars.top          │ │
         * │ │   paddingBottom = systemBars.bottom       │ │
         * │ │   ┌─────────────────────────────────────┐ │ │
         * │ │   │ Button                              │ │ │
         * │ │   └─────────────────────────────────────┘ │ │
         * │ │   ┌─────────────────────────────────────┐ │ │
         * │ │   │ Switch                              │ │ │
         * │ │   └─────────────────────────────────────┘ │ │
         * │ │   ┌─────────────────────────────────────┐ │ │
         * │ │   │ … toți copiii, împinși împreună     │ │ │
         * │ │   └─────────────────────────────────────┘ │ │
         * │ └───────────────────────────────────────────┘ │
         * │ ███ navigation bar ██████████████████████████ │
         * └───────────────────────────────────────────────┘
         *
         * VARIANTA GREȘITĂ: padding pe un copil
         * ┌───────────────────────────────────────────────┐
         * │ ███ status bar ███████████████████████████████ │
         * │ ┌───────────────────────────────────────────┐ │
         * │ │ LinearLayout (root) — fără padding        │ │
         * │ │   ┌─────────────────────────────────────┐ │ │
         * │ │   │ Button  ← doar el e împins în jos   │ │ │
         * │ │   └─────────────────────────────────────┘ │ │
         * │ │   ┌─────────────────────────────────────┐ │ │
         * │ │   │ Switch  ← rămâne sub bară           │ │ │
         * │ │   └─────────────────────────────────────┘ │ │
         * │ │   ┌─────────────────────────────────────┐ │ │
         * │ │   │ RatingBar ← rămâne sub bară         │ │ │
         * │ │   └─────────────────────────────────────┘ │ │
         * │ └───────────────────────────────────────────┘ │
         * │ ███ navigation bar ██████████████████████████ │
         * └───────────────────────────────────────────────┘
         *
         * CE FACE FIECARE LINIE DIN LAMBDA:
         *   insets.getInsets(Type.systemBars())  → cele 4 dimensiuni (status + navigare + caption)
         *   v.setPadding(left, top, right, bottom) → le aplică drept padding pe rădăcină
         *   return insets                        → lasă insets-urile să circule la copii
         *
         * return insets, nu CONSUMED: dacă ai întoarce CONSUMED, dispatch-ul s-ar opri
         * la rădăcină și niciun copil nu ar mai primi insets-urile. Aici nu contează,
         * pentru că niciun copil nu le cere, dar e regula generală.
         *
         * CAPCANA NR. 2: setPadding SUPRASCRIE padding-ul din XML, nu îl adună.
         * Rădăcina din DMA05 nu are android:padding, deci nu se pierde nimic. Dacă ar
         * avea 16dp, corect ar fi v.setPadding(16dp + left, 16dp + top, ...).
         *
         * Ce nu acoperă Type.systemBars(): tastatura. Când se deschide un EditText,
         * IME-ul nu e compensat — ar trebui Type.ime() într-un al doilea listener sau
         * în același. În DMA05 câmpurile sunt în partea de sus, deci nu se vede.
         */

        // ------------------------------------------------------------------------------
        // LISTENER: View.OnClickListener
        // ------------------------------------------------------------------------------
        // CE OBSERVĂ: un click — adică un down + up în interiorul limitelor view-ului,
        //   fără ca degetul să fi ieșit din view între timp.
        // CÂND SE DECLANȘEAZĂ: la up, dacă view-ul e clickable și nu e dezactivat.
        //   Se mai declanșează și la performClick() din cod.
        // STIL: clasă anonimă — interfața are o singură metodă, deci lambda ar fi mers.
        //   Alegerea e didactică: vezi mai jos echivalentul lambda.
        // ------------------------------------------------------------------------------
        binding.button.setOnClickListener(new View.OnClickListener() {
            // --------------------------------------------------------------------------
            // METODA: onClick
            // --------------------------------------------------------------------------
            // `v` este view-ul care a primit click-ul — aici, același binding.button.
            // Parametrul există pentru că aceeași instanță de listener poate fi
            // înregistrată pe mai multe view-uri; atunci `v` spune care a fost atins.
            // --------------------------------------------------------------------------
            @Override                                     // suprascriem metoda din interfață
            public void onClick(View v) {                 // `View`, nu `Button`: interfața e declarată pe View
                Toast.makeText(MainActivity.this, "Button clicked", Toast.LENGTH_SHORT).show();
            }
        });

        // ------------------------------------------------------------------------------
        // DE CE MainActivity.this ȘI NU this
        // ------------------------------------------------------------------------------
        // În interiorul clasei anonime, `this` e obiectul listener-ului, nu activitatea.
        // Iar Toast.makeText cere un Context. MainActivity.this e sintaxa prin care ieși
        // din clasa anonimă și ajungi la instanța exterioară. Într-o lambda, `this` se
        // referă la instanța care înconjoară lambda — adică MainActivity — deci
        // MainActivity.this ar fi redundant, dar legal.
        //
        // Echivalentul lambda al blocului întreg (NU există în fișierul real):
        //   binding.button.setOnClickListener(v ->
        //           Toast.makeText(MainActivity.this, "Button clicked", Toast.LENGTH_SHORT).show());
        //
        // ANATOMIA Toast.makeText(...).show():
        //   Toast.makeText(...)   → fabrică statică: construiește obiectul, nu îl afișează
        //   MainActivity.this     → Context-ul: de unde se iau resursele și pachetul
        //   "Button clicked"      → textul, un CharSequence
        //   Toast.LENGTH_SHORT    → durata: ~2 secunde (LENGTH_LONG ≈ 3,5 s)
        //   .show()               → atașează Toast-ul la coada de afișare
        //
        // .show() E OBLIGATORIU: fără el construiești obiectul Toast și îl arunci — pe
        // ecran nu apare nimic. Nu primești excepție, nu primești warning de compilare,
        // nu primești eroare la runtime. Linia
        //   Toast.makeText(this, "x", Toast.LENGTH_SHORT);
        // e perfect validă sintactic și complet inutilă. E cea mai frecventă greșeală
        // „invizibilă" din Android: codul rulează, aplicația nu crapă, feedback-ul lipsește.
        //
        // Ce se întâmplă în spate, când apelezi .show():
        //   Toast.makeText(context, text, duration)
        //      ├── creează un obiect Toast (nu e încă vizibil)
        //      └── .show()
        //            ├── cere serviciului de notificări al sistemului să îl afișeze
        //            ├── sistemul îl pune într-o COADĂ globală (un singur Toast o dată)
        //            ├── fereastra Toast apare deasupra aplicației, fără să blocheze input-ul
        //            └── după durată, dispare singură — nu trebuie să o închizi tu
        //
        // De ce Toast și nu TextView: Toast-ul nu are nevoie de un părinte în layout.
        // Apare într-o fereastră separată (WindowManager, TYPE_TOAST), deasupra
        // aplicației, și dispare singur. Zece Toaste declanșate rapid se afișează
        // secvențial, unul după altul, fiecare ~2 secunde — de aceea un Toast pe fiecare
        // pas de SeekBar produce o coadă de zeci de secunde.
        // ------------------------------------------------------------------------------

        // ------------------------------------------------------------------------------
        // LISTENER: CompoundButton.OnCheckedChangeListener (pe Switch)
        // ------------------------------------------------------------------------------
        // CE OBSERVĂ: starea `checked` a unui CompoundButton — bifat / nebifat.
        // CÂND SE DECLANȘEAZĂ: la atingere ȘI la setChecked(...) din cod.
        // STIL: lambda — interfața are o singură metodă:
        //   onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        // ------------------------------------------------------------------------------
        binding.switch1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // `buttonView` = widget-ul care a produs schimbarea; aici e ignorat.
            // `isChecked`  = NOUA stare, nu cea veche.
            Toast.makeText(MainActivity.this, "Switch clicked to " + isChecked, Toast.LENGTH_SHORT).show();
        });

        // ------------------------------------------------------------------------------
        // LISTENER: CompoundButton.OnCheckedChangeListener (pe CheckBox)
        // ------------------------------------------------------------------------------
        // Aceeași interfață, alt widget. Semnătura lambda e identică pentru că
        // CheckBox extinde tot CompoundButton.
        // ------------------------------------------------------------------------------
        binding.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(MainActivity.this, "CheckBox clicked to " + isChecked, Toast.LENGTH_SHORT).show();
        });

        /*
         * DE CE ACEEAȘI INTERFAȚĂ PENTRU DOUĂ WIDGET-URI DIFERITE
         *
         * Switch și CheckBox extind amândouă CompoundButton, care declară
         * setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener). De aceea
         * semnătura lambda e identică: (CompoundButton buttonView, boolean isChecked).
         * Nu tipul widget-ului decide listener-ul, ci CLASA DE BAZĂ.
         *
         * IERARHIA:
         *   View
         *    └── TextView
         *         └── Button
         *              └── CompoundButton          ← declară OnCheckedChangeListener
         *                   ├── CheckBox
         *                   ├── RadioButton
         *                   ├── Switch
         *                   └── ToggleButton
         *
         * "Switch clicked to " + isChecked → concatenarea apelează String.valueOf(boolean),
         * deci textul e ...true sau ...false. Nu ai nevoie de `if`.
         *
         * Listener-ul se declanșează și la schimbări din cod: un setChecked(true) ar
         * produce și el Toast. Consecința practică: dacă într-un onCreate restaurezi
         * starea cu setChecked(...), listener-ul înregistrat ÎNAINTE de restaurare va
         * produce efecte secundare.
         *
         * De ce Switch și nu SwitchCompat: layout-ul folosește <Switch>, clasa din
         * android.widget. SwitchCompat ar fi androidx.appcompat.widget.SwitchCompat.
         * Ambele extind CompoundButton, deci listener-ul e același.
         */

        // ------------------------------------------------------------------------------
        // LISTENER: SeekBar.OnSeekBarChangeListener
        // ------------------------------------------------------------------------------
        // CE OBSERVĂ: trei momente diferite ale interacțiunii cu bara:
        //   1. începutul atingerii
        //   2. fiecare schimbare de valoare
        //   3. sfârșitul atingerii
        // CÂND SE DECLANȘEAZĂ: la fiecare dintre cele trei momente; onProgressChanged
        //   se declanșează și la setProgress(...) din cod, cu fromUser = false.
        // STIL: clasă anonimă — TREI metode abstracte, deci interfața NU e funcțională.
        // ------------------------------------------------------------------------------
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            // --------------------------------------------------------------------------
            // METODA 1: onProgressChanged — valoarea s-a schimbat
            // --------------------------------------------------------------------------
            // `seekBar`  = bara care a produs evenimentul
            // `progress` = valoarea NOUĂ, între 0 și android:max (implicit 100)
            // `fromUser` = true dacă schimbarea vine de la deget, false dacă vine din cod
            // --------------------------------------------------------------------------
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Toast.makeText(MainActivity.this, "Progress changed to " + progress, Toast.LENGTH_SHORT).show();
            }

            // --------------------------------------------------------------------------
            // METODA 2: onStartTrackingTouch — degetul a atins bara
            // --------------------------------------------------------------------------
            // Se apelează O SINGURĂ DATĂ, la începutul tragerii, înainte de prima
            // schimbare de valoare. Corp gol: nu avem ce face la început.
            // --------------------------------------------------------------------------
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            // --------------------------------------------------------------------------
            // METODA 3: onStopTrackingTouch — degetul s-a ridicat
            // --------------------------------------------------------------------------
            // Se apelează O SINGURĂ DATĂ, la sfârșitul tragerii. Aici s-ar salva
            // valoarea finală într-o bază de date sau într-un ViewModel — nu la fiecare
            // pas. Corp gol: DMA05 nu salvează nimic.
            // --------------------------------------------------------------------------
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        /*
         * DE CE AICI NU MERGE LAMBDA
         *
         * SeekBar.OnSeekBarChangeListener are TREI metode abstracte. O interfață
         * funcțională are exact una. Cu trei, ești obligat la clasă anonimă care să le
         * implementeze pe toate. Compilatorul nu are cum să ghicească ce să pună în
         * celelalte două metode.
         *
         * CELE TREI MOMENTE:
         *   onProgressChanged     → la fiecare schimbare de valoare
         *   onStartTrackingTouch  → când degetul atinge bara
         *   onStopTrackingTouch   → când degetul se ridică
         *
         * De ce trei și nu una: „valoarea s-a schimbat" nu e același lucru cu
         * „utilizatorul interacționează". onProgressChanged poate veni din cod
         * (setProgress), din animație sau din deget. Celelalte două izolează gestul.
         *
         * `fromUser` e true când schimbarea vine de la utilizator și false când vine
         * din setProgress(...). Aici e ignorat — Toast-ul apare în ambele cazuri.
         * Corect ar fi `if (fromUser) { ... }`.
         *
         * Corpurile goale sunt legale și obligatorii. Trebuie să implementezi toate
         * cele trei metode; dacă nu ai ce face într-una, o lași goală. Dacă uiți una,
         * fișierul nu compilează: clasa anonimă ar fi abstractă.
         *
         * progress merge de la 0 la android:max. În XML nu există android:max, deci
         * valoarea implicită e 100. Fără android:progress, valoarea inițială e 0.
         *
         * DEFECTUL DIDACTIC: un Toast pentru fiecare pas înseamnă zeci de Toaste la o
         * singură tragere, puse în coadă. În ItemActivity (G1097) același listener scrie
         * într-un TextView — comportamentul corect.
         */

        // ------------------------------------------------------------------------------
        // LISTENER: RadioGroup.OnCheckedChangeListener
        // ------------------------------------------------------------------------------
        // CE OBSERVĂ: selecția grupului — care RadioButton e bifat.
        // CÂND SE DECLANȘEAZĂ: când un copil devine bifat, fie la atingere, fie la
        //   check(...) din cod. Se declanșează și cu checkedId = -1 după clearCheck().
        // STIL: clasă anonimă — interfața are o singură metodă, deci lambda ar fi mers.
        //   Din nou, alegere didactică: repo-ul alternează stilurile.
        // ------------------------------------------------------------------------------
        binding.radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            // --------------------------------------------------------------------------
            // METODA: onCheckedChanged
            // --------------------------------------------------------------------------
            // `group`     = grupul care a produs evenimentul
            // `checkedId` = ID-ul de RESURSĂ al butonului bifat (R.id.radioButton1 sau
            //               R.id.radioButton2), NU indexul lui (0 / 1) și NU obiectul.
            //               Poate fi -1 dacă nu e bifat nimic.
            // --------------------------------------------------------------------------
            @Override
            public void onCheckedChanged(@NonNull RadioGroup group, int checkedId) {
                // transformă ID-ul primit la runtime în obiectul RadioButton
                RadioButton selected = group.findViewById(checkedId);
                Toast.makeText(MainActivity.this, "Radio button clicked " + selected.getText(), Toast.LENGTH_SHORT).show();
            }
        });

        /*
         * checkedId E UN ID DE RESURSĂ, NU UN INDEX ȘI NU OBIECTUL
         *
         * Valoarea e R.id.radioButton1 sau R.id.radioButton2 — un int generat de aapt2,
         * stabil în APK. Confuzia „e poziția" e frecventă și duce la findViewById(0),
         * care întoarce null.
         *
         * De ce group.findViewById(checkedId) și nu findViewById(checkedId): căutarea
         * se face în subarborele grupului. Aici ar funcționa și varianta pe activitate,
         * pentru că ID-urile sunt unice în layout — dar forma cu `group` e cea corectă
         * conceptual: cauți în containerul care știe de butoane.
         *
         * Acesta e singurul findViewById care supraviețuiește cu ViewBinding. Binding-ul
         * îți dă binding.radioButton1 și binding.radioButton2, dar nu are cum să
         * transforme un int primit la runtime într-un câmp. Alternativa:
         *   RadioButton selected = checkedId == R.id.radioButton1
         *           ? binding.radioButton1
         *           : binding.radioButton2;
         *
         * De ce RadioGroup.OnCheckedChangeListener are o singură metodă, dar tot clasă
         * anonimă: din nou, stil alternat. Lambda ar fi perfect validă.
         *
         * ATENȚIE LA NUMELE METODEI: și CompoundButton, și RadioGroup au
         * setOnCheckedChangeListener, dar cu interfețe diferite. Nu e nici suprascriere,
         * nici supraîncărcare: sunt două metode declarate în două clase fără relație de
         * moștenire (RadioGroup extinde LinearLayout, nu CompoundButton). Se alege după
         * tipul receptorului.
         *
         * @NonNull vine din androidx.annotation. Nu schimbă comportamentul la runtime —
         * e contract pentru lint și pentru cititor: „grupul nu e niciodată null".
         *
         * CAPCANA NR. 3: dacă checkedId ar fi -1 (se întâmplă după clearCheck()),
         * findViewById(-1) întoarce null și selected.getText() aruncă NPE. În DMA05 nu
         * se apelează clearCheck(), deci nu se întâmplă. În G1097, primul buton are
         * android:checked="true" tocmai ca să nu existe niciodată „niciunul selectat".
         *
         * De ce selected.getText() și nu selected: RadioButton e un View; concatenarea
         * unui View ar apela toString(), care întoarce numele clasei plus hash.
         */

        // ------------------------------------------------------------------------------
        // LISTENER: CompoundButton.OnCheckedChangeListener (pe ToggleButton)
        // ------------------------------------------------------------------------------
        // Al treilea widget cu aceeași interfață. Dovada că listener-ul vine din
        // clasa de bază, nu din tipul concret al widget-ului.
        // ------------------------------------------------------------------------------
        binding.toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(MainActivity.this, "Toggle button clicked to " + isChecked, Toast.LENGTH_SHORT).show();
        });

        // ------------------------------------------------------------------------------
        // LISTENER: RatingBar.OnRatingBarChangeListener
        // ------------------------------------------------------------------------------
        // CE OBSERVĂ: valoarea stelelor — un `float`, nu un `int`.
        // CÂND SE DECLANȘEAZĂ: la atingere ȘI la setRating(...) din cod.
        // STIL: lambda — interfața are o singură metodă:
        //   onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser)
        // ------------------------------------------------------------------------------
        binding.ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            // `rating` e float pentru că android:stepSize="0.5" → 0.0, 0.5, 1.0, …
            // `fromUser` are același sens ca la SeekBar.
            Toast.makeText(MainActivity.this, "Rating changed to " + rating, Toast.LENGTH_SHORT).show();
        });


    }                                                     // sfârșitul lui onCreate
}                                                         // sfârșitul clasei MainActivity

/*
 * ======================================================================================
 * FAMILIILE DE LISTENERI — REGULA CARE DECIDE STILUL
 * ======================================================================================
 *
 *                 interfața are o singură metodă abstractă?
 *                               │
 *               ┌───────────────┴────────────────┐
 *              DA                                NU
 *               │                                 │
 *               ▼                                 ▼
 *       LAMBDA PERMISĂ                  CLASĂ ANONIMĂ OBLIGATORIE
 *       ─────────────                   ─────────────────────────
 *       View.OnClickListener            SeekBar.OnSeekBarChangeListener
 *         onClick(View)                   onProgressChanged(SeekBar, int, boolean)
 *                                         onStartTrackingTouch(SeekBar)
 *       CompoundButton.OnChecked...       onStopTrackingTouch(SeekBar)
 *         onCheckedChanged(CompoundButton, boolean)
 *                                       TextWatcher (nefolosit în DMA05)
 *       RadioGroup.OnChecked...           beforeTextChanged(CharSequence, int, int, int)
 *         onCheckedChanged(RadioGroup, int) onTextChanged(CharSequence, int, int, int)
 *                                         afterTextChanged(Editable)
 *       RatingBar.OnRatingBar...
 *         onRatingChanged(RatingBar, float, boolean)
 *
 *       View.OnApplyWindowInsets...
 *         onApplyWindowInsets(View, WindowInsetsCompat)
 *
 * REGULA, ÎNTR-O PROPOZIȚIE: o singură metodă abstractă → lambda permisă; două sau
 * mai multe → clasă anonimă obligatorie. Stilul din repo alternează deliberat, ca să
 * vezi ambele forme pe aceeași interfață (button e clasă anonimă, deși ar fi putut
 * fi lambda).
 *
 * CRONOLOGIA DECLANȘĂRII:
 *
 *   BUTTON (click)
 *     ACTION_DOWN ──▶ ACTION_UP ──▶ performClick() ──▶ onClick(v)
 *     (dacă degetul iese din view între down și up, click-ul NU se declanșează)
 *
 *   SWITCH / CHECKBOX / TOGGLEBUTTON (checked)
 *     atingere ──▶ toggle() ──▶ setChecked(!checked) ──▶ onCheckedChanged(view, isChecked)
 *     SAU setChecked(true) din cod ──▶ onCheckedChanged(view, true)
 *
 *   SEEKBAR (3 momente)
 *     ACTION_DOWN ──▶ onStartTrackingTouch(bar)
 *     ACTION_MOVE ──▶ onProgressChanged(bar, val, true)   ← de multe ori
 *     ACTION_UP   ──▶ onStopTrackingTouch(bar)
 *     SAU setProgress(50) din cod ──▶ onProgressChanged(bar, 50, false)
 *
 *   RADIOGROUP (selecție)
 *     atingere pe un RadioButton
 *       └── copilul se bifează
 *             └── grupul debifează ceilalți copii
 *                   └── onCheckedChanged(group, R.id.radioButtonN)   ← o singură dată
 *     SAU clearCheck() ──▶ onCheckedChanged(group, -1)   ← capcana nr. 3
 *
 *   RATINGBAR (valoare float)
 *     atingere pe stele ──▶ onRatingChanged(bar, rating, true)
 *     SAU setRating(3.5f) ──▶ onRatingChanged(bar, 3.5f, false)
 *
 * CE AU TOATE ÎN COMUN: listener-ul se declanșează DUPĂ ce starea s-a schimbat, nu
 * înainte. Când intri în onCheckedChanged, isChecked e deja noua valoare; când intri
 * în onProgressChanged, progress e deja noua valoare. Nu trebuie să citești starea
 * din widget — o primești ca parametru.
 *
 * CE DIFERĂ: numărul de evenimente per gest. Un click = un eveniment. O tragere de
 * SeekBar = un start, N progress, un stop. O bifă = un eveniment. De aceea SeekBar
 * are nevoie de trei metode, iar CheckBox de una.
 *
 * CE ÎNSEAMNĂ „INTERFAȚĂ FUNCȚIONALĂ" ÎN JAVA: o interfață cu exact o metodă
 * abstractă. Poate avea oricâte metode default și static, dar o singură metodă fără
 * implementare. View.OnClickListener are onClick abstractă și nimic altceva.
 * SeekBar.OnSeekBarChangeListener are trei metode abstracte, deci nu e funcțională.
 *
 * DE CE LAMBDA NU ARE NEVOIE DE new: expresia lambda ESTE implementarea metodei
 * abstracte. Compilatorul generează o clasă (sau un invokedynamic) și știe ce metodă
 * să lege pentru că există doar una. Când sunt trei, nu are cum să ghicească.
 *
 * LA RUNTIME: lambda care nu capturează variabile e un singleton. Lambda care
 * capturează (MainActivity.this e capturat implicit) alocă o instanță o singură dată,
 * la înregistrare. Costul e neglijabil, dar există.
 */
