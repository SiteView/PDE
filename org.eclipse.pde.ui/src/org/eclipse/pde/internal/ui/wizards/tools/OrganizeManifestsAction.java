/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

public class OrganizeManifestsAction implements IWorkbenchWindowActionDelegate {
	
	private ISelection fSelection;

	public OrganizeManifestsAction() {
		super();
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}

	public void run(IAction action) {
		
		if (!PlatformUI.getWorkbench().saveAllEditors(true))
			return;
		
		if (fSelection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) fSelection;
			Iterator it = ssel.iterator();
			ArrayList projects = new ArrayList();
			while (it.hasNext()) {
				Object element = it.next();
				IProject proj = null;
				if (element instanceof IFile)
					proj = ((IFile)element).getProject();
				else if (element instanceof IProject)
					proj = (IProject) element;
				if (proj != null && proj.exists(ICoreConstants.MANIFEST_PATH))
					projects.add(proj);
			}
			if (projects.size() > 0) {
				OrganizeManifestsWizard wizard = new OrganizeManifestsWizard(projects);
				final WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
				BusyIndicator.showWhile(
						PDEPlugin.getActiveWorkbenchShell().getDisplay(), 
						new Runnable() {
					public void run() {
						dialog.open();
					}
				});
			} else
				MessageDialog.openInformation(
						PDEPlugin.getActiveWorkbenchShell(),
						PDEUIMessages.OrganizeManifestsWizardPage_title,
						PDEUIMessages.OrganizeManifestsWizardPage_errorMsg);
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;
	}

}
