package tests.targets.security;

import dalvik.annotation.TestTargetClass;


@TestTargetClass(value=targets.MessageDigests.SHA_1.class)
public class MessageDigestTestSHA1 extends MessageDigestTest {

    public MessageDigestTestSHA1() {
        super("SHA-1");
        super.source1 = "abc";
        super.source2 = "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq";
        super.source3 = getLongMessage(1000000);
        super.expected1 = singleblock;
        super.expected2 = multiblock;
        super.expected3 = longmessage;
    }

    // results from fips180-2
    String singleblock = "a9993e364706816aba3e25717850c26c9cd0d89d";
    String multiblock = "84983e441c3bd26ebaae4aa1f95129e5e54670f1";
    String longmessage = "34aa973cd4c4daa4f61eeb2bdbad27316534016f";

}
