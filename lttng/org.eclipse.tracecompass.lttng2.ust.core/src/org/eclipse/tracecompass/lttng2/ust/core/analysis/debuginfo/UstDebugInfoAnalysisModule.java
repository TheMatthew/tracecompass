/*******************************************************************************
 * Copyright (c) 2015 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.lttng2.ust.core.analysis.debuginfo;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.lttng2.ust.core.analysis.debuginfo.UstDebugInfoStateProvider;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

/**
 * Analysis to provide TMF Callsite information by mapping IP (instruction
 * pointer) contexts to address/line numbers via debug information.
 *
 * @author Alexandre Montplaisir
 * @since 2.0
 */
public class UstDebugInfoAnalysisModule extends TmfStateSystemAnalysisModule {

    /**
     * Analysis ID, it should match that in the plugin.xml file
     */
    public static final String ID = "org.eclipse.linuxtools.lttng2.ust.analysis.debuginfo"; //$NON-NLS-1$

    @Override
    protected ITmfStateProvider createStateProvider() {
        return new UstDebugInfoStateProvider(checkNotNull(getTrace()));
    }

    @Override
    public boolean setTrace(ITmfTrace trace) throws TmfAnalysisException {
        if (!(trace instanceof LttngUstTrace)) {
            return false;
        }
        return super.setTrace(trace);
    }

    @Override
    protected @Nullable LttngUstTrace getTrace() {
        return (LttngUstTrace) super.getTrace();
    }

    @Override
    public Iterable<TmfAnalysisRequirement> getAnalysisRequirements() {
        // TODO specify actual requirements once the requirement-checking is
        // implemented. This analysis needs "ip" and "vpid" contexts.
        return checkNotNull(Collections.EMPTY_SET);
    }

    @Override
    public boolean canExecute(ITmfTrace trace) {
        /* The analysis can only work with LTTng-UST traces... */
        if (!(trace instanceof LttngUstTrace)) {
            return false;
        }
        LttngUstTrace ustTrace = (LttngUstTrace) trace;

        /* ... taken with UST >= 2.8 ... */
        if (!"lttng-ust".equals(ustTrace.getTracerName())) { //$NON-NLS-1$
            return false;
        }
        if (ustTrace.getTracerMajorVersion() < 2) {
            return false;
        }
        if (ustTrace.getTracerMajorVersion() == 2 && ustTrace.getTracerMinorVersion() < 8) {
            return false;
        }

        /* ... that respect the ip/vpid contexts requirements. */
        return super.canExecute(trace);
    }

    // ------------------------------------------------------------------------
    // Class-specific operations
    // ------------------------------------------------------------------------

    /**
     * Return all the binaries that were detected in the trace.
     *
     * @return The binaries (executables or libraries) referred to in the trace.
     */
    public Collection<BinaryFile> getAllBinaries() {
        waitForCompletion();
        ITmfStateSystem ss = checkNotNull(getStateSystem());

        Set<BinaryFile> files = new TreeSet<>();
        try {
            ImmutableList.Builder<Integer> builder = ImmutableList.builder();
            List<Integer> vpidQuarks = ss.getSubAttributes(-1, false);
            for (Integer vpidQuark : vpidQuarks) {
                builder.addAll(ss.getSubAttributes(vpidQuark, false));
            }
            List<Integer> baddrQuarks = builder.build();

            /*
             * For each "baddr" attribute, get the "buildId" sub-attribute,
             * whose value is the file path.
             */

            for (Integer baddrQuark : baddrQuarks) {

                List<Integer> buildIdQuarks = ss.getSubAttributes(baddrQuark, false);
                for (Integer buildIdQuark : buildIdQuarks) {
                    String buildId = ss.getAttributeName(buildIdQuark);

                    /*
                     * Explore the history of this attribute "horizontally",
                     * even though there should only be one valid interval.
                     */
                    ITmfStateInterval interval = StateSystemUtils.queryUntilNonNullValue(ss, buildIdQuark, ss.getStartTime(), Long.MAX_VALUE);
                    if (interval == null) {
                        /*
                         * If we created the attribute, we should have assigned
                         * a value to it!
                         */
                        throw new IllegalStateException();
                    }
                    String filePath = interval.getStateValue().unboxStr();

                    files.add(new BinaryFile(filePath, buildId));
                }
            }
        } catch (AttributeNotFoundException e) {
            throw new IllegalStateException(e);
        }
        return files;
    }

    /**
     * Get the binary file (executable or library) that corresponds to a given
     * instruction pointer, at a given time.
     *
     * @param ts
     *            The timestamp
     * @param vpid
     *            The VPID of the process we are querying for
     * @param ip
     *            The instruction pointer of the trace event. Normally comes
     *            from a 'ip' context.
     * @return The {@link BinaryFile} object, containing both the binary's path
     *         and its build ID.
     */
    public @Nullable LoadedBinaryFile getMatchingFile(long ts, long vpid, long ip) {
        waitForCompletion();
        final ITmfStateSystem ss = checkNotNull(getStateSystem());

        List<Integer> possibleBaddrQuarks = ss.getQuarks(String.valueOf(vpid), "*"); //$NON-NLS-1$

        /* Get the most probable base address from all the known ones */
        NavigableSet<Long> possibleBaddrs = FluentIterable.from(possibleBaddrQuarks)
                .transform(new Function<Integer, Long>(){
                    @Override
                    public Long apply(@Nullable Integer quark) {
                        String baddrStr = ss.getAttributeName(checkNotNull(quark).intValue());
                        return checkNotNull(Long.valueOf(baddrStr));
                    }
                }).toSortedSet(Ordering.natural());
        final Long potentialBaddr = possibleBaddrs.floor(ip);

        /* Make sure the 'ip' fits in the expected memory range */
        try {
            final List<ITmfStateInterval> fullState = ss.queryFullState(ts);

            final int baddrQuark = ss.getQuarkAbsolute(String.valueOf(vpid), String.valueOf(potentialBaddr));
            final long endAddr = fullState.get(baddrQuark).getStateValue().unboxLong();

            if (!(ip < endAddr)) {
                /*
                 * Not the correct memory range after all. We do not have
                 * information about the library that was loaded here.
                 */
                return null;
            }

            /*
             * We've found the correct base address, now to determine what
             * library was loaded there at that time.
             */
            List<Integer> buildIds = ss.getSubAttributes(baddrQuark, false);
            Optional<Integer> potentialBuildIdQuark = FluentIterable.from(buildIds).firstMatch(new Predicate<Integer>() {
                @Override
                public boolean apply(@Nullable Integer input) {
                    int quark = checkNotNull(input).intValue();
                    ITmfStateValue value = fullState.get(quark).getStateValue();
                    return (!value.isNull());
                }
            });

            if (!potentialBuildIdQuark.isPresent()) {
                /* We didn't have the information after all. */
                return null;
            }

            /* Ok, we have everything we need! Return the information. */
            long baddr = Long.parseLong(ss.getAttributeName(baddrQuark));

            int buildIdQuark = potentialBuildIdQuark.get().intValue();
            String buildId = ss.getAttributeName(buildIdQuark);
            String filePath = fullState.get(buildIdQuark).getStateValue().unboxStr();
            return new LoadedBinaryFile(baddr, filePath, buildId);

        } catch (AttributeNotFoundException e) {
            /* We're only using quarks we've checked for. */
            throw new IllegalStateException(e);
        } catch (StateSystemDisposedException e) {
            return null;
        }

    }

    /**
     * Simply extension to {@link BinaryFile} that adds the base address at
     * which the given binary or library is loaded.
     */
    public static class LoadedBinaryFile extends BinaryFile {

        private final long baseAddress;

        /**
         * Constructor
         *
         * @param baseAddress
         *            The base address at which the binary or library is loaded
         * @param filePath
         *            The file path of the loaded binary/library
         * @param buildId
         *            The build ID of the binary object
         */
        public LoadedBinaryFile(long baseAddress, String filePath, String buildId) {
            super(filePath, buildId);
            this.baseAddress = baseAddress;
        }

        /**
         * Return the base address at which the object is loaded.
         *
         * @return The base address
         */
        public long getBaseAddress() {
            return baseAddress;
        }

    }

    /**
     * Wrapper class to reference to a particular binary, which can be an
     * executable or library. It contains both the complete file path (at the
     * time the trace was taken) and the build ID of the binary.
     */
    public static class BinaryFile implements Comparable<BinaryFile> {

        private final String filePath;
        private final String buildId;
        private final String toString;

        /**
         * Constructor
         *
         * @param filePath
         *            The binary's path on the filesystem
         * @param buildId
         *            The binary's unique buildID (in base16 form).
         */
        public BinaryFile(String filePath, String buildId) {
            this.filePath = filePath;
            this.buildId = buildId;
            this.toString = new String(filePath + " (" + buildId + ')'); //$NON-NLS-1$
        }

        /**
         * Get the file's path, as was referenced to in the trace.
         *
         * @return The file path
         */
        public String getFilePath() {
            return filePath;
        }

        /**
         * Get the build ID of the binary. It should be a unique identifier.
         *
         * On Unix systems, you can use <pre>eu-readelf -n [binary]</pre> to get
         * this ID.
         *
         * @return The file's build ID.
         */
        public String getBuildId() {
            return buildId;
        }

        @Override
        public String toString() {
            return toString;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj == null || !(obj instanceof BinaryFile)) {
                return false;
            }
            BinaryFile other = (BinaryFile) obj;
            if (this.filePath == other.filePath &&
                    this.buildId == other.buildId) {
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + buildId.hashCode();
            result = prime * result +  filePath.hashCode();
            return result;
        }

        /**
         * Used for sorting. Sorts by using alphabetical order of the file
         * paths.
         */
        @Override
        public int compareTo(@Nullable BinaryFile o) {
            if (o == null) {
                return 1;
            }
            return this.filePath.compareTo(o.filePath);
        }
    }
}
