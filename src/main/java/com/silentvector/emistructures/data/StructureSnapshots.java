package com.silentvector.emistructures.data;

public final class StructureSnapshots {
    private static volatile StructureSnapshot current = StructureSnapshot.EMPTY;

    private StructureSnapshots() {
    }

    public static StructureSnapshot current() {
        return current;
    }

    public static void publish(StructureSnapshot snapshot) {
        current = snapshot;
    }

    public static void clear() {
        current = StructureSnapshot.EMPTY;
    }
}
