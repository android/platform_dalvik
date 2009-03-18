package tests.targets.security;

import dalvik.annotation.TestTargetClass;


@TestTargetClass(value=targets.MessageDigests.MD5.class)
public class MessageDigestTestMD5 extends MessageDigestTest {

    public MessageDigestTestMD5() {
        super("MD5");
        super.source1 = "abc";
        super.source2 = "abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu";
        super.source3 = getLongMessage(1000000);
        super.expected1 = singleblock;
        super.expected2 = multiblock;
        super.expected3 = longmessage;
    }

    String singleblock = "900150983cd24fb0d6963f7d28e17f72";
    String multiblock = "03dd8807a93175fb062dfb55dc7d359c";
    String longmessage = "7707d6ae4e027c70eea2a935c2296f21";

}
