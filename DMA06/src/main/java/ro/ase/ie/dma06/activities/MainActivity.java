/*
 * ======================================================================================
 * FIȘIER: MainActivity.java
 * ======================================================================================
 * LOCAȚIE:  DMA06/src/main/java/ro/ase/ie/dma06/activities/MainActivity.java
 * PACHET:   ro.ase.ie.dma06.activities
 * ROL:      meniul aplicației. Trei butoane, trei activități. Nu atinge nicio listă,
 *           niciun adapter, nicio dată. Tot ce face e să pornească ecranul potrivit.
 *
 * STRUCTURA FIȘIERULUI (3 părți):
 *   1. importuri        — 6 linii
 *   2. onCreate         — 8 linii: umflă layout-ul, apoi trei apeluri identice
 *   3. openOnClick      — 4 linii: findViewById + listener
 *
 * CE TREBUIE SĂ ȘTII ÎNAINTE:
 *   - `startActivity` nu întoarce nimic; ecranele nu trimit rezultat înapoi.
 *   - `Class<?>` e un tip de clasă, nu o instanță. `SingleColumnActivity.class` e obiectul.
 *   - `this` dintr-o lambda e MainActivity, nu lambda. De aceea `new Intent(this, ...)` merge.
 *
 * FLUXUL:
 *   MainActivity  ──btnStep1──▶  SingleColumnActivity    (ListView + ArrayAdapter)
 *                 ──btnStep2──▶  TwoColumnActivity      (ListView + NumberListAdapter)
 *                 ──btnStep3──▶  RecyclerViewActivity    (RecyclerView + NumberRecyclerAdapter)
 * ======================================================================================
 */

// ======================================================================================
// BLOCUL 1 — PACHETUL ȘI IMPORTURILE
// ======================================================================================
// Pachetul e cel care dă numele complet al clasei: ro.ase.ie.dma06.activities.MainActivity.
// El trebuie să coincidă cu folderul aici (activities/), spre deosebire de pachetele
// `data` și `adapters`, unde nu coincide — vezi 28.3.
// ======================================================================================

package ro.ase.ie.dma06.activities;                     // pachetul ecranelor; toate cele 4 activități stau aici

import android.content.Intent;                          // obiectul care descrie „pornesc activitatea X"
import android.os.Bundle;                               // starea salvată; aici nu e folosită
import android.view.View;                               // tipul returnat de findViewById
import androidx.appcompat.app.AppCompatActivity;        // activitatea de bază, cu tema AppCompat
import ro.ase.ie.dma06.R;                               // clasa generată din resurse

// `import android.view.View` deși nu construiești niciun View: e tipul variabilei locale
// din openOnClick. findViewById e generic din API 26: <T extends View> T findViewById(int).
// Aici compilatorul nu are de unde deduce T, pentru că rezultatul se atribuie unei
// variabile declarate explicit `View`. Deci `View` apare în cod și are nevoie de import.
//
// De ce AppCompatActivity și nu Activity: aduce AppCompatDelegate, care aplică tema
// Material pe orice versiune de Android și înlocuiește widget-urile la inflate cu
// variantele compatibile (AppCompatTextView, AppCompatButton).
//
// `R` e generată, nu scrisă. Importul are prefixul din `namespace` (ro.ase.ie.dma06),
// nu din folder. Dacă schimbi namespace în build.gradle.kts, importul se schimbă singur
// la următorul build — dar tu trebuie să știi de unde vine.

// ======================================================================================
// THEORY BLOCK: TREI ECRANE, UN MENIU
// ======================================================================================
/*
 * 1. CE ESTE O ACTIVITATE
 *    -   O componentă cu fereastră proprie. Are propriul ciclu de viață și propriul
 *        arbore de view-uri.
 *    -   Se declară în AndroidManifest.xml. Fără declarație, startActivity aruncă
 *        ActivityNotFoundException la runtime, nu la compilare.
 *
 * 2. CUM SE PORNESTE ALTA
 *    -   `startActivity(new Intent(context, Clasa.class))`.
 *    -   Intent-ul explicit (cu clasă) e singurul mod sigur în aceeași aplicație.
 *    -   Activitatea curentă NU e distrusă: e oprită și lăsată pe stivă (back stack).
 *
 * 3. STIVA, LA FIECARE PAS
 *    [MainActivity]                       ← la lansare
 *    [MainActivity, SingleColumnActivity] ← după click pe butonul 1
 *    [MainActivity]                       ← după Back
 *    MainActivity NU se recreează la revenire: primește onRestart → onStart → onResume.
 *    De aceea onCreate-ul meniului rulează o singură dată per instanță.
 *
 * 4. DE CE UN HELPER
 *    Cele trei butoane diferă prin exact două lucruri: id-ul și clasa. Restul e
 *    identic. Un helper parametrizat elimină șase linii duplicate și face evident
 *    ce variază.
 *
 * DIAGRAMA:
 *
 *   click btnStep1
 *     └─ OnClickListener.onClick(v)
 *          └─ startActivity(new Intent(this, SingleColumnActivity.class))
 *               ├─ ActivityManager primește cererea
 *               ├─ PackageManager verifică: există .activities.SingleColumnActivity?
 *               ├─ MainActivity.onPause()           ← rămâne pe stivă
 *               ├─ SingleColumnActivity.onCreate()  ← instanță NOUĂ
 *               └─ SingleColumnActivity.onResume()  ← vizibilă, primește input
 */

/** Opens each step of the demo. */
public class MainActivity extends AppCompatActivity {   // singura activitate cu intent-filter LAUNCHER

    // ==================================================================================
    // METODA 1 — onCreate
    // ==================================================================================
    // Teorie:  prima metodă a ciclului de viață; rulează o dată per instanță.
    // Logică:  1. super.onCreate  2. setContentView  3. trei apeluri openOnClick
    // Risc:    `super.onCreate` trebuie să fie primul; delegate-ul AppCompat are nevoie
    //          de el înainte ca fereastra să fie atinsă.
    // Ref:     capitolul 15 (Application și ciclu de viață).
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);             // 1. lansează ciclul de viață al părintelui
        setContentView(R.layout.activity_main);         // 2. umflă activity_main.xml în fereastră

        openOnClick(R.id.btnStep1, SingleColumnActivity.class);    // 3. buton 1 → treapta 1
        openOnClick(R.id.btnStep2, TwoColumnActivity.class);       //    buton 2 → treapta 2
        openOnClick(R.id.btnStep3, RecyclerViewActivity.class);    //    buton 3 → treapta 3
    }

    // `@Override` — metoda există în Activity. Adnotarea nu schimbă comportamentul, dar
    // îi spune compilatorului să verifice semnătura. Dacă ai scrie onCreate(Bundle b, int x),
    // fără @Override ai avea o metodă nouă, care nu e chemată niciodată.
    //
    // `protected`, nu `public` — Activity.onCreate e protected. Metoda e chemată de
    // framework, nu de codul tău.
    //
    // `savedInstanceState` — null la prima pornire, un Bundle când activitatea e recreată
    // (rotire, schimbare de temă, revenire după ce a fost distrusă în fundal). Aici nu e
    // folosit: meniul nu are stare.
    //
    // Ordinea celor trei linii nu e negociabilă. setContentView are nevoie de fereastra
    // creată de super.onCreate. openOnClick are nevoie de arborele construit de
    // setContentView, pentru că findViewById caută în el. Inversate, primele două dau
    // excepții, a treia întoarce null.

    // ==================================================================================
    // METODA 2 — openOnClick
    // ==================================================================================
    // Teorie:  un helper care primește id-ul butonului și clasa de pornit.
    // Logică:  1. findViewById  2. setOnClickListener  3. startActivity(new Intent(...))
    // Risc:    `findViewById` întoarce `View`, nu `Button`; dacă id-ul nu există în layout
    //          întoarce null și `setOnClickListener` dă NullPointerException.
    // Ref:     capitolul 04 (MainActivity - listă).
    // ==================================================================================
    private void openOnClick(int buttonId, Class<?> activityClass) {
        View button = findViewById(buttonId);           // caută butonul în arborele umflat la pasul 2
        button.setOnClickListener(v -> startActivity(new Intent(this, activityClass)));
        //                              ↑ lambda: `this` = MainActivity, deci e Context valid
    }

    // De ce un helper și nu trei blocuri identice: singurele lucruri care diferă între
    // cele trei butoane sunt id-ul și clasa. Restul e același cod. Dacă ai adăuga o
    // treaptă 4, adaugi o linie, nu șapte.
    //
    // `Class<?>` — al doilea parametru al constructorului Intent(Context, Class<?>) este
    // clasa componentei care trebuie pornită. `<?>` înseamnă „orice clasă"; aici toate
    // trei sunt activități, dar tipul nu o impune. Ai putea scrie
    // `Class<? extends Activity>` și ai avea o verificare în plus la compilare.
    //
    // `this` din lambda — într-o expresie lambda, `this` rămâne instanța care înconjoară
    // lambda, adică MainActivity. Într-o clasă anonimă (new View.OnClickListener() {...}),
    // `this` ar fi instanța clasei anonime, care NU e Context, și ar trebui scris
    // MainActivity.this. Aici lambda e mai scurtă exact din acest motiv.
    //
    // `v -> startActivity(...)` — parametrul v e view-ul care a primit click-ul, adică
    // butonul. Nu e folosit: știm deja ce buton e, pentru că listener-ul e instalat pe el.
    //
    // `startActivity` și nu `launcher`: ecranele celor trei trepte nu întorc nimic. Nu ai
    // nevoie de registerForActivityResult, pentru că nu aștepți un rezultat.
    //
    // `View button` și nu `MaterialButton`: helper-ul lucrează cu orice view care are
    // setOnClickListener. findViewById întoarce tipul declarat; aici e View, pentru că
    // helper-ul nu are nevoie de nimic specific unui buton.
    //
    // ==================================================================================
    // CE SE ÎNTÂMPLĂ LA BACK
    // ==================================================================================
    //
    //   SingleColumnActivity.onPause()
    //     → SingleColumnActivity.onStop()
    //     → SingleColumnActivity.onDestroy()        ← instanța moare
    //     → MainActivity.onRestart()
    //     → MainActivity.onStart()
    //     → MainActivity.onResume()                 ← meniul revine, FĂRĂ onCreate
    //
    // De ce contează: onCreate-ul meniului nu rulează a doua oară, deci nici
    // setContentView, nici openOnClick nu se repetă. Butoanele își păstrează
    // listener-ii pentru că arborele de view-uri nu a fost distrus.
    //
    // Ce s-ar întâmpla dacă MainActivity ar fi distrusă între timp (memorie puțină):
    // la revenire, onCreate rulează din nou, cu savedInstanceState nenull. Aici nu
    // contează, pentru că meniul nu are stare de restaurat.
    //
    // ==================================================================================
    // CE NU FACE FIȘIERUL ĂSTA
    // ==================================================================================
    //
    //   -   Nu creează niciun adapter.
    //   -   Nu citește nicio listă.
    //   -   Nu știe ce e un ViewHolder.
    //   -   Nu ține nicio referință la activitățile pe care le pornește.
    //
    // Ultima linie e importantă: după startActivity, MainActivity nu are nicio legătură
    // cu ecranul pornit. Nu poate citi date din el, nu poate afla când se închide, nu
    // primește nimic înapoi. Dacă ai avea nevoie de un rezultat, ai folosi
    // registerForActivityResult — dar niciuna dintre cele trei trepte nu întoarce ceva.
}
