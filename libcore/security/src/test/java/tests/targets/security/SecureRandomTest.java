package tests.targets.security;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

import junit.framework.TestCase;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
public abstract class SecureRandomTest extends TestCase {


    private final String algorithmName;
    
    private int counter=0;

    protected SecureRandomTest(String name) {
        this.algorithmName = name;
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    @TestTargets({
        @TestTargetNew(
                level=TestLevel.ADDITIONAL,
                method="getInstance",
                args={String.class}
        ),
        @TestTargetNew(
                level=TestLevel.ADDITIONAL,
                method="setSeed",
                args={long.class}
        ),
        @TestTargetNew(
                level=TestLevel.ADDITIONAL,
                method="nextBytes",
                args={byte[].class}
        ),
        @TestTargetNew(
                level=TestLevel.COMPLETE,
                method="method",
                args={}
        )
    })
    public void testSecureRandom() {
        SecureRandom secureRandom1 = null;
        try {
            secureRandom1 = SecureRandom.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException e) {
            fail(e.getMessage());
        }

        SecureRandom secureRandom2 = null;
        try {
            secureRandom2 = SecureRandom.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException e) {
            fail(e.getMessage());
        }

        byte[] testRandom1 = getRandomBytes(secureRandom1);
        byte[] testRandom2 = getRandomBytes(secureRandom2);

        assertFalse(Arrays.equals(testRandom1, testRandom2));


    }

    private byte[] getRandomBytes(SecureRandom random) {
        byte[] randomData = new byte[64];

        random.setSeed(System.currentTimeMillis()+counter);
        counter++;

        random.nextBytes(randomData);

        return randomData;
    }
}
