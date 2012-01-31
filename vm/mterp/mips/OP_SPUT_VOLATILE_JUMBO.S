%verify "executed"
%include "mips/OP_SPUT_JUMBO.S" {"prebarrier":"SMP_DMB_ST", "postbarrier":"SMP_DMB"}
