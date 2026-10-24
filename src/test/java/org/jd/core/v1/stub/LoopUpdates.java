package org.jd.core.v1.stub;

import java.util.Objects;

/** Loops whose update must survive an inner loop which only ends with a 'break' */
public class LoopUpdates {
    /** The shape of Guava's ImmutableSet.RegularSetBuilderImpl.rebuildHashTable: the update 'i++' follows the 'break' of the inner loop */
    public static Object[] rebuildHashTable(int newTableSize, Object[] elements, int n) {
        Object[] hashTable = new Object[newTableSize];
        int mask = hashTable.length - 1;

        for (int i = 0; i < n; i++) {
            Object e = Objects.requireNonNull(elements[i]);

            for (int j = e.hashCode(); ; j++) {
                int index = j & mask;

                if (hashTable[index] == null) {
                    hashTable[index] = e;
                    break;
                }
            }
        }
        return hashTable;
    }

    /** The same with a 'while (true)' and a counter which is updated on the line of the header */
    public static int countStored(int[] table, int n) {
        int stored = 0;

        for (int i = 0; i < n; i++) {
            int j = i;

            while (true) {
                if (table[j & 3] == 0) {
                    table[j & 3] = i + 1;
                    stored++;
                    break;
                }
                j++;
            }
        }
        return stored;
    }

    /** The shape of Guava's CharMatcher.removeFrom: the inner loop breaks to the outer one, which only advances the position and the spread */
    public static String removeFrom(String string, java.util.function.IntPredicate matcher, int first) {
        char[] chars = string.toCharArray();
        int pos = first;
        int spread = 1;

        OUT:
        while (true) {
            pos++;
            while (true) {
                if (pos == chars.length) {
                    break OUT;
                }
                if (matcher.test(chars[pos])) {
                    break;
                }
                chars[pos - spread] = chars[pos];
                pos++;
            }
            spread++;
        }
        return new String(chars, 0, pos - spread);
    }
}
