/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.target;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.itarget.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class TargetProfileWindow extends ApplicationWindow {

	protected ITargetModel fTargetModel;
	private FormToolkit fToolkit;
	private String fTargetName;

	public TargetProfileWindow(Shell parentShell, ITargetModel model, String targetName) {
		super(parentShell);
		setShellStyle(SWT.MAX | SWT.RESIZE | SWT.CLOSE | SWT.APPLICATION_MODAL);
		fTargetModel = model;
		fTargetName = targetName;
	}

	public void create() {
		super.create();
		getShell().setText(PDEUIMessages.TargetProfileWindow_title);
		getShell().setSize(500, 300);
	}

	protected Control createContents(Composite parent) {
		fToolkit = new FormToolkit(parent.getDisplay());
		CTabFolder folder = new CTabFolder(parent, SWT.NONE);
		folder.setLayout(new GridLayout());
		fToolkit.adapt(folder, true, true);
		fToolkit.adapt(parent);
		fToolkit.getColors().initializeSectionToolBarColors();
		Color selectedColor = fToolkit.getColors().getColor(IFormColors.TB_BG);
		folder.setSelectionBackground(new Color[] {selectedColor, fToolkit.getColors().getBackground()}, new int[] {100}, true);

		CTabItem item = new CTabItem(folder, SWT.NONE);
		item.setControl(createDefinitionTab(folder, fToolkit));
		item.setText(PDEUIMessages.TargetProfileWindow_definition);

		ITarget target = fTargetModel.getTarget();

		ITargetPlugin[] plugins = target.getPlugins();
		if (!target.useAllPlugins() && plugins.length > 0) {
			item = new CTabItem(folder, SWT.NONE);
			item.setControl(createTabularTab(folder, fToolkit, plugins));
			item.setText(PDEUIMessages.TargetProfileWindow_plugins);
		}

		ITargetFeature[] features = target.getFeatures();
		if (!target.useAllPlugins() && features.length > 0) {
			item = new CTabItem(folder, SWT.NONE);
			item.setControl(createTabularTab(folder, fToolkit, features));
			item.setText(PDEUIMessages.TargetProfileWindow_features);
		}

		item = new CTabItem(folder, SWT.NONE);
		item.setControl(createEnvironmentTab(folder, fToolkit));
		item.setText(PDEUIMessages.TargetProfileWindow_environment);

		IArgumentsInfo argInfo = target.getArguments();
		if (argInfo != null) {
			item = new CTabItem(folder, SWT.NONE);
			item.setControl(createArgumentsTab(folder, fToolkit, argInfo));
			item.setText(PDEUIMessages.TargetProfileWindow_launching);
		}

		IImplicitDependenciesInfo info = target.getImplicitPluginsInfo();
		if (info != null) {
			item = new CTabItem(folder, SWT.NONE);
			item.setControl(createTabularTab(folder, fToolkit, info.getPlugins()));
			item.setText(PDEUIMessages.TargetProfileWindow_implicit);
		}

		return folder;
	}

	private Control createDefinitionTab(Composite parent, FormToolkit toolkit) {
		ScrolledForm form = toolkit.createScrolledForm(parent);
		Composite body = form.getBody();
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = 5;
		body.setLayout(layout);

		ITarget target = fTargetModel.getTarget();
		createEntry(body, toolkit, PDEUIMessages.TargetDefinitionSection_name, fTargetName);
		createEntry(body, toolkit, PDEUIMessages.TargetDefinitionSection_targetLocation, getLocation(target));

		IAdditionalLocation[] locs = target.getAdditionalDirectories();
		if (locs.length > 0) {
			Label label = toolkit.createLabel(body, PDEUIMessages.TargetProfileWindow_additionalLocations);
			label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
			label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
			createTable(body, toolkit, locs);
		}

		String desc = target.getDescription();
		if (desc != null) {
			FormEntry entry = createEntry(body, toolkit, PDEUIMessages.TargetProfileWindow_targetDescription, desc, SWT.WRAP | SWT.V_SCROLL);
			GridData gd = new GridData(GridData.FILL_BOTH);
			gd.widthHint = 200;
			entry.getText().setLayoutData(gd);
			entry.getLabel().setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		}

		toolkit.paintBordersFor(form.getBody());
		return form;
	}

	private Control createEnvironmentTab(Composite parent, FormToolkit toolkit) {
		ScrolledForm form = toolkit.createScrolledForm(parent);
		Composite body = form.getBody();
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = 5;
		body.setLayout(layout);

		ITarget target = fTargetModel.getTarget();
		IEnvironmentInfo info = target.getEnvironment();

		String os = info == null ? Platform.getOS() : info.getDisplayOS();
		createEntry(body, toolkit, PDEUIMessages.EnvironmentSection_operationSystem, os);

		String ws = info == null ? Platform.getWS() : info.getDisplayWS();
		createEntry(body, toolkit, PDEUIMessages.EnvironmentSection_windowingSystem, ws);

		String arch = info == null ? Platform.getOSArch() : info.getDisplayArch();
		createEntry(body, toolkit, PDEUIMessages.EnvironmentSection_architecture, arch);

		String nl = info == null ? Platform.getNL() : info.getDisplayNL();
		createEntry(body, toolkit, PDEUIMessages.EnvironmentSection_locale, nl);

		ITargetJRE jreInfo = target.getTargetJREInfo();
		String jre = jreInfo == null ? JavaRuntime.getDefaultVMInstall().getName() : jreInfo.getCompatibleJRE();
		createEntry(body, toolkit, PDEUIMessages.TargetProfileWindow_jre, jre);

		toolkit.paintBordersFor(form.getBody());
		return form;
	}

	private Control createTabularTab(Composite parent, FormToolkit toolkit, Object[] objects) {
		ScrolledForm form = toolkit.createScrolledForm(parent);
		Composite body = form.getBody();
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 5;
		body.setLayout(layout);

		createTable(body, toolkit, objects);

		toolkit.paintBordersFor(form.getBody());
		return form;
	}

	private Control createTable(Composite parent, FormToolkit toolkit, Object[] objects) {
		int style = SWT.H_SCROLL | SWT.V_SCROLL | toolkit.getBorderStyle();

		TableViewer tableViewer = new TableViewer(parent, style);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		tableViewer.setInput(objects);
		tableViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		return tableViewer.getControl();
	}

	private Control createArgumentsTab(Composite parent, FormToolkit toolkit, IArgumentsInfo info) {
		ScrolledForm form = toolkit.createScrolledForm(parent);
		Composite body = form.getBody();
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 5;
		body.setLayout(layout);

		FormEntry entry = createEntry(body, toolkit, PDEUIMessages.TargetProfileWindow_program, info.getProgramArguments());
		entry.getText().setLayoutData(new GridData(GridData.FILL_BOTH));
		entry = createEntry(body, toolkit, PDEUIMessages.TargetProfileWindow_vm, info.getVMArguments());
		entry.getText().setLayoutData(new GridData(GridData.FILL_BOTH));

		toolkit.paintBordersFor(form.getBody());
		return form;
	}

	private FormEntry createEntry(Composite client, FormToolkit toolkit, String text, String value) {
		return createEntry(client, toolkit, text, value, SWT.NONE);
	}

	private FormEntry createEntry(Composite client, FormToolkit toolkit, String text, String value, int style) {
		FormEntry entry = new FormEntry(client, toolkit, text, style);
		entry.setValue(value);
		entry.setEditable(false);
		return entry;
	}

	private String getLocation(ITarget target) {
		ILocationInfo info = target.getLocationInfo();
		if (info == null || info.useDefault())
			return TargetPlatform.getDefaultLocation();
		try {
			return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(info.getPath());
		} catch (CoreException e) {
		}
		return ""; //$NON-NLS-1$
	}

	public boolean close() {
		fToolkit.dispose();
		return super.close();
	}

}
