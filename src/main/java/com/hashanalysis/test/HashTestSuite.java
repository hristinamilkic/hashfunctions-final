package com.hashanalysis.test;

import com.hashanalysis.hash.HashFunction;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class HashTestSuite {
    private final HashFunction hashFunction;
    private final int iterations;
    private final int threadCount;

    public HashTestSuite(HashFunction hashFunction, int iterations, int threadCount) {
        this.hashFunction = hashFunction;
        this.iterations = iterations;
        this.threadCount = threadCount;
    }

    public TestResults runAllTests() {
        TestResults results = new TestResults(hashFunction.getName());
        
        //  all tests
        results.setSpeedTest(runSpeedTest());
        results.setCollisionTest(runCollisionTest());
        results.setAvalancheTest(runAvalancheTest());
        results.setPopularTests(runPopularTests());
        
        return results;
    }

    private SpeedTestResult runSpeedTest() {
        // test with different input sizes
        Map<Integer, Double> speeds = new HashMap<>();
        int[] sizes = {64, 128, 256, 512, 1024, 4096, 8192, 16384, 32768, 65536};
        
        for (int size : sizes) {
            byte[] data = generateRandomData(size);
            double speed = measureSpeed(data);
            speeds.put(size, speed);
        }
        
        return new SpeedTestResult(speeds);
    }

    private CollisionTestResult runCollisionTest() {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<CollisionData>> futures = new ArrayList<>();
        
        // gen. random inputs in parallel
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> findCollisions()));
        }
        
     // collect 
        List<CollisionData> collisionData = new ArrayList<>();
        for (Future<CollisionData> future : futures) {
            try {
                collisionData.add(future.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        executor.shutdown();
        
        // collision data
        int totalCollisions = collisionData.stream()
            .mapToInt(CollisionData::getCollisionCount)
            .sum();
            
        double collisionRate = (double) totalCollisions / (iterations * threadCount);
        
        return new CollisionTestResult(totalCollisions, collisionRate, collisionData);
    }

    private AvalancheTestResult runAvalancheTest() {
        Map<Integer, Double> avalancheEffects = new HashMap<>();
        int[] bitPositions = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512};
        
        for (int bits : bitPositions) {
            double effect = measureAvalancheEffect(bits);
            avalancheEffects.put(bits, effect);
        }
        
        return new AvalancheTestResult(avalancheEffects);
    }

    private PopularTestResult runPopularTests() {
        Map<String, Boolean> results = new HashMap<>();
        
        // Test 1: Empty string
        results.put("Empty String", testEmptyString());
        
        // Test 2: All zeros
        results.put("All Zeros", testAllZeros());
        
        // Test 3: All ones
        results.put("All Ones", testAllOnes());
        
        // Test 4: Alternating bits
        results.put("Alternating Bits", testAlternatingBits());
        
        // Test 5: Known test vectors
        results.put("Known Test Vectors", testKnownVectors());
        
        return new PopularTestResult(results);
    }

    private double measureSpeed(byte[] data) {
        // Warm up
        for (int i = 0; i < 1000; i++) {
            hashFunction.hash(data);
        }
        
        // Actual measurement
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            hashFunction.hash(data);
        }
        long endTime = System.nanoTime();
        
        return (endTime - startTime) / (iterations * 1_000_000.0); // Convert to milliseconds
    }

    private CollisionData findCollisions() {
        Map<String, byte[]> hashes = new HashMap<>();
        int collisions = 0;
        
        for (int i = 0; i < iterations; i++) {
            byte[] input = generateRandomData(64);
            String hash = hashFunction.hashToString(input);
            
            if (hashes.containsKey(hash)) {
                collisions++;
            } else {
                hashes.put(hash, input);
            }
        }
        
        return new CollisionData(collisions, hashes);
    }

    private double measureAvalancheEffect(int bits) {
        byte[] original = generateRandomData(64);
        byte[] modified = original.clone();
        
        // Flip specified number of bits
        for (int i = 0; i < bits; i++) {
            int byteIndex = i / 8;
            int bitIndex = i % 8;
            modified[byteIndex] ^= (1 << bitIndex);
        }
        
        byte[] originalHash = hashFunction.hash(original);
        byte[] modifiedHash = hashFunction.hash(modified);
        
        int bitChanges = 0;
        for (int i = 0; i < originalHash.length; i++) {
            int xor = originalHash[i] ^ modifiedHash[i];
            while (xor != 0) {
                bitChanges += xor & 1;
                xor >>>= 1;
            }
        }
        
        return (double) bitChanges / (originalHash.length * 8);
    }

    private boolean testEmptyString() {
        byte[] empty = new byte[0];
        String hash = hashFunction.hashToString(empty);
        return hash != null && !hash.isEmpty();
    }

    private boolean testAllZeros() {
        byte[] zeros = new byte[64];
        String hash = hashFunction.hashToString(zeros);
        return hash != null && !hash.isEmpty();
    }

    private boolean testAllOnes() {
        byte[] ones = new byte[64];
        Arrays.fill(ones, (byte) 0xFF);
        String hash = hashFunction.hashToString(ones);
        return hash != null && !hash.isEmpty();
    }

    private boolean testAlternatingBits() {
        byte[] alternating = new byte[64];
        for (int i = 0; i < alternating.length; i++) {
            alternating[i] = (byte) ((i % 2 == 0) ? 0x55 : 0xAA);
        }
        String hash = hashFunction.hashToString(alternating);
        return hash != null && !hash.isEmpty();
    }

    private boolean testKnownVectors() {
        // Add known test vectors for each hash function
        Map<String, String> testVectors = getTestVectors();
        for (Map.Entry<String, String> entry : testVectors.entrySet()) {
            String input = entry.getKey();
            String expected = entry.getValue();
            String actual = hashFunction.hashToString(input.getBytes());
            if (!actual.equals(expected)) {
                return false;
            }
        }
        return true;
    }

    private Map<String, String> getTestVectors() {
        Map<String, String> vectors = new HashMap<>();
        // Example for MD5
        vectors.put("The quick brown fox jumps over the lazy dog", 
                   "9e107d9d372bb6826bd81d3542a419d6");
        // Add more test vectors for other hash functions
        return vectors;
    }

    private byte[] generateRandomData(int size) {
        byte[] data = new byte[size];
        new Random().nextBytes(data);
        return data;
    }

    // Result classes
    public static class TestResults {
        private final String hashName;
        private SpeedTestResult speedTest;
        private CollisionTestResult collisionTest;
        private AvalancheTestResult avalancheTest;
        private PopularTestResult popularTests;

        public TestResults(String hashName) {
            this.hashName = hashName;
        }

        // Getters and setters
        public void setSpeedTest(SpeedTestResult speedTest) { this.speedTest = speedTest; }
        public void setCollisionTest(CollisionTestResult collisionTest) { this.collisionTest = collisionTest; }
        public void setAvalancheTest(AvalancheTestResult avalancheTest) { this.avalancheTest = avalancheTest; }
        public void setPopularTests(PopularTestResult popularTests) { this.popularTests = popularTests; }
    }

    public static class SpeedTestResult {
        private final Map<Integer, Double> speeds;

        public SpeedTestResult(Map<Integer, Double> speeds) {
            this.speeds = speeds;
        }
    }

    public static class CollisionTestResult {
        private final int totalCollisions;
        private final double collisionRate;
        private final List<CollisionData> collisionData;

        public CollisionTestResult(int totalCollisions, double collisionRate, List<CollisionData> collisionData) {
            this.totalCollisions = totalCollisions;
            this.collisionRate = collisionRate;
            this.collisionData = collisionData;
        }
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

    public static class AvalancheTestResult {
        private final Map<Integer, Double> avalancheEffects;

        public AvalancheTestResult(Map<Integer, Double> avalancheEffects) {
            this.avalancheEffects = avalancheEffects;
        }
    }

    public static class PopularTestResult {
        private final Map<String, Boolean> results;

        public PopularTestResult(Map<String, Boolean> results) {
            this.results = results;
        }
    }
} 