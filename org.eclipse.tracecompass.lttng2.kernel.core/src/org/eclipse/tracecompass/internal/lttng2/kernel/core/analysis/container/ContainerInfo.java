package org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.container;

/**
 * Small class to hold information.
 *
 * @author Sebastien Lorrain
 *
 */
@SuppressWarnings("javadoc")
public class ContainerInfo {

    public int containerInode;
    public int containerParentTID;
    public String containerHostname;

    /**
     * @param containerInode
     *      The INode of the container (same is the one in /proc/$PID/ns/pid)
     * @param containerParentTID
     *      The process that spawned the container. It is the parent of the first process in the container
     * @param containerHostname
     *      The hostname of the container.
     */
    public ContainerInfo(int containerInode, int containerParentTID, String containerHostname)
    {
        this.containerInode = containerInode;
        this.containerParentTID = containerParentTID;
        this.containerHostname = containerHostname;
    }

}
