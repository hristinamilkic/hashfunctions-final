package com.hashanalysis.hash;

import java.nio.ByteBuffer;

public interface HashFunction {
    String getName();
    byte[] hash(byte[] input);
    String hashToString(byte[] input);
    int getHashLength();
    double getSpeed(byte[] input);
    double getAvalancheEffect(byte[] input);
    double getCollisionRate(byte[] input);
} 