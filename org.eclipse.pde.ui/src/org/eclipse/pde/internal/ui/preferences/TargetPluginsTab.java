/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak <brockj@tpg.com.au> - bug 189533
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.DependencyManager;
import org.eclipse.pde.internal.core.ExternalFeatureModelManager;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ModelProviderEvent;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEState;
import org.eclipse.pde.internal.core.PluginPathFinder;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.TargetPlatformResetJob;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetFeature;
import org.eclipse.pde.internal.core.itarget.ITargetPlugin;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.target.TargetErrorDialog;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.SharedPartWithButtons;
import org.eclipse.pde.internal.ui.util.PersistablePluginObject;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.pde.internal.ui.wizards.provisioner.AddTargetPluginsWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.progress.IProgressConstants;

public class TargetPluginsTab extends SharedPartWithButtons{
	private CheckboxTableViewer fPluginListViewer;
	private TargetPlatformPreferencePage fPage;
	private boolean fReloaded;
	private CheckboxTreeViewer fPluginTreeViewer;
	private HashSet fChangedModels = new HashSet();
	private PDEState fCurrentState;
	private PageBook fBook;
	private Button fGroupPlugins;
	private HashMap fTreeViewerContents;
	private Label fCounterLabel;
	private int fCounter;
	private Map fCurrentFeatures;
	private ArrayList fAdditionalLocations = new ArrayList();
	
	class ReloadOperation implements IRunnableWithProgress {
		private String location;
		
		public ReloadOperation(String platformPath) {
			this.location = platformPath;
		}
		
		private URL[] computePluginURLs() {
			URL[] base  = PluginPathFinder.getPluginPaths(location);		
			if (fAdditionalLocations.size() == 0)
				return base;
			
			File[] extraLocations = new File[fAdditionalLocations.size() * 2];
			for (int i = 0; i < extraLocations.length; i++) {
				String location = fAdditionalLocations.get(i/2).toString();
				File dir = new File(location);
				extraLocations[i] = dir;
				dir = new File(dir, "plugins"); //$NON-NLS-1$
				extraLocations[++i] = dir;
			}

			URL[] additional = PluginPathFinder.scanLocations(extraLocations);			
			if (additional.length == 0)
				return base;
			
			URL[] result = new URL[base.length + additional.length];
			System.arraycopy(base, 0, result, 0, base.length);
			System.arraycopy(additional, 0, result, base.length, additional.length);			
			return result;
		}
			
		public void run(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {	
			monitor.beginTask(PDEUIMessages.TargetPluginsTab_readingPlatform, 10);
			SubProgressMonitor parsePluginMonitor = new SubProgressMonitor(monitor, 9);
			fCurrentState = new PDEState(computePluginURLs(), true, parsePluginMonitor);
			loadFeatures(new SubProgressMonitor(monitor, 1));
			monitor.done();
			fTreeViewerContents.clear();
			initializeTreeContents(getCurrentModels());
		}
		
		private void loadFeatures(IProgressMonitor monitor) {
			IFeatureModel[] externalModels = ExternalFeatureModelManager.createModels(location, fAdditionalLocations, monitor);
			IFeatureModel[] workspaceModels = PDECore.getDefault().getFeatureModelManager().getWorkspaceModels();
			int numFeatures = externalModels.length + workspaceModels.length;
			fCurrentFeatures = new HashMap((4/3) * (numFeatures) + 1);
			for (int i = 0; i < externalModels.length; i++) {
				String id = externalModels[i].getFeature().getId();
				if (id != null)
					fCurrentFeatures.put(id, externalModels[i]);
			}
			for (int i = 0; i < workspaceModels.length; i++) {
				String id = workspaceModels[i].getFeature().getId();
				if (id != null)
					fCurrentFeatures.put(id, workspaceModels[i]);
			}
			monitor.done();
		}
		
	}
	
	public class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getCurrentModels();
		}
	}
	
	public class TreePluginContentProvider extends DefaultContentProvider 
		implements ITreeContentProvider{
		
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof File) {
				Set files = (Set)fTreeViewerContents.get(parentElement);
				if (files != null) {
					Object[] result = files.toArray();
					return result;
				}
			}
			return new Object[0];
		}

		public Object getParent(Object element) {
			if (element instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase) element;
				String installPath = model.getInstallLocation();
				if (installPath != null)
					return new File(installPath).getParentFile();
			}
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof File)
				return fTreeViewerContents.containsKey(element);
			return false;
		}

		public Object[] getElements(Object inputElement) {
			if (fTreeViewerContents == null)
				return initializeTreeContents(getCurrentModels()).toArray();
			return fTreeViewerContents.keySet().toArray();
		}

	}
	
	public TargetPluginsTab(TargetPlatformPreferencePage page) {
		super(new String[]{ PDEUIMessages.ExternalPluginsBlock_reload,
			  PDEUIMessages.TargetPluginsTab_add,
			  null,
			  null,
			  PDEUIMessages.WizardCheckboxTablePart_selectAll,
			  PDEUIMessages.WizardCheckboxTablePart_deselectAll,
			  PDEUIMessages.ExternalPluginsBlock_workingSet, 
			  PDEUIMessages.ExternalPluginsBlock_addRequired});
		this.fPage = page;
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String additional = preferences.getString(ICoreConstants.ADDITIONAL_LOCATIONS);
		StringTokenizer tokenizer = new StringTokenizer(additional, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			fAdditionalLocations.add(tokenizer.nextToken().trim());
		}
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	void computeDelta() {
		int type = 0;
		IModel[] changedArray = null;
		if (fChangedModels.size() > 0) {
			type |= IModelProviderEvent.MODELS_CHANGED;
			changedArray = (IModel[]) fChangedModels.toArray(new IModel[fChangedModels.size()]);
		}
		fChangedModels.clear();
		if (type != 0) {
			ExternalModelManager registry =
				PDECore.getDefault().getModelManager().getExternalModelManager();
			ModelProviderEvent event =
				new ModelProviderEvent(
					registry,
					type,
					null,
					null,
					changedArray);
			registry.fireModelProviderEvent(event);
		}
	}

	public Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 5;
		container.setLayout(layout);
		
		super.createControl(container, SWT.NONE, 2, null);
		
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		
		fGroupPlugins = new Button (container, SWT.CHECK);
		gd = new GridData();
		gd.horizontalSpan = 2;
		fGroupPlugins.setLayoutData(gd);
		fGroupPlugins.setText(PDEUIMessages.TargetPluginsTab_groupPlugins);
		fGroupPlugins.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSwitchView();
			}
		});
		
		initializeView();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.TARGET_PLUGINS_PREFERENCE_PAGE);
		return container;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.parts.SharedPartWithButtons#createButtons(org.eclipse.swt.widgets.Composite, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createButtons(Composite parent, FormToolkit toolkit) {
		super.createButtons(parent, toolkit);
		
		fCounterLabel = new Label(fButtonContainer, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessVerticalSpace = true;
		gd.verticalAlignment = SWT.BOTTOM;
		fCounterLabel.setLayoutData(gd);
	}
	
	protected void initializeView() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		boolean groupPlugins = preferences.getBoolean(ICoreConstants.GROUP_PLUGINS_VIEW);
		fGroupPlugins.setSelection(groupPlugins);
		if (groupPlugins) {
			fBook.showPage(fPluginTreeViewer.getControl());
		}
		else {
			fBook.showPage(fPluginListViewer.getControl());
		}
	}
	
	protected void createMainControl(Composite parent, int style, int span, FormToolkit toolkit) {
		fBook = new PageBook(parent, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 100;
		gd.widthHint = 250;
		fBook.setLayoutData(gd);
		createTableViewer(fBook);
		createTreeViewer(fBook);
	}
	
	private void createTableViewer(Composite container) {
		fPluginListViewer = CheckboxTableViewer.newCheckList(container, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 100;
		gd.widthHint = 250;
		fPluginListViewer.getControl().setLayoutData(gd);
		fPluginListViewer.setContentProvider(new PluginContentProvider());
		fPluginListViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fPluginListViewer.setComparator(ListUtil.PLUGIN_COMPARATOR);
		fPluginListViewer.addCheckStateListener(new ICheckStateListener() {

			public void checkStateChanged(CheckStateChangedEvent event) {
				IPluginModelBase model = (IPluginModelBase) event.getElement();
				boolean checked = event.getChecked();
				if (fChangedModels.contains(model) && model.isEnabled() == checked) {
					fChangedModels.remove(model);
				} else if (model.isEnabled() != checked) {
					fChangedModels.add(model);
				}
				
				// handle checking the TreeViewer
				fPluginTreeViewer.setChecked(model, checked);
				String path = model.getInstallLocation();
				if (path != null) {
					File parent = new File(path).getParentFile();
					if (checked) {
						fPluginTreeViewer.setChecked(parent, true);
						handleGrayChecked(parent, false);
					} else
						handleGrayChecked(parent, true);
				}
				
				// update the counter
				if (checked) 
					setCounter(fCounter + 1);
				else
					setCounter(fCounter - 1);
			}
			
		});
	}
	
	private void createTreeViewer(Composite container) {
		fPluginTreeViewer = new CheckboxTreeViewer(container, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER );
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 100;
		gd.widthHint = 250;
		fPluginTreeViewer.getControl().setLayoutData(gd);
		fPluginTreeViewer.setContentProvider(new TreePluginContentProvider());
		fPluginTreeViewer.setComparator(ListUtil.PLUGIN_COMPARATOR);
		fPluginTreeViewer.setLabelProvider(new PDELabelProvider() {
			
			public Image getImage(Object obj) {
				if (obj instanceof File) {
					return get(PDEPluginImages.DESC_SITE_OBJ);
				}
				return super.getImage(obj);
			}
		});
		fPluginTreeViewer.addCheckStateListener(new ICheckStateListener() {

			public void checkStateChanged(CheckStateChangedEvent event) {
				Object element = event.getElement();
				boolean checked = event.getChecked();
				
				// if user selected a plugin in the TreeViewer
				if (element instanceof IPluginModelBase) {
					IPluginModelBase model = (IPluginModelBase) event.getElement();
					if (fChangedModels.contains(model) && model.isEnabled() == checked) {
						fChangedModels.remove(model);
					} else if (model.isEnabled() != checked) {
						fChangedModels.add(model);
					}
					// update Table Viewer
					fPluginListViewer.setChecked(model, checked);
					
					// update parent in tree
					String path = model.getInstallLocation();
					if (path != null) {
						File parent = new File(path).getParentFile();
						if (checked) {
							fPluginTreeViewer.setChecked(parent, true);
							handleGrayChecked(parent, false);
						}
						else 
							handleGrayChecked(parent, true);
					}
					
					// update table
					if (checked) 
						setCounter(fCounter + 1);
					else
						setCounter(fCounter - 1);
					
				// else if the user selected and eclipse directory in the TreeViewer
				} else if (element instanceof File) {
					int changedCount = 0;
					Set plugins = (Set) fTreeViewerContents.get(element);
					// iterator through plugins from selected eclipse directory
					IPluginModelBase [] models = (IPluginModelBase[]) plugins.toArray( new IPluginModelBase[ plugins.size()]);
					for (int i  = 0 ; i < models.length; i++) {
						if (fChangedModels.contains(models[i]) && models[i].isEnabled() == checked) {
							fChangedModels.remove(models[i]);
						} else if (models[i].isEnabled() != checked) {
							fChangedModels.add(models[i]);
						}
						if (checked && !fPluginTreeViewer.getChecked(models[i]))
							++changedCount;
						else if (!checked && fPluginTreeViewer.getChecked(models[i]))
							--changedCount;
						fPluginListViewer.setChecked(models[i], checked);
					}
					// update the element in the TreeViewer
					fPluginTreeViewer.setSubtreeChecked(element, checked);
					fPluginTreeViewer.setGrayed(element, false);
					fPluginTreeViewer.setChecked(element, checked);
					setCounter(fCounter + changedCount);
				}
			}
			
		});
		fPluginTreeViewer.setAutoExpandLevel(2);
	}
	
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	protected void loadTargetProfile(ITarget target) {
		if (target.useAllPlugins()) {
			handleSelectAll(true);
			return;
		}
		Map required = new HashMap(), missingFeatures = new HashMap();
		Set optional = new HashSet();
		ITargetFeature[] targetFeatures = target.getFeatures();
		Stack features = new Stack();
		
		FeatureModelManager featureManager = null;
		if (fCurrentFeatures == null)
			featureManager = PDECore.getDefault().getFeatureModelManager();
		for (int i = 0 ; i < targetFeatures.length; i++) {
			IFeatureModel model = (featureManager != null) ? featureManager.findFeatureModel(targetFeatures[i].getId()) :
				(IFeatureModel)fCurrentFeatures.get(targetFeatures[i].getId());
			if (model != null)
				features.push(model.getFeature());
			else if (!targetFeatures[i].isOptional()) {
				missingFeatures.put(targetFeatures[i].getId(), targetFeatures[i]);
				break;
			}
			while (!features.isEmpty()) {
				IFeature feature = (IFeature) features.pop();
				IFeaturePlugin [] plugins = feature.getPlugins();
				for (int j = 0; j < plugins.length; j++) {
					if (target.isValidFeatureObject(plugins[j])) {
						if (targetFeatures[i].isOptional() || plugins[j].isFragment())
							optional.add(plugins[j].getId());
						else
							required.put(plugins[j].getId(), plugins[j]);
					}
				}
				IFeatureChild[] children = feature.getIncludedFeatures();
				for (int j = 0; j < children.length; j++) {
					if (!target.isValidFeatureObject(children[j]))
						continue;
					model = (featureManager != null) ? featureManager.findFeatureModel(children[j].getId()) :
						(IFeatureModel)fCurrentFeatures.get(children[j].getId());
					if (model != null)
						features.push(model.getFeature());
					else if (!targetFeatures[i].isOptional() && !missingFeatures.containsKey(children[j].getId())){
						// create dummy feature to display feature is missing
						ITargetFeature missingFeature = target.getModel().getFactory().createFeature();
						missingFeature.setId(children[j].getId());
						missingFeatures.put(children[j].getId(), missingFeature);
					}
				}
			}
		}

		ITargetPlugin[] plugins = target.getPlugins();
		for (int i = 0 ; i < plugins.length; i++) {
			if (plugins[i].isOptional())
				optional.add(plugins[i].getId());
			else
				required.put(plugins[i].getId(), plugins[i]);
		}
		
		IPluginModelBase workspacePlugins[] = PDECore.getDefault().getModelManager().getWorkspaceModels();
		for (int i = 0; i < workspacePlugins.length; i++) {
			if (workspacePlugins[i].isEnabled())
				required.remove(workspacePlugins[i].getBundleDescription().getSymbolicName());
		}
		
		IPluginModelBase[] models = getCurrentModels();
		int counter = 0;
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getPluginBase().getId();
			if (id == null)
				continue;
			if (required.containsKey(id) || optional.contains(id)) {
				++counter;
				if (!fPluginListViewer.getChecked(models[i])) {
					fPluginTreeViewer.setChecked(models[i], true);
					fPluginListViewer.setChecked(models[i], true);
					if (!models[i].isEnabled())
						fChangedModels.add(models[i]);
					// handle checking the parent
					String path = models[i].getInstallLocation();
					if (path != null) {
						File parent = new File(path).getParentFile();
						fPluginTreeViewer.setChecked(parent, true);
						handleGrayChecked(parent, false);
					}
					
				}
				required.remove(id);
			} else {
				if (fPluginListViewer.getChecked(models[i])) {
					fPluginTreeViewer.setChecked(models[i], false);
					fPluginListViewer.setChecked(models[i], false);
					if (models[i].isEnabled())
						fChangedModels.add(models[i]);
					// handle updating parent
					String path = models[i].getInstallLocation();
					if (path != null) {
						File parent = new File(path).getParentFile();
						handleGrayChecked(parent, true);
					}
				}
			}		
		}
		setCounter(counter);
		if (!required.isEmpty() || !missingFeatures.isEmpty()) 
			TargetErrorDialog.showDialog(fPage.getShell(), missingFeatures.values().toArray(),
					required.values().toArray());
	}
	
	protected void handleReload() {
		String platformPath = fPage.getPlatformPath();
		if (platformPath != null && platformPath.length() > 0) {
			ReloadOperation op = new ReloadOperation(platformPath);
			try {
				PlatformUI.getWorkbench().getProgressService().run(true, false, op);
			} catch (InvocationTargetException e) {
			} catch (InterruptedException e) {
			}
			fPluginListViewer.setInput(PDECore.getDefault().getModelManager().getExternalModelManager());
			fPluginTreeViewer.setInput(PDECore.getDefault().getModelManager().getExternalModelManager());
			fChangedModels.clear();
			handleSelectAll(true);
			if (fTreeViewerContents.size() > 1)
				fPluginTreeViewer.collapseAll();
			fReloaded = true;
			fPage.getSourceBlock().resetExtensionLocations(getCurrentModels());
		}
		fPage.resetNeedsReload();
	}	

	protected void handleReload(ArrayList additionalLocations) {
		fAdditionalLocations = additionalLocations;
		handleReload();
	}

	public void initialize() {
		String platformPath = fPage.getPlatformPath();
		if (platformPath != null && platformPath.length() == 0)
			return;
		
		fPluginTreeViewer.setUseHashlookup(true);
		ExternalModelManager manager = PDECore.getDefault().getModelManager().getExternalModelManager();
		fPluginListViewer.setInput(manager);
		fPluginTreeViewer.setInput(manager);
		IPluginModelBase[] allModels = getCurrentModels();

		Vector selection = new Vector();
		Set parentsToCheck = new HashSet();
		for (int i = 0; i < allModels.length; i++) {
			IPluginModelBase model = allModels[i];
			if (model.isEnabled()) {
				selection.add(model);
			}
			// if model is to be selected, add parent to list of parents to be updated
			String path = model.getInstallLocation();
			if (path != null ) {
				File installFile = new File(path);
				File parentFile = installFile.getParentFile();
				if (model.isEnabled())
					parentsToCheck.add(parentFile);
			}
		}
		Object[] elements = selection.toArray();
		fPluginListViewer.setCheckedElements(elements);	
		
		// handle checking for the TreeViewer
		Object[] parents = parentsToCheck.toArray();
		Object[] checkedValues= new Object[parents.length + elements.length];
		System.arraycopy(parents, 0, checkedValues, 0, parents.length);
		System.arraycopy(elements, 0, checkedValues, parents.length, elements.length);
		fPluginTreeViewer.setCheckedElements(checkedValues);
		for (int i = 0; i < parents.length; i++) {
			handleGrayChecked((File)parents[i], false);
		}
		setCounter(elements.length);
	}
	
	// returns a Set which contains all the new File objects representing a new location
	public Set initializeTreeContents(IPluginModelBase[] allModels) {
		HashSet parents = new HashSet();
		if (fTreeViewerContents == null) 
			fTreeViewerContents = new HashMap();
		for (int i = 0; i < allModels.length; i++) {
			IPluginModelBase model = allModels[i];
			String path = model.getInstallLocation();
			if (path != null ) {
				File installFile = new File(path);
				File parentFile = installFile.getParentFile();
				Set models = (Set)fTreeViewerContents.get(parentFile);
				if (models == null) {
					models = new HashSet();
					models.add(model);
					fTreeViewerContents.put(parentFile, models);
					parents.add(parentFile);
				} else {
					models.add(model);
				}
			}
		}
		return parents;
	}
	
	public void performOk() {
		savePreferences();
		if (fReloaded) {
			updateModels();
			Job job = new TargetPlatformResetJob(fCurrentState);
			job.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_PLUGIN_OBJ);
			job.schedule();
			fReloaded = false;
			fChangedModels.clear();
		} else {
			updateModels();
			computeDelta();
		}		
	}
	
	private void savePreferences() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		IPath newPath = new Path(fPage.getPlatformPath());
		IPath defaultPath = new Path(org.eclipse.pde.core.plugin.TargetPlatform.getDefaultLocation());
		String mode =
			newPath.equals(defaultPath)
				? ICoreConstants.VALUE_USE_THIS
				: ICoreConstants.VALUE_USE_OTHER;
		preferences.setValue(ICoreConstants.TARGET_MODE, mode);
		preferences.setValue(ICoreConstants.PLATFORM_PATH, fPage.getPlatformPath());

		if (fCounter == 0) {
			preferences.setValue(ICoreConstants.CHECKED_PLUGINS, ICoreConstants.VALUE_SAVED_NONE);
		} else if (fCounter == fPluginListViewer.getTable().getItemCount()) {
			preferences.setValue(ICoreConstants.CHECKED_PLUGINS, ICoreConstants.VALUE_SAVED_ALL);		
		} else {
			StringBuffer saved = new StringBuffer();
			TableItem[] models = fPluginListViewer.getTable().getItems();
			for (int i = 0; i < models.length; i++) {
				if (models[i].getChecked())
					continue;
				IPluginModelBase model = (IPluginModelBase)models[i].getData();
				if (saved.length() > 0) 
					saved.append(" "); //$NON-NLS-1$
				saved.append(model.getPluginBase().getId());	
			}
			preferences.setValue(ICoreConstants.CHECKED_PLUGINS, saved.toString());					
		}
		
		String[] locations = fPage.getPlatformLocations();
		for (int i = 0; i < locations.length && i < 5; i++) {
			preferences.setValue(ICoreConstants.SAVED_PLATFORM + i, locations[i]);
		}
		preferences.setValue(ICoreConstants.GROUP_PLUGINS_VIEW, fGroupPlugins.getSelection());
		
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < fAdditionalLocations.size(); i++) {
			if (buffer.length() > 0)
				buffer.append(","); //$NON-NLS-1$
			buffer.append(fAdditionalLocations.get(i).toString());
		}
		preferences.setValue(ICoreConstants.ADDITIONAL_LOCATIONS, buffer.toString());
		PDECore.getDefault().savePluginPreferences();
	}
	
	private void updateModels() {
		Iterator iter = fChangedModels.iterator();
		while (iter.hasNext()) {
			IPluginModelBase model = (IPluginModelBase) iter.next();
			model.setEnabled(fPluginListViewer.getChecked(model));
		}
	}
	
	public void handleSelectAll(boolean selected) {
		fPluginListViewer.setAllChecked(selected);
		fPluginTreeViewer.setAllChecked(selected);
		
		IPluginModelBase[] allModels = getCurrentModels();
		for (int i = 0; i < allModels.length; i++) {
			IPluginModelBase model = allModels[i];
			if (model.isEnabled() != selected) {
				fChangedModels.add(model);
			} else if (fChangedModels.contains(model) && model.isEnabled() == selected) {
				fChangedModels.remove(model);
			}
		}
		if (selected) {
			setCounter(fPluginListViewer.getTable().getItemCount());
		} else
			setCounter(0);
	}
	
	private void handleWorkingSets() {
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog = manager.createWorkingSetSelectionDialog(fPluginListViewer.getControl().getShell(), true);
		if (dialog.open() == Window.OK) {
			HashSet set = getPluginIDs(dialog.getSelection());
			IPluginModelBase[] models = getCurrentModels();
			int counter = 0;
			for (int i = 0; i < models.length; i++) {
				String id = models[i].getPluginBase().getId();
				if (id == null)
					continue;
				if (set.contains(id)) {
					if (!fPluginListViewer.getChecked(models[i])) {
						fPluginListViewer.setChecked(models[i], true);
						counter += 1;
						if (!models[i].isEnabled())
							fChangedModels.add(models[i]);
					}
					if (!fPluginTreeViewer.getChecked(models[i])) {
						fPluginTreeViewer.setChecked(models[i], true);
						String path = models[i].getInstallLocation();
						if (path != null) {
							File parent = new File(path).getParentFile();
							fPluginTreeViewer.setChecked(parent, true);
							handleGrayChecked(parent, false);
						}
					}
					set.remove(id);
				}
				if (set.isEmpty())
					break;				
			}
			setCounter(fCounter + counter);
		}
	}
	
	/*
	 * Sets the parent object to the right state.  This includes the checked state 
	 * and/or the gray state.
	 * 
	 * @param parent The parent object to check the current state of.
	 * @param handleCheck True if you wish the set the checked value of the object, False otherwise.
	 */
	// added the second boolean because many time I know the parent will be checked.  
	// Therefore I didn't want to waste time calling .setChecked() again.
	protected void handleGrayChecked(File parent, boolean handleCheck) {
		boolean gray = false, check = false, allChecked = true;
		Set models = (Set)fTreeViewerContents.get(parent);
		Iterator it = models.iterator();
		while (it.hasNext()) {
			Object model = it.next();
			boolean checked = fPluginTreeViewer.getChecked(model);
			check = check || checked;
			allChecked = allChecked && checked;
			if (!gray && checked) {
				gray = true;
			} else if (gray && !checked) {
				allChecked = false;
				break;
			}	
		}
		if (!allChecked && gray) 
			fPluginTreeViewer.setGrayed(parent, true);
		else 
			fPluginTreeViewer.setGrayed(parent, false);
		if (handleCheck) {
			if (check)
				fPluginTreeViewer.setChecked(parent, true);
			else
				fPluginTreeViewer.setChecked(parent, false);
		}
	}
	
	private HashSet getPluginIDs(IWorkingSet[] workingSets) {
		HashSet set = new HashSet();
		for (int i = 0; i < workingSets.length; i++) {
			IAdaptable[] elements = workingSets[i].getElements();
			for (int j = 0; j < elements.length; j++) {
				Object element = elements[j];
				if (element instanceof PersistablePluginObject) {
					set.add(((PersistablePluginObject)element).getPluginID());
				} else {
					if (element instanceof IJavaProject)
						element = ((IJavaProject)element).getProject();
					if (element instanceof IProject) {
						IPluginModelBase model = PluginRegistry.findModel((IProject)element);
						if (model != null)
							set.add(model.getPluginBase().getId());
					}
				}
			}
		}
		return set;
	}
	
	private void handleAddRequired() {
		Object[] checked = fPluginListViewer.getCheckedElements();
		if (checked.length == 0)
			return;
		String[] implicit = fPage.getImplicitPlugins();
		State state = getCurrentState().getState();
		Set set = DependencyManager.getDependencies(checked, implicit, state);
		Set parents = new HashSet();
		IPluginModelBase[] models = getCurrentModels();
		int counter = 0;
		for (int i = 0; i < models.length; i++) {
			if (set.contains(models[i].getPluginBase().getId())) {
				fPluginListViewer.setChecked(models[i], true);
				fPluginTreeViewer.setChecked(models[i], true);
				fChangedModels.add(models[i]);
				counter += 1;
				String path = models[i].getInstallLocation();
				if (path != null) {
					parents.add(new File(path).getParentFile());
				}
			}
		}
		Iterator it = parents.iterator();
		while (it.hasNext()) 
			handleGrayChecked((File)it.next(), true);
		
		setCounter(fCounter + counter);
	}
	
	protected IPluginModelBase[] getCurrentModels() {
		if (fCurrentState != null)
			return fCurrentState.getTargetModels();
		return PDECore.getDefault().getModelManager().getExternalModels();
	}
	
	protected PDEState getCurrentState() {
		return (fCurrentState != null) ? fCurrentState : TargetPlatformHelper.getPDEState();
	}
	
	protected void handleSwitchView() {
		if (fGroupPlugins.getSelection()) 
			fBook.showPage(fPluginTreeViewer.getControl());
		else 
			fBook.showPage(fPluginListViewer.getControl());			
	
	}
	
	private void setCounter(int value) {
		fCounter = value;
		int total = fPluginListViewer.getTable().getItemCount();
		String message =
			NLS.bind(PDEUIMessages.WizardCheckboxTablePart_counter, (new String[] { Integer.toString(fCounter), Integer.toString(total) }));
		fCounterLabel.setText(message);
	}

	protected void buttonSelected(Button button, int index) {
		switch (index) {
		case 0:
			handleReload();
			fPage.resetTargetProfile();
			break;
		case 1:
			handleAdd();
			break;
		case 4: 
			handleSelectAll(true);
			break;
		case 5:
			handleSelectAll(false);
			break;
		case 6:
			handleWorkingSets();
			break;
		case 7:
			handleAddRequired();
			break;
		}
	}
	
	private void handleAdd() {
		AddTargetPluginsWizard wizard = new AddTargetPluginsWizard();
		WizardDialog dialog = new WizardDialog(fPage.getShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 400, 450);
		dialog.open();
		
		File[] dirs = wizard.getDirectories();
		if (dirs.length == 0) {
			// no new URLs found/to add
			return;
		}
		for (int i = 0; i < dirs.length; i++) {
			fAdditionalLocations.add(dirs[i].getPath());
			File temp = new File(dirs[i], "plugins"); //$NON-NLS-1$
			if (temp.exists())
				dirs[i] = temp;
		}		
		
		URL[] pluginLocs = PluginPathFinder.scanLocations(dirs);
		Object[] checkedPlugins = null;
		if (fCurrentState == null) {
			checkedPlugins = fPluginListViewer.getCheckedElements();
			createCopyState();
		}
		BundleDescription[] descriptions = fCurrentState.addAdditionalBundles(pluginLocs);
		addNewBundles(descriptions, checkedPlugins);
	}
	
	private void createCopyState() {
		fCurrentState = new PDEState(TargetPlatformHelper.getPDEState());
		IPluginModelBase[] bases = fCurrentState.getTargetModels();
		for (int j = 0; j < bases.length; j++) {
			long bundleId = bases[j].getBundleDescription().getBundleId();
			BundleDescription newDesc = fCurrentState.getState().getBundle(bundleId);
			bases[j].setBundleDescription(newDesc);
		}
	}
	
	private void addNewBundles(BundleDescription[] descriptions, Object[] checkedPlugins) {
		if (descriptions.length > 0) {
			IPluginModelBase[] models = fCurrentState.createTargetModels(descriptions);
			// add new models to tree viewer
			Set parents = initializeTreeContents(models);
			
			fPluginListViewer.setInput(PDECore.getDefault().getModelManager().getExternalModelManager());
			fPluginTreeViewer.setInput(PDECore.getDefault().getModelManager().getExternalModelManager());
			
			if (checkedPlugins == null) {
				for (int i = 0; i < models.length; i++) {
					fPluginListViewer.setChecked(models[i], true);
					fPluginTreeViewer.setChecked(models[i], true);
				}
			} else {
				Object[] newCheckedPlugins = new Object[checkedPlugins.length + models.length];
				System.arraycopy(checkedPlugins, 0, newCheckedPlugins, 0, checkedPlugins.length);
				System.arraycopy(models, 0, newCheckedPlugins, checkedPlugins.length, models.length);
				fPluginListViewer.setCheckedElements(newCheckedPlugins);
				fPluginTreeViewer.setCheckedElements(newCheckedPlugins);
			}
			for (int i = 0; i < models.length; i++) {
				fChangedModels.add(models[i]);
			}
			Iterator it = parents.iterator();
			while (it.hasNext())
				fPluginTreeViewer.setChecked(it.next(), true);
			// have to use getCheckedElements() instead of newCheckedPlugins because a new plug-in might have replaced an plug-in model in the original checked list.
			setCounter(fPluginListViewer.getCheckedElements().length);
			fPage.getSourceBlock().resetExtensionLocations(getCurrentModels());
			fReloaded = true;
		}
	}

}
