/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.widgets.FormToolkit;

public abstract class PDEDetails extends AbstractFormPart implements IDetailsPage, IContextPart {

	public PDEDetails() {
	}
	
	public boolean canPaste(Clipboard clipboard) {
		return true;
	}
	
	public boolean doGlobalAction(String actionId) {
		return false;
	}
	
	protected void markDetailsPart(Control control) {
		control.setData("part", this); //$NON-NLS-1$
	}
	
	protected void createSpacer(FormToolkit toolkit, Composite parent, int span) {
		Label spacer = toolkit.createLabel(parent, ""); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		spacer.setLayoutData(gd);
	}
	public void cancelEdit() {
		super.refresh();
	}
	
	/**
	 * @param parent
	 * @param toolkit
	 * @param columnSpan
	 * @param text
	 */
	public Label createLabel(Composite parent, FormToolkit toolkit,
			int columnSpan, String text, Color foreground) {
		
		Label label = toolkit.createLabel(parent, text, SWT.WRAP);
		//label.setForeground(foreground);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		//data.widthHint = 150;
		data.horizontalSpan = columnSpan;
		label.setLayoutData(data);
		if (foreground != null) {
			label.setForeground(foreground);
		}
		return label;
	}
}
