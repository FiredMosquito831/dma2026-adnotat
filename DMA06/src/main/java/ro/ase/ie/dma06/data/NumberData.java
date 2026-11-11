/*
 * ======================================================================================
 * FILE: NumberData.java
 * ======================================================================================
 * LOCATION: DMA06/src/main/java/ro/ase/ie/dma06/data/NumberData.java
 * PACKAGE:  ro.ase.ie.data          ← WATCH OUT: it does not contain `dma06`, although the folder does
 * ROLE:     the only data source of the app. It produces a list with the numbers 1..100.
 *           It does not know what a View, a Context, an Adapter or an activity is.
 *
 * FILE STRUCTURE (3 parts):
 *   1. constants      — 2 lines: FIRST_NUMBER, LAST_NUMBER
 *   2. constructor    — private and empty, with a comment that explains why it is empty
 *   3. getNumbers()   — 7 lines: loop and return
 *
 * WHAT YOU NEED TO KNOW BEFOREHAND:
 *   - `final` on the class + `private` constructor + `static` methods = utility class.
 *   - The method returns a NEW list on every call, not a shared one.
 *   - Zero `android.*` imports: this class would also run on the desktop.
 * ======================================================================================
 */

// ======================================================================================
// BLOCK 1 — THE PACKAGE AND THE IMPORTS
// ======================================================================================
// The declared package does NOT contain `dma06`, although the folder is ro/ase/ie/dma06/data/.
// It is not a typo: that is how it is in the repo, and the same holds for the two
// adapters, which declare ro.ase.ie.adapters.
//
// Why it compiles: Gradle gives the compiler the explicit list of source files, and
// javac does not require the folder to match the package when the files are passed directly.
// The folder–package match matters only when the compiler looks for a class on the classpath.
//
// The practical consequence: imports use the declared package, not the folder path.
//   import ro.ase.ie.data.NumberData;          ← correct, that is how it is in the repo
//   import ro.ase.ie.dma06.data.NumberData;    ← wrong, the class does not exist there
//
// At runtime nothing changes: the ClassLoader loads by the name in the package, not by the
// folder path. Folders are only organization on disk.
// ======================================================================================

package ro.ase.ie.data;          // WATCH OUT: the declared package does NOT contain `dma06`

import java.util.ArrayList;      // the concrete implementation of the list
import java.util.List;           // the interface used in signatures

// Two imports, both from java.util. Zero android.* imports: the class could run
// in an ordinary JUnit test, without an emulator. The package split is not cosmetic —
// it allows the source to be tested without Android.

// ======================================================================================
// THEORY BLOCK: THE UTILITY CLASS
// ======================================================================================
/*
 * 1. WHAT IT IS
 *    A class that has no state and no instances. All the methods are `static`, so
 *    they are called on the class: `NumberData.getNumbers()`, never `new NumberData().get...`.
 *
 * 2. WHY A PRIVATE CONSTRUCTOR
 *    -   `private NumberData() {}` blocks `new NumberData()` from any other class.
 *    -   Without it, the compiler would generate an implicit public constructor and anyone
 *        could create pointless instances.
 *    -   It is not an error to have useless instances; it is noise. And noise hides intent.
 *    -   The private constructor also blocks inheritance: a subclass would have to call
 *        `super()`, which is inaccessible. That is why `final` on the class is almost redundant —
 *        but we write it anyway, to be explicit.
 *
 * 3. WHY STATIC METHODS
 *    -   They do not depend on any instance state.
 *    -   They are loaded once, with the class, not with every object.
 *    -   They can be called from any context, including from a JUnit test without Android.
 *
 * 4. ALTERNATIVES AND WHY THEY ARE NOT USED HERE
 *    -   Singleton: it would impose a shared instance. Here every screen wants ITS OWN list.
 *    -   Interface with `default` methods: there is nothing to implement — there is no contract,
 *        there is a single function.
 *    -   Class with a public constructor: it would have allowed `new NumberData()`, which makes no sense.
 *
 * 5. THE RULE YOU CAN SEE IN THE CODE
 *    `final class` + `private constructor` + `static methods` = the same combination as in
 *    java.lang.Math, java.util.Collections, java.util.Arrays. It is not a seminar trick;
 *    it is the pattern from the JDK.
 *
 * DIAGRAM:
 *
 *   NumberData                    (the class; it is never instantiated)
 *   ├── FIRST_NUMBER = 1          static final
 *   ├── LAST_NUMBER  = 100        static final
 *   ├── NumberData()              private  ← blocks `new`
 *   └── getNumbers()              static   ← the only access point
 */

/** The data source shared by all three steps. */
public final class NumberData {  // final: it cannot be extended; a utility class has nothing to inherit

    // ==================================================================================
    // BLOCK 2 — THE CONSTANTS
    // ==================================================================================
    // private  — nobody outside needs them
    // static   — they belong to the class, not to an instance (and there are no instances anyway)
    // final    — they never change; the compiler can substitute the value directly
    //
    // Why two constants and not 100 written directly: if you want 1–1000, you change a single
    // line. And the loop becomes readable: "from FIRST_NUMBER to LAST_NUMBER".
    // ==================================================================================
    private static final int FIRST_NUMBER = 1;    // the first number in the list
    private static final int LAST_NUMBER = 100;   // the last number, INCLUSIVE

    // ==================================================================================
    // BLOCK 3 — THE PRIVATE CONSTRUCTOR
    // ==================================================================================
    // Theory:  blocks instantiation; the class has only static methods.
    // Logic:   the body is intentionally empty; the comment documents the decision.
    // Risk:    if you remove it, the compiler generates an implicit public one and `new`
    //          becomes legal.
    // Ref:     course 2 (classes and objects).
    // ==================================================================================
    private NumberData() {
        // This class only has static methods
    }

    // Why the constructor is empty and does not throw: some utility classes write
    // `throw new AssertionError()` in the constructor, to catch calls made through reflection.
    // There is no need here: the constructor is private, and reflection is not a lab scenario.
    //
    // The comment inside — "This class only has static methods" — documents
    // WHY it is empty. An empty private constructor with no explanation looks like an oversight.

    // ==================================================================================
    // BLOCK 4 — getNumbers()
    // ==================================================================================
    // Theory:  a static method that builds a collection; it touches no state.
    // Logic:   1. create ArrayList  2. loop from 1 to 100  3. add  4. return
    // Risk:    if you returned a shared list (a static field), the changes made in one
    //          screen would show up in all the others.
    // Ref:     chapter 04 (MainActivity - list).
    // ==================================================================================
    public static List<Integer> getNumbers() {
        List<Integer> numbers = new ArrayList<>();     // 1. a new list on every call
        for (int number = FIRST_NUMBER; number <= LAST_NUMBER; number++) {   // 2. 1..100
            numbers.add(number);                       // 3. autoboxing: int → Integer
        }
        return numbers;                                // 4. 100 elements
    }

    // A new list on every call — this is essential. There is no shared static field.
    // Each of the three activities calls getNumbers() in onCreate and receives
    // ITS OWN list. If it were a singleton, a change made in one screen would
    // show up in all the others, without you knowing where it came from.
    //
    // List in the signature, ArrayList in the implementation — you return the interface, you build
    // the implementation. Whoever receives the list does not depend on ArrayList; you can switch to
    // LinkedList without touching anything else. The rule is called "program to an interface".
    //
    // new ArrayList<>() with an empty diamond — the generic type is inferred from context (Java 7+).
    //
    // `<=` and not `<` — LAST_NUMBER is INCLUSIVE. The loop gives exactly 100 elements: 1..100.
    // With `<` you would have 99 — visible at once, because the last row would be 99.
    //
    // Autoboxing — numbers.add(number) receives an int, but the list is List<Integer>.
    // The compiler automatically converts it with Integer.valueOf(number). For the values
    // −128…127, Integer.valueOf returns cached instances, so for 1–100 no
    // 100 new objects are created.
    //
    // Why Integer and not int: Java generics do not accept primitive types. List<int>
    // does not compile. Hence the autoboxing, hence Integer in getItem in the adapters.
    //
    // ==================================================================================
    // WHEN THE CLASS IS LOADED
    // ==================================================================================
    //
    //   The first call to NumberData.getNumbers()
    //     → the ClassLoader loads NumberData
    //     → it initializes the static fields: FIRST_NUMBER = 1, LAST_NUMBER = 100
    //     → only then does it execute the body of the method
    //
    // Initialization happens once per process, not on every call. That is why
    // the constants can be read without any worry about order: they are ready before the first
    // method.
    //
    // ==================================================================================
    // WHY IT IS EASY TO TEST
    // ==================================================================================
    //
    //   @Test
    //   public void getNumbers_returnsHundredElements() {
    //       List<Integer> numbers = NumberData.getNumbers();
    //       assertEquals(100, numbers.size());
    //       assertEquals(Integer.valueOf(1), numbers.get(0));
    //       assertEquals(Integer.valueOf(100), numbers.get(99));
    //   }
    //
    // The test runs on the JVM, without an emulator, because no android.* type appears
    // in the file. If NumberData had received a Context to read resources, the test
    // would have needed Robolectric or a device. The package split is not
    // cosmetic: it decides whether you can test in 50 milliseconds or in 50 seconds.
}
