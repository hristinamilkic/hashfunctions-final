package com.hashanalysis.test;

import com.hashanalysis.hash.HashFunction;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CollisionTest {
    private final HashFunction hashFunction;
    private final int iterations;
    private final int threadCount;
    private final Random random;

    public CollisionTest(HashFunction hashFunction, int iterations, int threadCount) {
        this.hashFunction = hashFunction;
        this.iterations = iterations;
        this.threadCount = threadCount;
        this.random = new Random();
    }

    public CollisionTestResult runTests() {
        ConcurrentHashMap<String, AtomicInteger> hashCounts = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger totalCollisions = new AtomicInteger(0);

        for (int i = 0; i < iterations; i++) {
            executor.submit(() -> {
                byte[] data = generateRandomData(1024);
                String hash = hashFunction.hashToString(data);
                
                AtomicInteger count = hashCounts.computeIfAbsent(hash, k -> new AtomicInteger(0));
                int currentCount = count.incrementAndGet();
                
                if (currentCount > 1) {
                    totalCollisions.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        double collisionRate = (double) totalCollisions.get() / iterations;
        return new CollisionTestResult(hashFunction, totalCollisions.get(), collisionRate);
    }

    private CollisionData runBirthdayAttack() {
        Map<String, byte[]> hashes = new HashMap<>();
        int collisions = 0;
        
        // Birthday attack: generate random inputs until we find a collision
        for (int i = 0; i < iterations; i++) {
            byte[] input = generateRandomData(64);
            String hash = hashFunction.hashToString(input);
            
            if (hashes.containsKey(hash)) {
                collisions++;
                // Store the collision pair
                byte[] original = hashes.get(hash);
                if (collisions <= 10) { // Store only first 10 collisions
                    System.out.println("Birthday Attack Collision found:");
                    System.out.println("Input 1: " + bytesToHex(original));
                    System.out.println("Input 2: " + bytesToHex(input));
                    System.out.println("Hash: " + hash);
                }
            } else {
                hashes.put(hash, input);
            }
        }
        
        return new CollisionData(collisions, hashes);
    }

    private CollisionData runBitFlipTest() {
        Map<String, byte[]> hashes = new HashMap<>();
        int collisions = 0;
        
        // Generate base input
        byte[] baseInput = generateRandomData(64);
        
        // Try flipping different bits
        for (int i = 0; i < iterations; i++) {
            byte[] modified = baseInput.clone();
            // Flip random bits
            int bitsToFlip = random.nextInt(8) + 1; // Flip 1-8 bits
            for (int j = 0; j < bitsToFlip; j++) {
                int byteIndex = random.nextInt(modified.length);
                int bitIndex = random.nextInt(8);
                modified[byteIndex] ^= (1 << bitIndex);
            }
            
            String hash = hashFunction.hashToString(modified);
            if (hashes.containsKey(hash)) {
                collisions++;
                if (collisions <= 10) {
                    System.out.println("Bit Flip Collision found:");
                    System.out.println("Original: " + bytesToHex(baseInput));
                    System.out.println("Modified: " + bytesToHex(modified));
                    System.out.println("Hash: " + hash);
                }
            } else {
                hashes.put(hash, modified);
            }
        }
        
        return new CollisionData(collisions, hashes);
    }

    private CollisionData runSimilarInputTest() {
        Map<String, byte[]> hashes = new HashMap<>();
        int collisions = 0;
        
        // Generate base input
        byte[] baseInput = generateRandomData(64);
        
        // Try similar inputs (changing small portions)
        for (int i = 0; i < iterations; i++) {
            byte[] modified = baseInput.clone();
            // Change a small portion of the input
            int startIndex = random.nextInt(modified.length - 4);
            for (int j = 0; j < 4; j++) {
                modified[startIndex + j] = (byte) random.nextInt(256);
            }
            
            String hash = hashFunction.hashToString(modified);
            if (hashes.containsKey(hash)) {
                collisions++;
                if (collisions <= 10) {
                    System.out.println("Similar Input Collision found:");
                    System.out.println("Original: " + bytesToHex(baseInput));
                    System.out.println("Modified: " + bytesToHex(modified));
                    System.out.println("Hash: " + hash);
                }
            } else {
                hashes.put(hash, modified);
            }
        }
        
        return new CollisionData(collisions, hashes);
    }

    private CollisionData runPrefixCollisionTest() {
        Map<String, byte[]> hashes = new HashMap<>();
        int collisions = 0;
        
        // Try different prefixes
        byte[] baseInput = generateRandomData(32);
        
        for (int i = 0; i < iterations; i++) {
            byte[] input = new byte[64];
            // Generate random prefix
            byte[] prefix = generateRandomData(32);
            System.arraycopy(prefix, 0, input, 0, 32);
            System.arraycopy(baseInput, 0, input, 32, 32);
            
            String hash = hashFunction.hashToString(input);
            if (hashes.containsKey(hash)) {
                collisions++;
                if (collisions <= 10) {
                    System.out.println("Prefix Collision found:");
                    System.out.println("Input 1: " + bytesToHex(hashes.get(hash)));
                    System.out.println("Input 2: " + bytesToHex(input));
                    System.out.println("Hash: " + hash);
                }
            } else {
                hashes.put(hash, input);
            }
        }
        
        return new CollisionData(collisions, hashes);
    }

    private byte[] generateRandomData(int size) {
        byte[] data = new byte[size];
        random.nextBytes(data);
        return data;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    public static class CollisionData {
        private final int collisionCount;
        private final Map<String, byte[]> hashes;

        public CollisionData(int collisionCount, Map<String, byte[]> hashes) {
            this.collisionCount = collisionCount;
            this.hashes = hashes;
        }

        public int getCollisionCount() { return collisionCount; }
    }

    public static class CollisionTestResult {
        public final HashFunction hashFunction;
        public final int collisions;
        public final double collisionRate;

        public CollisionTestResult(HashFunction hashFunction, int collisions, double collisionRate) {
            this.hashFunction = hashFunction;
            this.collisions = collisions;
            this.collisionRate = collisionRate;
        }
    }
} 