/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.configeditor.pages.execute;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.titan.common.parsers.cfg.indices.ExecuteSectionHandler.ExecuteItem;

/**
 * @author Kristof Szabados
 * */
public final class ExecuteSectionDragSourceListener implements DragSourceListener {

	private final TableViewer viewer;
	private final ExecuteSubPage executeSubPage;

	public ExecuteSectionDragSourceListener(final ExecuteSubPage executeSubPage, final TableViewer viewer) {
		this.executeSubPage = executeSubPage;
		this.viewer = viewer;
	}

	@Override
	public void dragFinished(final DragSourceEvent event) {
		final IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		if (!selection.isEmpty()) {
			viewer.getTable().setRedraw(false);
			if (event.detail == DND.DROP_MOVE) {
				executeSubPage.removeSelectedExecuteItems();
			}
			viewer.getTable().setRedraw(true);
			viewer.refresh();
		}
	}

	@Override
	public void dragSetData(final DragSourceEvent event) {
		if (ExecuteItemTransfer.getInstance().isSupportedType(event.dataType)) {
			final IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
			final List<ExecuteItem> items = new ArrayList<ExecuteItem>();
			if (!selection.isEmpty()) {
				for (final Iterator<?> it = selection.iterator(); it.hasNext();) {
					final Object element = it.next();
					if (element instanceof ExecuteItem) {
						items.add((ExecuteItem) element);
					}
				}
				event.data = items.toArray(new ExecuteItem[items.size()]);
			}
		}
	}

	@Override
	public void dragStart(final DragSourceEvent event) {
		final IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		event.doit = !selection.isEmpty() && (selection.getFirstElement() instanceof ExecuteItem);
	}

}
