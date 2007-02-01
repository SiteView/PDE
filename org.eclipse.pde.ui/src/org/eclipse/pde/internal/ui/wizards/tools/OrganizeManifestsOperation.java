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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ISharedExtensionsModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.search.dependencies.AddNewDependenciesOperation;
import org.eclipse.pde.internal.ui.search.dependencies.CalculateUsesOperation;
import org.eclipse.pde.internal.ui.search.dependencies.GatherUnusedDependenciesOperation;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;

public class OrganizeManifestsOperation implements IRunnableWithProgress, IOrganizeManifestsSettings {
	
	// if operation is executed without setting operations, these defaults will be used
	protected boolean fAddMissing = true; // add all packages to export-package
	protected boolean fMarkInternal = true; // mark export-package as internal
	protected String fPackageFilter = VALUE_DEFAULT_FILTER;
	protected boolean fRemoveUnresolved = true; // remove unresolved export-package
	protected boolean fCalculateUses = false; // calculate the 'uses' directive for exported packages
	protected boolean fModifyDep = true; // modify import-package / require-bundle
	protected boolean fRemoveDependencies = true; // if true: remove, else mark optional
	protected boolean fUnusedDependencies; // find/remove unused dependencies - long running op
	protected boolean fRemoveLazy = true; // remove lazy/auto start if no activator
	protected boolean fPrefixIconNL; // prefix icon paths with $nl$
	protected boolean fUnusedKeys; // remove unused <bundle-localization>.properties keys
	protected boolean fAddDependencies;
	
	private ArrayList fProjectList;
	private IProject fCurrentProject;
	
	public OrganizeManifestsOperation(ArrayList projectList) {
		fProjectList = projectList;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		monitor.beginTask(PDEUIMessages.OrganizeManifestJob_taskName, fProjectList.size());
		for (int i = 0; i < fProjectList.size() && !monitor.isCanceled(); i++)
			cleanProject((IProject)fProjectList.get(i), new SubProgressMonitor(monitor, 1));
	}
	
	private void cleanProject(IProject project, IProgressMonitor monitor) {
		fCurrentProject = project;
		monitor.beginTask(fCurrentProject.getName(), getTotalTicksPerProject());
		
		final Exception[] ee = new Exception[1];
		ModelModification modification = new ModelModification(fCurrentProject) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBundlePluginModelBase)
					try {
						runCleanup(monitor, (IBundlePluginModelBase)model);
					} catch (InvocationTargetException e) {
						ee[0] = e;
					} catch (InterruptedException e) {
						ee[0] = e;
					}
			}
		};
		PDEModelUtility.modifyModel(modification, monitor);
		if (ee[0] != null)
			PDEPlugin.log(ee[0]);
	}
	
	
	private void runCleanup(IProgressMonitor monitor, IBundlePluginModelBase modelBase) throws InvocationTargetException, InterruptedException {
		
		IBundle bundle = modelBase.getBundleModel().getBundle();
		ISharedExtensionsModel sharedExtensionsModel = modelBase.getExtensionsModel();
		IPluginModelBase extensionsModel = null;
		if (sharedExtensionsModel instanceof IPluginModelBase)
			extensionsModel = (IPluginModelBase)sharedExtensionsModel;
		
		String projectName = fCurrentProject.getName();
		
		if (fAddMissing || fRemoveUnresolved) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_export, projectName));
			if (!monitor.isCanceled())
				OrganizeManifest.organizeExportPackages(bundle, fCurrentProject, fAddMissing, fRemoveUnresolved);
			if (fAddMissing)
				monitor.worked(1);
			if (fRemoveUnresolved)
				monitor.worked(1);
		}
		
		if (fMarkInternal) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_filterInternal, projectName));
			if (!monitor.isCanceled())
				OrganizeManifest.markPackagesInternal(bundle, fPackageFilter);
			monitor.worked(1);
		}
		
		if (fModifyDep) {
			String message = fRemoveDependencies ?
					NLS.bind(PDEUIMessages.OrganizeManifestsOperation_removeUnresolved, projectName) :
						NLS.bind(PDEUIMessages.OrganizeManifestsOperation_markOptionalUnresolved, projectName);
			monitor.subTask(message);
			if (!monitor.isCanceled())
				OrganizeManifest.organizeImportPackages(bundle, fRemoveDependencies);
			monitor.worked(1);
			
			if (!monitor.isCanceled())
				OrganizeManifest.organizeRequireBundles(bundle, fRemoveDependencies);
			monitor.worked(1);
		}
		
		if (fCalculateUses) {
			// we don't set the subTask because it is done in the CalculateUsesOperation, for each package it scans
			if (!monitor.isCanceled()) {
				CalculateUsesOperation op = new CalculateUsesOperation(fCurrentProject, modelBase);
				op.run(new SubProgressMonitor(monitor, 2));
			}
		}
		
		if (fAddDependencies) {
			monitor.subTask(NLS.bind (PDEUIMessages.OrganizeManifestsOperation_additionalDeps, projectName));
			if (!monitor.isCanceled()) {
				AddNewDependenciesOperation op = new AddNewDependenciesOperation(fCurrentProject, modelBase);
				op.run(new SubProgressMonitor(monitor, 4));
			}
		}
		
		if (fUnusedDependencies) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_unusedDeps, projectName));
			if (!monitor.isCanceled()) {
				SubProgressMonitor submon = new SubProgressMonitor(monitor, 4);
				GatherUnusedDependenciesOperation udo = new GatherUnusedDependenciesOperation(modelBase);
				udo.run(submon);
				GatherUnusedDependenciesOperation.removeDependencies(modelBase, udo.getList().toArray());
				submon.done();
			}
		}
		
		if (fRemoveLazy) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_lazyStart, fCurrentProject.getName()));
			if (!monitor.isCanceled())
				OrganizeManifest.removeUnneededLazyStart(bundle);
			monitor.worked(1);
		}
		
		if (fPrefixIconNL) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_nlIconPath, projectName));
			if (!monitor.isCanceled())
				OrganizeManifest.prefixIconPaths(extensionsModel);
			monitor.worked(1);
		}
		
		if (fUnusedKeys) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_unusedKeys, projectName));
			if (!monitor.isCanceled())
				OrganizeManifest.removeUnusedKeys(fCurrentProject, bundle, extensionsModel);
			monitor.worked(1);
		}
	}

	private int getTotalTicksPerProject() {
		int ticks = 0;
		if (fAddMissing)		ticks += 1;
		if (fMarkInternal)		ticks += 1;
		if (fRemoveUnresolved)	ticks += 1;
		if (fCalculateUses)		ticks += 4;
		if (fModifyDep)			ticks += 2;
		if (fUnusedDependencies)ticks += 4;
		if (fAddDependencies)	ticks += 4;
		if (fRemoveLazy)		ticks += 1;
		if (fPrefixIconNL)		ticks += 1;
		if (fUnusedKeys)		ticks += 1;
		return ticks;
	}
	
	
	public void setOperations(IDialogSettings settings) {
		fAddMissing = !settings.getBoolean(PROP_ADD_MISSING);
		fMarkInternal = !settings.getBoolean(PROP_MARK_INTERNAL);
		fPackageFilter = settings.get(PROP_INTERAL_PACKAGE_FILTER);
		fRemoveUnresolved = !settings.getBoolean(PROP_REMOVE_UNRESOLVED_EX);
		fCalculateUses = settings.getBoolean(PROP_CALCULATE_USES);
		fModifyDep = !settings.getBoolean(PROP_MODIFY_DEP);
		fRemoveDependencies = !settings.getBoolean(PROP_RESOLVE_IMP_MARK_OPT);
		fUnusedDependencies = settings.getBoolean(PROP_UNUSED_DEPENDENCIES);
		fRemoveLazy = !settings.getBoolean(PROP_REMOVE_LAZY);
		fPrefixIconNL = settings.getBoolean(PROP_NLS_PATH);
		fUnusedKeys = settings.getBoolean(PROP_UNUSED_KEYS);
		fAddDependencies = settings.getBoolean(PROP_ADD_DEPENDENCIES);
	}
}
