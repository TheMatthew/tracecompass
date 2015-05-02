package org.eclipse.tracecompass.internal.tmf.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.common.core.collect.BufferedBlockingQueue;
import org.eclipse.tracecompass.tmf.core.util.Pair;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

public class QueueStatus extends TmfView {

    public static final String ID = "org.eclipse.tracecompass.tmf.ui.views.queuestatus"; //$NON-NLS-1$

    private final Thread poller;
    private TableViewer fTable;

    public QueueStatus() {
        super(ID);
        poller = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(100);
                        final List<Pair<String, Integer>> content = new ArrayList<>();
                        for (Entry<String, BufferedBlockingQueue<?>> entry : BufferedBlockingQueue.QUEUES.entrySet()) {
                            if (!entry.getValue().isEmpty()) {
                                content.add(new Pair<>(entry.getKey(), entry.getValue().size()));
                            }
                        }
                        Collections.sort(content, new Comparator<Pair<String, Integer>>(){

                            @Override
                            public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
                                return Integer.compare(o1.getSecond(), o2.getSecond());
                            }

                        });
                        if (fTable != null && fTable.getContentProvider() != null) {
                            fTable.getTable().getDisplay().asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    fTable.setInput(content);
                                }
                            });
                        }
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        poller.start();
    }

    @Override
    public void createPartControl(Composite parent) {
        fTable = new TableViewer(parent, SWT.NONE);
        fTable.getTable().setLayout(GridLayoutFactory.swtDefaults().create());
        fTable.getTable().setLayoutData(GridDataFactory.swtDefaults().grab(true, true).create());
        TableViewerColumn col = new TableViewerColumn(fTable, SWT.NONE);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof Pair) {
                    Pair pair = (Pair) element;
                    if (pair.getFirst() instanceof String) {
                        return (String) pair.getFirst();
                    }
                }
                return "";
            }
        });
        col.getColumn().setText("Queue");
        col.getColumn().setWidth(120);
        col = new TableViewerColumn(fTable, SWT.NONE);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof Pair) {
                    Pair pair = (Pair) element;
                    if (pair.getSecond() instanceof Integer) {
                        return Integer.toString((int) pair.getSecond());
                    }
                }
                return "";
            }
        });
        col.getColumn().setText("Size");
        col.getColumn().setWidth(200);
        fTable.getTable().setHeaderVisible(true);
        fTable.setContentProvider(ArrayContentProvider.getInstance());
    }

    @Override
    public void setFocus() {
    }

}
