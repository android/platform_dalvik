

#undef H
#define H(_op) dvmMterp_##_op
DEFINE_GOTO_TABLE(gDvmMterpHandlers)

#undef H
#define H(_op) #_op
DEFINE_GOTO_TABLE(gDvmMterpHandlerNamesIa32)

