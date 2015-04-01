package org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.container;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;

/**
 *
 * This class/builder was designed because we cannot guarantee that we will receive the statedump in order.
 *
 * This builder caches statedump_process_status events on the fly and stores the information in a
 * Task class. If the container holding the task has been added to the statesystem, we can process the task.
 * Otherwise, this builder holds tasks that dosent have their parent container yet.
 *
 * When a new task is processed or a new container is created, every other tasks are re-processed.
 *
 * @author Francis Jolivet
 *
 */

public class ContainerStatedumpBuilder {

    private boolean rootContainerInfoAdded = false;
    private final int fRoot_NSLevel = 0;
    private HashMap<Integer, Task> hmTasks; //tid, Task
    private LinkedList<Task> unresolvedTasks;//ppid, Task

    /**
     * Constructor
     */
    public ContainerStatedumpBuilder()
    {
        hmTasks = new HashMap<>();
        unresolvedTasks = new LinkedList<>();
    }

    /**
     * Takes the statesystembuilder and a new task to be processed
     * @param ssb
     *      The statesystembuilder
     * @param t
     *      The task to add and process. If it cannot be processed right away,
     *      it will be cached until it's parent container is found.
     */
    public void addTask(ITmfStateSystemBuilder ssb, Task t)
    {
        insertTask(ssb, t);
    }

    private void insertTask(ITmfStateSystemBuilder ssb, Task t)
    {
        //Store the new entry in the hashmap
        checkNotNull(t);
        //Since we encounter the deepest level first, we can encounter multiple time the statedump for all levels of the task in the namespaces
        if(!hmTasks.containsKey(t.tID)){
            hmTasks.put(t.tID, t);
            if(t.ns_level == fRoot_NSLevel)
            {
                if(!rootContainerInfoAdded)
                {
                    ContainerInfo cinfo = new ContainerInfo(t.ns_inode, 0, "TODO_ADD_HOSTNAME");
                    ContainerManager.setRootContainerInfo(ssb, t.ts, cinfo);
                    rootContainerInfoAdded = true;
                }

                int containerQuark = ContainerManager.findContainerQuark(ssb, t.ns_inode);
                ContainerManager.setCpuCurrentlyRunningTask(ssb, containerQuark, t.ts, t.cpuID, t.vTID);
                ContainerManager.updateContainerCPUState(ssb, containerQuark, t.ts, t.cpuID, t.cpuState);

            }
            else if (isParentContainerAdded(ssb, t))
            {
                addTaskAndResolveOrphans(ssb, t);
            }
            else {
                // Add task to orphans
                unresolvedTasks.add(t);
            }
        }
    }

    private void addTaskAndResolveOrphans(ITmfStateSystemBuilder ssb, Task t) {
        //Parent container was added, we can add the task
        processTask(ssb, t);

        int containerQuark = ContainerManager.findContainerQuark(ssb, t.ns_inode);
        ContainerManager.setCpuCurrentlyRunningTask(ssb, containerQuark, t.ts, t.cpuID, t.vTID);
        ContainerManager.updateContainerCPUState(ssb, containerQuark, t.ts, t.cpuID, t.cpuState);

        // We added a task, resolve all conflict!
        // Maybe a parent will adopt nice children...
        boolean retryUnresolvedTasks = false;
        do{
            retryUnresolvedTasks = false;
            /*
             * Premature Optimisation is the root of all evil.
             * This would be O(n^2) if statedump would not arrive in order.
             * In fact, this never occurs in practice.
             */

            for (Iterator<Task> iterator = unresolvedTasks.iterator(); iterator.hasNext();) {
                Task orphan = iterator.next();
                if (isParentContainerAdded(ssb, orphan)) {
                    processTask(ssb, orphan);

                    int orphanContainerQuark = ContainerManager.findContainerQuark(ssb, orphan.ns_inode);
                    ContainerManager.setCpuCurrentlyRunningTask(ssb, orphanContainerQuark, t.ts, t.cpuID, t.vTID);
                    ContainerManager.updateContainerCPUState(ssb, orphanContainerQuark, t.ts, t.cpuID, t.cpuState);

                    iterator.remove();
                    retryUnresolvedTasks = true; // we added a task, try to resolve orphan again
                }
            }
        }
        while(retryUnresolvedTasks);
    }

    private boolean isParentContainerAdded(ITmfStateSystemBuilder ssb, Task task)
    {
        /* The only time that a parent container is not yet instanciated is when a task got a
         * parent container that is NOT root container and that this parent does not yet exists
         */

        if(task.ns_level > 1)
        {
            Task parent = hmTasks.get(task.pPID);
            int rootContainer = ContainerManager.getRootContainerQuark(ssb);
            int parentContainer = ContainerManager.findContainerQuark(ssb, parent.ns_inode);
            if (rootContainer == parentContainer)
            {
                //The parent container was not added yet if we are here.
                return false;
            }
        }
        return true;
    }

    private void processTask(ITmfStateSystemBuilder ssb, Task t)
    {
        //This function takes for granted that task NS != root
        assert(t.ns_level != fRoot_NSLevel);

        Task parent = hmTasks.get(t.pPID);
        if(parent != null)
        {
            //We found the task container, add it to it.
            if(ContainerManager.findContainerQuark(ssb, t.ns_inode) != ContainerManager.getRootContainerQuark(ssb))
            {
                int containerQuark = ContainerManager.findContainerQuark(ssb, t.ns_inode);
                ContainerManager.appendTask(ssb, containerQuark, t.ts, t.vTID, t.tID);

            }
            else if(t.ns_inode != parent.ns_inode) //The container does not exist. Add it
            {
                //This function takes for granted that the parent container of the task exists
                assert (isParentContainerAdded(ssb, t));
                ContainerInfo cInfo = new ContainerInfo(t.ns_inode, parent.tID, "TODO_ADD_HOSTNAME");
                ContainerManager.addContainerAndTask(ssb, t.ts, parent.ns_inode, t.tID, t.vTID, cInfo);
            }
        }
    }

}
