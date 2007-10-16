/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.osgi.framework.Version;

public class SourceLocationManager implements ICoreConstants {
	private SourceLocation[] fExtensionLocations = null;

	class SearchResult {
		SearchResult(SourceLocation loc, File file) {
			this.loc = loc;
			this.file = file;
		}
		SourceLocation loc;
		File file;
	}
	
	public void reset() {
		fExtensionLocations = null;
	}

	public SourceLocation[] getUserLocations() {
		ArrayList userLocations = new ArrayList();
		String pref = PDECore.getDefault().getPluginPreferences().getString(P_SOURCE_LOCATIONS);
		if (pref.length() > 0)
			parseSavedSourceLocations(pref, userLocations);
		return (SourceLocation[]) userLocations.toArray(new SourceLocation[userLocations.size()]);
	}

	public SourceLocation[] getExtensionLocations() {
		if (fExtensionLocations == null) {
			fExtensionLocations = processExtensions();
		}
		return fExtensionLocations;
	}
	
	public void setExtensionLocations(SourceLocation[] locations) {
		fExtensionLocations = locations;
	}

	public File findSourceFile(IPluginBase pluginBase, IPath sourcePath) {
		IPath relativePath = getRelativePath(pluginBase, sourcePath);
		SearchResult result = findSourceLocation(relativePath);
		return result != null ? result.file : null;
	}
	
	public File findSourcePlugin(IPluginBase pluginBase) {
		return findSourceFile(pluginBase, null);
	}

	public IPath findSourcePath(IPluginBase pluginBase, IPath sourcePath) {
		IPath relativePath = getRelativePath(pluginBase, sourcePath);
		SearchResult result = findSourceLocation(relativePath);
		return result == null ? null : result.loc.getPath().append(relativePath);
	}

	private IPath getRelativePath(IPluginBase pluginBase, IPath sourcePath) {
		try {
			String pluginDir = pluginBase.getId();
			if (pluginDir == null)
				return null;
			String version = pluginBase.getVersion();
			if (version != null) {
				Version vid = new Version(version);
				pluginDir += "_" + vid.toString(); //$NON-NLS-1$
			}
			IPath path = new Path(pluginDir);
			return sourcePath == null ? path : path.append(sourcePath);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public SearchResult findSourceLocation(IPath relativePath) {
		if (relativePath == null)
			return null;
		SearchResult result = findSearchResult(getUserLocations(), relativePath);
		return (result != null) ? result : findSearchResult(getExtensionLocations(), relativePath);
	}

	private SearchResult findSearchResult(SourceLocation[] locations, IPath sourcePath) {
		for (int i = 0; i < locations.length; i++) {
			IPath fullPath = locations[i].getPath().append(sourcePath);
			File file = fullPath.toFile();
			if (file.exists())
				return new SearchResult(locations[i], file);
		}
		return null;
	}

	private SourceLocation parseSourceLocation(String text) {
		String path;
		try {
			text = text.trim();
			int commaIndex = text.lastIndexOf(',');
			if (commaIndex == -1)
				return new SourceLocation(new Path(text));
			
			int atLoc = text.indexOf('@');
			path =
				(atLoc == -1)
					? text.substring(0, commaIndex)
					: text.substring(atLoc + 1, commaIndex);
		} catch (RuntimeException e) {
			return null;
		}
		return new SourceLocation(new Path(path));
	}

	private void parseSavedSourceLocations(String text, ArrayList entries) {
		text = text.replace(File.pathSeparatorChar, ';');
		StringTokenizer stok = new StringTokenizer(text, ";"); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			SourceLocation location = parseSourceLocation(token);
			if (location != null)
				entries.add(location);
		}
	}

	public static SourceLocation[] computeSourceLocations(IPluginModelBase[] models) {
		ArrayList result = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			processExtensions(models[i], result);
		}
		return (SourceLocation[])result.toArray(new SourceLocation[result.size()]);
	}
	
	private static void processExtensions(IPluginModelBase model, ArrayList result) {
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (int j = 0; j < extensions.length; j++) {
			IPluginExtension extension = extensions[j];
			if ((PDECore.PLUGIN_ID + ".source").equals(extension.getPoint())) { //$NON-NLS-1$
				processExtension(extension, result);
			}
		}				
	}
	
	private static SourceLocation[] processExtensions() {
		ArrayList result = new ArrayList();
		IExtension[] extensions = PDECore.getDefault().getExtensionsRegistry().findExtensions(PDECore.PLUGIN_ID + ".source"); //$NON-NLS-1$
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] children = extensions[i].getConfigurationElements();
			RegistryContributor contributor = (RegistryContributor)extensions[i].getContributor();
			long bundleId = Long.parseLong(contributor.getActualId());
			BundleDescription desc = PDECore.getDefault().getModelManager().getState().getState().getBundle(Long.parseLong(contributor.getActualId()));
			IPluginModelBase base = null;
			if (desc != null) 
				base = PluginRegistry.findModel(desc);
			// desc might be null if the workspace contains a plug-in with the same Bundle-SymbolicName
			else {
				ModelEntry entry = PluginRegistry.findEntry(contributor.getActualName());
				IPluginModelBase externalModels[] = entry.getExternalModels();
				for (int j = 0; j < externalModels.length; j++) {
					BundleDescription extDesc = externalModels[j].getBundleDescription();
					if (extDesc != null && extDesc.getBundleId() == bundleId)
						base = externalModels[j];
				}
			}
			if (base == null)
				continue;
			for (int j = 0; j < children.length; j++) {
				if (children[j].getName().equals("location")) { //$NON-NLS-1$
					String pathValue = children[j].getAttribute("path"); //$NON-NLS-1$
					IPath path = new Path(base.getInstallLocation()).append(pathValue);
					if (path.toFile().exists()) {
						SourceLocation location = new SourceLocation(path);
						location.setUserDefined(false);
						if (!result.contains(location))
							result.add(location);
					}
				}
			}
		}
		return (SourceLocation[]) result.toArray(new SourceLocation[result.size()]);
	}
	
	private static  void processExtension(IPluginExtension extension, ArrayList result) {
		IPluginObject[] children = extension.getChildren();
		for (int j = 0; j < children.length; j++) {
			if (children[j].getName().equals("location")) { //$NON-NLS-1$
				IPluginElement element = (IPluginElement) children[j];
				String pathValue = element.getAttribute("path").getValue(); //$NON-NLS-1$b	
				ISharedPluginModel model = extension.getModel();
				IPath path = new Path(model.getInstallLocation()).append(pathValue);
				if (path.toFile().exists()) {
					SourceLocation location = new SourceLocation(path);
					location.setUserDefined(false);
					if (!result.contains(location))
						result.add(location);
				}
			}
		}
	}

}
