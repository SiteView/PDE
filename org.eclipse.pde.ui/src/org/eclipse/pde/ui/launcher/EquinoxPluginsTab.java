/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.launcher.EquinoxPluginBlock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.PlatformUI;

public class EquinoxPluginsTab extends AbstractLauncherTab {

	private Image fImage;
	private EquinoxPluginBlock fPluginBlock;
	protected Listener fListener = new Listener();
	private Combo fDefaultAutoStart;
	private Spinner fDefaultStartLevel;
	
	class Listener extends SelectionAdapter implements ModifyListener{
		public void widgetSelected(SelectionEvent e) {
			updateLaunchConfigurationDialog();
		}

		public void modifyText(ModifyEvent e) {
			updateLaunchConfigurationDialog();
		}
	}

	public EquinoxPluginsTab() {
		fImage = PDEPluginImages.DESC_REQ_PLUGINS_OBJ.createImage();
		fPluginBlock = new EquinoxPluginBlock(this);
	}

	public void dispose() {
		fPluginBlock.dispose();
		fImage.dispose();
		super.dispose();
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		createDefaultsGroup(composite);
		fPluginBlock.createControl(composite);

		setControl(composite);
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.LAUNCHER_ADVANCED);
	}
	
	private void createDefaultsGroup(Composite container) {
		Composite defaults = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout(5, false);
		defaults.setLayout(layout);
		defaults.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		
		Label startLevelLabel = new Label(defaults, SWT.NONE);
		startLevelLabel.setText("Default Start Level:");
		
		fDefaultStartLevel = new Spinner(defaults, SWT.BORDER);
		fDefaultStartLevel.setMinimum(1);
		fDefaultStartLevel.addModifyListener(fListener);
		
		Label label = new Label(defaults, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.minimumWidth = 50;
		label.setLayoutData(gd);
		
		Label autoStartLabel = new Label(defaults, SWT.NONE);
		autoStartLabel.setText("Default Auto-Start:");
		
		fDefaultAutoStart = new Combo(defaults, SWT.BORDER | SWT.READ_ONLY);
		fDefaultAutoStart.setItems(new String[] {Boolean.toString(true), Boolean.toString(false)});
		fDefaultAutoStart.select(0);
		fDefaultAutoStart.addSelectionListener(fListener);
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config) {
		try {
			boolean auto = config.getAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, true);
			fDefaultAutoStart.setText(Boolean.toString(auto));
			int level = config.getAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, 4);
			fDefaultStartLevel.setSelection(level);
			fPluginBlock.initializeFrom(config);
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, true);
		config.setAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, 4);
		fPluginBlock.setDefaults(config);
	}
	
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, 
				Boolean.toString(true).equals(fDefaultAutoStart.getText()));
		config.setAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, fDefaultStartLevel.getSelection());
		fPluginBlock.performApply(config);
	}

	public String getName() {
		return PDEUIMessages.AdvancedLauncherTab_name; 
	}

	public Image getImage() {
		return fImage;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#activated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void activated(ILaunchConfigurationWorkingCopy config) {
	}

	public void validatePage() {
	}

}
