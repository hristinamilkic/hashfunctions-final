package com.hashanalysis.hash.implementations;

import com.hashanalysis.hash.BaseHashFunction;
import org.bouncycastle.crypto.digests.*;

public class HashImplementations {
    public static class MD5Hash extends BaseHashFunction {
        public MD5Hash() {
            super(new MD5Digest(), "MD5", 128);
        }
    }

    public static class SHA256Hash extends BaseHashFunction {
        public SHA256Hash() {
            super(new SHA256Digest(), "SHA-256", 256);
        }
    }

    public static class SHA3Hash extends BaseHashFunction {
        public SHA3Hash() {
            super(new SHA3Digest(256), "SHA-3", 256);
        }
    }

    public static class Blake2bHash extends BaseHashFunction {
        public Blake2bHash() {
            super(new Blake2bDigest(256), "Blake2b", 256);
        }
    }

    public static class Blake3Hash extends BaseHashFunction {
        public Blake3Hash() {
            super(new Blake3Digest(256), "Blake3", 256);
        }
    }

    public static class RIPEMD160Hash extends BaseHashFunction {
        public RIPEMD160Hash() {
            super(new RIPEMD160Digest(), "RIPEMD-160", 160);
        }
    }

    // Factory method to get all hash implementations
    public static BaseHashFunction[] getAllImplementations() {
        return new BaseHashFunction[] {
            new MD5Hash(),
            new SHA256Hash(),
            new SHA3Hash(),
            new Blake2bHash(),
            new Blake3Hash(),
            new RIPEMD160Hash()
        };
    }
} 