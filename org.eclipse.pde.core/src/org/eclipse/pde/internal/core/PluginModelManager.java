/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.service.resolver.BundleDelta;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.StateDelta;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.IModelProviderListener;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.osgi.framework.Version;

public class PluginModelManager implements IAdaptable {
	private static final String OSGI_RUNTIME ="org.eclipse.osgi"; //$NON-NLS-1$
	private IModelProviderListener fProviderListener;
	private ExternalModelManager fExternalManager;
	private WorkspaceModelManager fWorkspaceManager;
	private SearchablePluginsManager fSearchablePluginsManager;
	private ArrayList fListeners;
	private Map fEntries;
	
	private PDEState fState;

	public PluginModelManager(WorkspaceModelManager wm, ExternalModelManager em) {
		fProviderListener = new IModelProviderListener() {
			public void modelsChanged(IModelProviderEvent e) {
				handleModelsChanged(e);
			}
		};
		fWorkspaceManager = wm;
		fExternalManager = em;
		fExternalManager.addModelProviderListener(fProviderListener);
		fWorkspaceManager.addModelProviderListener(fProviderListener);
		fListeners = new ArrayList();
		fSearchablePluginsManager = new SearchablePluginsManager(this);
	}
	
	public String getTargetVersion() {
		ModelEntry entry = findEntry(OSGI_RUNTIME); 
		if (entry == null) 
			return ICoreConstants.TARGET32;
		
		IPluginModelBase model = entry.getActiveModel();
		String version = model.getPluginBase().getVersion();
		if (VersionUtil.validateVersion(version).getSeverity() == IStatus.OK) {
			Version vid = new Version(version);
			int major = vid.getMajor();
			int minor = vid.getMinor();
			if (major == 3 && minor == 0)
				return ICoreConstants.TARGET30;
			if (major == 3 && minor == 1)
				return ICoreConstants.TARGET31;
		}			
		return ICoreConstants.TARGET32;	
	}

	public Object getAdapter(Class key) {
		return null;
	}
	
	public void addPluginModelListener(IPluginModelListener listener) {
		if (!fListeners.contains(listener))
			fListeners.add(listener);
	}
	
	public void removePluginModelListener(IPluginModelListener listener) {
		if (fListeners.contains(listener))
			fListeners.remove(listener);
	}
	
	public boolean isEmpty() {
		return getEntryTable().isEmpty();
	}

	public ModelEntry[] getEntries() {
		Collection values = getEntryTable().values();
		return (ModelEntry[]) values.toArray(new ModelEntry[values.size()]);
	}
	
	/*
	 * @return enabled plug-ins only
	 */
	public IPluginModelBase[] getPlugins() {
		Collection values = getEntryTable().values();
		ArrayList result = new ArrayList();
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			IPluginModelBase model = entry.getActiveModel();
			if (model != null && model.isEnabled())
				result.add(model);
		}
		return (IPluginModelBase[])result.toArray(new IPluginModelBase[result.size()]);
	}
	
	/*
	 * @return all plug-ins (enabled and disabled)
	 */
	public IPluginModelBase[] getAllPlugins() {
		Collection values = getEntryTable().values();
		ArrayList result = new ArrayList();
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			result.add(entry.getActiveModel());
		}
		return (IPluginModelBase[])result.toArray(new IPluginModelBase[result.size()]);
	}
	
	public IPluginModel[] getPluginsOnly() {
		Collection values = getEntryTable().values();
		ArrayList result = new ArrayList();
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			IPluginModelBase model = entry.getActiveModel();
			if (model.isEnabled() && model instanceof IPluginModel)
				result.add(model);
		}
		return (IPluginModel[])result.toArray(new IPluginModel[result.size()]);
	}
	
	public IFragmentModel[] getFragments() {
		Collection values = getEntryTable().values();
		ArrayList result = new ArrayList();
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			IPluginModelBase model = entry.getActiveModel();
			if (model instanceof IFragmentModel)
				result.add(model);
		}
		return (IFragmentModel[])result.toArray(new IFragmentModel[result.size()]);
		
	}
	
	public ModelEntry findEntry(IProject project) {
		initializeTable();
		IPluginModelBase model = fWorkspaceManager.getWorkspacePluginModel(project);
		return model == null ? null : findEntry(model.getPluginBase().getId());
	}
	
	public IPluginModelBase findModel(IProject project) {
		ModelEntry entry = findEntry(project);
		return (entry != null) ? entry.getActiveModel() : null;
	}
	
	public IPluginModelBase findModel(BundleDescription desc) {
		return desc == null ? null : findModel(desc.getSymbolicName());
	}
	
	public ModelEntry findEntry(String id) {
		if (id == null)
			return null;
		else if (id.equalsIgnoreCase("system.bundle")) //$NON-NLS-1$
			return (ModelEntry) getEntryTable().get("org.eclipse.osgi"); //$NON-NLS-1$
		return (ModelEntry) getEntryTable().get(id);
	}
	
	public IPluginModelBase findModel(String id) {
		ModelEntry entry = findEntry(id);
		return (entry == null) ?  null : entry.getActiveModel();
	}
	
	public IPluginModelBase findPlugin(String id, String version, int match) {
		return findModel(id);
	}
	
	public IPluginModel findPluginModel(String id) {
		IPluginModelBase model = findModel(id);
		return (model != null && model instanceof IPluginModel) ? (IPluginModel)model : null;
	}
	
	public IFragmentModel findFragmentModel(String id) {
		IPluginModelBase model = findModel(id);
		return (model != null && model instanceof IFragmentModel) ? (IFragmentModel)model : null;
	}
	
	private void handleModelsChanged(IModelProviderEvent e) {
		PluginModelDelta delta = new PluginModelDelta();
		boolean javaSearchAffected = false;
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_REMOVED) != 0) {
			IModel[] removed = e.getRemovedModels();
			for (int i = 0; i < removed.length; i++) {
				if (!(removed[i] instanceof IPluginModelBase)) continue;
				IPluginModelBase model = (IPluginModelBase) removed[i];
				IPluginBase plugin = model.getPluginBase();
				ModelEntry entry = updateTable(plugin.getId(), model, false, delta);
				if (entry != null && (model.getUnderlyingResource() != null || entry.isInJavaSearch()))
					javaSearchAffected = true;			
			}
		}
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_ADDED) != 0) {
			IModel[] added = e.getAddedModels();
			for (int i = 0; i < added.length; i++) {
				if (!(added[i] instanceof IPluginModelBase)) continue;
				IPluginModelBase model = (IPluginModelBase) added[i];
				IPluginBase plugin = model.getPluginBase();
				ModelEntry entry = updateTable(plugin.getId(), model, true, delta);
				if (entry != null && (model.getUnderlyingResource() != null  || entry.isInJavaSearch()))
					javaSearchAffected = true;
			}
		}
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_CHANGED) != 0) {
			IModel[] changed = e.getChangedModels();
			for (int i = 0; i < changed.length; i++) {
				if (!(changed[i] instanceof IPluginModelBase)) continue;
				IPluginModelBase model = (IPluginModelBase) changed[i];
				BundleDescription desc = model.getBundleDescription();
				String oldID = desc == null ? null : desc.getSymbolicName();
				String newID = model.getPluginBase().getId();
				
				if (oldID == null && newID == null)
					continue;
				
				ModelEntry entry = null;
				if (oldID == null && newID != null) {
					entry = updateTable(newID, model, true, delta);
				} else if (oldID != null && newID == null) {
					entry = updateTable(oldID, model, false, delta);
					model.setBundleDescription(null);
				} else if (oldID.equals(newID)) {
					fState.addBundle(model, true);
					entry = (ModelEntry)getEntryTable().get(oldID);
					delta.addEntry(entry, PluginModelDelta.CHANGED);		
				} else {
					entry = updateTable(oldID, model, false, delta);
					entry = updateTable(newID, model, true, delta);
				}
				// bug 133070
				if (model.getUnderlyingResource() == null) {
					ModelEntry tableEntry = (ModelEntry)getEntryTable().get(newID);
					if (tableEntry != null && tableEntry.getWorkspaceModel() != null) {
						fState.removeBundleDescription(model.getBundleDescription());
					}
				}
				if (entry != null && (model.getUnderlyingResource() != null || entry.isInJavaSearch()))
					javaSearchAffected = true;
			}
		}
		
		if (fState != null) {
			StateDelta stateDelta =	(e.getEventTypes() & IModelProviderEvent.TARGET_CHANGED) != 0 
									? null 
									: fState.resolveState(true);
			updateAffectedEntries(stateDelta);
		}
		if (javaSearchAffected)
			fSearchablePluginsManager.updateClasspathContainer();
		fireDelta(delta);
	}
	
	private ModelEntry updateTable(
		String id,
		IPluginModelBase model,
		boolean added,
		PluginModelDelta delta) {
		if (id == null)
			return null;
		boolean workspace = model.getUnderlyingResource()!=null;
		Map entries = getEntryTable();
		ModelEntry entry = (ModelEntry) entries.get(id);
		int kind = 0;
		if (added && entry == null) {
			entry = new ModelEntry(this, id);
			entries.put(id, entry);
			kind = PluginModelDelta.ADDED;
		}
		if (added) {
			if (workspace)
				entry.setWorkspaceModel(model);
			else
				entry.setExternalModel(model);
		} else if (entry != null) {
			if (workspace) entry.setWorkspaceModel(null);
			else
				entry.setExternalModel(null);
			if (entry.isEmpty()) {
				entries.remove(id);
				kind = PluginModelDelta.REMOVED;
			}
		}
		if (workspace) {
			if (added) {
				addWorkspaceBundleToState(model);
			} else {
				removeWorkspaceBundleFromState(model, id);
			}
		}
		if (kind==0) kind = PluginModelDelta.CHANGED;
		delta.addEntry(entry, kind);
		return entry;
	}

	private void updateAffectedEntries(StateDelta delta) {	
		Map map = new HashMap();
		if (delta == null) {
			ModelEntry[] entries = getEntries();
			for (int i = 0; i < entries.length; i++) {
				ModelEntry entry = entries[i];
				try {
					if (entry != null && entry.shouldUpdateClasspathContainer(true)) {
						IProject project = entry.getWorkspaceModel().getUnderlyingResource().getProject();
						IJavaProject jProject = JavaCore.create(project);
						if (!map.containsKey(jProject))
							map.put(jProject, entry.getClasspathContainer(true));
					}
				} catch (CoreException e) {
				}				
			}
		} else {
			BundleDelta[] deltas = delta.getChanges();
			for (int i = 0; i < deltas.length; i++) {
				try {
					String id = deltas[i].getBundle().getSymbolicName();
					ModelEntry entry = findEntry(id);
					if (entry != null && entry.shouldUpdateClasspathContainer(true)) {
						IProject project = entry.getWorkspaceModel().getUnderlyingResource().getProject();
						IJavaProject jProject = JavaCore.create(project);
						if (!map.containsKey(jProject))
							map.put(jProject, entry.getClasspathContainer(true));
					}
				} catch (CoreException e) {
				}
			}
			addAdditionalDependencyProjects(map);
		}
			
		if (map.size() > 0) {
			try {
				IJavaProject[] jProjects = (IJavaProject[])map.keySet().toArray(new IJavaProject[map.size()]);
				IClasspathContainer[] containers = (IClasspathContainer[])map.values().toArray(new IClasspathContainer[map.size()]);
				JavaCore.setClasspathContainer(
					new Path(PDECore.CLASSPATH_CONTAINER_ID),
					jProjects,
					containers,
					null);
			} catch (JavaModelException e) {
			}
		}
	}
	
	private void addAdditionalDependencyProjects(Map map) {
		if (map.size() == 0)
			return;
		Stack changedProjects = new Stack();
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) 
			changedProjects.push(((IJavaProject)it.next()).getProject());
	
		IProject[] projs = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList projects = new ArrayList();
		for (int i = 0; i < projs.length; i++) 
			try {
				if (!changedProjects.contains(projs[i]) && projs[i].isOpen() && projs[i].hasNature(JavaCore.NATURE_ID)) 
					projects.add(JavaCore.create(projs[i]));	
			} catch (CoreException e) {
			}
		while (!changedProjects.isEmpty()) {
			IProject refreshProject = (IProject)changedProjects.pop();
			ListIterator li = projects.listIterator();
			while (li.hasNext()) {
				IJavaProject jp = (IJavaProject)li.next();
				if (jp.isOnClasspath(refreshProject)) {
					li.remove();
					IProject baseProject = jp.getProject();
					changedProjects.push(baseProject);
					ModelEntry entry = findEntry(baseProject);
					try {
						if (entry != null && entry.shouldUpdateClasspathContainer(true)) 
							map.put(jp, entry.getClasspathContainer(true));
					} catch (CoreException e) {
					}
				}
			}
		}
	}
	
	/*
	 * Allow access to the table only through this getter.
	 * It always calls initialize to make sure the table is initialized.
	 * If more than one thread tries to read the table at the same time,
	 *  and the table is not initialized yet, thread2 would wait. 
	 *  This way there are no partial reads.
	 */
	private Map getEntryTable() {
		initializeTable();
		return fEntries;
	}

	/*
	 * This method must be synchronized so that only one thread
	 * initializes the table, and the rest would block until
	 * the table is initialized.
	 * 
	 */
	private synchronized void initializeTable() {
		if (fEntries != null) return;
		fEntries = Collections.synchronizedMap(new TreeMap());
		fState = new PDEState(
						WorkspaceModelManager.getPluginPaths(),
						ExternalModelManager.getPluginPaths(),
						true,
						new NullProgressMonitor());
		
		fExternalManager.initializeModels(fState.getTargetModels());	
		addToTable(fExternalManager.getAllModels(), false);
		if (fState.isCombined()) {
			IPluginModelBase[] models = fState.getWorkspaceModels();
			fWorkspaceManager.initializeModels(models);
			addToTable(models, true);
		} else {
			addToTable(fWorkspaceManager.getAllModels(), true);			
			addWorkspaceBundlesToNewState();
		}
		fSearchablePluginsManager.initialize();
	}
	
	public PDEState getState() {
		initializeTable();
		return fState;
	}
	
	public void setState(PDEState state) {
		fState = state;
	}

	private void addToTable(IPluginModelBase[] models, boolean workspace) {
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			String id = model.getPluginBase().getId();
			if (id == null)
				continue;
			ModelEntry entry = (ModelEntry) fEntries.get(id);
			if (entry == null) {
				entry = new ModelEntry(this, id);
				fEntries.put(id, entry);
			}
			if (workspace)
				entry.setWorkspaceModel(model);
			else
				entry.setExternalModel(model);
		}
	}
		
	private void addWorkspaceBundlesToNewState() {
		IPluginModelBase[] models = fWorkspaceManager.getAllModels();
		for (int i = 0; i < models.length; i++) {
			addWorkspaceBundleToState(models[i]);
		}
		if (models.length > 0)
			fState.resolveState(true);
	}
	
	private void addWorkspaceBundleToState(IPluginModelBase model) {
		String id = model.getPluginBase().getId();
		if (id == null)
			return;

		fState.addBundle(model, false);
		
		BundleDescription desc = model.getBundleDescription();
		if (desc != null) {
			HostSpecification spec = desc.getHost();
			if (spec != null 
				&& spec.getName() != null
				&& ("true".equals(System.getProperty("pde.allowCycles")) //$NON-NLS-1$ //$NON-NLS-2$
					||ClasspathUtilCore.isPatchFragment(desc)
					|| desc.getImportPackages().length > 0 
					|| desc.getRequiredBundles().length > 0)) {
				IPluginModelBase host = findModel(spec.getName());
				if (host != null)
					fState.addBundle(host, true);
			}
		}
	}
	
	private void removeWorkspaceBundleFromState(IPluginModelBase model, String id) {
		BundleDescription description = model.getBundleDescription();
		if (description == null)
			return;
		
		fState.removeBundleDescription(description);
		
		ModelEntry entry = findEntry(id);
		IPluginModelBase external = entry == null ? null : entry.getExternalModel();
		if (external != null) {
			fState.addBundleDescription(external.getBundleDescription());
		}
	}
	
	private void fireDelta(PluginModelDelta delta) {
		Object [] entries = fListeners.toArray();
		for (int i=0; i<entries.length; i++) {
			((IPluginModelListener)entries[i]).modelsChanged(delta);
		}
	}

	public void shutdown() {
		if (fWorkspaceManager != null)	
			fWorkspaceManager.removeModelProviderListener(fProviderListener);
		if (fExternalManager != null)
			fExternalManager.removeModelProviderListener(fProviderListener);
		if (fState != null)
			fState.shutdown();
		fSearchablePluginsManager.shutdown();
	}
	
	public void setInJavaSearch(ModelEntry [] entries, boolean value, IProgressMonitor monitor) throws CoreException {
		PluginModelDelta delta = new PluginModelDelta();
		for (int i=0; i<entries.length; i++) {
			ModelEntry entry = entries[i];
			if (entry.isInJavaSearch()!=value) {
				entry.setInJavaSearch(value);
				delta.addEntry(entry, PluginModelDelta.CHANGED);
			}
		}
		if (delta.getKind()!=0) {
			fSearchablePluginsManager.persistStates( monitor);
			fireDelta(delta);
		}
	}
	 
	void searchablePluginsRemoved() {
		ModelEntry [] entries = getEntries();
		PluginModelDelta delta = new PluginModelDelta();
		
		for (int i=0; i<entries.length; i++) {
			ModelEntry entry = entries[i];
			if (entry.isInJavaSearch()) {
				entry.setInJavaSearch(false);
				delta.addEntry(entry, PluginModelDelta.CHANGED);
			}
		}
		if (delta.getKind()!=0)
			fireDelta(delta);
	}
	
	public IFileAdapterFactory getFileAdapterFactory() {
		return fSearchablePluginsManager;
	}
	/**
	 * @return Returns the searchablePluginsManager.
	 */
	public SearchablePluginsManager getSearchablePluginsManager() {
		initializeTable();
		return fSearchablePluginsManager;
	}
	
	public IPluginModelBase[] getExternalModels() {
		initializeTable();
		return fExternalManager.getAllModels();
	}
	
	public IPluginModelBase[] getWorkspaceModels() {
		initializeTable();
		return fWorkspaceManager.getAllModels();
	}
}
