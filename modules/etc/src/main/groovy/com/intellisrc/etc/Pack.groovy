package com.intellisrc.etc

import groovy.transform.CompileStatic

/**
 * imported from: org.bouncycastle.util
 * https://github.com/bcgit/bc-java/blob/master/core/src/main/java/org/bouncycastle/util/Pack.java
 * Utility methods for converting byte arrays into ints and longs, and back again.
 */
@CompileStatic
class Pack {
    static short bigEndianToShort(byte[] bs, int off) {
        int n = (bs[off] & 0xff) << 8
        n |= (bs[++off] & 0xff)
        return (short) n
    }

    static int bigEndianToInt(byte[] bs, int off) {
        int n = bs[off] << 24
        n |= (bs[++off] & 0xff) << 16
        n |= (bs[++off] & 0xff) << 8
        n |= (bs[++off] & 0xff)
        return n
    }

    static void bigEndianToInt(byte[] bs, int off, int[] ns) {
        for (int i = 0; i < ns.length; ++i) {
            ns[i] = bigEndianToInt(bs, off)
            off += 4
        }
    }

    static byte[] intToBigEndian(int n) {
        byte[] bs = new byte[4]
        intToBigEndian(n, bs, 0)
        return bs
    }

    static void intToBigEndian(int n, byte[] bs, int off) {
        bs[off] = (byte) (n >>> 24)
        bs[++off] = (byte) (n >>> 16)
        bs[++off] = (byte) (n >>> 8)
        bs[++off] = (byte) (n)
    }

    static byte[] intToBigEndian(int[] ns) {
        byte[] bs = new byte[4 * ns.length]
        intToBigEndian(ns, bs, 0)
        return bs
    }

    static void intToBigEndian(int[] ns, byte[] bs, int off) {
        for (int i = 0; i < ns.length; ++i) {
            intToBigEndian(ns[i], bs, off)
            off += 4
        }
    }

    static long bigEndianToLong(byte[] bs, int off) {
        int hi = bigEndianToInt(bs, off)
        int lo = bigEndianToInt(bs, off + 4)
        return ((long) (hi & 0xffffffffL) << 32) | (long) (lo & 0xffffffffL)
    }

    static void bigEndianToLong(byte[] bs, int off, long[] ns) {
        for (int i = 0; i < ns.length; ++i) {
            ns[i] = bigEndianToLong(bs, off)
            off += 8
        }
    }

    static byte[] longToBigEndian(long n) {
        byte[] bs = new byte[8]
        longToBigEndian(n, bs, 0)
        return bs
    }

    static void longToBigEndian(long n, byte[] bs, int off) {
        intToBigEndian((int) (n >>> 32), bs, off)
        intToBigEndian((int) (n & 0xffffffffL), bs, off + 4)
    }

    static byte[] longToBigEndian(long[] ns) {
        byte[] bs = new byte[8 * ns.length]
        longToBigEndian(ns, bs, 0)
        return bs
    }

    static void longToBigEndian(long[] ns, byte[] bs, int off) {
        for (int i = 0; i < ns.length; ++i) {
            longToBigEndian(ns[i], bs, off)
            off += 8
        }
    }

    static short littleEndianToShort(byte[] bs, int off) {
        int n = bs[off] & 0xff
        n |= (bs[++off] & 0xff) << 8
        return (short) n
    }

    static int littleEndianToInt(byte[] bs, int off) {
        int n = bs[off] & 0xff
        n |= (bs[++off] & 0xff) << 8
        n |= (bs[++off] & 0xff) << 16
        n |= bs[++off] << 24
        return n
    }

    static void littleEndianToInt(byte[] bs, int off, int[] ns) {
        for (int i = 0; i < ns.length; ++i) {
            ns[i] = littleEndianToInt(bs, off)
            off += 4
        }
    }

    static void littleEndianToInt(byte[] bs, int bOff, int[] ns, int nOff, int count) {
        for (int i = 0; i < count; ++i) {
            ns[nOff + i] = littleEndianToInt(bs, bOff)
            bOff += 4
        }
    }

    static int[] littleEndianToInt(byte[] bs, int off, int count) {
        int[] ns = new int[count]
        for (int i = 0; i < ns.length; ++i) {
            ns[i] = littleEndianToInt(bs, off)
            off += 4
        }
        return ns
    }

    static byte[] shortToLittleEndian(short n) {
        byte[] bs = new byte[2]
        shortToLittleEndian(n, bs, 0)
        return bs
    }

    static void shortToLittleEndian(short n, byte[] bs, int off) {
        bs[off] = (byte) (n)
        bs[++off] = (byte) (n >>> 8)
    }

    static byte[] intToLittleEndian(int n) {
        byte[] bs = new byte[4]
        intToLittleEndian(n, bs, 0)
        return bs
    }

    static void intToLittleEndian(int n, byte[] bs, int off) {
        bs[off] = (byte) (n)
        bs[++off] = (byte) (n >>> 8)
        bs[++off] = (byte) (n >>> 16)
        bs[++off] = (byte) (n >>> 24)
    }

    static byte[] intToLittleEndian(int[] ns) {
        byte[] bs = new byte[4 * ns.length]
        intToLittleEndian(ns, bs, 0)
        return bs
    }

    static void intToLittleEndian(int[] ns, byte[] bs, int off) {
        for (int i = 0; i < ns.length; ++i) {
            intToLittleEndian(ns[i], bs, off)
            off += 4
        }
    }

    static long littleEndianToLong(byte[] bs, int off) {
        int lo = littleEndianToInt(bs, off)
        int hi = littleEndianToInt(bs, off + 4)
        return ((long) (hi & 0xffffffffL) << 32) | (long) (lo & 0xffffffffL)
    }

    static void littleEndianToLong(byte[] bs, int off, long[] ns) {
        for (int i = 0; i < ns.length; ++i) {
            ns[i] = littleEndianToLong(bs, off)
            off += 8
        }
    }

    static byte[] longToLittleEndian(long n) {
        byte[] bs = new byte[8]
        longToLittleEndian(n, bs, 0)
        return bs
    }

    static void longToLittleEndian(long n, byte[] bs, int off) {
        intToLittleEndian((int) (n & 0xffffffffL), bs, off)
        intToLittleEndian((int) (n >>> 32), bs, off + 4)
    }

    static byte[] longToLittleEndian(long[] ns) {
        byte[] bs = new byte[8 * ns.length]
        longToLittleEndian(ns, bs, 0)
        return bs
    }

    static void longToLittleEndian(long[] ns, byte[] bs, int off) {
        for (int i = 0; i < ns.length; ++i) {
            longToLittleEndian(ns[i], bs, off)
            off += 8
        }
    }
}
