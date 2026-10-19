package avilaos.tests;

public final class TestSupport {
    private static int checks;

    private TestSupport() { }

    public static void equal(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Esperado: " + expected + "; recibido: " + actual);
        }
        checks++;
    }

    public static void isTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("La condición debía ser verdadera");
        }
        checks++;
    }

    public static void throwsType(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (!type.isInstance(failure)) {
                throw new AssertionError("Se esperaba " + type.getSimpleName(), failure);
            }
            checks++;
            return;
        }
        throw new AssertionError("No se lanzó " + type.getSimpleName());
    }

    public static int count() { return checks; }
}
