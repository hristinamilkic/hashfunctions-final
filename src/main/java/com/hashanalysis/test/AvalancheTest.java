package com.hashanalysis.test;

import com.hashanalysis.hash.HashFunction;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

public class AvalancheTest {
    private final HashFunction hashFunction;
    private final int iterations;
    private final Random random;

    public AvalancheTest(HashFunction hashFunction, int iterations) {
        this.hashFunction = hashFunction;
        this.iterations = iterations;
        this.random = new Random();
    }

    public AvalancheTestResult runTests() {
        DoubleAdder totalAvalancheEffect = new DoubleAdder();
        DoubleAdder totalSquaredDiff = new DoubleAdder();
        AtomicInteger totalBits = new AtomicInteger(0);

        for (int i = 0; i < iterations; i++) {
            byte[] originalData = generateRandomData(1024);
            byte[] modifiedData = flipRandomBit(originalData);

            byte[] originalHash = hashFunction.hash(originalData);
            byte[] modifiedHash = hashFunction.hash(modifiedData);

            int bitDifferences = countBitDifferences(originalHash, modifiedHash);
            totalAvalancheEffect.add((double) bitDifferences / (originalHash.length * 8));
            totalBits.addAndGet(originalHash.length * 8);

            // Calculate squared difference for standard deviation
            double expectedBits = originalHash.length * 4; // Expected 50% bit changes
            double diff = bitDifferences - expectedBits;
            totalSquaredDiff.add(diff * diff);
        }

        double avalancheEffect = totalAvalancheEffect.sum() / iterations;
        double standardDeviation = Math.sqrt(totalSquaredDiff.sum() / iterations);

        return new AvalancheTestResult(hashFunction, avalancheEffect, standardDeviation);
    }

    private byte[] generateRandomData(int size) {
        byte[] data = new byte[size];
        random.nextBytes(data);
        return data;
    }

    private byte[] flipRandomBit(byte[] data) {
        byte[] modified = data.clone();
        int byteIndex = random.nextInt(data.length);
        int bitIndex = random.nextInt(8);
        modified[byteIndex] ^= (1 << bitIndex);
        return modified;
    }

    private int countBitDifferences(byte[] a, byte[] b) {
        int differences = 0;
        for (int i = 0; i < a.length; i++) {
            differences += Integer.bitCount(a[i] ^ b[i]);
        }
        return differences;
    }

    public static class AvalancheTestResult {
        public final HashFunction hashFunction;
        public final double avalancheEffect;
        public final double standardDeviation;

        public AvalancheTestResult(HashFunction hashFunction, double avalancheEffect, double standardDeviation) {
            this.hashFunction = hashFunction;
            this.avalancheEffect = avalancheEffect;
            this.standardDeviation = standardDeviation;
        }
    }
} 