package com.hashanalysis.hash;

import org.bouncycastle.crypto.Digest;
import java.util.HexFormat;

public abstract class BaseHashFunction implements HashFunction {
    protected final Digest digest;
    protected final String name;
    protected final int hashLength;
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    protected BaseHashFunction(Digest digest, String name, int hashLength) {
        this.digest = digest;
        this.name = name;
        this.hashLength = hashLength;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public byte[] hash(byte[] input) {
        digest.reset();
        digest.update(input, 0, input.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        return hash;
    }

    @Override
    public String hashToString(byte[] input) {
        return HEX_FORMAT.formatHex(hash(input));
    }

    @Override
    public int getHashLength() {
        return hashLength;
    }

    @Override
    public double getSpeed(byte[] input) {
        // Warm up
        for (int i = 0; i < 1000; i++) {
            hash(input);
        }
        
        // Actual measurement
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            hash(input);
        }
        long endTime = System.nanoTime();
        
        return (endTime - startTime) / (1000.0 * 1_000_000.0);
    }

    @Override
    public double getAvalancheEffect(byte[] input) {
        if (input.length == 0) return 0;
        
        byte[] originalHash = hash(input);
        int totalBitChanges = 0;
        int totalBits = originalHash.length * 8;
        int samples = Math.min(input.length * 8, 1000); 
        for (int i = 0; i < samples; i++) {
            int byteIndex = i / 8;
            int bitIndex = i % 8;
            
            byte[] modifiedInput = input.clone();
            modifiedInput[byteIndex] ^= (1 << bitIndex);
            
            byte[] modifiedHash = hash(modifiedInput);
            int bitChanges = 0;
            
            for (int j = 0; j < originalHash.length; j++) {
                int xor = originalHash[j] ^ modifiedHash[j];
                while (xor != 0) {
                    bitChanges += xor & 1;
                    xor >>>= 1;
                }
            }
            
            totalBitChanges += bitChanges;
        }
        
        return (double) totalBitChanges / (samples * totalBits);
    }

    @Override
    public double getCollisionRate(byte[] input) {
        if (input.length == 0) return 0;
        
        int numTests = 1000;
        int collisions = 0;
        byte[] originalHash = hash(input);
        
        for (int i = 0; i < numTests; i++) {
            byte[] modifiedInput = input.clone();
            // random bit
            int byteIndex = (int) (Math.random() * input.length);
            int bitIndex = (int) (Math.random() * 8);
            modifiedInput[byteIndex] ^= (1 << bitIndex);
            
            byte[] modifiedHash = hash(modifiedInput);
            if (java.util.Arrays.equals(originalHash, modifiedHash)) {
                collisions++;
            }
        }
        
        return (double) collisions / numTests;
    }
} 