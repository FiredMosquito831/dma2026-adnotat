/*
 * ======================================================================================
 * FIȘIER: PlaceCiocanul.java — ECRANUL DE TEST
 * ======================================================================================
 * CE ESTE ACEST FIȘIER:
 *   O activitate minimă, deschisă de al doilea FAB din ecranul principal. Are un
 *   singur buton, care afișează un `Toast`. Nu citește și nu scrie niciun `Movie`.
 *
 * DE CE EXISTĂ:
 *   Este un ecran de probă adăugat în versiunea 2: verifică faptul că al doilea FAB
 *   pornește o activitate și că manifestul o declară. Nu are rol în fluxul de date.
 *
 * DE CE DEPINDE:
 *   - `R.layout.activity_place_ciocanul` și două ID-uri: `main`, `floatingActionButton2`;
 *   - `R.string.app_name` (folosit greșit — vezi RISC-ul din `onCreate`);
 *   - intrarea din `AndroidManifest.xml`, fără care nu poate fi pornită.
 *
 * CINE DEPINDE DE EL:
 *   Doar `MainActivity.initSecondListener()`, care îl lansează prin `launcher`.
 *
 * CE NU FACE:
 *   Nu întoarce niciun rezultat: nu apelează `setResult`, deci la întoarcere codul
 *   este `RESULT_CANCELED` și callback-ul din MainActivity nu face nimic.
 *
 * CE S-AR RUPE FĂRĂ EL:
 *   `btnVlad` ar porni o clasă inexistentă -> `ActivityNotFoundException` la runtime.
 * ======================================================================================
 */

// Pachetul ecranelor. Aici se află și MainActivity și ItemActivity.
package com.example.group1098.activities;

// Bundle: doar în semnătura lui `onCreate`, ca peste tot.
import android.os.Bundle;

// View: tipul parametrului din `onClick`.
import android.view.View;

// Toast: mesajul scurt afișat la apăsarea butonului.
import android.widget.Toast;

// AppCompatActivity: aceeași bază ca celelalte două activități.
import androidx.appcompat.app.AppCompatActivity;

// Insets: valorile barelor de sistem.
import androidx.core.graphics.Insets;

// ViewCompat: listener-ul de insets, compatibil pe orice versiune.
import androidx.core.view.ViewCompat;

// WindowCompat: varianta de edge-to-edge care primește un `Window`, nu o activitate.
//   MainActivity folosește `EdgeToEdge.enable(this)`; aici se folosește această formă.
import androidx.core.view.WindowCompat;

// WindowInsetsCompat: descrierea insets-urilor.
import androidx.core.view.WindowInsetsCompat;

// R: layout-ul și ID-urile.
import com.example.group1098.R;

// FloatingActionButton: butonul singur al ecranului.
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// (două linii goale în sursă — păstrate ca atare)

/*
 * ======================================================================================
 * BLOC DE TEORIE: UN AL TREILEA ECRAN, ȘI DOUĂ API-URI PENTRU ACELAȘI LUCRU
 * ======================================================================================
 *
 * 1. **O activitate = un ecran.** `PlaceCiocanul` este a treia activitate a aplicației.
 *    Se declară în manifest (38.8), se pornește cu `Intent` explicit și se închide cu
 *    butonul Back al sistemului. Atât este necesar ca să existe.
 *
 * 2. **Două variante de edge-to-edge.** `MainActivity` folosește `EdgeToEdge.enable(this)`,
 *    care primește activitatea; aici se folosește `WindowCompat.enableEdgeToEdge(getWindow())`,
 *    care primește fereastra. Efectul este același: conținutul este desenat sub barele
 *    de sistem, iar padding-ul se aplică manual, din insets. Cele două rețete coexistă
 *    în același proiect: un detaliu de igienă, nu o eroare.
 *
 * 3. **Toast vs Snackbar.** `Toast` apare peste orice ecran, nu are nevoie de un părinte
 *    și dispare singur. `Snackbar` are nevoie de un `CoordinatorLayout` și stă în partea
 *    de jos a ecranului. Pentru un mesaj de test, `Toast` este alegerea minimală.
 *
 * 4. **Ce lipsește ca să fie un ecran real:** un `RecyclerView` cu ID și cu adaptor, un
 *    `ProgressBar` dimensionat corect, un mesaj din `strings.xml` în locul textului
 *    hardcodat și un `setResult` dacă ecranul ar trebui să întoarcă date.
 * ======================================================================================
 */
// Activitatea. Fără `implements`: acest ecran nu primește click-uri din nicio listă.
public class PlaceCiocanul extends AppCompatActivity {

    // Butonul ecranului. Numele `btn2` este ales de autor; ID-ul din XML este
    //   `floatingActionButton2`. Cele două nu trebuie să coincidă — legătura se face
    //   prin `findViewById`, nu prin nume.
    private FloatingActionButton btn2;

    // ==================================================================================
    // CÂMPUL `string` — DECLARAT, ATRIBUIT, NICIODATĂ CITIT
    // ==================================================================================
    // RISC: câmpul nu este folosit nicăieri. Este „cod mort": niciun `getText`, niciun
    //   `Toast`, niciun `Log` nu îl citește. Compilatorul nu se plânge, pentru că
    //   atribuirea este un efect secundar valid.
    // Numele `string` este și el o alegere slabă: descrie TIPUL, nu conținutul.
    // ==================================================================================
    private String string;

    // ==================================================================================
    // METODA: onCreate — TOT ECRANUL, ÎNTR-O SINGURĂ METODĂ
    // ==================================================================================
    // CE FACE: umflă layout-ul, aplică insets-urile, leagă butonul de un Toast și
    //   atribuie (inutil) câmpul `string`.
    // CUM: 1) super; 2) edge-to-edge; 3) setContentView; 4) insets; 5) findViewById;
    //   6) listener; 7) atribuirea greșită a lui `string`.
    // RISC: spre deosebire de MainActivity, aici nu există `initControls()` și
    //   `initListeners()`. Cu un singur buton, separarea nu merită — dar ordinea
    //   rămâne importantă: `findViewById` trebuie după `setContentView`.
    // REF: MainActivity (38.1) — aceeași schemă, cu metode separate.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 1. Lanțul clasei de bază.
        super.onCreate(savedInstanceState);

        // 2. Edge-to-edge, varianta cu `Window`. `getWindow()` întoarce fereastra
        //    activității; metoda o configurează să deseneze sub bare.
        // OBSERVAȚIE: este echivalentul lui `EdgeToEdge.enable(this)` din MainActivity,
        //    scris cu altă API. Ambele fac același lucru; proiectul le amestecă.
        WindowCompat.enableEdgeToEdge(getWindow());

        // 3. Arborele de view-uri din layout-ul ecranului nou.
        setContentView(R.layout.activity_place_ciocanul);

        // 4. Insets-urile, pe containerul `R.id.main` din acest layout (un RelativeLayout).
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {

            // 4a. Barele de sistem.
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // 4b. Padding egal cu barele.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            // 4c. Se întoarce obiectul primit.
            return insets;
        });

        // 5. Butonul, după ID-ul din `activity_place_ciocanul.xml`.
        btn2 = findViewById(R.id.floatingActionButton2);

        // 6. Listener-ul butonului: un Toast. Tot comportamentul ecranului.
        btn2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // `PlaceCiocanul.this` = Context-ul (în clasa anonimă, `this` ar fi
                //   listener-ul). `Toast.LENGTH_LONG` = ~3,5 secunde.
                // RISC: textul este hardcodat în cod, nu în `strings.xml`. Lint dă
                //   avertismentul „HardcodedText", iar textul nu poate fi tradus.
                //   Înainte de orice predare, el trebuie înlocuit cu un mesaj real.
                Toast.makeText(PlaceCiocanul.this, "Caca", Toast.LENGTH_LONG).show();
            }
        });

        // 7. ULTIMA LINIE A METODEI — și cea mai instructivă.
        // ==================================================================================
        // RISC: `String.valueOf(R.string.app_name)` NU întoarce numele aplicației.
        // ==================================================================================
        // `R.string.app_name` este un `int`: ID-ul numeric al resursei, generat în `R.java`.
        // `String.valueOf` are mai multe suprascrieri; cu un `int` o alege pe cea care
        //   transformă numărul în cifre. Rezultatul este un text de forma „2131689472",
        //   nu „Group1098".
        //
        //   GRESIT:  String.valueOf(R.string.app_name)      -> "2131689472"
        //   CORECT:  getString(R.string.app_name)           -> "Group1098"
        //   CORECT:  String.valueOf(getString(R.string.app_name))
        //
        // DE CE NU SE VEDE: valoarea nu este afișată nicăieri. Câmpul `string` este
        //   mort, deci bug-ul nu are efect vizibil — deocamdată. Prima încercare de
        //   a-l pune într-un `TextView` ar afișa un număr.
        // DE REȚINUT: `R.<tip>.<nume>` este MEREU un `int`. Textul se obține doar prin
        //   `getString(...)` / `getText(...)` / `getDrawable(...)`, adică prin `Resources`.
        string = String.valueOf(R.string.app_name);
    }
}
