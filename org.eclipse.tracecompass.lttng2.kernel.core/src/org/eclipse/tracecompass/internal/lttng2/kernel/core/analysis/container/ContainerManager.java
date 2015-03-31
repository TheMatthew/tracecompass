package org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.container;

import java.util.List;

import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;

public class ContainerManager {

    //This is a hack to have a unique container ID
    private static int nextContainerID = 0;
    private static int getNextContainerID() { return nextContainerID++; }

    public static void setRootContainerInfo(ITmfStateSystemBuilder ssb, long timestamp, ContainerInfo rootContainerInfo){
        int rootContainerQuark = ContainerManager.getRootContainerQuark(ssb);
        ContainerManager.setContainerInfo(ssb, rootContainerQuark, timestamp, rootContainerInfo);
    }

    public static void addContainerAndTask(final ITmfStateSystemBuilder ssb, long timestamp, int parentNSInum, int childTid, int childVTid, ContainerInfo cInfo) {
        int newContainerQuark = ContainerManager.attachNewContainer(ssb, parentNSInum, timestamp, cInfo);
        ContainerManager.appendTask(ssb, newContainerQuark, timestamp, childVTid, childTid);
    }

    private static int attachNewContainer(ITmfStateSystemBuilder ssb, int parentContainerINode, long timestamp, ContainerInfo containerInfo){

        int parentContainerQuark = findContainerQuark(ssb, parentContainerINode);

        //TODO fix the containerNumber hack!!
        int containerQuark = ssb.getQuarkRelativeAndAdd(
                parentContainerQuark,
                ContainerAttributes.CONTAINERS_SECTION,
                ContainerAttributes.CONTAINER_ID + Integer.toString(ContainerManager.getNextContainerID()));


        setContainerInfo(ssb, containerQuark, timestamp, containerInfo);

        return containerQuark;
    }

    private static void setContainerInfo(ITmfStateSystemBuilder ssb, int containerQuark, long timestamp, ContainerInfo containerInfo) {

        //This ensure that the quark is indeed a container quark
        assert(ssb.getAttributeName(containerQuark).startsWith(ContainerAttributes.CONTAINER_ID));

        try{
            int containerInfoQuark = ssb.getQuarkRelativeAndAdd(containerQuark, ContainerAttributes.CONTAINER_INFO);
            int containerPPIDQuark = ssb.getQuarkRelativeAndAdd(containerInfoQuark, ContainerAttributes.PPID);
            ssb.modifyAttribute(timestamp, TmfStateValue.newValueInt(containerInfo.containerParentTID), containerPPIDQuark);

            int containerINodeQuark = ssb.getQuarkRelativeAndAdd(containerInfoQuark, ContainerAttributes.CONTAINER_INODE);
            ssb.modifyAttribute(timestamp, TmfStateValue.newValueInt(containerInfo.containerInode), containerINodeQuark);

            int containerHostnameQuark = ssb.getQuarkRelativeAndAdd(containerInfoQuark, ContainerAttributes.HOSTNAME);
            ssb.modifyAttribute(timestamp, TmfStateValue.newValueString(containerInfo.containerHostname), containerHostnameQuark);
        } catch( AttributeNotFoundException e)
        {
            //This should not happen...
            e.printStackTrace();
        }

    }

    public static int getRootContainerQuark(ITmfStateSystemBuilder ssb)
    {
        return ssb.getQuarkAbsoluteAndAdd(ContainerAttributes.ROOT);
    }

    public static int findContainerQuark(ITmfStateSystemBuilder ssb, int containerINode) {
        //The root of the namespace containers. This quark is the parent of all namespaces!
        int rootQuark = getRootContainerQuark(ssb);
        int parentContainerQuark = -1;

        try {
            List<Integer> ContainerINodeList = ssb.getSubAttributes(rootQuark, true, ContainerAttributes.CONTAINER_INODE);

            if(ContainerINodeList.size() >= 1)
            {
                // Expand container INodes and iterates over all INodes found.
                // Check to see if we can find the requested INode

                for(Integer quarkContainerINode : ContainerINodeList){
                    int iNode = ssb.queryOngoingState(quarkContainerINode).unboxInt();
                    if(iNode == containerINode)
                    {
                        // We got the tid in a container somewhere, return the container quark!
                        int containerInfoQuark = ssb.getParentAttributeQuark(quarkContainerINode);
                        parentContainerQuark = ssb.getParentAttributeQuark(containerInfoQuark);
                        break;
                    }
                }
            }

        } catch (AttributeNotFoundException e) {
            // This would mean that manipulation on container quark trees occured BEFORE setting the root node
            e.printStackTrace();
        }

        return parentContainerQuark == -1?
                rootQuark : parentContainerQuark;
    }

    public static void appendTask(ITmfStateSystemBuilder ssb, int containerQuark, long timestamp, int VTID, int real_TID){

        //This ensure that the quark is indeed a container quark
        assert(ssb.getAttributeName(containerQuark).startsWith(ContainerAttributes.CONTAINER_ID));

        int VTIDSectionQuark = getVTIDsSection(ssb, containerQuark);

        int vtidQuark = ssb.getQuarkRelativeAndAdd(VTIDSectionQuark, Integer.toString(VTID));

        int real_tidQuark = ssb.getQuarkRelativeAndAdd(vtidQuark, ContainerAttributes.REAL_TID);
        try {
            ssb.modifyAttribute(timestamp, TmfStateValue.newValueInt(real_TID), real_tidQuark);
        } catch (StateValueTypeException | AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static boolean removeTask(ITmfStateSystemBuilder ssb, long timestamp, int realTIDToRemove)
    {
        int containerQuark = getContainerFromTask(ssb, realTIDToRemove);

        if(containerQuark != ContainerManager.getRootContainerQuark(ssb))
        {
            /*
             * This is highly inefficient...we get all TID of container and search for corresponding quark.
             * At the same time, we don't have the VTID information in the tracepoint!!
             * This is O(n), where n is the number of task in a container
             */
            try {
            List<Integer> RealTIDList = ssb.getSubAttributes(containerQuark, true, ContainerAttributes.REAL_TID);

                if(RealTIDList.size() >= 1)
                {
                    for(Integer realTIDQuark : RealTIDList){
                        int realTIDvalue = ssb.queryOngoingState(realTIDQuark).unboxInt();
                        if(realTIDToRemove == realTIDvalue){

                            int VTIDQuark = ssb.getParentAttributeQuark(realTIDQuark);
                            ssb.removeAttribute(timestamp, VTIDQuark);
                            return true;
                        }
                    }
                }
            } catch (AttributeNotFoundException e) {
                // TODO Auto-generated catch block

                //This would happen if the task is not yet added and we try to remove it!
                e.printStackTrace();
            }
        }
        return false;
    }

    private static int getVTIDsSection(ITmfStateSystemBuilder ssb, int containerQuark){
        //This ensure that the quark is indeed a container quark
        assert(ssb.getAttributeName(containerQuark).startsWith(ContainerAttributes.CONTAINER_ID));
        int VTIDsSectionQuark = -1;
        VTIDsSectionQuark = ssb.getQuarkRelativeAndAdd(containerQuark, ContainerAttributes.CONTAINER_TASKS);
        return VTIDsSectionQuark;
    }

    public static int getContainerFromTask(ITmfStateSystemBuilder ssb, int taskTid)//returns containerQuark
    {
        //The root of the namespace containers. This quark is the parent of all namespaces!
        int rootQuark = getRootContainerQuark(ssb);
        int parentContainerQuark = -1;

        try {
            List<Integer> VTIDsSection = ssb.getSubAttributes(rootQuark, true, ContainerAttributes.CONTAINER_TASKS);

            if(VTIDsSection.size() >= 1)
            {
                // Expand VTIDs section and iterates over all VTID found in containers
                // Check their real TID, compare it to the concerned TID

                for(Integer quarkVTIDsSection : VTIDsSection){
                    List<Integer> VTIDs = ssb.getSubAttributes(quarkVTIDsSection, false);

                    for(Integer quarkVTid : VTIDs){
                        int real_tid_quark = ssb.getQuarkRelative(quarkVTid, ContainerAttributes.REAL_TID);
                        int tid_in_container = ssb.queryOngoingState(real_tid_quark).unboxInt();
                        if(taskTid == tid_in_container)
                        {
                            // We got the tid in a container somewhere, return the container quark!
                            parentContainerQuark = ssb.getParentAttributeQuark(quarkVTid);
                            parentContainerQuark = ssb.getParentAttributeQuark(parentContainerQuark);
                            break;
                        }
                    }
                }
            }

        } catch (AttributeNotFoundException e) {
            // This would mean that manipulation on container quark trees occured BEFORE setting the root node
            e.printStackTrace();
        }

        return parentContainerQuark == -1?
                rootQuark : parentContainerQuark;
    }

    public static void updateContainerCPUState(ITmfStateSystemBuilder ssb, int containerQuark, long timeStamp, int cpuId, ITmfStateValue CpuState){

        //This ensure that the quark is indeed a container quark
        assert(ssb.getAttributeName(containerQuark).startsWith(ContainerAttributes.CONTAINER_ID));

        try {
            //The CPUs root quark of the container container passed in agrument.
            int cpuQuark = ssb.getQuarkRelativeAndAdd(containerQuark, new String[] {ContainerAttributes.CONTAINER_CPU, Integer.toString(cpuId)});
            ssb.modifyAttribute(timeStamp, CpuState, cpuQuark);
        } catch (StateValueTypeException | AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int rootQuark = ssb.getQuarkAbsoluteAndAdd(ContainerAttributes.ROOT);

        //If the container is not the root, propagate CPU state changes up to the root recursively
        if(containerQuark != rootQuark){

            int parentNestedContainersQuark = ssb.getParentAttributeQuark(containerQuark);
            int parentContainerQuark = ssb.getParentAttributeQuark(parentNestedContainersQuark);

            switch(CpuState.unboxInt())
            {
            case ContainerCpuState.CPU_STATUS_IDLE:
                ContainerManager.updateContainerCPUState(ssb, parentContainerQuark, timeStamp, cpuId, ContainerCpuState.CPU_STATUS_IDLE_VALUE);
                break;
            case ContainerCpuState.CPU_STATUS_RUNNING:
                ContainerManager.updateContainerCPUState(ssb, parentContainerQuark, timeStamp, cpuId, ContainerCpuState.CPU_STATUS_SHARED_VALUE);
                break;

            case ContainerCpuState.CPU_STATUS_SHARED:
                ContainerManager.updateContainerCPUState(ssb, parentContainerQuark, timeStamp, cpuId, ContainerCpuState.CPU_STATUS_SHARED_VALUE);
                break;
            default:
                break;
            }
        }
    }

    public static void setCpuCurrentlyRunningTask(ITmfStateSystemBuilder ssb, int containerQuark, long timeStamp, int cpuId, int vtid){

        //This ensure that the quark is indeed a container quark
        assert(ssb.getAttributeName(containerQuark).startsWith(ContainerAttributes.CONTAINER_ID));

        //The CPUs root quark of the container container passed in agrument.
        int cpuQuark = ssb.getQuarkRelativeAndAdd(containerQuark, new String[] {ContainerAttributes.CONTAINER_CPU, Integer.toString(cpuId)});

        int cpuRunningVTIDQuark = ssb.getQuarkRelativeAndAdd(cpuQuark, ContainerAttributes.RUNNING_VTID);
        try {
            ssb.modifyAttribute(timeStamp, TmfStateValue.newValueInt(vtid), cpuRunningVTIDQuark);
        } catch (StateValueTypeException | AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            // This should never happens...
            e.printStackTrace();
        }
    }
}
