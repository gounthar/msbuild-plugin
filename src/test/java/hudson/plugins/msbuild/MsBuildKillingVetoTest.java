/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014, Kyle Sweeney, Gregory Boissinot and other contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.msbuild;

import static org.junit.jupiter.api.Assertions.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.util.ProcessKillingVeto.VetoCause;
import hudson.util.ProcessTree.ProcessCallable;
import hudson.util.ProcessTreeRemoting.IOSProcess;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import org.junit.jupiter.api.Test;
import com.google.common.collect.Lists;

class MsBuildKillingVetoTest {

    private MsBuildKillingVeto testee;

    @BeforeEach
    void setUp() {
        testee = new MsBuildKillingVeto();
    }

    @Test
    void testProcessIsNull() {
        assertNull(testee.vetoProcessKilling(null), "Should return null if process is null");
    }

    @Test
    void testCommandLineIsEmpty() {
        IOSProcess emptyArgsProcess = mockProcess();
        assertNull(testee.vetoProcessKilling(emptyArgsProcess), "Should return null if command line arguments are empty");
    }

    @Test
    void testSparesMsPDBSrv() {
        VetoCause veto = testee.vetoProcessKilling(mockProcess("C:\\Program Files (x86)\\Microsoft Visual Studio\\bin\\mspdbsrv.exe", "something", "else"));
        assertNotNull(veto);
        assertEquals("MSBuild Plugin vetoes killing mspdbsrv.exe, see JENKINS-9104 for all the details", veto.getMessage());
    }

    @Test
    void testIgnoresCase() {
        VetoCause veto = testee.vetoProcessKilling(mockProcess("C:\\Program Files (x86)\\Microsoft Visual Studio\\bin\\MsPdbSrv.exe", "something", "else"));
        assertNotNull(veto);
        assertEquals("MSBuild Plugin vetoes killing mspdbsrv.exe, see JENKINS-9104 for all the details", veto.getMessage());
    }

    @Test
    void testPathDoesNotMatter() {
        VetoCause veto = testee.vetoProcessKilling(mockProcess("D:/Tools/mspdbsrv.exe"));
        assertNotNull(veto);
        assertEquals("MSBuild Plugin vetoes killing mspdbsrv.exe, see JENKINS-9104 for all the details", veto.getMessage());
    }

    @Test
    void testLeavesOthersAlone() {
        assertNull(testee.vetoProcessKilling(mockProcess("D:/Tools/somethingElse.exe")));
        assertNull(testee.vetoProcessKilling(mockProcess("C:\\Program Files (x86)\\Microsoft Visual Studio\\bin\\cl.exe")));
        assertNull(testee.vetoProcessKilling(mockProcess("C:\\Program Files (x86)\\Microsoft Visual Studio\\bin\\link.exe")));
    }

    private IOSProcess mockProcess(final String... cmdLine) {
        return new IOSProcess() {
            @Override
            public void killRecursively() {
            }

            @Override
            public void kill() {
            }

            @Override
            public int getPid() {
                return 0;
            }

            @Override
            public IOSProcess getParent() {
                return null;
            }

            @NonNull
            @Override
            public EnvVars getEnvironmentVariables() {
                return null;
            }

            @NonNull
            @Override
            public List<String> getArguments() {
                return Lists.newArrayList(cmdLine);
            }

            @Override
            public <T> T act(ProcessCallable<T> arg0) {
                return null;
            }
        };
    }
}
