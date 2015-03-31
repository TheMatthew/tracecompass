package org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.container;

public class ContainerInfo {

    public int containerInode;
    public int containerParentTID;
    public String containerHostname;


    public ContainerInfo(int containerInode, int containerParentTID, String containerHostname)
    {
        this.containerInode = containerInode;
        this.containerParentTID = containerParentTID;
        this.containerHostname = containerHostname;
    }

}
