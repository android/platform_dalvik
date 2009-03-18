package tests.targets.security;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

import junit.framework.TestCase;

import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;

public abstract class AlgorithmParameterGeneratorTest extends TestCase {

    private final String algorithmName;
    private final TestHelper<AlgorithmParameters> helper;

    protected AlgorithmParameterGeneratorTest(String algorithmName, TestHelper<AlgorithmParameters> helper) {
        this.algorithmName = algorithmName;
        this.helper = helper;
    }

    @TestTargets({
        @TestTargetNew(
                level=TestLevel.ADDITIONAL,
                method="getInstance",
                args={String.class}
        ),
        @TestTargetNew(
                level=TestLevel.ADDITIONAL,
                method="init",
                args={int.class}
        ),
        @TestTargetNew(
                level=TestLevel.COMPLETE,
                method="method",
                args={}
        )
    })
    public void testAlgorithmParameterGenerator() {
        AlgorithmParameterGenerator generator = null;
        try {
            generator = AlgorithmParameterGenerator.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException e) {
            fail(e.getMessage());
        }

        generator.init(512);

        AlgorithmParameters parameters = generator.generateParameters();
        
        assertNotNull("generated parameters are null", parameters);

        helper.test(parameters);
    }
}