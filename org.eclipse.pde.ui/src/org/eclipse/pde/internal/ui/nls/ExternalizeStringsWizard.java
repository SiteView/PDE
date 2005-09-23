/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.nls;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ui.PDEPluginImages;

public class ExternalizeStringsWizard extends Wizard {
	private ExternalizeStringsWizardPage page1;
	private ModelChangeTable fModelChangeTable;

	public ExternalizeStringsWizard(ModelChangeTable changeTable) {
		setWindowTitle("Externalize Strings Wizard");
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_EXTSTR_WIZ);
		setNeedsProgressMonitor(true);
		fModelChangeTable = changeTable;
	}
	
	public boolean performFinish() {
		try {
			getContainer().run(false, false, new ExternalizeStringsOperation());
		} catch (InvocationTargetException e) {
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}
	
	public void addPages() {
		page1 = new ExternalizeStringsWizardPage(fModelChangeTable);
		addPage(page1);
	}
}
