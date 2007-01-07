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
package org.eclipse.pde.internal.ui.wizards.product;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.InternalTargetPlatform;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

public class ProductFileWizadPage extends WizardNewFileCreationPage {
	
	public final static int USE_DEFAULT = 0;
	public final static int USE_PRODUCT = 1;
	public final static int USE_LAUNCH_CONFIG = 2;
	
	private Button fBasicButton;
	private Button fProductButton;
	private Combo fProductCombo;
	private Button fLaunchConfigButton;
	private Combo fLaunchConfigCombo;
	private Group fGroup;
	
	private IPluginModelBase fModel;
	
	public ProductFileWizadPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setDescription(PDEUIMessages.ProductFileWizadPage_title);
		setTitle(PDEUIMessages.NewProductFileWizard_title);
		initializeModel(selection);
	}
	
	private void initializeModel(IStructuredSelection selection) {
		Object selected = selection.getFirstElement();
		if (selected instanceof IAdaptable) {
			IResource resource = (IResource)((IAdaptable)selected).getAdapter(IResource.class);
			if (resource != null) {
				IProject project = resource.getProject();
				fModel = PDECore.getDefault().getModelManager().findModel(project);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createAdvancedControls(org.eclipse.swt.widgets.Composite)
	 */
	protected void createAdvancedControls(Composite parent) {
		fGroup = new Group(parent, SWT.NONE);
		fGroup.setText(PDEUIMessages.ProductFileWizadPage_groupTitle); 
		fGroup.setLayout(new GridLayout(2, false));
		fGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fBasicButton = new Button(fGroup, SWT.RADIO);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fBasicButton.setLayoutData(gd);
		fBasicButton.setText(PDEUIMessages.ProductFileWizadPage_basic); 
		
		fProductButton = new Button(fGroup, SWT.RADIO);
		fProductButton.setText(PDEUIMessages.ProductFileWizadPage_existingProduct); 
		fProductButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fProductCombo.setEnabled(fProductButton.getSelection());
			}
		});
		
		fProductCombo = new Combo(fGroup, SWT.SINGLE|SWT.READ_ONLY);
		fProductCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fProductCombo.setItems(InternalTargetPlatform.getProductNames());
		
		fLaunchConfigButton = new Button(fGroup, SWT.RADIO);
		fLaunchConfigButton.setText(PDEUIMessages.ProductFileWizadPage_existingLaunchConfig); 
		fLaunchConfigButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fLaunchConfigCombo.setEnabled(fLaunchConfigButton.getSelection());
			}
		});
		
		fLaunchConfigCombo = new Combo(fGroup, SWT.SINGLE|SWT.READ_ONLY);
		fLaunchConfigCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fLaunchConfigCombo.setItems(getLaunchConfigurations());
		
		initializeState();
	}
	
	private void initializeState() {
		fLaunchConfigCombo.setEnabled(false);
		if (fLaunchConfigCombo.getItemCount() > 0)
			fLaunchConfigCombo.setText(fLaunchConfigCombo.getItem(0));
		
		if (fModel != null && fModel.getPluginBase().getId() != null) {
			IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				String point = extensions[i].getPoint();
				if ("org.eclipse.core.runtime.products".equals(point)) { //$NON-NLS-1$
					String id = extensions[i].getId();
					if (id != null) {
						String full = fModel.getPluginBase().getId() + "." + id; //$NON-NLS-1$
						if (fProductCombo.indexOf(full) != -1) {
							fProductCombo.setText(full);
							fProductButton.setSelection(true);
							return;
						}
					}
				}
			}
		}
		
		fBasicButton.setSelection(true);

		fProductCombo.setEnabled(false);
		if (fProductCombo.getItemCount() > 0)
			fProductCombo.setText(fProductCombo.getItem(0));

	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validatePage()
	 */
	protected boolean validatePage() {
		if (!getFileName().trim().endsWith(".product")) { //$NON-NLS-1$
			setErrorMessage(PDEUIMessages.ProductFileWizadPage_error); 
			return false;
		}
		if (getFileName().trim().length() <= 8) {
			return false;
		}
		return super.validatePage();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validateLinkedResource()
	 */
	protected IStatus validateLinkedResource() {
		return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createLinkTarget()
	 */
	protected void createLinkTarget() {
	}
	
	private String[] getLaunchConfigurations() {
		ArrayList list = new ArrayList();
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = manager.getLaunchConfigurationType("org.eclipse.pde.ui.RuntimeWorkbench"); //$NON-NLS-1$
			ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
			for (int i = 0; i < configs.length; i++) {
				if (!DebugUITools.isPrivate(configs[i]))
					list.add(configs[i].getName());
			}
		} catch (CoreException e) {
		}
		return (String[])list.toArray(new String[list.size()]);
	}
	
	public ILaunchConfiguration getSelectedLaunchConfiguration() {
		if (!fLaunchConfigButton.getSelection())
			return null;
		
		String configName = fLaunchConfigCombo.getText();
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = manager.getLaunchConfigurationType("org.eclipse.pde.ui.RuntimeWorkbench"); //$NON-NLS-1$
			ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
			for (int i = 0; i < configs.length; i++) {
				if (configs[i].getName().equals(configName) && !DebugUITools.isPrivate(configs[i]))
					return configs[i];
			}
		} catch (CoreException e) {
		}
		return null;
	}
	
	public String getSelectedProduct() {
		return fProductButton.getSelection() ? fProductCombo.getText() : null;
	}
	
	public int getInitializationOption() {
		if (fBasicButton.getSelection())
			return USE_DEFAULT;
		if (fProductButton.getSelection())
			return USE_PRODUCT;
		return USE_LAUNCH_CONFIG;
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		Dialog.applyDialogFont(fGroup);
		setFileName(".product"); //$NON-NLS-1$
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.PRODUCT_FILE_PAGE );
	}
	
}
