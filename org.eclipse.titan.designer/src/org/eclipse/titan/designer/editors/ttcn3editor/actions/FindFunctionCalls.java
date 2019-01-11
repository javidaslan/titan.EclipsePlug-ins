/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.ttcn3editor.actions;

import java.util.HashSet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.titan.designer.AST.Assignment.Assignment_type;
import org.eclipse.titan.designer.consoles.TITANDebugConsole;
import org.eclipse.titan.designer.editors.referenceSearch.ReferenceSearch;
import org.eclipse.titan.designer.editors.ttcn3editor.TTCN3Editor;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

/**
 * @author Richárd Koch-Gömöri
 * */
public final class FindFunctionCalls extends AbstractHandler implements IEditorActionDelegate {
	private IEditorPart targetEditor = null;
	private ISelection selection = TextSelection.emptySelection();

	@Override
	public void run(final IAction action) {
		if (targetEditor == null || !(targetEditor instanceof TTCN3Editor)) {
			return;
		}

		HashSet<Assignment_type> functionAssignmentType = new HashSet<>();
		functionAssignmentType.add(Assignment_type.A_FUNCTION);
		functionAssignmentType.add(Assignment_type.A_FUNCTION_RVAL);
		functionAssignmentType.add(Assignment_type.A_FUNCTION_RTEMP);
		functionAssignmentType.add(Assignment_type.A_EXT_FUNCTION);
		functionAssignmentType.add(Assignment_type.A_EXT_FUNCTION_RVAL);
		functionAssignmentType.add(Assignment_type.A_EXT_FUNCTION_RTEMP);

		ReferenceSearch.runAction(targetEditor, selection, functionAssignmentType);
	}

	@Override
	public void selectionChanged(final IAction action, final ISelection selection) {
		this.selection = selection;
	}

	@Override
	public void setActiveEditor(final IAction action, final IEditorPart targetEditor) {
		this.targetEditor = targetEditor;
	}

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		targetEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		if (targetEditor == null || !(targetEditor instanceof TTCN3Editor)) {
			return null;
		}

		HashSet<Assignment_type> functionAssignmentType = new HashSet<>();
		functionAssignmentType.add(Assignment_type.A_FUNCTION);
		functionAssignmentType.add(Assignment_type.A_FUNCTION_RVAL);
		functionAssignmentType.add(Assignment_type.A_FUNCTION_RTEMP);
		functionAssignmentType.add(Assignment_type.A_EXT_FUNCTION);
		functionAssignmentType.add(Assignment_type.A_EXT_FUNCTION_RVAL);
		functionAssignmentType.add(Assignment_type.A_EXT_FUNCTION_RTEMP);

		ReferenceSearch.runAction(targetEditor, selection, functionAssignmentType);
		return null;
	}
}