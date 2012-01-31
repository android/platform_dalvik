%verify "executed"
%include "mips/OP_IPUT_OBJECT_JUMBO.S" {"prebarrier":"SMP_DMB_ST", "postbarrier":"SMP_DMB"}
