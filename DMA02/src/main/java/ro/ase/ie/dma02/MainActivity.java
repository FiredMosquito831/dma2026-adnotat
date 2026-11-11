/*
 * ======================================================================================
 * MainActivity.java — SINGURA ACTIVITATE A PROIECTULUI DMA02
 * ======================================================================================
 * CE ESTE:
 *   Activitatea care apare în launcher și care deține ecranul din `activity_main.xml`.
 *   Extinde `AppCompatActivity`, nu `Activity`: are nevoie de tema Material3/AppCompat.
 *
 * DE CE EXISTĂ:
 *   1. Demonstrează ciclul de viață: șase metode suprascrise, fiecare doar loghează.
 *   2. Leagă layout-ul de cod: `setContentView(R.layout.activity_main)`.
 *   3. Demonstrează un `Intent` implicit: `pressMe` deschide share sheet-ul.
 *
 * CE SE STRICĂ FĂRĂ EL:
 *   - fără `setContentView`, ecranul rămâne gol și `findViewById` întoarce null;
 *   - fără `super.onCreate`, framework-ul aruncă `SuperNotCalledException`;
 *   - fără `pressMe`, `android:onClick` crapă la click, nu la compilare.
 *
 * REF: android.app.Activity (Android 36); AppCompatActivity (androidx.appcompat).
 * ======================================================================================
 */

package ro.ase.ie.dma02;                             // același pachet ca DMA02App

import android.content.Intent;                       // folosit doar în pressMe
import android.os.Bundle;                            // parametrul din onCreate
import android.util.Log;                             // scrierea în Logcat
import android.view.View;                            // parametrul din pressMe — cerut de android:onClick

import androidx.appcompat.app.AppCompatActivity;     // varianta AppCompat a lui Activity

/*
 * ======================================================================================
 * THEORY BLOCK: ciclul de viață al unei activități
 * ======================================================================================
 * 1. CE ESTE O ACTIVITATE
 *    O componentă cu fereastră. Sistemul o creează, o afișează, o ascunde și o
 *    distruge după reguli fixe. Tu nu apelezi `onCreate`; îl apelează framework-ul.
 *
 * 2. CELE ȘAPTE STĂRI (șase metode + onRestart)
 *    onCreate → onStart → onResume → onPause → onStop → onDestroy
 *    `onRestart` apare doar la revenirea din `onStop`.
 *
 * 3. `super` ESTE OBLIGATORIU
 *    `ActivityThread` verifică după fiecare metodă dacă ai apelat `super`. Dacă nu,
 *    aruncă `SuperNotCalledException`. Nu e convenție, e verificare la runtime.
 *
 * 4. `AppCompatActivity` vs `Activity`
 *    `AppCompatActivity` aduce backport-ul AppCompat: inflatare compatibilă, teme
 *    Material pe versiuni vechi, `AppCompatDelegate` pentru dark mode. Cere o temă
 *    care coboară din `Theme.AppCompat` (aici: `Theme.Material3.*`).
 *
 * 5. ORDINEA DIN FIȘIER NU E ORDINEA DE EXECUȚIE
 *    Metodele sunt în ordine alfabetică (generate de IntelliJ), nu cronologică.
 *
 * ASCII: CICLUL COMPLET
 *
 *   onCreate ──► onStart ──► onResume ──► [vizibil, interactiv]
 *                                            │
 *                          Home / altă activitate
 *                                            ▼
 *                                        onPause ──► onStop ──► [ascuns]
 *                                                          │
 *                                        revenire ──► onRestart ──► onStart ──► onResume
 *                                                          │
 *                                        Back ──► onDestroy
 * ======================================================================================
 */
public class MainActivity extends AppCompatActivity {   // (1) activitate cu suport AppCompat

    // Tag-ul din Logcat: "MainActivity".
    // `static final` = aparține clasei, nu instanței. La rotire se creează o instanță
    // nouă, dar tag-ul rămâne același — exact ce vrei în Logcat.
    private static final String TAG = MainActivity.class.getSimpleName();

    // ==================================================================================
    // METODA 1/7: onCreate(Bundle) — CONSTRUCȚIA
    // ==================================================================================
    // THEORY: prima metodă a ciclului. Aici se inflatează layout-ul și se leagă
    //         view-urile. `savedInstanceState` e null la prima creare și non-null
    //         la recreare (rotire, revenire după kill).
    // LOGIC:
    //   1. `super.onCreate(...)` — obligatoriu primul.
    //   2. `setContentView(...)` — creează arborele de view-uri.
    //   3. `Log.w(...)` — marchează sfârșitul construcției.
    // REF: Activity.java (android-36), linia 1861 — `onCreate`.
    // RISK: fără `super`, `SuperNotCalledException`. Fără `setContentView`, ecran gol.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);              // pas 1: framework-ul își face inițializările
        setContentView(R.layout.activity_main);          // pas 2: XML-ul devine arbore de View-uri
        Log.w(TAG, "onCreate");                          // pas 3: primul log al activității
    }

    // ==================================================================================
    // METODA 2/7: onDestroy() — DISTRUGEREA
    // ==================================================================================
    // THEORY: ultima metodă a ciclului. Apare la Back, la `finish()` sau la rotire.
    //         NU este garantată: dacă sistemul omoară procesul, nu rulează.
    // LOGIC: `super` + un rând în Logcat.
    // REF: Activity.java (android-36), linia 2932 — `onDestroy`.
    // RISK: nu pune aici salvarea de date — s-ar putea să nu apuce să ruleze.
    //       Pentru salvare există `onPause` / `onStop` / `onSaveInstanceState`.
    @Override
    protected void onDestroy() {
        super.onDestroy();                              // curățenia framework-ului
        Log.w(TAG, "onDestroy");
    }

    // ==================================================================================
    // METODA 3/7: onStart() — DEVENIREA VIZIBILĂ
    // ==================================================================================
    // THEORY: activitatea devine vizibilă, dar nu primește încă input. Este perechea
    //         lui `onStop`.
    // LOGIC: `super` + un rând în Logcat.
    // REF: Activity.java (android-36), linia 2148 — `onStart`.
    // RISK: aici încă nu poți presupune că utilizatorul poate apăsa ceva.
    @Override
    protected void onStart() {
        super.onStart();                                // framework-ul își face tranziția
        Log.w(TAG, "onStart");
    }

    // ==================================================================================
    // METODA 4/7: onStop() — ASCUNDEREA
    // ==================================================================================
    // THEORY: activitatea nu mai este vizibilă: Home, altă activitate full-screen,
    //         ecran stins. Este perechea lui `onStart`.
    // LOGIC: `super` + un rând în Logcat.
    // REF: Activity.java (android-36), linia 2889 — `onStop`.
    // RISK: după `onStop`, activitatea poate fi distrusă fără `onDestroy` garantat.
    @Override
    protected void onStop() {
        super.onStop();                                 // framework-ul își face tranziția
        Log.w(TAG, "onStop");
    }

    // ==================================================================================
    // METODA 5/7: onPause() — PIERDEREA FOCUSULUI
    // ==================================================================================
    // THEORY: activitatea își pierde focusul: altă activitate vine peste ea, dialog,
    //         share sheet, Home. Poate fi încă vizibilă.
    // LOGIC: `super` + un rând în Logcat.
    // REF: Activity.java (android-36), linia 2660 — `onPause`.
    // RISK: `onPause` blochează tranziția către activitatea următoare. Orice lucrezi
    //       aici întârzie ecranul următor.
    @Override
    protected void onPause() {
        super.onPause();                                // framework-ul își face tranziția
        Log.w(TAG, "onPause");
    }

    // ==================================================================================
    // METODA 6/7: onResume() — DEVENIREA INTERACTIVĂ
    // ==================================================================================
    // THEORY: activitatea este în prim-plan și primește input. Este perechea lui
    //         `onPause`.
    // LOGIC: `super` + un rând în Logcat.
    // REF: Activity.java (android-36), linia 2226 — `onResume`.
    // RISK: aici poți porni animații și listeners; nu face treabă grea, pentru că
    //       întârzie primul frame interactiv.
    @Override
    protected void onResume() {
        super.onResume();                               // framework-ul își face tranziția
        Log.w(TAG, "onResume");
    }

    // ==================================================================================
    // METODA 7/7: pressMe(View) — UN `Intent` IMPLICIT
    // ==================================================================================
    // THEORY: `android:onClick="pressMe"` din layout leagă butonul de această metodă
    //         prin REFLECȚIE, nu la compilare. Framework-ul caută în Context-ul
    //         view-ului o metodă `public void` cu exact un parametru `View`.
    // LOGIC:
    //   1. Varianta 1 (comentată): ACTION_VIEW + https:// → browser.
    //   2. Varianta 2 (comentată): ACTION_DIAL + tel: → tastatură telefonică.
    //   3. Varianta 3 (activă): ACTION_SEND + text/plain → share sheet.
    // REF: Intent.java, Intent.ACTION_SEND, Intent.createChooser.
    // RISK: `Uri` NU e importat. Dacă decomentezi primele două variante, fișierul
    //       nu compilează: `cannot find symbol: class Uri`.
    public void pressMe(View view) {                    // semnătura e dictată de android:onClick="pressMe"
        // --------------------------------------------------------------------------
        // VARIANTA 1 (comentată): deschide un URL în browserul implicit.
        // --------------------------------------------------------------------------
        // `ACTION_VIEW` + `https://` = „arată-mi acest lucru".
        // Ar cere `import android.net.Uri;` — de aceea blocul e comentat.
  /*      Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse("https://developer.android.com"));
        startActivity(web);*/

        // --------------------------------------------------------------------------
        // VARIANTA 2 (comentată): deschide tastatura telefonică, fără să sune.
        // --------------------------------------------------------------------------
        // `ACTION_DIAL` NU cere permisiunea CALL_PHONE. Varianta care sună,
        // `ACTION_CALL`, ar cere-o.
        /*Intent call = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:0212345678"));
        startActivity(call);*/

        // --------------------------------------------------------------------------
        // VARIANTA 3 (activă): share sheet.
        // --------------------------------------------------------------------------
        Intent share = new Intent(Intent.ACTION_SEND);                  // acțiunea: „trimite"
        share.setType("text/plain");                                     // MIME: obligatoriu pentru rezolvare
        share.putExtra(Intent.EXTRA_TEXT, "Hello!");                     // payload-ul
        startActivity(Intent.createChooser(share, "Share via DMA"));     // chooser forțat, cu titlu

    }
}   // închide clasa MainActivity
