package learning.fintech;

import java.math.BigDecimal;

public final class WorkspaceJoinTest {
    public static void main(String[] args) {
        assertEquals("allow", WorkspaceJoin.decision("Ada@schoolbank.example", "schoolbank.example",
                new BigDecimal("250.00")));
        assertEquals("review", WorkspaceJoin.decision("ada@schoolbank.example", "schoolbank.example",
                new BigDecimal("10000")));
        try {
            WorkspaceJoin.decision("ada@other.example", "schoolbank.example", BigDecimal.ONE);
            throw new AssertionError("Mismatched company email admitted");
        } catch (IllegalArgumentException expected) {
            assertEquals("Employee email must match the company domain", expected.getMessage());
        }
        System.out.println("WorkspaceJoinTest passed");
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual);
    }
}
