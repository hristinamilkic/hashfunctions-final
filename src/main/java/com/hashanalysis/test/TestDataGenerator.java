package com.hashanalysis.test;

import java.util.Random;

public class TestDataGenerator {
    private final Random random;

    public TestDataGenerator() {
        this.random = new Random();
    }

    public byte[] generateRandomData(int size) {
        byte[] data = new byte[size];
        random.nextBytes(data);
        return data;
    }

    public byte[] generateSequentialData(int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) (i % 256);
        }
        return data;
    }

    public byte[] generateAlternatingData(int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) ((i % 2 == 0) ? 0x55 : 0xAA);
        }
        return data;
    }

    public byte[] generateRepeatingPattern(int size, byte[] pattern) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = pattern[i % pattern.length];
        }
        return data;
    }

    public byte[] generateBitFlipData(byte[] original, int numBits) {
        byte[] data = original.clone();
        for (int i = 0; i < numBits; i++) {
            int byteIndex = random.nextInt(data.length);
            int bitIndex = random.nextInt(8);
            data[byteIndex] ^= (1 << bitIndex);
        }
        return data;
    }

    public byte[] generateSimilarData(byte[] original, double similarity) {
        byte[] data = original.clone();
        int bitsToChange = (int) ((1 - similarity) * data.length * 8);
        return generateBitFlipData(data, bitsToChange);
    }

    public byte[] generateKnownTestVector(String name) {
        switch (name.toLowerCase()) {
            case "empty":
                return new byte[0];
            case "all_zeros":
                return new byte[64];
            case "all_ones":
                byte[] ones = new byte[64];
                for (int i = 0; i < ones.length; i++) {
                    ones[i] = (byte) 0xFF;
                }
                return ones;
            case "alternating":
                return generateAlternatingData(64);
            case "sequential":
                return generateSequentialData(64);
            default:
                return generateRandomData(64);
        }
    }
} 