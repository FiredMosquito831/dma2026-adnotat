/*
 * ======================================================================================
 * FIȘIER: MainActivity.java
 * ======================================================================================
 * ROL:
 *   Ecranul principal al aplicației DMA03. Un singur `onCreate` care leagă cinci
 *   componente: lista, meniul contextual, sertarul, bara de sus și butonul Back.
 *
 * CE DEMONSTREAZĂ:
 *   1. DrawerLayout + NavigationView   — meniu lateral glisant peste conținut.
 *   2. MaterialToolbar                 — titlu, iconiță hamburger, meniu de acțiuni.
 *   3. PopupMenu                       — meniu contextual ancorat pe un rând.
 *   4. OnBackPressedCallback           — interceptarea butonului Back.
 *
 * DEPINDE DE:
 *   - androidx.activity           → EdgeToEdge, OnBackPressedCallback
 *   - com.google.android.material → MaterialToolbar, NavigationView
 *   - androidx.drawerlayout       → DrawerLayout (adus tranzitiv de `material`)
 *
 * CE NU FACE:
 *   - nu pornește SecondActivity (nu există niciun `startActivity` în tot fișierul);
 *   - nu salvează nimic în `savedInstanceState`;
 *   - nu are adapter scris de mână — lista se hrănește din `@array/items`.
 *
 * REF: cap. 14 — DMA03: navigare, meniuri și dialoguri.
 * ======================================================================================
 */

package ro.ase.ie.dma03;

// ======================================================================================
// IMPORT 1: android.content.Intent — IMPORTAT, DAR NEFOLOSIT
// ======================================================================================
// CE ESTE: descrierea unei acțiuni sau a unui ecran de pornit.
// DE CE E AICI: singurul mod de a deschide SecondActivity ar fi
//   `startActivity(new Intent(this, SecondActivity.class))`.
// DE CE E O PROBLEMĂ: în tot fișierul nu există niciun `new Intent(...)`, niciun
//   `startActivity`. Importul e SINGURA urmă a legăturii care lipsește.
// CUM VERIFICI: Ctrl+F după „Intent" în tot proiectul → un singur rezultat, linia asta.
// ======================================================================================
import android.content.Intent;

// ======================================================================================
// IMPORT 2: android.os.Bundle — starea salvată la recreare
// ======================================================================================
// CE ESTE: un dicționar cheie-valoare pe care sistemul îl păstrează între instanțe.
// DE CE E AICI: semnătura lui `onCreate(Bundle)` o cere.
// DE CE E INUTIL AICI: codul nu citește niciodată `savedInstanceState`. La rotire,
//   totul revine la starea inițială: titlu „Inbox", „Inbox" bifat, lista sus.
// ======================================================================================
import android.os.Bundle;

// ======================================================================================
// IMPORT 3: android.widget.ListView — lista de 20 de rânduri
// ======================================================================================
// CE ESTE: un `ViewGroup` care afișează o listă verticală derulabilă.
// DE CE E AICI: ecranul principal e, practic, o listă cu un meniu contextual.
// DE CE NU `RecyclerView`: 20 de rânduri statice, fără animații, fără view-uri
//   reciclate complicate. `ListView` e suficient și cere zero cod de adapter.
// ======================================================================================
import android.widget.ListView;

// ======================================================================================
// IMPORT 4: android.widget.PopupMenu — meniul ancorat pe rând
// ======================================================================================
// CE ESTE: un meniu plutitor, atașat de un view anume (ancora).
// DE CE E AICI: apăsarea lungă pe un rând trebuie să deschidă Open / Rename / Delete
//   lângă rândul respectiv, nu în colțul ecranului.
// DE CE NU `androidx.appcompat.widget.PopupMenu`: ar fi mers, dar varianta de
//   platformă e suficientă. Diferența se vede doar la stilul fundalului și la ripple.
// ======================================================================================
import android.widget.PopupMenu;

// ======================================================================================
// IMPORT 5: android.widget.Toast — mesajul scurt, fără acțiune
// ======================================================================================
// CE ESTE: un text care apare și dispare singur, fără să blocheze ecranul.
// DE CE E AICI: toate ramurile de meniu sunt demonstrații — un `Toast` e suficient.
// DE CE NU `Snackbar`: `Snackbar` are nevoie de un view părinte și de un `.show()`
//   suplimentar. Pentru o confirmare fără acțiune, `Toast` e mai direct.
// ======================================================================================
import android.widget.Toast;

// ======================================================================================
// IMPORT 6-13: androidx.* — infrastructura activității
// ======================================================================================
// EdgeToEdge              → desenare sub barele de sistem (API 21+).
// OnBackPressedCallback   → mecanismul modern pentru butonul Back.
// AppCompatActivity       → activitatea de bază, cu suport AppCompat.
// Insets                  → dreptunghiurile barelor de sistem, în pixeli.
// GravityCompat           → START/END, sigure pentru RTL.
// ViewCompat              → listener-ul de insets, compatibil pe toate API-urile.
// WindowInsetsCompat      → tipurile de insets (systemBars, ime, displayCutout).
// DrawerLayout            → containerul cu sertar (vine tranzitiv prin `material`).
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
// IMPORT 14-15: Material — bara și sertarul
// ======================================================================================
// MaterialToolbar  → `Toolbar` cu stilizare Material 3 aplicată automat.
// NavigationView   → conținutul sertarului: meniu, iconițe, bifare, insets proprii.
// ======================================================================================
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

/*
 * ======================================================================================
 * THEORY BLOCK: ACTIVITY ȘI CICLUL DE VIAȚĂ
 * ======================================================================================
 *
 * 1. CE E O ACTIVITY
 *    -   O componentă Android cu o fereastră proprie. Sistemul o creează, o afișează,
 *        o ascunde și o distruge după propriile reguli.
 *    -   Nu o instanțiezi tu cu `new MainActivity()`. O pornește sistemul, pe baza
 *        unui `Intent` sau a iconiței din lansator.
 *    -   Are un ciclu de viață cu stări: CREATED → STARTED → RESUMED → PAUSED →
 *        STOPPED → DESTROYED. Între stări, sistemul cheamă metode de callback.
 *
 * 2. `onCreate` — PRIMUL CALLBACK, O SINGURĂ DATĂ PER INSTANȚĂ
 *    -   Aici se construiește interfața: `setContentView`, `findViewById`, listenere.
 *    -   Se cheamă la pornire și la fiecare recreare (rotire, schimbare de temă,
 *        schimbare de limbă). La recreare primește `savedInstanceState` cu datele
 *        salvate anterior — aici e `null` prima dată și e ignorat mereu.
 *    -   Nu e locul pentru operații lungi: blocul rulează pe thread-ul principal,
 *        iar sistemul așteaptă să termine înainte să deseneze primul cadru.
 *
 * 3. ORDINEA OBLIGATORIE ÎN `onCreate`
 *    -   `super.onCreate(...)` PRIMUL. Părintele inițializează infrastructura
 *        (inclusiv `OnBackPressedDispatcher`-ul folosit mai jos).
 *    -   `setContentView(...)` ÎNAINTE de orice `findViewById`. Altfel primești
 *        `null`, nu excepție — și `NullPointerException` apare mai târziu, pe o
 *        linie fără legătură aparentă.
 *    -   `findViewById` și listenere DUPĂ `setContentView`.
 *
 * 4. DE CE UN SINGUR `onCreate` E O PROBLEMĂ DIDACTICĂ, NU UNA TEHNICĂ
 *    -   Funcționează perfect. Dar șapte responsabilități într-o metodă de 100 de
 *        linii se citesc greu și se testează și mai greu.
 *    -   Într-o aplicație reală: metode private `setupToolbar()`, `setupDrawer()`,
 *        `setupList()`. Aici sunt lăsate inline ca să vezi ordinea reală a apelurilor.
 *
 * 5. CE ÎNSEAMNĂ „CONTEXT"
 *    -   `this` dintr-o activitate e un `Context` (mai exact `ContextThemeWrapper`):
 *        dă acces la resurse, la tema aplicației și la serviciile sistemului.
 *    -   Widget-urile cer un context TEMATIZAT. `getApplicationContext()` nu are
 *        tema activității — de aceea `new Button(SecondActivity.this)` și nu
 *        `new Button(getApplicationContext())`.
 *
 * REF: cap. 15 — DMA02: Application și ciclu de viață; documentația
 *      `android.app.Activity` și `androidx.activity.ComponentActivity`.
 * ======================================================================================
 */

public class MainActivity extends AppCompatActivity {

    // ==================================================================================
    // CÂMP 1: drawerLayout
    // ==================================================================================
    // CE ESTE: referința la `DrawerLayout`-ul din XML.
    // UNDE E FOLOSIT: în trei locuri — listener-ul sertarului (`closeDrawer`),
    //   callback-ul Back (`isDrawerOpen` + `closeDrawer`) și hamburger (`openDrawer`).
    // DE CE E CÂMP ȘI NU LOCALĂ: toate cele trei utilizări sunt în `onCreate`, deci
    //   ar fi mers și locală. E câmp din obișnuință, ca să fie la îndemână dacă
    //   adaugi metode noi.
    // `private` — nimic din afară nu are nevoie de el.
    // ==================================================================================
    private DrawerLayout drawerLayout;

    // ==================================================================================
    // CÂMP 2: listView
    // ==================================================================================
    // CE ESTE: referința la lista din XML.
    // UNDE E FOLOSIT: în listener-ul de apăsare lungă, ca să citească textul rândului
    //   apăsat (`getItemAtPosition(position)`).
    // DE CE E CÂMP: lambda de long press se execută DUPĂ ce `onCreate` s-a terminat.
    //   O variabilă locală ar fi prinsă în closure (lambda capturează doar variabile
    //   effectively final), dar un câmp e mai clar și supraviețuiește cât trăiește
    //   activitatea.
    // ==================================================================================
    private ListView listView;

    /*
     * ----------------------------------------------------------------------------------
     * METHOD: onCreate(Bundle savedInstanceState)
     * ----------------------------------------------------------------------------------
     * THEORY:
     *   Callback-ul de creare al activității. Rulează o dată per instanță, pe thread-ul
     *   principal, înainte ca primul cadru să fie desenat. Aici se construiește tot
     *   arborele de view-uri și se leagă toate listenerele.
     *
     * LOGIC (ordinea reală din cod):
     *   1. `super.onCreate`                — inițializează părintele (inclusiv dispatcher-ul Back)
     *   2. `EdgeToEdge.enable`             — fereastra se întinde sub barele de sistem
     *   3. `setContentView`                — umflă `activity_main.xml`
     *   4. insets pe `R.id.main`           — padding ca să nu stai sub ceas
     *   5. `listView` + apăsare lungă      — `PopupMenu` ancorat pe rând
     *   6. `drawerLayout`/`toolbar`/`nav`  — cele trei componente găsite după id
     *   7. listener sertar                 — schimbă titlul, închide sertarul
     *   8. callback Back                   — închide sertarul sau deleagă
     *   9. hamburger                       — deschide sertarul
     *  10. meniu toolbar                   — Search / Share / Settings
     *
     * REF:
     *   cap. 14 (secțiunile 14.1–14.6), documentația `Activity#onCreate`.
     *
     * RISK:
     *   - `findViewById` înainte de `setContentView` → `null`, apoi NPE într-un listener.
     *   - `super.onCreate` omis → `SuperNotCalledException` la runtime.
     *   - listenerii mutați înainte de `setContentView` → primesc view-uri `null`.
     * ----------------------------------------------------------------------------------
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // PASUL 1: super.onCreate — lansează ciclul de viață al părintelui.
        // Fără el, `getOnBackPressedDispatcher()` de mai jos ar primi un dispatcher
        // neconfigurat. `@Override` e și o verificare: dacă semnătura greșește,
        // compilatorul te avertizează.
        super.onCreate(savedInstanceState);

        // PASUL 2: EdgeToEdge — fereastra se întinde sub status bar și sub bara de
        // navigare. Cheamă `WindowCompat.setDecorFitsSystemWindows(window, false)`.
        // Fără el, aplicația ar avea bare de sistem opace și nu ai avea nevoie de
        // blocul de insets de mai jos. Cu el, AI nevoie — altfel primul rând din
        // listă intră sub ceas.
        EdgeToEdge.enable(this);

        // PASUL 3: setContentView — `LayoutInflater` citește XML-ul, creează
        // obiectele Java pentru fiecare tag și le atașează la fereastră.
        // Din acest moment, `findViewById` poate găsi ceva. Înainte, arborele nu există.
        // `R.layout` e resursa de layout (fișierul întreg); `R.id` e un identificator
        // dintr-un view. Două spații de nume diferite, generate în același `R.java`.
        setContentView(R.layout.activity_main);

        // PASUL 4: insets — când sistemul calculează cât ocupă barele, aplică pe
        // view-ul cu `id=main` un padding egal cu ele. Conținutul se mută în zona
        // sigură, fundalul rămâne întins pe tot ecranul.
        //
        // `ViewCompat.setOnApplyWindowInsetsListener` înregistrează un listener
        // compatibil: pe API vechi folosește metodele vechi, pe API nou pe cele noi.
        // Listener-ul rulează imediat după `setContentView` și apoi la fiecare
        // schimbare (rotire, tastatură, bare care apar/dispar). Nu rulează o singură
        // dată — de aceea codul din interior trebuie să fie idempotent.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // `Type.systemBars()` = status bar + navigation bar + caption bar.
            // Nu include tastatura (`Type.ime()`) și nici decupajul (`Type.displayCutout()`).
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // `setPadding` SUPRASCRIE padding-ul existent. Dacă `CoordinatorLayout`-ul
            // ar avea `android:padding="8dp"` în XML, cele 8dp ar dispărea la primul
            // insets. Corect ar fi `v.setPadding(base + insets.left, ...)`, cu valorile
            // de bază salvate într-un câmp. Aici XML-ul nu are padding, deci nu se vede.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            // `return insets` — NU `WindowInsetsCompat.CONSUMED`. Insets-urile merg mai
            // departe, la copii. Dacă le-ai consuma, `AppBarLayout` nu ar mai ști de ele
            // și `liftOnScroll` ar primi valori greșite. Consumi doar când ai tratat tu
            // complet zona și vrei să oprești propagarea.
            return insets;
        });

        // PASUL 5: lista. Metoda e generică (`<T extends View> T findViewById(int)`),
        // deci compilatorul deduce tipul din atribuire; nu e nevoie de cast.
        // Căutarea se face pe activitate, adică în tot arborele ferestrei. Dacă ai avea
        // două view-uri cu același id în layout-uri diferite, ai primi primul găsit —
        // de aceea id-urile trebuie să fie unice într-un layout.
        listView = findViewById(R.id.listView);

        // `setOnItemLongClickListener` înregistrează un listener pentru apăsare lungă
        // pe ORICE rând al listei. Nu există un listener „pe rând": `ListView` are unul
        // singur, pentru toate rândurile, și îți spune în parametri pe care rând s-a apăsat.
        //
        // Lambda cu patru parametri pentru `AdapterView.OnItemLongClickListener`:
        //   parent   : AdapterView<?> — `ListView`-ul însuși (aici nu e folosit)
        //   view     : View           — RÂNDUL apăsat; devine ancora meniului
        //   position : int            — indexul rândului în listă, 0-based
        //   id       : long           — id-ul elementului; pentru `ArrayAdapter` e
        //                               egal cu `position`. Tipul e `long` pentru că
        //                               `AdapterView` poate lucra și cu id-uri din
        //                               baze de date (`SQLite` folosește `long`).
        //
        // NU există `setOnItemClickListener` nicăieri în fișier. Un tap simplu pe rând
        // nu face nimic. Doar apăsarea lungă reacționează. E o alegere didactică:
        // proiectul demonstrează meniul contextual, nu navigarea.
        //
        // Ce se întâmplă la un tap simplu, concret: `ListView` primește `ACTION_DOWN`,
        // pornește un `CheckForLongPress` cu timeout-ul sistemului (~500 ms), iar la
        // `ACTION_UP` fără listener de click nu se întâmplă nimic. Rândul nici măcar
        // nu rămâne evidențiat — `ListView` în modul `CHOICE_MODE_NONE` nu ține selecție.
        //
        // De ce listener-ul e pe listă și nu pe adapter: adapterul produce view-uri,
        // nu evenimente. `ListView` e cel care știe poziția și rândul apăsat.
        //
        // ------------------------------------------------------------------------------
        // METHOD (lambda): onItemLongClick(parent, view, position, id)
        // THEORY: callback pentru apăsarea lungă pe un rând de listă.
        // LOGIC:  1. construiește un `PopupMenu` ancorat pe `view`;
        //         2. umflă `list_item_menu`;
        //         3. înregistrează listener-ul de element;
        //         4. `show()` și `return true`.
        // REF:    26.1.9–26.1.11
        // RISK:   fără `return true`, se declanșează și click-ul normal.
        // ------------------------------------------------------------------------------
        // Long press shows the popup menu
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            // `new PopupMenu(this, view)` — `this` e contextul (activitatea, pentru
            // temă), `view` e ancora. `PopupWindow` calculează poziția pornind de la
            // dreptunghiul view-ului și îl plasează dedesubt, dacă încape, sau deasupra,
            // dacă nu. De aceea meniul apare lipit de rând, nu în colțul ecranului.
            //
            // De ce NU `listView` ca ancoră: `ListView` e un dreptunghi mare, cât tot
            // ecranul. Meniul ar apărea la marginea lui — adică sus, lângă bară — și ar
            // acoperi rândul apăsat. De ce NU `toolbar`: ar arăta ca un meniu de overflow
            // al barei, nu ca un meniu contextual.
            PopupMenu popup = new PopupMenu(this, view);   // anchored to the pressed row

            // `getMenuInflater()` vine din `Context` (activitatea) și întoarce un
            // `MenuInflater` — un parser pentru `<menu>`, nu pentru layout-uri.
            // `inflate(resursă, meniu)` — al doilea argument e `Menu`-ul DESTINAȚIE,
            // obținut cu `popup.getMenu()`. Nu e ca la `setContentView`, unde dai doar
            // resursa. De ce `R.menu.list_item_menu` și nu `top_app_bar`: sunt două
            // meniuri diferite, pentru două contexte diferite. `list_item_menu` are
            // „Open / Rename / Delete" — acțiuni pe un rând. `top_app_bar` are
            // „Search / Share / Settings" — acțiuni pe ecran.
            //
            // Ce se întâmplă dacă uiți `inflate`: meniul e gol. `popup.show()` afișează
            // un dreptunghi fără elemente, iar listener-ul nu se apelează niciodată.
            popup.getMenuInflater().inflate(R.menu.list_item_menu, popup.getMenu());

            // `setOnMenuItemClickListener` se apelează ÎNAINTE de `popup.show()`;
            // dacă îl înregistrezi după, meniul e deja afișat și click-urile din prima
            // secundă se pierd.
            popup.setOnMenuItemClickListener(menuItem -> {
                // `getItemId()` — id-ul din XML (`R.id.action_open` etc.). Nu poziția:
                // dacă reordonezi elementele în XML, id-urile rămân aceleași, pozițiile nu.
                int itemId = menuItem.getItemId();

                // `position` e capturat din lambda exterioară. E un parametru de metodă,
                // deci effectively final — Java permite capturarea. Dacă ai încerca să-l
                // modifici între timp (`position++`), compilatorul ar refuza.
                //
                // `getItemAtPosition(position)` întoarce un `CharSequence` — textul din
                // `@array/items`, pentru că `ListView` și-a construit singur un
                // `ArrayAdapter` din atributul `android:entries` din XML. Nu ai scris
                // niciun adapter în cod.
                //
                // `.toString()` e redundant pentru un `String`, dar inofensiv. Devine
                // necesar în momentul în care pui obiecte în listă: acolo `toString()`
                // e contractul de afișare.
                String selected = listView.getItemAtPosition(position).toString();

                // `return true` = „am tratat eu click-ul". Meniul se închide și
                // evenimentul nu mai circulă. `return false` = „nu mă interesează" —
                // sistemul încearcă handler-ul implicit.
                //
                // Ce se întâmplă exact cu `return false` pe `PopupMenu`: `PopupMenu`
                // închide meniul oricum, la click. Valoarea se propagă la
                // `MenuBuilder.performItemAction`, care, dacă nu e consumată, continuă
                // cu acțiunea implicită a elementului — de exemplu un `Intent` setat pe
                // item. La noi elementele nu au `Intent`, deci `false` nu schimbă nimic
                // vizibil. Dar contractul rămâne: `true` = tratat.
                if (itemId == R.id.action_open) {
                    // `Toast.makeText(...).show()` — două apeluri. `makeText`
                    // CONSTRUIEȘTE, `show()` AFIȘEAZĂ. Fără `.show()`, nu se întâmplă
                    // nimic, fără eroare. E cea mai frecventă greșeală la început.
                    // `LENGTH_SHORT` ≈ 2 secunde; `LENGTH_LONG` ≈ 3,5 secunde. Valorile
                    // sunt indicii, nu garanții: producătorul poate ajusta durata.
                    Toast.makeText(this, "Open: " + selected, Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.action_rename) {
                    Toast.makeText(this, "Rename: " + selected, Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.action_delete) {
                    Toast.makeText(this, "Delete: " + selected, Toast.LENGTH_SHORT).show();
                    return true;
                }
                // `return false` la final e cod mort — toate cele trei id-uri sunt
                // acoperite mai sus. Rămâne corect: dacă adaugi un al patrulea element
                // în `list_item_menu.xml` și uiți să-l tratezi, click-ul cade frumos în
                // handler-ul implicit în loc să facă ceva imprevizibil.
                return false;
            });

            // `popup.show()` — până aici doar ai configurat meniul. Abia acum apare pe
            // ecran. Ordinea e: construiești → umfli → înregistrezi listener → afișezi.
            popup.show();

            // `return true` din listener-ul de apăsare lungă — spune listei „apăsarea
            // lungă a fost tratată". Dacă ai întoarce `false`, sistemul ar considera
            // apăsarea lungă netratată și ar putea declanșa și un click normal în urma
            // ei. Rezultatul: meniul apare și, simultan, se execută acțiunea de click.
            //
            // Mecanismul din spate: `AdapterView` ține un flag intern
            // (`mHasPerformedLongPress`). Listener-ul îl setează doar dacă întoarce
            // `true`. La `ACTION_UP`, dacă flagul e setat, click-ul e ignorat. Dacă nu,
            // `performClick()` rulează normal.
            //
            // Ordinea contează: `popup.show()` ÎNAINTE de `return true`. Invers nu se
            // poate — `return` încheie lambda.
            return true;   // true = long press handled, so the normal click doesn't also fire
        });


        // PASUL 6: legarea celor trei componente. Toate trei id-urile trebuie să existe
        // în `activity_main.xml`, altfel primești `null` și un `NullPointerException`
        // la primul apel. Ordinea celor trei linii nu contează — sunt independente.
        // Contează doar să fie după `setContentView`.
        //
        // `toolbar` și `navigationView` sunt LOCALE, `drawerLayout` e CÂMP.
        // Inconsecvență reală în cod: toate trei sunt folosite doar în `onCreate`.
        // Merge și cu toate trei locale; merge și cu toate trei câmpuri. Alege o
        // variantă și ține-te de ea.
        drawerLayout = findViewById(R.id.drawerLayout);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        NavigationView navigationView = findViewById(R.id.navigationView);


        // PASUL 7: listener-ul sertarului.
        //
        // Ce face fiecare ramură: schimbă TITLUL barei de sus. Conținutul listei
        // rămâne același — cele 20 de „Item N". Aplicația e o demonstrație de
        // navigare, nu una de date.
        //
        // `toolbar.setTitle("Inbox")` suprascrie `app:title="Inbox"` din XML.
        // Valoarea din XML e doar cea inițială; după primul click, titlul vine din cod.
        //
        // `nav_settings` e tratat diferit — un `Toast`, nu un titlu. E singurul element
        // din submeniul „Other", deci nu are sens să schimbe titlul ecranului.
        //
        // De ce `item.getItemId()` și nu `item.getTitle()`: titlul e text vizibil,
        // care se poate traduce sau schimba. Id-ul e stabil. Comparația cu `R.id.*`
        // e singura care rezistă la o schimbare de limbă.
        //
        // Ce se întâmplă dacă niciun `if` nu se potrivește: listener-ul continuă,
        // cheamă `item.setChecked(true)` și închide sertarul. Elementul necunoscut
        // rămâne bifat, dar titlul nu se schimbă. Nu e o eroare — e un comportament
        // implicit.
        //
        // ------------------------------------------------------------------------------
        // METHOD (lambda): onMenuItemClick(item) — selecția din sertar
        // THEORY: callback pentru un element din `NavigationView`.
        // LOGIC:  1. citește id-ul; 2. actualizează titlul; 3. setează bifa;
        //         4. închide sertarul; 5. `return true`.
        // REF:    26.1.13
        // RISK:   fără `closeDrawer`, sertarul rămâne peste ecran.
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

            // `item.setChecked(true)` — meniul are `android:checkableBehavior="single"`
            // (vezi 26.6), deci se comportă ca un grup de radio butoane: un singur
            // element bifat. Apelul explicit garantează starea după ce sertarul se
            // închide și după redeschidere.
            //
            // De ce e nevoie de `setChecked` explicit dacă meniul e deja „single":
            // `checkableBehavior` face grupul exclusiv, dar nu înseamnă că
            // `NavigationView` marchează automat elementul la click. Codul nu se
            // bazează pe comportamentul implicit — îl setează singur. E o alegere
            // defensivă, bună.
            item.setChecked(true);

            // `closeDrawer(GravityCompat.START)` — închide sertarul din partea `start`.
            // Trebuie să corespundă cu `android:layout_gravity="start"` din XML. Dacă
            // în XML e `start` și în cod `end`, sertarul rămâne deschis.
            //
            // De ce `setNavigationItemSelectedListener` NU închide sertarul singur:
            // `NavigationView` e un container de meniu. Nu are referință la
            // `DrawerLayout` și nu știe că există unul. Sunt vecini în arbore, nu
            // părinte-copil. Listener-ul e doar un callback: „s-a apăsat elementul X".
            // Ce faci cu informația — schimbi titlul, închizi sertarul, pornești un
            // ecran — e decizia ta, în codul tău.
            //
            // Consecința practică: dacă uiți `closeDrawer`, sertarul rămâne peste
            // ecran după selecție. Utilizatorul vede titlul schimbat, dar nu vede
            // conținutul. E primul lucru care lipsește dintr-o implementare grăbită.
            drawerLayout.closeDrawer(GravityCompat.START);

            // `return true` — spune `NavigationView` că click-ul a fost tratat. Dacă
            // întorci `false`, elementul nu se marchează vizual ca selectat. Contractul
            // documentat al variantei vechi (`OnNavigationItemSelectedListener`) e
            // explicit: „true to display the item as the selected item". Varianta nouă
            // (`MenuItem.OnMenuItemClickListener`) propagă valoarea în `MenuBuilder`,
            // iar `NavigationView` o folosește la fel.
            //
            // De ce codul cheamă totuși `setChecked(true)`: pentru că se bazează pe
            // contract, nu pe implementare. Dacă `NavigationView` s-ar schimba, bifarea
            // rămâne corectă.
            return true;
        });

        // PASUL 8: butonul Back. De ce nu `@Override public void onBackPressed()`:
        // metoda a fost marcată deprecated din API 33, iar `OnBackPressedDispatcher`
        // e mecanismul oficial. Pe lângă asta, dispatcher-ul permite MAI MULTE
        // callback-uri în lanț, fiecare cu prioritatea lui — o singură suprascriere
        // nu poate face asta.
        //
        // `new OnBackPressedCallback(true)` — argumentul e starea inițială:
        // `true` = activ. Un callback dezactivat nu primește evenimente.
        //
        // `addCallback(this, ...)` — primul argument e `LifecycleOwner` (activitatea).
        // Dispatcher-ul ține callback-ul doar cât timp activitatea e în `STARTED`.
        // La `onStop`, callback-ul e scos automat; la `onStart`, revine. Nu trebuie
        // să-l elimini manual. Varianta fără `LifecycleOwner` lasă callback-ul în
        // listă până îl scoți tu — dacă uiți, ține o referință la activitate.
        //
        // Logica, pas cu pas:
        //   1. S-a apăsat Back.
        //   2. Sertarul e deschis? → închide-l. Sfârșit. Utilizatorul rămâne pe ecran.
        //   3. Sertarul e închis? → `setEnabled(false)`, apoi re-trimite evenimentul.
        //
        // De ce `setEnabled(false)` înainte de re-trimitere: fără el,
        // `onBackPressed()` ar intra din nou în ACELAȘI callback, care ar vedea
        // sertarul închis și ar re-trimite evenimentul — recursie infinită,
        // `StackOverflowError`. Dezactivându-te pe tine însuți, lași următorul handler
        // din lanț (cel implicit, care închide activitatea) să primească evenimentul.
        //
        // Mecanismul exact: `OnBackPressedDispatcher` ține callback-urile într-o
        // colecție ordonată. `onBackPressed()` o parcurge de la ultimul adăugat spre
        // primul și se oprește la primul callback ACTIV care tratează evenimentul.
        // Dacă callback-ul tău se reapelează fără să se dezactiveze, parcurgerea îl
        // găsește tot pe el. `setEnabled(false)` îl scoate din colecția de callback-uri
        // active, deci parcurgerea trece mai departe.
        //
        // Ce e „handler-ul implicit": `ComponentActivity` înregistrează un fallback
        // care cheamă `super.onBackPressed()` — adică `finish()` pe activitate. Dacă
        // nu ar exista, Back ar fi ignorat.
        //
        // De ce nu `finish()` direct: `finish()` ar închide activitatea, dar ar sări
        // peste orice alt callback din lanț (de exemplu un `NavController` care ar
        // trebui să gestioneze Back întâi). Delegarea păstrează lanțul intact.
        //
        // ------------------------------------------------------------------------------
        // METHOD: handleOnBackPressed()
        // THEORY: callback-ul care interceptează Back înaintea sistemului.
        // LOGIC:  1. dacă sertarul e deschis, închide-l;
        //         2. altfel dezactivează-te și delegă evenimentul.
        // REF:    26.1.14
        // RISK:   fără `setEnabled(false)`, recursie infinită.
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


        // PASUL 9: hamburger-ul. Iconița din stânga barei (cele trei linii) deschide
        // sertarul. Iconița există deja în XML — `app:navigationIcon="@drawable/ic_menu"`.
        // Fără listener, e doar decorativă: se vede, se apasă, nu se întâmplă nimic.
        // Listener-ul e cel care o transformă în buton.
        //
        // `setNavigationOnClickListener` — un singur listener pentru iconița de
        // navigare a toolbar-ului. Nu primește poziție sau id: e un singur buton.
        //
        // De ce nu `ActionBarDrawerToggle`: varianta clasică leagă automat iconița,
        // sertarul și accesibilitatea, dar cere ca toolbar-ul să fie setat ca
        // `ActionBar` (`setSupportActionBar`). Aici toolbar-ul e folosit ca view
        // obișnuit, deci legătura se face manual. E mai puțin cod, dar și mai puțin
        // automatism — de exemplu, iconița nu se animă în săgeată când sertarul e
        // deschis, iar descrierea de accesibilitate nu se actualizează automat.
        //
        // `GravityCompat.START` — aceeași direcție ca `layout_gravity` din XML.
        // A patra apariție în fișier.
        // Hamburger (navigation) icon
        toolbar.setNavigationOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // PASUL 10: meniul din toolbar. Meniul e deja umflat —
        // `app:menu="@menu/top_app_bar"` din XML. Aici doar reacționezi la click.
        // Dacă ai fi folosit `setSupportActionBar(toolbar)`, ai fi suprascris
        // `onCreateOptionsMenu` și `onOptionsItemSelected`; cu toolbar-ul ca view
        // simplu, ăsta e cârligul corect.
        //
        // `action_settings` are `app:showAsAction="never"` — de aceea ajunge în
        // meniul „⋮" (overflow), nu pe bară. `action_search` și `action_share` au
        // `ifRoom` — apar direct pe bară dacă încap.
        //
        // Comentariul `// open settings` marchează un loc gol. Ramura afișează un
        // `Toast`, dar intenția era deschiderea unui ecran de setări. E un TODO
        // deghizat.
        //
        // `return false` la final e cod mort — toate cele trei id-uri sunt acoperite
        // mai sus. Rămâne corect: dacă adaugi un al patrulea element în meniu și uiți
        // să-l tratezi, click-ul cade frumos în handler-ul implicit în loc să arunce
        // excepție.
        //
        // Ce se întâmplă cu `return false` pe un element din overflow: `Toolbar`
        // propagă valoarea la `MenuBuilder`, care încearcă acțiunea implicită a
        // elementului (un `Intent`, un `ActionProvider`). La noi nu există niciuna,
        // deci meniul se închide și atât.
        //
        // ------------------------------------------------------------------------------
        // METHOD (lambda): onMenuItemClick(item) — meniul barei
        // THEORY: callback pentru Search / Share / Settings.
        // LOGIC:  1. citește id-ul; 2. afișează un `Toast`; 3. `return true`.
        // REF:    26.1.16
        // RISK:   `return false` pe un id necunoscut lasă evenimentul să circule.
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
