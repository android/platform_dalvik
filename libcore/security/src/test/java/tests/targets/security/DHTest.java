package tests.targets.security;

import dalvik.annotation.BrokenTest;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

import junit.framework.TestCase;

import targets.KeyPairGenerators;

import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.spec.DHParameterSpec;
@TestTargetClass(KeyPairGenerators.DH.class)
public class DHTest extends TestCase {

    @TestTargetNew(
        level = TestLevel.ADDITIONAL,
        method = "method",
        args = {}
    )
    public void testDHGen() throws Exception
    {
        KeyPairGenerator gen = null;
        try {
            gen = KeyPairGenerator.getInstance("DH");
        } catch (NoSuchAlgorithmException e) {
            fail(e.getMessage());
        }
        
        AlgorithmParameterGenerator algorithmparametergenerator = AlgorithmParameterGenerator.getInstance("DH");
        algorithmparametergenerator.init(960, new SecureRandom());
        AlgorithmParameters algorithmparameters = algorithmparametergenerator.generateParameters();
        DHParameterSpec dhparameterspec = algorithmparameters.getParameterSpec(DHParameterSpec.class);

        
        //gen.initialize(1024);
        gen.initialize(dhparameterspec);
        KeyPair key = gen.generateKeyPair();
    }
}
