package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import java.util.List;
import java.util.Vector;

import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

public class StackbarsExecution {

    // tid of the first event thread
    private int fTidStart;
    // tid of the last event thread
    private int fTidEnd;

    // time start
    private long fStartTime;

    // time end (or duration)
    private long fEndTime;

    // Name
    private String fNameStart;
    private String fNameEnd;

    // List of Stackbars entries
    private List<TimeGraphEntry> fStackbarsEntries;

    // state vector -> time, duration, state
    // private Vector<StateTimeSource> fStates;

    // child vector -> SbExec
    private Vector<StackbarsExecution> fChildren;

    public StackbarsExecution()
    {
        fStackbarsEntries = null;
        fNameStart = "Unknown";
        fNameEnd = "Unknown";
        // fStates = new Vector<>();
    }

    public void setEntries(List<TimeGraphEntry> list)
    {
        fStackbarsEntries = list;
    }

    public List<TimeGraphEntry> getEntries()
    {
        return fStackbarsEntries;
    }

    /*
     * public void addState(StateTimeSource state) { fStates.add(state); }
     */

    public int getTidStart() {
        return fTidStart;
    }

    public int getTidEnd() {
        return fTidEnd;
    }

    public void setTid(int tid) {
        this.fTidStart = tid;
        this.fTidEnd = tid;
    }

    public void setTidEnd(int tid) {
        this.fTidEnd = tid;
    }

    public void setNameStart(String name) {
        this.fNameStart = name;
    }

    public void setNameEnd(String name) {
        this.fNameEnd = name;
    }

    public String getNameStart() {
        return this.fNameStart;
    }

    public String getNameEnd() {
        return this.fNameEnd;
    }

    public long getStartTime() {
        return fStartTime;
    }

    public void setStartTime(long fStartTime) {
        this.fStartTime = fStartTime;
    }

    public long getEndTime() {
        return fEndTime;
    }

    public void setEndTime(long fEndTime) {
        this.fEndTime = fEndTime;
    }

    /*
     * public Vector<StateTimeSource> getStates() { return fStates; }
     */

    public Vector<StackbarsExecution> getChildren() {
        return fChildren;
    }

    public void addChild(StackbarsExecution child) {
        if (fChildren == null)
        {
            fChildren = new Vector<>();
        }
        fChildren.add(child);
    }

    public void removeLastChild() {
        if (fChildren != null)
        {
            fChildren.remove(fChildren.size() - 1);
        }
    }

    /*
     * public static class StateTimeSource{
     *
     * private long fTime; private int fState; private String fSource;
     *
     * public long getTime() { return fTime; }
     *
     * public int getState() { return fState; }
     *
     * public String getSource() { return fSource; }
     *
     * public StateTimeSource(long time, int state, String source) { fTime =
     * time; fState = state; fSource = source; } }
     */
}
