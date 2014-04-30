/*
 * Copyright (C) 2014 The Android Open Source Project
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
#ifndef DALVIK_REPORTBACKTRACE_H_
#define DALVIK_REPORTBACKTRACE_H_

#include <signal.h>

#define SIG_BACKTRACE       SIGURG
#define SI_GET_BACKTRACE    (-10)
#define SI_GOT_BACKTRACE    (SI_GET_BACKTRACE - 1)

int     reportBacktrace(siginfo_t* info);
int     sys_sigwaitinfo(const sigset_t *set, siginfo_t *info);

#endif  // DALVIK_REPORTBACKTRACE_H_
