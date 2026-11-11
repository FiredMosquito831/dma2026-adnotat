/*
 * ======================================================================================
 * FIȘIER: SecondActivity.java
 * ======================================================================================
 * ROL:
 *   Ecran de laborator pentru trei lucruri care nu încap în MainActivity:
 *     1. un dialog de confirmare construit cu `AlertDialog.Builder`;
 *     2. un `Snackbar` cu acțiune de anulare;
 *     3. un view creat din cod (`new Button`) și adăugat în arbore.
 *
 * CE DEMONSTREAZĂ:
 *   1. AlertDialog.Builder   — lanț de settere + `show()`.
 *   2. Snackbar.make(...)    — feedback tranzitoriu cu buton de acțiune.
 *   3. addButton()           — `new Button(ctx)`, `setId`, `LayoutParams`, `addView`.
 *
 * DEPINDE DE:
 *   - android.app.AlertDialog        → varianta de PLATFORMĂ (nu AppCompat)
 *   - com.google.android.material    → Snackbar
 *   - android:onClick="btnClick"     → legătura din XML, prin reflecție
 *
 * CE NU FACE:
 *   - nu e pornită din MainActivity (niciun `Intent` în tot proiectul);
 *   - nu salvează butoanele adăugate la rotire;
 *   - nu validează nimic — orice click pe „Do it!" adaugă un buton.
 *
 * REF: cap. 14 (secțiunea despre dialoguri), documentația `AlertDialog.Builder`,
 *      `Snackbar`, `ViewGroup.LayoutParams`.
 * ======================================================================================
 */

package ro.ase.ie.dma03;

// ======================================================================================
// IMPORT 1: android.app.AlertDialog — ATENȚIE, varianta de PLATFORMĂ
// ======================================================================================
// CE ESTE: dialogul standard al sistemului Android.
// DE CE E O ALEGERE DISCUTABILĂ: există și `androidx.appcompat.app.AlertDialog`.
//   Varianta de platformă folosește `android.R.attr.alertDialogTheme` al temei
//   curente; varianta AppCompat forțează tema și stilul bibliotecii. Cu
//   `Theme.Material3.DayNight.NoActionBar`, diferența vizuală poate fi mică sau
//   nulă, dar varianta AppCompat e cea care respectă tema aplicației în toate
//   cazurile. Funcțional sunt identice.
// DE CE CONTEAZĂ TOTUȘI: dacă tema aplicației ar fi una AppCompat pură (fără
//   Material3), `android.app.AlertDialog` ar cădea pe stilul de sistem — butoane
//   altfel colorate, alt font, altă umbră.
// ======================================================================================
import android.app.AlertDialog;

// ======================================================================================
// IMPORT 2: android.content.DialogInterface — interfața butoanelor dialogului
// ======================================================================================
// CE ESTE: interfața care conține `OnClickListener`-ul dialogului.
// DE CE E IMPORTATĂ SEPARAT: `OnClickListener` e o interfață IMBRICATĂ în ea:
//   `DialogInterface.OnClickListener`. Fără import, ar trebui scris numele complet.
// ======================================================================================
import android.content.DialogInterface;

// ======================================================================================
// IMPORT 3: android.os.Bundle — starea salvată la recreare
// ======================================================================================
// Semnătura lui `onCreate(Bundle)` o cere. Codul nu îl citește niciodată.
// ======================================================================================
import android.os.Bundle;

// ======================================================================================
// IMPORT 4-5: android.util.DisplayMetrics + android.util.TypedValue
// ======================================================================================
// FOLOSITE DOAR ÎN `addButton`, pentru conversia 50dp → pixeli.
// `DisplayMetrics` = densitatea ecranului (câți pixeli are un dp).
// `TypedValue.applyDimension` = conversia propriu-zisă, pentru orice unitate.
// REZULTATUL E COD MORT (vezi 26.2.7). Importurile rămân pentru că linia care le
// folosește există.
// ======================================================================================
import android.util.DisplayMetrics;
import android.util.TypedValue;

// ======================================================================================
// IMPORT 6-7: android.view.View + android.view.ViewGroup
// ======================================================================================
// `View` — tipul parametrului din `btnClick(View view)`. Semnătura e impusă de
//   `android:onClick` din XML.
// `ViewGroup` — clasa de bază pentru `LayoutParams`. `ViewGroup.LayoutParams` e
//   tipul cerut de `setLayoutParams`; `LinearLayout.LayoutParams` ar fi subtipul
//   cu `weight`.
// ======================================================================================
import android.view.View;
import android.view.ViewGroup;

// ======================================================================================
// IMPORT 8-10: android.widget.Button + LinearLayout + Toast
// ======================================================================================
// `Button`        — widget-ul creat din cod.
// `LinearLayout`  — containerul în care se adaugă butoanele.
// `Toast`         — mesajul de anulare din butonul negativ.
// ======================================================================================
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

// ======================================================================================
// IMPORT 11-15: androidx.* — infrastructura activității
// ======================================================================================
// Identice cu cele din MainActivity: EdgeToEdge, AppCompatActivity, Insets,
// ViewCompat, WindowInsetsCompat. Blocul de insets e copiat cuvânt cu cuvânt.
// ======================================================================================
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// ======================================================================================
// IMPORT 16: com.google.android.material.snackbar.Snackbar
// ======================================================================================
// CE ESTE: bara de feedback de la baza ecranului, cu text + acțiune opțională.
// DE CE VINE DIN MATERIAL: `Snackbar` nu face parte din platformă. De aceea
//   `SecondActivity` are nevoie de `libs.material` în dependențe — la fel ca
//   `MainActivity` pentru toolbar.
// CE S-AR ÎNTÂMPLA CU `Toast`: ai pierde butonul de acțiune. `Toast` nu are
//   `setAction`, nu poate fi închis de utilizator și nu are durată indefinită.
//   Pentru „anulează ultima acțiune", `Snackbar` e singura variantă din platformă.
// ======================================================================================
import com.google.android.material.snackbar.Snackbar;

/*
 * ======================================================================================
 * THEORY BLOCK: TREI MODALITĂȚI DE FEEDBACK ȘI UN VIEW CONSTRUIT DIN COD
 * ======================================================================================
 *
 * 1. DIALOG — BLOCHEAZĂ, CERE O DECIZIE
 *    -   `AlertDialog` apare peste ecran, într-o fereastră separată. Utilizatorul
 *        trebuie să răspundă înainte să continue.
 *    -   Se construiește cu `AlertDialog.Builder`: un obiect care adună configurația
 *        (titlu, mesaj, butoane) și abia la `show()` creează dialogul.
 *    -   Fiecare setter întoarce ACELAȘI builder → lanț de apeluri.
 *
 * 2. TOAST — TREACE, NU CERE NIMIC
 *    -   Text scurt, fără acțiune, fără layout. Apare și dispare singur.
 *    -   Nu are nevoie de un view părinte: e desenat într-o fereastră proprie.
 *
 * 3. SNACKBAR — TREACE, DAR POATE FI ANULAT
 *    -   Bară la baza ecranului, cu text + un buton de acțiune opțional.
 *    -   SPRE DEOSEBIRE DE TOAST, are nevoie de un VIEW: se inserează în arborele
 *        de view-uri, nu într-o fereastră separată. De aici al doilea argument
 *        din `make(...)`.
 *    -   Cu un `CoordinatorLayout` părinte, poate fi glisat pentru închidere și
 *        poate împinge un FAB în sus. Cu un `LinearLayout`, funcționează, dar
 *        fără aceste bonusuri.
 *
 * 4. VIEW CONSTRUIT DIN COD
 *    -   Orice widget are un constructor care cere un `Context` TEMATIZAT.
 *    -   Un view creat din cod nu are id, nu are `LayoutParams` și nu e în arbore.
 *        Trei operații separate:
 *          a) `setId(...)`            → ca să poată fi găsit cu `findViewById`;
 *          b) `setLayoutParams(...)`  → ca părintele să știe cum să-l măsoare;
 *          c) `parent.addView(...)`   → ca să intre în arbore și să fie desenat.
 *    -   Ordinea contează: `addView` fără `LayoutParams` funcționează (părintele
 *        generează unele implicite), dar `findViewById` fără `setId` nu găsește nimic.
 *
 * 5. DE CE UN VIEW ADĂUGAT DIN COD DISPARE LA ROTIRE
 *    -   Arborele de view-uri se reconstruiește din XML la fiecare recreare.
 *    -   Butonul adăugat trăiește doar în instanța veche a activității.
 *    -   Ca să supraviețuiască, ar trebui salvat în `savedInstanceState` (o listă de
 *        id-uri/text) și reconstruit în `onCreate`.
 *
 * REF: cap. 14 (dialoguri și Snackbar), documentația `AlertDialog.Builder`,
 *      `Snackbar`, `ViewGroup.addView`.
 * ======================================================================================
 */

public class SecondActivity extends AppCompatActivity {

    // ==================================================================================
    // CÂMP: inflatedLayout
    // ==================================================================================
    // CE ESTE: rădăcina din `activity_second.xml` — un `LinearLayout` vertical.
    // DE CE E CÂMP ȘI NU LOCALĂ: e folosit în TREI metode — `onCreate` (inițializare),
    //   `btnClick` (Snackbar) și `addButton` (`addView`).
    // FĂRĂ `private` — inconsecvență față de `MainActivity`, unde câmpurile sunt
    //   `private`. Nu strică nimic (suntem în același pachet), dar convenția e `private`.
    // NUMELE E ÎNȘELĂTOR: nu e un layout „umflat" la runtime, e layout-ul din XML.
    //   Umflarea a făcut-o `setContentView`. Numele sugerează intenția — un container
    //   în care se adaugă view-uri — nu mecanismul.
    // ==================================================================================
    LinearLayout inflatedLayout;

    /*
     * ----------------------------------------------------------------------------------
     * METHOD: onCreate(Bundle savedInstanceState)
     * ----------------------------------------------------------------------------------
     * THEORY:
     *   Callback-ul de creare. Aici se umflă `activity_second.xml` și se obține
     *   referința la containerul în care vor fi adăugate butoanele.
     *
     * LOGIC:
     *   1. `super.onCreate` — părintele.
     *   2. `EdgeToEdge.enable` — desenare sub bare.
     *   3. `setContentView` — umflă layout-ul.
     *   4. insets pe `R.id.main` — padding pentru barele de sistem.
     *   5. `inflatedLayout = findViewById(R.id.main)` — referința la container.
     *
     * REF: cap. 14, documentația `Activity#onCreate`.
     *
     * RISK:
     *   - `inflatedLayout` inițializat înainte de `setContentView` → `null`, apoi
     *     `NullPointerException` la primul `addView`.
     * ----------------------------------------------------------------------------------
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_second);

        // Blocul de insets e IDENTIC cu cel din MainActivity — copiat. Aici
        // `R.id.main` e pe `LinearLayout`-ul rădăcină, nu pe un `CoordinatorLayout`.
        // Funcționează la fel: padding pe rădăcină, copiii se mută în zona sigură.
        //
        // De ce e copiat și nu extras într-o funcție utilitară: într-un proiect de
        // laborator, copierea e acceptabilă. Într-o aplicație reală, ai avea o funcție
        // `applyInsets(View root)` într-o clasă de utilitare — sau ai folosi
        // `ViewCompat.setOnApplyWindowInsetsListener` o singură dată, într-o clasă
        // de bază.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ULTIMA linie — după `setContentView`. Dacă ar fi înainte, ar primi `null`
        // și primul `addView` ar arunca `NullPointerException`.
        //
        // Ce face `findViewById(R.id.main)` aici: caută în arborele
        // `activity_second.xml`. Id-ul `main` există și în `activity_main.xml`, dar
        // cele două layout-uri nu coexistă niciodată în același arbore — fiecare
        // activitate are fereastra ei. Nu e o coliziune.
        inflatedLayout = findViewById(R.id.main);
    }

    /*
     * ----------------------------------------------------------------------------------
     * METHOD: btnClick(View view)
     * ----------------------------------------------------------------------------------
     * THEORY:
     *   Metodă publică, cu semnătură fixă, chemată prin REFLECȚIE de platformă atunci
     *   când se apasă butonul cu `android:onClick="btnClick"` din XML. Nu există
     *   `setOnClickListener` nicăieri în cod — toată legătura e în XML.
     *
     * LOGIC:
     *   1. Creează un `AlertDialog.Builder` cu contextul activității.
     *   2. Înlănțuie: titlu → mesaj → cancelable → buton pozitiv → buton negativ.
     *   3. În butonul pozitiv: `addButton()` întoarce id-ul noului buton.
     *   4. Afișează un `Snackbar` cu acțiunea „Cancel", care șterge butonul după id.
     *   5. În butonul negativ: doar un `Toast`.
     *   6. `builder.show()` — abia aici dialogul apare pe ecran.
     *
     * REF:
     *   cap. 14 (secțiunea despre dialoguri), documentația `android:onClick`,
     *   `AlertDialog.Builder`, `Snackbar.make`.
     *
     * RISK:
     *   - metoda făcută `private` → compilează, dar la click:
     *     `IllegalStateException: Could not find method btnClick(View) ...`
     *   - parametrul schimbat din `View` în alt tip → aceeași excepție, la runtime.
     *   - `builder.show()` uitat → dialogul e construit și niciodată afișat.
     * ----------------------------------------------------------------------------------
     */
    public void btnClick(View view) {
        // Semnătura e IMPUSĂ de XML. În `activity_second.xml`, butonul are
        // `android:onClick="btnClick"`. Android caută la runtime, prin reflecție, o
        // metodă `public void btnClick(View)` în contextul activității.
        //
        // CONSECINȚA: dacă o faci `private`, sau îi schimbi parametrul în `Object`,
        // sau o redenumești fără să modifici XML-ul, COMPILAREA TRECE. Eroarea apare
        // la click: `IllegalStateException: Could not find method btnClick(View) in a
        // parent or ancestor Context`. Unul dintre puținele locuri din Android unde
        // greșeala nu se vede la build.
        //
        // `view` e butonul apăsat. Aici nu e folosit — codul nu are nevoie să știe
        // care buton a fost. Dar parametrul e obligatoriu: reflecția caută exact
        // semnătura cu un `View`.
        //
        // De ce `public` și nu `protected`: reflecția caută metode publice.
        // `protected` sau package-private nu sunt găsite din `View`-ul care face
        // apelul (alt pachet).
        //
        // ALTERNATIVA MODERNĂ: `setOnClickListener` în `onCreate`. E mai sigur
        // (compilatorul verifică), dar mai verbose. `android:onClick` e acceptat
        // pentru demonstrații; Google îl consideră depășit pentru cod nou.

        // `AlertDialog.Builder` — construiește dialogul bucată cu bucată. Fiecare
        // setter întoarce ACELAȘI builder, de aceea se pot înlănțui. Lanțul se
        // încheie cu `setNegativeButton`, iar `builder.show()` de la finalul metodei
        // afișează dialogul.
        //
        // De ce funcționează lanțul: `setTitle`, `setMessage`, `setCancelable`,
        // `setPositiveButton`, `setNegativeButton` au toate `return this;` în
        // implementare. Nu e magie — e convenția „fluent builder".
        //
        // `SecondActivity.this` și nu `this`: în interiorul unui listener, `this` ar
        // însemna listener-ul. `SecondActivity.this` e sintaxa pentru instanța clasei
        // exterioare. Aici suntem încă în corpul metodei, deci `this` ar fi mers —
        // dar forma explicită e o obișnuință bună, pentru că în doi pași vei fi în
        // `onClick`.
        AlertDialog.Builder builder = new AlertDialog.Builder(SecondActivity.this);
        builder.setTitle("Add new control ...")
                .setMessage("Are you sure you want to add a new control?")

                // `setCancelable(true)` e valoarea implicită. Scrisă explicit,
                // documentează comportamentul: se poate închide cu Back sau cu tap în
                // afară. Consecința: dacă utilizatorul închide așa, NICIUN buton nu se
                // execută — nici „Do it!", nici „Cancel". Dacă ai nevoie de o acțiune
                // garantată, ai nevoie de `setOnCancelListener`.
                //
                // Cu `setCancelable(false)`, dialogul nu se mai poate închide decât
                // prin cele două butoane. Pentru o confirmare critică e alegerea
                // corectă; pentru una obișnuită, enervează utilizatorul.
                .setCancelable(true)

                // Butonul pozitiv e acțiunea principală — apare în dreapta, colorat.
                // Textul „Do it!" e explicit; „OK" ar fi vag.
                //
                // `which` e `DialogInterface.BUTTON_POSITIVE` aici. Nu e folosit —
                // fiecare buton are listener-ul lui. Dacă ai avea un singur listener
                // pentru toate butoanele, `which` ar fi singura modalitate de a distinge.
                //
                // De ce listener-ul e o clasă anonimă și nu un lambda:
                // `DialogInterface.OnClickListener` are o singură metodă abstractă,
                // deci ar merge și un lambda (`(dialog, which) -> { ... }`). Codul
                // alege forma veche, din generația Java 7. Ambele compilează; lambda
                // e mai scurt.
                .setPositiveButton("Do it!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Creează butonul ÎNAINTE de Snackbar, ca să ai ce șterge.
                        // Id-ul returnat e SINGURA legătură cu butonul nou: nu ai o
                        // referință la obiect, doar un număr.
                        int id = addButton();

                        // `Snackbar.make(context, view, text, duration)` — patru argumente:
                        //   context  : SecondActivity.this — pentru temă și resurse
                        //   view     : inflatedLayout — view-ul de referință
                        //   text     : "Cancel previous action."
                        //   duration : Snackbar.LENGTH_LONG ≈ 2,75 s
                        //
                        // DE CE al doilea argument trebuie să fie un VIEW: `Snackbar`
                        // nu e o fereastră separată ca `Toast`-ul. E un view obișnuit,
                        // adăugat în arborele existent. Ca să știe unde să-l adauge,
                        // are nevoie de un punct de plecare: pornește de la view-ul dat
                        // și urcă prin părinți până găsește un `CoordinatorLayout` sau
                        // `android.R.id.content`. Fără un `View`, nu are de unde să
                        // pornească — de aceea nu există o suprascriere
                        // `make(Context, String, int)`.
                        //
                        // Ce se întâmplă exact cu `inflatedLayout`: e un `LinearLayout`
                        // simplu. `Snackbar` urcă de la el la `android.R.id.content`
                        // (containerul ferestrei) și își adaugă bara acolo. Rezultatul:
                        // bara apare jos, peste conținut.
                        //
                        // Ce ai câștiga cu un `CoordinatorLayout`: `SwipeDismissBehavior`
                        // (glisezi ca să închizi) și coordonarea cu un
                        // `FloatingActionButton` (FAB-ul se mută în sus cât timp
                        // Snackbar-ul e vizibil). Aici nu există FAB, deci diferența e
                        // doar glisarea.
                        Snackbar.make(SecondActivity.this, inflatedLayout, "Cancel previous action.", Snackbar.LENGTH_LONG)

                                // `setAction("Cancel", listener)` — adaugă butonul din
                                // dreapta. Textul e scurt cu intenție: un „Undo last
                                // action" ar înghesui mesajul pe ecrane mici.
                                //
                                // Textul și eticheta sunt ambele „Cancel" — o alegere
                                // nefericită de nume. Mesajul „Cancel previous action."
                                // spune ce s-a întâmplat; butonul „Cancel" ar trebui să
                                // fie „Undo", pentru că ANULEAZĂ adăugarea, nu anulează
                                // Snackbar-ul. Funcțional, butonul șterge butonul adăugat.
                                .setAction("Cancel", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // `id` e capturat din lambda exterioară. E o
                                        // variabilă locală `int`, atribuită o singură
                                        // dată → effectively final → poate fi prinsă.
                                        // Dacă ai scrie `id++` între declarație și
                                        // folosire, compilatorul ar refuza captura.
                                        //
                                        // `inflatedLayout.findViewById(id)` — funcționează
                                        // pentru că butonul a primit un id explicit cu
                                        // `setId(counter)`. Fără `setId`, `findViewById`
                                        // ar returna `null` și `removeView(null)` ar
                                        // arunca `NullPointerException`.
                                        Button viewById = inflatedLayout.findViewById(id);

                                        // `removeView(viewById)` — scoate view-ul din
                                        // container. Butonul dispare de pe ecran; obiectul
                                        // rămâne în memorie până la următoarea colectare
                                        // de gunoi.
                                        inflatedLayout.removeView(viewById);
                                    }
                                }).show();
                        // `.show()` la final — fără el, Snackbar-ul e construit și
                        // niciodată afișat. Aceeași capcană ca la `Toast`.
                        //
                        // Momentul afișării: Snackbar-ul e creat ÎN `onClick`-ul
                        // butonului pozitiv, deci apare după ce dialogul s-a închis.
                        // Dacă l-ai crea înainte de `builder.show()`, ar apărea sub
                        // dialog.
                        //
                        // Durata `LENGTH_LONG`: ~2,75 secunde (valorile din
                        // `SnackbarManager`: 1 500 ms pentru `SHORT`, 2 750 ms pentru
                        // `LONG`). Suficient ca utilizatorul să apese „Cancel".
                        // `LENGTH_INDEFINITE` ar ține bara până la o acțiune sau un
                        // swipe — prea agresiv pentru o confirmare.

                    }
                })

                // Butonul negativ — anulare. Doar un `Toast`, nicio modificare de stare.
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(SecondActivity.this, "Object creation cancelled", Toast.LENGTH_LONG).show();
                    }
                });

        // `builder.show()` — creează dialogul și îl afișează. Echivalent cu
        // `builder.create().show()`. Se apelează O SINGURĂ DATĂ, după ce lanțul de
        // settere s-a încheiat.
        //
        // De ce `show()` e separat și nu în lanț: pentru că lanțul se termină cu `;`
        // după `setNegativeButton`, iar `show()` e o metodă a builder-ului, nu un
        // setter. Ar fi mers și `builder.setTitle(...).show()`, dar codul alege forma
        // explicită.
        //
        // Ce se întâmplă dacă apelezi `show()` de două ori: se creează două dialoguri,
        // ambele afișate. Al doilea apare peste primul. Nu e o excepție — doar un bug
        // vizual.
        //
        // Ce se întâmplă dacă apelezi un setter după `show()`: nu are efect asupra
        // dialogului afișat. Builder-ul copiază valorile în dialog la `create()`;
        // după aceea, setterele modifică doar builder-ul, nu dialogul viu.
        builder.show();
    }


    // ==================================================================================
    // CÂMP: counter
    // ==================================================================================
    // DECLARAT ÎNTRE METODE — legal în Java, neobișnuit ca stil.
    // DOUĂ ROLURI SIMULTAN: generator de id-uri și sursă pentru etichetă.
    // DE CE NU `static`: ar fi împărțit între instanțe. La recreare, activitatea nouă
    //   ar continua de la valoarea veche — id-uri care nu se repetă, dar și un contor
    //   care crește la infinit. Aici `counter` e per instanță, deci se resetează la
    //   rotire.
    // ==================================================================================
    int counter = 0;

    /*
     * ----------------------------------------------------------------------------------
     * METHOD: addButton()
     * ----------------------------------------------------------------------------------
     * THEORY:
     *   Un view construit din cod e un obiect Java obișnuit până când îl adaugi într-un
     *   părinte. Are nevoie de trei lucruri ca să fie util:
     *     1. un CONTEXT TEMATIZAT (constructorul);
     *     2. un ID (ca să poată fi regăsit);
     *     3. LayoutParams (ca părintele să știe cum să-l măsoare).
     *
     * LOGIC:
     *   1. Calculează 50dp în pixeli (rezultatul e ignorat — cod mort).
     *   2. Creează `new Button(SecondActivity.this)`.
     *   3. Setează textul cu pre-increment: „Button_1", „Button_2", ...
     *   4. Setează id-ul = counter (1, 2, 3, ...).
     *   5. Creează `ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)`.
     *   6. `addView(newBtn)` — îl adaugă la sfârșitul `LinearLayout`-ului.
     *   7. Întoarce id-ul, ca apelantul să poată șterge butonul.
     *
     * REF:
     *   cap. 14 (secțiunea despre view-uri dinamice), documentația `View#setId`,
     *   `ViewGroup.LayoutParams`, `ViewGroup#addView`.
     *
     * RISK:
     *   - context de aplicație în loc de activitate → pierzi tema;
     *   - `setId` uitat → butonul nu poate fi găsit și șters;
     *   - `counter` nu se salvează → la rotire butoanele dispar și id-urile se repetă.
     * ----------------------------------------------------------------------------------
     */
    public int addButton()
    {
        // COD MORT. `hPixels` e calculat și NICIODATĂ folosit. Intenția era probabil
        // `newBtn.setHeight(hPixels)` sau un `LayoutParams` cu înălțime de 50dp.
        // Compilatorul dă un warning (`variable hPixels is never used`), nu o eroare.
        //
        // Ce face totuși, util de știut: `TypedValue.applyDimension(COMPLEX_UNIT_DIP,
        // 50, metrics)` convertește 50dp în pixeli fizici, folosind densitatea
        // ecranului. La 3x, întoarce 150. E conversia corectă atunci când ai nevoie de
        // pixeli într-un calcul — de exemplu pentru animații sau pentru `setPadding`.
        //
        // De ce nu `50 * getResources().getDisplayMetrics().density`: e echivalent,
        // dar `applyDimension` tratează și `SP`, `PT`, `MM`, `IN` cu aceeași
        // semnătură. O funcție, toate unitățile.
        //
        // De ce `DisplayMetrics` se obține o singură dată:
        // `getResources().getDisplayMetrics()` întoarce obiectul configurației
        // curente. Apelat de mai multe ori, întoarce același obiect. Nu e o operație
        // scumpă, dar e inutil să o repeți.
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int hPixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50,
                displayMetrics);

        // `new Button(Context)` — constructorul cere un CONTEXT TEMATIZAT.
        // `SecondActivity.this` are tema `Theme.DMA03`. Dacă ai pasa
        // `getApplicationContext()`, butonul ar pierde tema și, pe unele atribute
        // Material, ar arunca excepție la inflate.
        //
        // De ce `new Button(ctx)` și nu `LayoutInflater.from(ctx).inflate(...)`:
        // pentru un buton simplu, constructorul e mai direct. Inflater-ul are sens
        // când vrei un layout XML întreg, cu stiluri și copii.
        Button newBtn = new Button(SecondActivity.this);

        // `++counter` (pre-increment) — întâi incrementează, apoi folosește. Primul
        // buton e `Button_1`, iar `counter` devine 1. Cu `counter++` (post-increment),
        // primul ar fi `Button_0` și ai avea un buton cu id 0.
        newBtn.setText("Button_" + (++counter));

        // `setId(counter)` — id-uri 1, 2, 3... Id-urile de resursă sunt în zona
        // `0x7f......`, deci nu se ciocnesc. `View.NO_ID` e `-1`, deci 0 ar fi fost
        // o valoare nefericită — încă un motiv pentru pre-increment.
        //
        // FĂRĂ `setId`, butonul există dar e imposibil de găsit cu `findViewById`.
        // Ar apărea pe ecran și nu l-ai mai putea șterge.
        //
        // De ce id-urile mici sunt sigure: `findViewById` caută după valoarea
        // întreagă. Un id de 1 nu se ciocnește cu `R.id.button` (care e `0x7f0b....`).
        // Singura cerință e să fie unic în subarborele în care cauți.
        //
        // ALTERNATIVA MODERNĂ: `View.generateViewId()` întoarce un id unic, garantat.
        // Ar fi mai sigur dacă ai avea mai multe surse de id-uri. Aici `counter` e
        // suficient.
        newBtn.setId(counter);

        // `ViewGroup.LayoutParams` — clasa de bază. `LinearLayout` o acceptă. Dacă ai
        // nevoie de `layout_weight`, trebuie `LinearLayout.LayoutParams` — clasa de
        // bază nu are acel câmp.
        //
        // `MATCH_PARENT` pe lățime — butonul umple containerul, ca butonul original
        // din XML. `WRAP_CONTENT` pe înălțime — cât are nevoie textul.
        //
        // Ce înseamnă cele două constante: `MATCH_PARENT` = `-1` (umple părintele),
        // `WRAP_CONTENT` = `-2` (cât conținutul). Sunt valori negative intenționat,
        // ca să nu se confunde cu dimensiuni reale în pixeli.
        //
        // FĂRĂ `setLayoutParams`, `addView` aplică valorile implicite ale
        // containerului — tot `MATCH_PARENT`/`WRAP_CONTENT` pentru `LinearLayout`,
        // deci practic același rezultat. Explicit e mai sigur când containerul se
        // schimbă.
        //
        // Ce se întâmplă dacă pasezi `LinearLayout.LayoutParams`: funcționează și mai
        // bine — părintele nu mai trebuie să convertească.
        // `ViewGroup.LayoutParams` e acceptat pentru că
        // `LinearLayout.generateLayoutParams(ViewGroup.LayoutParams)` știe să-l
        // împacheteze.
        ViewGroup.LayoutParams layoutParams =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        newBtn.setLayoutParams(layoutParams);

        // `addView` — adaugă la sfârșitul `LinearLayout`-ului vertical, deci sub
        // butonul existent. Al doilea buton apare sub primul, al treilea sub al doilea.
        //
        // `addView` are două efecte: atașează view-ul la arbore și declanșează o
        // cerere de layout (`requestLayout`). Butonul apare imediat, fără să mai faci
        // nimic.
        inflatedLayout.addView(newBtn);

        // `return counter` — id-ul tocmai atribuit. Apelantul îl păstrează în `id` și
        // îl folosește în acțiunea Snackbar-ului.
        //
        // Ce se întâmplă la rotire: `counter` se resetează la 0, iar butoanele
        // adăugate dispar — nu sunt salvate în `savedInstanceState`. La rotire,
        // activitatea e recreată, `onCreate` rulează din nou, iar
        // `activity_second.xml` are din nou un singur buton.
        //
        // De ce se resetează și id-urile: dacă ai adăuga din nou butoane după rotire,
        // primul ar primi iar id-ul 1. Nu se ciocnește cu nimic, pentru că butoanele
        // vechi au dispărut odată cu arborele.
        return counter;
    }
}
