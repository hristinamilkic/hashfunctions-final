package com.hashanalysis.benchmark;

import com.hashanalysis.hash.HashFunction;
import com.hashanalysis.hash.implementations.HashImplementations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class HashBenchmark {
    private final List<HashFunction> hashFunctions;
    private final Map<String, List<BenchmarkResult>> results;

    public HashBenchmark() {
        this.hashFunctions = Arrays.asList(HashImplementations.getAllImplementations());
        this.results = new HashMap<>();
    }

    public void runBenchmark(Path filePath) throws IOException {
        byte[] data = Files.readAllBytes(filePath);
        String fileName = filePath.getFileName().toString();

        for (HashFunction hashFunction : hashFunctions) {
            BenchmarkResult result = new BenchmarkResult(
                hashFunction.getName(),
                hashFunction.getSpeed(data),
                hashFunction.getAvalancheEffect(data),
                hashFunction.getHashLength(),
                hashFunction.getCollisionRate(data)
            );

            results.computeIfAbsent(fileName, k -> new ArrayList<>()).add(result);
        }
    }

    public Map<String, List<BenchmarkResult>> getResults() {
        return results;
    }

    public static class BenchmarkResult {
        private final String hashName;
        private final double speed; // millisekunde po operaciji
        private final double avalancheEffect; // 0.0 to 1.0
        private final int hashLength;
        private final double collisionRate; // 0.0 to 1.0

        public BenchmarkResult(String hashName, double speed, double avalancheEffect, int hashLength, double collisionRate) {
            this.hashName = hashName;
            this.speed = speed;
            this.avalancheEffect = avalancheEffect;
            this.hashLength = hashLength;
            this.collisionRate = collisionRate;
        }

        public String getHashName() { return hashName; }
        public double getSpeed() { return speed; }
        public double getAvalancheEffect() { return avalancheEffect; }
        public int getHashLength() { return hashLength; }
        public double getCollisionRate() { return collisionRate; }
    }
} 