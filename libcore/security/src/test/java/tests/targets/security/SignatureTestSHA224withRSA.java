package tests.targets.security;

import dalvik.annotation.TestTargetClass;

@TestTargetClass(targets.Signatures.SHA224withRSA.class)
public class SignatureTestSHA224withRSA extends SignatureTest {

    public SignatureTestSHA224withRSA() {
        super("SHA224withRSA", "RSA");
    }
}
