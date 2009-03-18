package tests.targets.security;

import dalvik.annotation.TestTargetClass;


@TestTargetClass(value=targets.MessageDigests.SHA_256.class)
public class MessageDigestTestSHA256 extends MessageDigestTest {

    public MessageDigestTestSHA256() {
        super("SHA-256");
        super.source1 = "abc";
        super.source2 = "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq";
        super.source3 = getLongMessage(1000000);
        super.expected1 = singleblock;
        super.expected2 = multiblock;
        super.expected3 = longmessage;
    }

    // results from fips180-2
    String singleblock = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    String multiblock = "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1";
    String longmessage = "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0";

}
