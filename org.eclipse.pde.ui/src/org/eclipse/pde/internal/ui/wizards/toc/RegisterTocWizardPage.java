/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.toc;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.plugin.AbbreviatedFragmentModel;
import org.eclipse.pde.internal.core.plugin.AbbreviatedPluginModel;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * RegisterTocWizardPage
 *
 */
public class RegisterTocWizardPage extends WizardPage {

	public static final int NUM_COLUMNS = 2;

	public final static String F_PAGE_NAME = "register-toc"; //$NON-NLS-1$	

	public final static String F_TOC_EXTENSION_POINT_ID = 
		"org.eclipse.help.toc"; //$NON-NLS-1$

	public final static String F_TOC_ELEMENT_TOC = "toc"; //$NON-NLS-1$

	private Button fPrimaryChkBox;
	
	protected IModel fTocModel;
	
	private IPluginModelBase fPluginModelBase;
	
	private IProject fPluginProject;
	
	private boolean fPluginExists;
	
	private boolean fDataIsPrimary;
	
	private IFile fPluginFile;
	
	private boolean fIsFragment;
	
	/**
	 * @param pageName
	 */
	public RegisterTocWizardPage(IModel model) {
		super(F_PAGE_NAME);

		fTocModel = model;
		initialize();
	}

	/**
	 * 
	 */
	private void initialize() {
		
		setTitle(PDEUIMessages.TocWizardPage_title);
		setDescription(PDEUIMessages.RegisterTocWizardPage_wizardPageDescription);		
		
		fPrimaryChkBox = null;
		
		fDataIsPrimary = true;

		// Get the project the TOC is stored in
		fPluginProject = fTocModel.getUnderlyingResource().getProject();

		initializePluginModel();
	}

	/**
	 * 
	 */
	private void initializePluginModel() {
		IPath pluginPath = new Path(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
		IPath fragmentPath = new Path(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
		// Check to see if a plugin or fragment file exists in the project
		// storing the table of contents
		if (fPluginProject.exists(pluginPath)) {
			fPluginFile = fPluginProject.getFile(pluginPath.toPortableString());
			fPluginModelBase = 
				new AbbreviatedPluginModel(fPluginFile, F_TOC_EXTENSION_POINT_ID);	
			fPluginExists = true;
			fIsFragment = false;
		} else if (fPluginProject.exists(fragmentPath)) {
			fPluginFile = fPluginProject.getFile(fragmentPath.toPortableString());
			fPluginModelBase = 
				new AbbreviatedFragmentModel(fPluginFile, F_TOC_EXTENSION_POINT_ID);
			fPluginExists = true;
			fIsFragment = true;
		} else {
			fPluginFile = fPluginProject.getFile(pluginPath.toPortableString());
			fPluginModelBase = null;
			fPluginExists = false;
			fIsFragment = false;
		}
		// Load the plug-in or fragment if it exists in a lightweight model
		if (fPluginExists) {
			try {
				fPluginModelBase.load();
			} catch (CoreException e) {
				PDECore.logException(e);
			}		
		}
	}

	/**
	 * 
	 */
	private void updateUI() {

		if (fPluginExists) {
			// Find all TOC extensions within the host plug-in
			IPluginExtension[] extensions = findExtensions(fPluginModelBase,
					F_TOC_EXTENSION_POINT_ID);

			// Process all TOC elements
			processTocElements(extensions);
		}	
	}

	/**
	 * @param extensions
	 */
	private void processTocElements(IPluginExtension[] extensions) {
		// Query cheat sheet extensions for information required to update
		// the description text and category combo widgets
		// Linear search:  Process all cheat sheet extensions found
		for (int i = 0; i < extensions.length; i++) {
			if (extensions[i].getChildCount() == 0) {
				// Extension has no children, skip to the next extension
				continue;
			}
			IPluginExtension extension = extensions[i];
			IPluginObject[] pluginObjects = extension.getChildren();
			// Process all children
			for (int j = 0; j < pluginObjects.length; j++) {
				if (pluginObjects[j] instanceof IPluginElement) {
					IPluginElement element = (IPluginElement)pluginObjects[j];
					if (element.getName().equals(F_TOC_ELEMENT_TOC)) {
						// TOC element
						processTocElement(element, getDataTocFile());
					}					
					
				}
			}
		}
	}

	/**
	 * @param extensions
	 */
	private void processTocElement(IPluginElement parentElement,
			String generatedID) {
		// Get the id attribute
		IPluginAttribute fileAttribute = 
			parentElement.getAttribute(RegisterTocOperation.F_TOC_ATTRIBUTE_FILE);

		// Check for the generated ID for this TOC
		// If a TOC exists with the generated ID already, read its
		// description and populate the description text accordingly		
		if ((fileAttribute != null) && 
				PDETextHelper.isDefined(fileAttribute.getValue()) &&
				generatedID.equals(fileAttribute.getValue())) {
			// Matching TOC extension found
			// Process children if any
			if (parentElement.getChildCount() > 0) {
				// Update the description text widget
				updateUIPrimary(parentElement);
			}
		}		
	}
	
	private void updateUIPrimary(IPluginElement parentElement) {
		IPluginObject pluginObject = parentElement.getChildren()[0];
		if (pluginObject instanceof IPluginElement) {
			IPluginElement element = (IPluginElement)pluginObject;
			if (element.getName().equals(RegisterTocOperation.F_TOC_ATTRIBUTE_PRIMARY) &&
					PDETextHelper.isDefinedAfterTrim(element.getText())) {
				// Triggers listener to update data description on load
				fPrimaryChkBox.setSelection(Boolean.getBoolean(element.getText().trim()));
			}
		}		
	}

	public boolean getDataPrimary()
	{	return fDataIsPrimary;
	}

	public boolean pluginExists() {
		return fPluginExists;
	}
	
	public String getDataTocFile() {
		// Retrieve the full path to the TOC
		IPath path = 
			fTocModel.getUnderlyingResource().getFullPath();
		return path.removeFirstSegments(1).toPortableString();
	}
	
	public IFile getPluginFile() {
		return fPluginFile;
	}

	public boolean isFragment() {
		return fIsFragment;
	}
	
	public IProject getPluginProject() {
		return fPluginProject;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		createUI(parent);
		createUIListeners();

		updateUI();
		validateUI();
	}

	/**
	 * @param parent
	 */
	private void createUI(Composite parent) {
		// Create the container
		Composite container = createUIContainer(parent);

		// Create the primary checkbox
		createUIPrimaryChkBox(container);

		// Set the control for the reciever
		// Must be done otherwise a null assertion error is generated
		setControl(container);
		// Apply the dialog font to all controls using the default font
		Dialog.applyDialogFont(container);
		// Provide functionality for the help button
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container,
				IHelpContextIds.REGISTER_TOC);
	}

	/**
	 * @param parent
	 * @return
	 */
	private Composite createUIContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = NUM_COLUMNS;
		container.setLayout(layout);
		return container;
	}

	private void createUIPrimaryChkBox(Composite parent)
	{	fPrimaryChkBox = new Button(parent, SWT.CHECK);
		fPrimaryChkBox.setText(PDEUIMessages.RegisterTocWizardPage_labelPrimary);
		GridData data = new GridData();
		data.horizontalSpan = NUM_COLUMNS;
		fPrimaryChkBox.setLayoutData(data);
		fPrimaryChkBox.setSelection(true);
	}
	
	/**
	 * 
	 */
	private void createUIListeners() {
		// Create listeners for the primary check box
		createUIListenersPrimaryChkBox();
	}

	/**
	 * 
	 */
	private void createUIListenersPrimaryChkBox() {
		fPrimaryChkBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fDataIsPrimary = fPrimaryChkBox.getSelection();
			}
		});
	}	

	/**
	 * 
	 */
	private void validateUI() {
		setPageComplete(true);
	}	
	
	/**
	 * @param model
	 * @param extensionPointID
	 * @return
	 */
	public IPluginExtension[] findExtensions(IPluginModelBase model,
			String extensionPointID) {
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		
		ArrayList tocExtensions = new ArrayList();
		for (int i = 0; i < extensions.length; i++) {
			String point = extensions[i].getPoint();
			if (extensionPointID.equals(point)) {
				tocExtensions.add(extensions[i]);
			}
		}
		return (IPluginExtension[]) tocExtensions.toArray(
				new IPluginExtension[tocExtensions.size()]);
	}	
}