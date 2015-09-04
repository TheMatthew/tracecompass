/*******************************************************************************
 * Copyright (c) 2015 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.ust.core.analysis.debuginfo;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.lookup.TmfCallsite;

/**
 * Utility class to get file name, function/symbol name and line number from a
 * given offset. In TMF this is represented as a {@link TmfCallsite}.
 *
 * @author Alexandre Montplaisir
 */
public final class FileOffsetMapper {

    private static final String ADDR2LINE_EXECUTABLE = "addr2line"; //$NON-NLS-1$

    private FileOffsetMapper() {}

    /**
     * Generate the callsites from a given binary file and address offset.
     *
     * Due to function inlining, it is possible for one offset to actually have
     * multiple call sites. This is why we can return more than one callsite per
     * call.
     *
     * @param file
     *            The binary file to look at
     * @param offset
     *            The memory offset in the file
     * @return The list of callsites corresponding to the offset, reported from
     *         the "highest" inlining location, down to the initial definition.
     */
    public static @Nullable Iterable<TmfCallsite> getCallsiteFromOffset(File file, long offset) {
        if (!Files.exists((file.toPath()))) {
            throw new IllegalArgumentException();
        }
        return getCallsiteFromOffsetWithAddr2line(file, offset);
    }

    private static @Nullable Iterable<TmfCallsite> getCallsiteFromOffsetWithAddr2line(File file, long offset) {
        List<TmfCallsite> callsites = new LinkedList<>();

        List<String> output = getOutputFromCommand(checkNotNull(Arrays.asList(
                ADDR2LINE_EXECUTABLE, "-i", "-e", file.toString(), "0x" + Long.toHexString(offset))));  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

        if (output == null) {
            /* Command returned an error */
            return null;
        }

        for (String outputLine : output) {
            String[] elems = outputLine.split(":"); //$NON-NLS-1$
            String fileName = elems[0];
            if (fileName.equals("??")) { //$NON-NLS-1$
                continue;
            }
            long lineNumber = Long.parseLong(elems[1]);

            callsites.add(new TmfCallsite(fileName, null, lineNumber));
        }

        return callsites;
    }

    private static @Nullable List<String> getOutputFromCommand(List<String> command) {
        try {
            Path tempFile = Files.createTempFile("output", null); //$NON-NLS-1$

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            builder.redirectOutput(Redirect.to(tempFile.toFile()));

            Process p = builder.start();
            int ret = p.waitFor();

            List<String> lines = checkNotNull(Files.readAllLines(tempFile, Charset.defaultCharset()));
            Files.delete(tempFile);

            return (ret == 0 ? lines : null);

        } catch (IOException | InterruptedException e) {
            return null;
        }
    }
}
