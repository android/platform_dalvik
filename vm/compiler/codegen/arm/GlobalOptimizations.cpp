/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "Dalvik.h"
#include "vm/compiler/CompilerInternals.h"
#include "ArmLIR.h"
#include "vm/compiler/Loop.h"

/* Max number of hoistable loop load operations. */
#define MAX_LOAD_HOISTS 25  // Should be more than enough, considering ARM only has 16 registers.
/* Max number of operations that access dalvik registers. */
#define MAX_REGISTER_OPS 50

/* Enable verbose prints for loop load hoisting by setting this define.
#define LOOP_LOAD_HOIST_VERBOSE
 */

/*
 * Identify unconditional branches that jump to the immediate successor of the
 * branch itself.
 */
static void applyRedundantBranchElimination(CompilationUnit *cUnit)
{
    ArmLIR *thisLIR;

    for (thisLIR = (ArmLIR *) cUnit->firstLIRInsn;
         thisLIR != (ArmLIR *) cUnit->lastLIRInsn;
         thisLIR = NEXT_LIR(thisLIR)) {

        /* Branch to the next instruction */
        if (thisLIR->opcode == kThumbBUncond) {
            ArmLIR *nextLIR = thisLIR;

            while (true) {
                nextLIR = NEXT_LIR(nextLIR);

                /*
                 * Is the branch target the next instruction?
                 */
                if (nextLIR == (ArmLIR *) thisLIR->generic.target) {
                    thisLIR->flags.isNop = true;
                    break;
                }

                /*
                 * Found real useful stuff between the branch and the target.
                 * Need to explicitly check the lastLIRInsn here since with
                 * method-based JIT the branch might be the last real
                 * instruction.
                 */
                if (!isPseudoOpcode(nextLIR->opcode) ||
                    (nextLIR = (ArmLIR *) cUnit->lastLIRInsn))
                    break;
            }
        }
    }
}

/*
 * Perform a pass to hoist all frame pointer load instructions that
 * are independent, outside the loop.
 */
static void applyLoopLoadHoisting(CompilationUnit *cUnit)
{
    ArmLIR *thisLIR, *labelLIR, *lastLIR, *insertLIR;
    ArmLIR *loadLIR[MAX_LOAD_HOISTS];
    ArmLIR *regLIR[MAX_REGISTER_OPS];
    u8 defLoadMask = 0;
    u8 defMask = 0;
    u8 masterDefMask = ~((1ULL << kRegEnd) - 1);
    bool isValidLoop = false;
    int loadCount = 0;
    int regCount = 0;
    int loadindex;
    int hoistCount = 0;

#ifdef LOOP_LOAD_HOIST_VERBOSE
    ALOGD("GlobalOpts LoopLoadHoisting applied on '%s'", cUnit->method->name);
    cUnit->printMe = true;
#endif

    labelLIR = (ArmLIR *) cUnit->firstLIRInsn;
    lastLIR = (ArmLIR *) cUnit->lastLIRInsn;
    insertLIR = NULL;

    /* Find the insert point */
    while ((labelLIR != lastLIR) &&
           (labelLIR->flags.isNop ||
            (labelLIR->opcode != kArmPseudoNormalBlockLabel))) {
        if ((cUnit->loopAnalysis->branchesAdded) &&
            (labelLIR->opcode == kThumbBUncond) &&
            (insertLIR == NULL) && (!labelLIR->flags.isNop))
            insertLIR = labelLIR;

        labelLIR = NEXT_LIR(labelLIR);
    }

    if (labelLIR->opcode != kArmPseudoNormalBlockLabel) {
#ifdef LOOP_LOAD_HOIST_VERBOSE
        ALOGD("Can't hoist - no loop label found!");
#endif
        return;
    }

    if (insertLIR == NULL) {
        insertLIR = labelLIR;
    }
    else if ((ArmLIR *) insertLIR->generic.target != labelLIR) {
#ifdef LOOP_LOAD_HOIST_VERBOSE
        ALOGD("Can't hoist - branch target does not match!");
#endif
        return;
    }

    /* Search for eligible load instructions to hoist */
    for (thisLIR = labelLIR;
         thisLIR != lastLIR;
         thisLIR = NEXT_LIR(thisLIR)) {
        bool handled = false;
        int flags;

        /* Skip non-interesting instructions */
        if (thisLIR->flags.isNop || isPseudoOpcode(thisLIR->opcode))
            continue;

        flags = EncodingMap[thisLIR->opcode].flags;

        /* If it's a load instruction, check if it's a hoist candidate. */
        if (((flags & IS_LOAD) != 0) &&
            ((thisLIR->useMask & ENCODE_DALVIK_REG) != 0)) {
            if (regCount >= MAX_REGISTER_OPS) {
#ifdef LOOP_LOAD_HOIST_VERBOSE
                ALOGD("Out of register list space!");
#endif
                return;
            }
            regLIR[regCount++] = thisLIR;

            if ((((defLoadMask | defMask) & thisLIR->defMask) == 0) &&
                (loadCount < MAX_LOAD_HOISTS)) {
                defLoadMask |= thisLIR->defMask;
                loadLIR[loadCount++] = thisLIR;
                handled = true;
            } else {
                masterDefMask |= thisLIR->defMask;
            }
        /* If it's a store instruction, check if it matches a previous
           hoistable load instruction. If so, reset the global def-flag to
           indicate that the load is still hoistable. */
        } else if (((flags & IS_STORE) != 0) &&
                   ((thisLIR->defMask & ENCODE_DALVIK_REG) != 0)) {
            if (regCount >= MAX_REGISTER_OPS) {
#ifdef LOOP_LOAD_HOIST_VERBOSE
                ALOGD("Out of register list space!");
#endif
                return;
            }
            regLIR[regCount++] = thisLIR;

            if ((thisLIR->useMask & defLoadMask) != 0) {
                handled = true;
                for (int i = loadCount - 1; i >= 0; i--) {
                    if ((thisLIR->aliasInfo == loadLIR[i]->aliasInfo) &&
                        (thisLIR->operands[0] == loadLIR[i]->operands[0])) {
                        defMask &= ~(loadLIR[i]->defMask);
                        break;
                    }
                }
            }
        /* If it's a branch instruction, check if it's the loop branch.
           If it matches the label, mark it as a valid loop. */
        } else if ((flags & IS_BRANCH) != 0) {
            handled = true;
            if (labelLIR == (ArmLIR *) thisLIR->generic.target) {
                isValidLoop = true;
            } else if ((thisLIR->opcode >= kThumbBlx1) &&
                       (thisLIR->opcode <= kThumbBlxR))
            {
#ifdef LOOP_LOAD_HOIST_VERBOSE
                ALOGD("Trace contains branch to subroutine!");
#endif
                return;
            }
        } else if (thisLIR->opcode == kThumbUndefined) {
            break;
        }

        /* If it's not a 'special' instruction, accumulate into def-flags. */
        if (!handled)
            defMask |= thisLIR->defMask;
    }

    defLoadMask &= ~(defMask | masterDefMask);
    if (!isValidLoop || (defLoadMask == 0)) {
#ifdef LOOP_LOAD_HOIST_VERBOSE
        ALOGD("Loop not valid, or defLoadMask (0x%llx) was zero!", defLoadMask);
#endif
        return;
    }

#ifdef LOOP_LOAD_HOIST_VERBOSE
    ALOGD("Masks: masterDef: 0x%llx, def: 0x%llx, final defLoad: 0x%llx",
          masterDefMask, defMask, defLoadMask);
#endif

    /* Try to hoist the load operations */
    for (loadindex = 0; loadindex < loadCount; loadindex++) {
        thisLIR = loadLIR[loadindex];

        /* Host this load? */
        if ((thisLIR->defMask & defLoadMask) == thisLIR->defMask) {
            int i;
            bool foundAlias = false;
            for (i = 0; i < regCount; i++) {
                if ((thisLIR->aliasInfo == regLIR[i]->aliasInfo) &&
                    (thisLIR->operands[0] != regLIR[i]->operands[0])) {
                    foundAlias = true;
                    for (int k = loadindex; k < loadCount; k++) {
                        if (loadLIR[k] == regLIR[i]) {
                            loadLIR[k]->defMask = -1;
                            break;
                        }
                    }
#ifdef LOOP_LOAD_HOIST_VERBOSE
                    ALOGD("Register alias found between these two load ops:");
                    dvmDumpLIRInsn((LIR*)thisLIR, NULL);
                    dvmDumpLIRInsn((LIR*)regLIR[i], NULL);
#endif
                    break;
                }
            }

            if (!foundAlias) {
#ifdef LOOP_LOAD_HOIST_VERBOSE
                ALOGD("Hoisting this load op:");
                dvmDumpLIRInsn((LIR*)thisLIR, NULL);
#endif
                ArmLIR *newLoadLIR = (ArmLIR *) dvmCompilerNew(sizeof(ArmLIR),
                                                               true);
                *newLoadLIR = *thisLIR;
                dvmCompilerInsertLIRBefore((LIR *) insertLIR,
                                           (LIR *) newLoadLIR);
                thisLIR->flags.isNop = true;
                hoistCount++;
            }
        }
    }

    if (cUnit->printMe)
        ALOGD("GlobalOpt LoopLoadHoist hoisted %d load ops.", hoistCount);
}

void dvmCompilerApplyGlobalOptimizations(CompilationUnit *cUnit)
{
    applyRedundantBranchElimination(cUnit);

    if (cUnit->jitMode == kJitLoop) {
        applyLoopLoadHoisting(cUnit);
    }
}
