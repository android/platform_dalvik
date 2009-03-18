package targets;

import dalvik.annotation.VirtualTestTarget;

import java.security.Signature;

/**
 * @hide
 */
public interface Signatures {
    /**
     * @hide
     */
    abstract class Internal extends Signature {
        protected Internal() {
            super(null);
        }
    }

    @VirtualTestTarget
    static abstract class SHA512withRSA extends Internal {
        protected abstract void method();
    }

    @VirtualTestTarget
    static abstract class  SHA384withRSA extends Internal {
        protected abstract void method();
    }

    @VirtualTestTarget
    static abstract class  SHA256withRSA extends Internal {
        protected abstract void method();
    }

    @VirtualTestTarget
    static abstract class  SHA224withRSA extends Internal {
        protected abstract void method();
    }

    @VirtualTestTarget
    static abstract class  SHA1withRSA extends Internal {
        protected abstract void method();
    }

    @VirtualTestTarget
    static abstract class  MD5withRSA extends Internal {
        protected abstract void method();
    }

    @VirtualTestTarget
    static abstract class  MD2withRSA extends Internal {
        protected abstract void method();
    }

    @VirtualTestTarget
    static abstract class  SHA1withDSA extends Internal {
        protected abstract void method();
    }

    @VirtualTestTarget
    static abstract class  NONEwithDSA extends Internal {
        protected abstract void method();
    }
}
