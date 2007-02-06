/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapData;

public class FeatureAdvancedPage extends PDEFormPage {
	public static final String PAGE_ID = "advanced"; //$NON-NLS-1$

	private InstallSection fInstallSection;

	private HandlerSection fHandlerSection;

	private DataSection fDataSection;

	private DataDetailsSection fDataDetailsSection;

	private DataPortabilitySection fDataPortabilitySection;

	public FeatureAdvancedPage(PDEFormEditor editor, String title) {
		super(editor, PAGE_ID, title);
	}

	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();

		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormTableWrapLayout(true, 2));

		fInstallSection = new InstallSection(this, form.getBody());
		fHandlerSection = new HandlerSection(this, form.getBody());
		fDataSection = new DataSection(this, form.getBody());
		TableWrapData twdata = new TableWrapData(TableWrapData.FILL_GRAB);
		twdata.heightHint = 300;
		twdata.grabVertical = true;
		twdata.rowspan = 2;
		fDataSection.getSection().setLayoutData(twdata);
		fDataDetailsSection = new DataDetailsSection(this, form.getBody());
		fDataDetailsSection.getSection().setLayoutData(
				new TableWrapData(TableWrapData.FILL_GRAB));
		fDataPortabilitySection = new DataPortabilitySection(this, form
				.getBody());
		twdata = new TableWrapData(TableWrapData.FILL_GRAB);
		twdata.grabVertical = true;
		fDataPortabilitySection.getSection().setLayoutData(twdata);

		managedForm.addPart(fInstallSection);
		managedForm.addPart(fHandlerSection);
		managedForm.addPart(fDataSection);
		managedForm.addPart(fDataDetailsSection);
		managedForm.addPart(fDataPortabilitySection);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(),IHelpContextIds.MANIFEST_FEATURE_INSTALLATION);

		form.setText(PDEUIMessages.FeatureEditor_AdvancedPage_heading);
		fDataSection.fireSelection();
	}
}
