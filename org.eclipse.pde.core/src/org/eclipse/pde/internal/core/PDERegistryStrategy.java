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
package org.eclipse.pde.internal.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.spi.IDynamicExtensionRegistry;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.core.runtime.spi.RegistryStrategy;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.osgi.util.tracker.ServiceTracker;

public class PDERegistryStrategy extends RegistryStrategy{

	/**
	 * Tracker for the XML parser service
	 */
	private ServiceTracker xmlTracker = null;
	
	private Object fKey = null;
	
	private ModelListener fModelListener = null;
	private ExtensionListener fExtensionListener = null;
	private PDEExtensionRegistry fPDERegistry = null;
	
	
	class RegistryListener {
		IExtensionRegistry fRegistry;
		
		protected final void removeModels(IPluginModelBase[] bases, boolean onlyInactive) {
			for (int i = 0; i < bases.length; i++) {
//				resetModel(bases[i]);
				if (onlyInactive && bases[i].isEnabled())
					continue;
				removeBundle(fRegistry, bases[i]);
			}
		}
		
		public void setRegistry(IExtensionRegistry registry) {
			fRegistry = registry;
		}
	}
	
	class ModelListener extends RegistryListener implements IPluginModelListener{
		
		public void modelsChanged(PluginModelDelta delta) {
			if (fRegistry == null)
				createRegistry();
			// can ignore removed models since the ModelEntries is empty
			ModelEntry[] entries = delta.getChangedEntries();
			for (int i = 0; i < entries.length; i++) {
				// remove all external models if there are any workspace models since they are considered 'activeModels'.  See ModelEntry.getActiveModels().
				removeModels(entries[i].getExternalModels(), !entries[i].hasWorkspaceModels());
				removeModels(entries[i].getWorkspaceModels(), true);
				addBundles(fRegistry, entries[i].getActiveModels());
			}
			entries = delta.getAddedEntries();
			for (int i = 0; i < entries.length; i++) 
				addBundles(fRegistry, entries[i].getActiveModels());
		}
		
	}
	
	class ExtensionListener extends RegistryListener implements IExtensionDeltaListener {
		
		public void extensionsChanged(IExtensionDeltaEvent event) {
			if (fRegistry == null)
				createRegistry();
			removeModels(event.getRemovedModels(), false);
			removeModels(event.getChangedModels(), false);
			addBundles(fRegistry, event.getChangedModels());
			addBundles(fRegistry, event.getAddedModels());
		}
		
	}
		
	public PDERegistryStrategy(File[] storageDirs, boolean[] cacheReadOnly, Object key, PDEExtensionRegistry registry) {
		super(storageDirs, cacheReadOnly);
		init();
		fKey = key;
		fPDERegistry = registry;
	}
	
	protected void init() {
		connectListeners();
	}
	
	protected void connectListeners() {
		// Listen for model changes to register new bundles and unregister removed bundles
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		manager.addPluginModelListener(fModelListener = new ModelListener());
		manager.addExtensionDeltaListener(fExtensionListener = new ExtensionListener());
	}
	
	protected void setListenerRegistry(IExtensionRegistry registry) {
		if (fModelListener != null)
			fModelListener.setRegistry(registry);
		if (fExtensionListener != null)
			fExtensionListener.setRegistry(registry);
	}
	
	public void onStart(IExtensionRegistry registry, boolean loadedFromCache) {
		super.onStart(registry, loadedFromCache);
		setListenerRegistry(registry);
		if (!loadedFromCache)
			processBundles(registry);
	}
	
	public void onStop(IExtensionRegistry registry) {
		super.onStop(registry);
		setListenerRegistry(null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.spi.RegistryStrategy#getXMLParser()
	 */
	public SAXParserFactory getXMLParser() {
		if (xmlTracker == null) {
			xmlTracker = new ServiceTracker(PDECore.getDefault().getBundleContext(), SAXParserFactory.class.getName(), null);
			xmlTracker.open();
		}
		return (SAXParserFactory) xmlTracker.getService();
	}
	
	private void processBundles(IExtensionRegistry registry) {
		addBundles(registry, fPDERegistry.getModels());
	}
	
	private void addBundles(IExtensionRegistry registry, IPluginModelBase[] bases) {
		for (int i = 0; i < bases.length; i++)
			addBundle(registry, bases[i]);
	}
	
	private void addBundle(IExtensionRegistry registry, IPluginModelBase base) {
		IContributor contributor = createContributor(base);
		if (contributor == null)
			return;
		if (((IDynamicExtensionRegistry)registry).hasContributor(contributor)) 
			return;
		
		File input = getFile(base);
		if (input == null)
			return;
		InputStream is = getInputStream(input, base);
		if (is == null)
			return;
		registry.addContribution(
				new BufferedInputStream(is),
				contributor, 
				true, 
				input.getPath(), 
				null,
				fKey);
	}
	
	private void removeBundle(IExtensionRegistry registry, IPluginModelBase base) {
		if (registry instanceof IDynamicExtensionRegistry) {
			IContributor contributor = createContributor(base);
			if (contributor != null && ((IDynamicExtensionRegistry)registry).hasContributor(contributor)) {
				((IDynamicExtensionRegistry)registry).removeContributor(createContributor(base), fKey);
			}
		}
	}
	
//	added for releasing cached information from IPluginModelBase
//	private void resetModel(IPluginModelBase model) {
//		IPluginBase base = model.getPluginBase();
//		if (base instanceof BundlePluginBase) {
//			IExtensions ext = ((BundlePluginBase)base).getExtensionsRoot();
//			if (ext != null && ext instanceof AbstractExtensions) {
//				((AbstractExtensions)ext).reset();
//			}
//		} else if (base instanceof AbstractExtensions){
//			((AbstractExtensions)base).resetExtensions();
//		}
//	}
	
	private File getFile(IPluginModelBase base) {
		String loc = base.getInstallLocation();
		File file = new File(loc);
		if (!file.exists())
			return null;
		if (file.isFile())
			return file;
		String fileName = (base.isFragmentModel()) ? "fragment.xml" : "plugin.xml"; //$NON-NLS-1$ //$NON-NLS-2$
		File inputFile = new File(file, fileName);
		return (inputFile.exists()) ? inputFile : null;
	}
	
	private InputStream getInputStream(File file, IPluginModelBase base) {
		if (file.getName().endsWith(".jar")) { //$NON-NLS-1$
			try {
				ZipFile jfile = new ZipFile(file, ZipFile.OPEN_READ);
				String fileName = (base.isFragmentModel()) ? "fragment.xml" : "plugin.xml"; //$NON-NLS-1$ //$NON-NLS-2$
				ZipEntry entry = jfile.getEntry(fileName);
				if (entry != null)
					return jfile.getInputStream(entry);
			} catch (IOException e) {
			}
			return null;
		}
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
		}
		return null;
	}
	
	public IContributor createContributor(IPluginModelBase base) {
		BundleDescription desc = base == null ? null : base.getBundleDescription();
		// return null if the IPluginModelBase does not have a BundleDescription (since then we won't have a valid 'id')
		if (desc == null)
			return null;
		String name = desc.getSymbolicName();
		String id = Long.toString(desc.getBundleId());
		String hostName = null;
		String hostId = null;
		
		HostSpecification host = desc.getHost();
		// make sure model is a singleton.  If it is a fragment, make sure host is singleton
		if (host != null && host.getBundle() != null && !host.getBundle().isSingleton() ||
				host == null && !desc.isSingleton())
			return null;
		if (host != null) {
			BundleDescription hostDesc = host.getBundle();
			hostName = hostDesc.getSymbolicName();
			hostId = Long.toString(hostDesc.getBundleId());
		}
		return new RegistryContributor(id, name, hostId, hostName);
	}
	
	public void dispose() {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		manager.removePluginModelListener(fModelListener);
		manager.removeExtensionDeltaListener(fExtensionListener);
		if (xmlTracker != null) {
			xmlTracker.close();
			xmlTracker = null;
		}
	}
	
	private void createRegistry() {
		fPDERegistry.getRegistry();
	}

	public long getContributionsTimestamp() {
		IPluginModelBase[] bases = fPDERegistry.getModels();
		long timeStamp = 0;
		for (int i = 0; i < bases.length; i++) {
			File location = getFile(bases[i]);
			if (location != null)
			timeStamp ^= location.lastModified();
		}
		return timeStamp;
	}

}