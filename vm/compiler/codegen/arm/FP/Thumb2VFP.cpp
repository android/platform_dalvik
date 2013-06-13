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

static bool genArithOpFloat(CompilationUnit *cUnit, MIR *mir,
                            RegLocation rlDest, RegLocation rlSrc1,
                            RegLocation rlSrc2)
{
    int op = kThumbBkpt;
    RegLocation rlResult;

    /*
     * Don't attempt to optimize register usage since these opcodes call out to
     * the handlers.
     */
    switch (mir->dalvikInsn.opcode) {
        case OP_ADD_FLOAT_2ADDR:
        case OP_ADD_FLOAT:
            op = kThumb2Vadds;
            break;
        case OP_SUB_FLOAT_2ADDR:
        case OP_SUB_FLOAT:
            op = kThumb2Vsubs;
            break;
        case OP_DIV_FLOAT_2ADDR:
        case OP_DIV_FLOAT:
            op = kThumb2Vdivs;
            break;
        case OP_MUL_FLOAT_2ADDR:
        case OP_MUL_FLOAT:
            op = kThumb2Vmuls;
            break;
        case OP_REM_FLOAT_2ADDR:
        case OP_REM_FLOAT:
        case OP_NEG_FLOAT: {
            return genArithOpFloatPortable(cUnit, mir, rlDest, rlSrc1,
                                              rlSrc2);
        }
        default:
            return true;
    }
    rlSrc1 = loadValue(cUnit, rlSrc1, kFPReg);
    rlSrc2 = loadValue(cUnit, rlSrc2, kFPReg);
    rlResult = dvmCompilerEvalLoc(cUnit, rlDest, kFPReg, true);
    newLIR3(cUnit, (ArmOpcode)op, rlResult.lowReg, rlSrc1.lowReg,
            rlSrc2.lowReg);
    storeValue(cUnit, rlDest, rlResult);
    return false;
}

static ArmLIR* genVectorMultiplyAndAccumulate(CompilationUnit *cUnit,
                                              MIR *vmulMIR,
                                              MIR *vaddMIR)
{
    RegLocation rlSrc1FromVmul;
    RegLocation rlSrc2FromVmul;
    RegLocation rlSrc1FromVadd;
    RegLocation rlDestFromVadd;
    RegLocation rlResult;

    /* Use reg src1 and src2 from the multiply */
    if (vmulMIR->ssaRep->numUses == 2) {
        rlSrc1FromVmul = dvmCompilerGetSrc(cUnit, vmulMIR, 0);
        rlSrc2FromVmul = dvmCompilerGetSrc(cUnit, vmulMIR, 1);
    } else if (vmulMIR->ssaRep->numUses == 3) {
        rlSrc1FromVmul = dvmCompilerGetSrcWide(cUnit, vmulMIR, 0, 1);
        rlSrc2FromVmul = dvmCompilerGetSrc(cUnit, vmulMIR, 2);
    } else {
        rlSrc1FromVmul = dvmCompilerGetSrcWide(cUnit, vmulMIR, 0, 1);
        rlSrc2FromVmul = dvmCompilerGetSrcWide(cUnit, vmulMIR, 2, 3);
        assert(vaddMIR->ssaRep->numUses == 4);
    }

    /* Use reg src1 and dest from the accumulate */
    if (vaddMIR->ssaRep->numUses > 2) {
        rlSrc1FromVadd = dvmCompilerGetSrcWide(cUnit, vaddMIR, 0, 1);
    } else {
        rlSrc1FromVadd = dvmCompilerGetSrc(cUnit, vaddMIR, 0);
    }
    if (vaddMIR->ssaRep->numDefs == 1) {
        rlDestFromVadd = dvmCompilerGetDest(cUnit, vaddMIR, 0);
    } else {
        assert(vaddMIR->ssaRep->numDefs == 2);
        rlDestFromVadd = dvmCompilerGetDestWide(cUnit, vaddMIR, 0, 1);
    }

    rlSrc1FromVmul = loadValueWide(cUnit, rlSrc1FromVmul, kFPReg);
    assert(rlSrc1FromVmul.wide);
    rlSrc2FromVmul = loadValueWide(cUnit, rlSrc2FromVmul, kFPReg);
    assert(rlSrc2FromVmul.wide);
    /*
     * Make sure that the dest reg for vmla is loaded with the
     * elements of the accumulate.
     */
    rlSrc1FromVadd = loadValueWide(cUnit, rlSrc1FromVadd, kFPReg);
    assert(rlSrc1FromVadd.wide);
    rlResult = dvmCompilerEvalLoc(cUnit, rlDestFromVadd, kFPReg, true);
    assert(rlDestFromVadd.wide);
    assert(rlResult.wide);

    ArmLIR* vmlaLIR = newLIR3(cUnit,
                              kThumb2Vmlad,
                              S2D(rlResult.lowReg, rlResult.highReg),
                              S2D(rlSrc1FromVmul.lowReg, rlSrc1FromVmul.highReg),
                              S2D(rlSrc2FromVmul.lowReg, rlSrc2FromVmul.highReg));

    /* Multiply replaced, make sure MIR is not used */
    vmulMIR->dalvikInsn.opcode = OP_NOP;

    /* Accumulate replaced, make sure MIR is not used */
    vaddMIR->dalvikInsn.opcode = OP_NOP;

    /*
     * Make sure that dest is saved, use the
     * reg location from the accumulate.
     */
    storeValueWide(cUnit, rlDestFromVadd, rlResult);

    return vmlaLIR;
}

static bool checkMIRForSSARegUse(CompilationUnit *cUnit, MIR *mir, int ssaReg)
{
    int i;
    MIR* check;
    if(mir == NULL) {
        return false;
    }
    for(check = mir; check; check = check->next) {
       for(i=0; i < check->ssaRep->numUses; i++) {
          if(check->ssaRep->uses[i] == ssaReg) {
              return true;
          }
       }
    }
    return false;
}

/*
 * Optimize multiply and accumulate using
 * Thumb2 Vector Multiply Accumulate.
 * 1) Multiply dest reg must be defined locally.
 * 2) Accumulate must use the result from
 *    the multiply.
 * 3) Accumulate dest and src1 must be same.
 * 4) Multiply dest reg must not be used after
 *    the accumulate.
 *
 *      [vmuld]     d1, dX, dX
 *      [vaddd]     d0, d0, d1
 *
 * Result:
 *      [vmlad]     d0, dX, dX
 */
static bool tryGenVectorMultiplyAndAccumulate(CompilationUnit *cUnit, MIR *vmulMIR, MIR *vaddMIR, BasicBlock *bb)
{
    assert(vmulMIR);
    assert(vaddMIR);
    assert(bb->dataFlowInfo);
    BitVector* defV = bb->dataFlowInfo->defV;
    if(defV != NULL &&
        dvmIsBitSet(defV, vmulMIR->dalvikInsn.vA)) {

        int vmulDestReg = dvmConvertSSARegToDalvik(cUnit, vmulMIR->ssaRep->defs[0]);
        int vaddDestReg = dvmConvertSSARegToDalvik(cUnit, vaddMIR->ssaRep->defs[0]);
        int vaddSrc1Reg = dvmConvertSSARegToDalvik(cUnit, vaddMIR->ssaRep->uses[0]);
        int vaddSrc2Reg;
        if(vaddMIR->ssaRep->numUses > 2) {
            vaddSrc2Reg = dvmConvertSSARegToDalvik(cUnit, vaddMIR->ssaRep->uses[2]);
        } else {
            vaddSrc2Reg = dvmConvertSSARegToDalvik(cUnit, vaddMIR->ssaRep->uses[1]);
        }

        if(DECODE_REG(vaddSrc2Reg) == DECODE_REG(vmulDestReg) &&
           DECODE_REG(vaddSrc1Reg) == DECODE_REG(vaddDestReg)) {

            if(!checkMIRForSSARegUse(cUnit, vaddMIR->next, vmulMIR->ssaRep->defs[0])) {
                genVectorMultiplyAndAccumulate(cUnit, vmulMIR, vaddMIR);
                return true;
            }
        }
    }
    return false;
}

static bool genArithOpDouble(CompilationUnit *cUnit, MIR *mir,
                             RegLocation rlDest, RegLocation rlSrc1,
                             RegLocation rlSrc2, BasicBlock *bb)
{
    int op = kThumbBkpt;
    RegLocation rlResult;

    switch (mir->dalvikInsn.opcode) {
        case OP_ADD_DOUBLE_2ADDR:
        case OP_ADD_DOUBLE:
            op = kThumb2Vaddd;
            break;
        case OP_SUB_DOUBLE_2ADDR:
        case OP_SUB_DOUBLE:
            op = kThumb2Vsubd;
            break;
        case OP_DIV_DOUBLE_2ADDR:
        case OP_DIV_DOUBLE:
            op = kThumb2Vdivd;
            break;
        case OP_MUL_DOUBLE_2ADDR:
        case OP_MUL_DOUBLE:
            op = kThumb2Vmuld;
            break;
        case OP_REM_DOUBLE_2ADDR:
        case OP_REM_DOUBLE:
        case OP_NEG_DOUBLE: {
            return genArithOpDoublePortable(cUnit, mir, rlDest, rlSrc1,
                                               rlSrc2);
        }
        default:
            return true;
    }

    if(op == kThumb2Vmuld &&
       mir->next != NULL &&
       (mir->next->dalvikInsn.opcode == OP_ADD_DOUBLE ||
        mir->next->dalvikInsn.opcode == OP_ADD_DOUBLE_2ADDR)) {
        /*
         * Found Multiply followed by Accumulate.
         * Try to optimize using Vector Multiply and Accumulate.
         */
        if(tryGenVectorMultiplyAndAccumulate(cUnit, mir, mir->next, bb)) {
            return false;
        }
    }

    rlSrc1 = loadValueWide(cUnit, rlSrc1, kFPReg);
    assert(rlSrc1.wide);
    rlSrc2 = loadValueWide(cUnit, rlSrc2, kFPReg);
    assert(rlSrc2.wide);
    rlResult = dvmCompilerEvalLoc(cUnit, rlDest, kFPReg, true);
    assert(rlDest.wide);
    assert(rlResult.wide);
    newLIR3(cUnit, (ArmOpcode)op, S2D(rlResult.lowReg, rlResult.highReg),
            S2D(rlSrc1.lowReg, rlSrc1.highReg),
            S2D(rlSrc2.lowReg, rlSrc2.highReg));
    storeValueWide(cUnit, rlDest, rlResult);
    return false;
}

static bool genConversion(CompilationUnit *cUnit, MIR *mir)
{
    Opcode opcode = mir->dalvikInsn.opcode;
    int op = kThumbBkpt;
    bool longSrc = false;
    bool longDest = false;
    int srcReg;
    RegLocation rlSrc;
    RegLocation rlDest;
    RegLocation rlResult;

    switch (opcode) {
        case OP_INT_TO_FLOAT:
            longSrc = false;
            longDest = false;
            op = kThumb2VcvtIF;
            break;
        case OP_FLOAT_TO_INT:
            longSrc = false;
            longDest = false;
            op = kThumb2VcvtFI;
            break;
        case OP_DOUBLE_TO_FLOAT:
            longSrc = true;
            longDest = false;
            op = kThumb2VcvtDF;
            break;
        case OP_FLOAT_TO_DOUBLE:
            longSrc = false;
            longDest = true;
            op = kThumb2VcvtFd;
            break;
        case OP_INT_TO_DOUBLE:
            longSrc = false;
            longDest = true;
            op = kThumb2VcvtID;
            break;
        case OP_DOUBLE_TO_INT:
            longSrc = true;
            longDest = false;
            op = kThumb2VcvtDI;
            break;
        case OP_LONG_TO_DOUBLE:
        case OP_FLOAT_TO_LONG:
        case OP_LONG_TO_FLOAT:
        case OP_DOUBLE_TO_LONG:
            return genConversionPortable(cUnit, mir);
        default:
            return true;
    }
    if (longSrc) {
        rlSrc = dvmCompilerGetSrcWide(cUnit, mir, 0, 1);
        rlSrc = loadValueWide(cUnit, rlSrc, kFPReg);
        srcReg = S2D(rlSrc.lowReg, rlSrc.highReg);
    } else {
        rlSrc = dvmCompilerGetSrc(cUnit, mir, 0);
        rlSrc = loadValue(cUnit, rlSrc, kFPReg);
        srcReg = rlSrc.lowReg;
    }
    if (longDest) {
        rlDest = dvmCompilerGetDestWide(cUnit, mir, 0, 1);
        rlResult = dvmCompilerEvalLoc(cUnit, rlDest, kFPReg, true);
        newLIR2(cUnit, (ArmOpcode)op, S2D(rlResult.lowReg, rlResult.highReg),
                srcReg);
        storeValueWide(cUnit, rlDest, rlResult);
    } else {
        rlDest = dvmCompilerGetDest(cUnit, mir, 0);
        rlResult = dvmCompilerEvalLoc(cUnit, rlDest, kFPReg, true);
        newLIR2(cUnit, (ArmOpcode)op, rlResult.lowReg, srcReg);
        storeValue(cUnit, rlDest, rlResult);
    }
    return false;
}

static bool genInlineSqrt(CompilationUnit *cUnit, MIR *mir)
{
    ArmLIR *branch;
    RegLocation rlSrc = dvmCompilerGetSrcWide(cUnit, mir, 0, 1);
    RegLocation rlDest = inlinedTargetWide(cUnit, mir, true);
    rlSrc = loadValueWide(cUnit, rlSrc, kFPReg);
    RegLocation rlResult = dvmCompilerEvalLoc(cUnit, rlDest, kFPReg, true);
    newLIR2(cUnit, kThumb2Vsqrtd, S2D(rlResult.lowReg, rlResult.highReg),
            S2D(rlSrc.lowReg, rlSrc.highReg));
    newLIR2(cUnit, kThumb2Vcmpd, S2D(rlResult.lowReg, rlResult.highReg),
            S2D(rlResult.lowReg, rlResult.highReg));
    newLIR0(cUnit, kThumb2Fmstat);
    branch = newLIR2(cUnit, kThumbBCond, 0, kArmCondEq);
    dvmCompilerClobberCallRegs(cUnit);
    LOAD_FUNC_ADDR(cUnit, r2, (int) (double (*)(double)) sqrt);
    newLIR3(cUnit, kThumb2Fmrrd, r0, r1, S2D(rlSrc.lowReg, rlSrc.highReg));
    newLIR1(cUnit, kThumbBlxR, r2);
    newLIR3(cUnit, kThumb2Fmdrr, S2D(rlResult.lowReg, rlResult.highReg),
            r0, r1);
    ArmLIR *label = newLIR0(cUnit, kArmPseudoTargetLabel);
    label->defMask = ENCODE_ALL;
    branch->generic.target = (LIR *)label;
    storeValueWide(cUnit, rlDest, rlResult);
    return false;
}

static bool genCmpFP(CompilationUnit *cUnit, MIR *mir, RegLocation rlDest,
                     RegLocation rlSrc1, RegLocation rlSrc2)
{
    bool isDouble;
    int defaultResult;
    RegLocation rlResult;

    switch(mir->dalvikInsn.opcode) {
        case OP_CMPL_FLOAT:
            isDouble = false;
            defaultResult = -1;
            break;
        case OP_CMPG_FLOAT:
            isDouble = false;
            defaultResult = 1;
            break;
        case OP_CMPL_DOUBLE:
            isDouble = true;
            defaultResult = -1;
            break;
        case OP_CMPG_DOUBLE:
            isDouble = true;
            defaultResult = 1;
            break;
        default:
            return true;
    }
    if (isDouble) {
        rlSrc1 = loadValueWide(cUnit, rlSrc1, kFPReg);
        rlSrc2 = loadValueWide(cUnit, rlSrc2, kFPReg);
        dvmCompilerClobberSReg(cUnit, rlDest.sRegLow);
        rlResult = dvmCompilerEvalLoc(cUnit, rlDest, kCoreReg, true);
        loadConstant(cUnit, rlResult.lowReg, defaultResult);
        newLIR2(cUnit, kThumb2Vcmpd, S2D(rlSrc1.lowReg, r1Src2.highReg),
                S2D(rlSrc2.lowReg, rlSrc2.highReg));
    } else {
        rlSrc1 = loadValue(cUnit, rlSrc1, kFPReg);
        rlSrc2 = loadValue(cUnit, rlSrc2, kFPReg);
        dvmCompilerClobberSReg(cUnit, rlDest.sRegLow);
        rlResult = dvmCompilerEvalLoc(cUnit, rlDest, kCoreReg, true);
        loadConstant(cUnit, rlResult.lowReg, defaultResult);
        newLIR2(cUnit, kThumb2Vcmps, rlSrc1.lowReg, rlSrc2.lowReg);
    }
    assert(!FPREG(rlResult.lowReg));
    newLIR0(cUnit, kThumb2Fmstat);

    genIT(cUnit, (defaultResult == -1) ? kArmCondGt : kArmCondMi, "");
    newLIR2(cUnit, kThumb2MovImmShift, rlResult.lowReg,
            modifiedImmediate(-defaultResult)); // Must not alter ccodes
    genBarrier(cUnit);

    genIT(cUnit, kArmCondEq, "");
    loadConstant(cUnit, rlResult.lowReg, 0);
    genBarrier(cUnit);

    storeValue(cUnit, rlDest, rlResult);
    return false;
}
