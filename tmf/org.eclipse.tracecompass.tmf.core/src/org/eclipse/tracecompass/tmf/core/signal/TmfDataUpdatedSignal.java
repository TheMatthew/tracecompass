
package org.eclipse.tracecompass.tmf.core.signal;


/**
 * Signal indicating data updated.
 *
 * @version 1.0
 * @author MC
 * @since 2.0
 */
public class TmfDataUpdatedSignal extends TmfSignal {

    private final Object fData;

    /**
     * Constructor for a new signal.
     *
     * @param source
     *            The object sending this signal
     * @param data
     *            The data
     */
    public TmfDataUpdatedSignal(Object source, Object data) {
        super(source);
        fData = data;
    }

    /**
     * Get the event filter being applied
     *
     * @return The filter
     */
    public Object getData() {
        return fData;
    }
}
