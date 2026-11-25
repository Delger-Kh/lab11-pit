package mn.edu.must.sqat;

// ЛАБ 11 — B2: B1-ийн ЯГ УТГЫН assertion-ууд + ЗААГ УТГЫН тестүүд.
// Зорилго: "changed conditional boundary" (>= -> >, < -> <=) мутантуудыг
// алахын тулд нөхцөл бүрийн яг заагийг ба заагийн доод талыг тестлэнэ.

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ScholarshipCalculatorTest {

    private static final double EPS = 1e-9;

    ScholarshipCalculator calc;

    @BeforeEach
    void setUp() { calc = new ScholarshipCalculator(); }

    // =====================================================================
    // B1: ЯГ УТГЫН assertion-ууд (starter-ийн оролтууд)
    // =====================================================================

    @Test
    void letterGrade_returnsExactLetterForEachRange() {
        assertEquals("A", calc.letterGrade(95));
        assertEquals("B", calc.letterGrade(85));
        assertEquals("C", calc.letterGrade(75));
        assertEquals("D", calc.letterGrade(65));
        assertEquals("F", calc.letterGrade(30));
    }

    @Test
    void gpa_returnsExactAverage() {
        assertEquals(3.5, calc.gpa(new double[]{95, 85}), EPS); // (4 + 3) / 2
        assertEquals(1.5, calc.gpa(new double[]{75, 65}), EPS); // (2 + 1) / 2
        assertEquals(0.0, calc.gpa(new double[]{40}), EPS);     // F = 0
    }

    @Test
    void scholarship_returnsExactPercentForEachRule() {
        assertEquals(100, calc.scholarship(3.9, 16, false));
        assertEquals(70,  calc.scholarship(3.5, 13, false));
        assertEquals(50,  calc.scholarship(3.1, 5, false));
        assertEquals(0,   calc.scholarship(2.0, 1, false));
        assertEquals(0,   calc.scholarship(4.0, 20, true));
    }

    // =====================================================================
    // B2: letterGrade — 90/89.99, 80/79.99, 70/69.99, 60/59.99, 0, 100
    // =====================================================================

    @ParameterizedTest(name = "letterGrade({0}) = {1}")
    @CsvSource({
        "100,   A",   // дээд хязгаар (score > 100 -> >= 100 мутантыг алана)
        "90,    A",   // A-гийн зааг (>= 90 -> > 90)
        "89.99, B",   // заагийн доор
        "80,    B",
        "79.99, C",
        "70,    C",
        "69.99, D",
        "60,    D",
        "59.99, F",
        "0,     F"    // доод хязгаар (score < 0 -> <= 0 мутантыг алана)
    })
    void letterGrade_boundaries(double score, String expected) {
        assertEquals(expected, calc.letterGrade(score));
    }

    @ParameterizedTest(name = "letterGrade({0}) throws")
    @ValueSource(doubles = {-1, -0.01, 100.01, 101})
    void letterGrade_outOfRange_throws(double score) {
        assertThrows(IllegalArgumentException.class, () -> calc.letterGrade(score));
    }

    // =====================================================================
    // B2: gpa — switch-ийн бүх үсгэн дүн тус бүрдээ
    // =====================================================================

    @ParameterizedTest(name = "gpa([{0}]) = {1}")
    @CsvSource({
        "95, 4.0",   // A
        "85, 3.0",   // B
        "75, 2.0",   // C
        "65, 1.0",   // D
        "30, 0.0"    // F (default салаа)
    })
    void gpa_eachLetterHasExactPoints(double score, double expected) {
        assertEquals(expected, calc.gpa(new double[]{score}), EPS);
    }

    @Test
    void gpa_allFiveLetters_average() {
        // (4 + 3 + 2 + 1 + 0) / 5 = 2.0 — давталт бүх элементийг нийлүүлж байгааг шалгана
        assertEquals(2.0, calc.gpa(new double[]{95, 85, 75, 65, 30}), EPS);
    }

    @Test
    void gpa_invalidInputs_throw() {
        assertThrows(IllegalArgumentException.class, () -> calc.gpa(null));
        assertThrows(IllegalArgumentException.class, () -> calc.gpa(new double[]{}));
        assertThrows(IllegalArgumentException.class, () -> calc.gpa(new double[]{90, 101}));
    }

    // =====================================================================
    // B2: scholarship — 3.8/15, 3.4/12, 3.0 заагууд ба оролтын хязгаар
    // =====================================================================

    @ParameterizedTest(name = "scholarship({0}, {1}) = {2}")
    @CsvSource({
        "3.8,  15, 100",  // 100%-ийн яг зааг (gpa >= 3.8, credits >= 15)
        "3.79, 15,  70",  // голч заагаас доор
        "3.8,  14,  70",  // кредит заагаас доор
        "4.0,  15, 100",  // голчийн дээд хязгаар
        "3.4,  12,  70",  // 70%-ийн яг зааг
        "3.39, 12,  50",  // голч заагаас доор
        "3.4,  11,  50",  // кредит заагаас доор
        "3.0,   0,  50",  // 50%-ийн яг зааг, кредит 0 (хүчинтэй)
        "2.99, 20,   0",  // 50%-ийн заагаас доор
        "0.0,   0,   0",  // голч 0, кредит 0 — доод хязгаарууд хүчинтэй
        "4.0,   0,  50"   // голч 4 хүчинтэй, кредит дутуу
    })
    void scholarship_boundaries(double gpa, int credits, int expected) {
        assertEquals(expected, calc.scholarship(gpa, credits, false));
    }

    @Test
    void scholarship_violationAlwaysZero_evenForTopStudent() {
        assertEquals(0, calc.scholarship(4.0, 20, true));
        assertEquals(0, calc.scholarship(3.8, 15, true));
    }

    @ParameterizedTest(name = "scholarship({0}, {1}) throws")
    @CsvSource({
        "-0.01, 10",  // голч 0-ээс бага
        "4.01,  10",  // голч 4-өөс их
        "4.1,   10",
        "3.0,   -1"   // кредит сөрөг
    })
    void scholarship_invalidInputs_throw(double gpa, int credits) {
        assertThrows(IllegalArgumentException.class, () -> calc.scholarship(gpa, credits, false));
    }
}
