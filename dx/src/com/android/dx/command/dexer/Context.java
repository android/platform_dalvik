package com.android.dx.command.dexer;

import com.android.dx.dex.cf.CodeStatistics;
import com.android.dx.dex.cf.OptimizerOptions;

public class Context {
    public CodeStatistics codeStatistics = new CodeStatistics();

    public OptimizerOptions optimizerOptions = new OptimizerOptions();
}
