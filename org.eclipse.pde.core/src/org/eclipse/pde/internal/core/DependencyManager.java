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
package org.eclipse.pde.internal.core;

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class DependencyManager {
	
	/** 
	 * @return a set of plug-in IDs
	 * 
	 */
	public static Set getSelfAndDependencies(IPluginModelBase model) {
		return getDependencies(new Object[] {model}, new String[0], TargetPlatformHelper.getState(), false);
	}
	
	/** 
	 * @return a set of plug-in IDs
	 * 
	 */
	public static Set getSelfandDependencies(IPluginModelBase[] models) {
		return getDependencies(models, new String[0], TargetPlatformHelper.getState(), false);
	}
	
	/** 
	 * @return a set of plug-in IDs
	 * 
	 */
	public static Set getDependencies(Object[] selected, String[] implicit, State state, boolean removeSelf) {
		Set set = new TreeSet();
		for (int i = 0; i < selected.length; i++) {
			if (!(selected[i] instanceof IPluginModelBase))
				continue;
			IPluginModelBase model = (IPluginModelBase)selected[i];
			addBundleAndDependencies(model.getBundleDescription(), set);
			IPluginExtension[] extensions = model.getPluginBase().getExtensions();
			for (int j = 0; j < extensions.length; j++) {
				String point = extensions[j].getPoint();
				if (point != null) {
					int dot = point.lastIndexOf('.');
					if (dot != -1) {
						String id = point.substring(0, dot);
						addBundleAndDependencies(state.getBundle(id, null), set);
					}
				}
			}
		}
		
		for (int i = 0; i < implicit.length; i++) {
			addBundleAndDependencies(state.getBundle(implicit[i], null), set);
		}
		
		if (removeSelf) {
			for (int i = 0; i < selected.length; i++) {
				if (!(selected[i] instanceof IPluginModelBase))
					continue;
				IPluginModelBase model = (IPluginModelBase)selected[i];
				set.remove(model.getPluginBase().getId());
			}
		}
		return set;
	}
	
	private static void addBundleAndDependencies(BundleDescription desc, Set set) {
		if (desc != null && set.add(desc.getSymbolicName())) {
			BundleSpecification[] required = desc.getRequiredBundles();
			for (int i = 0; i < required.length; i++) {
				addBundleAndDependencies((BundleDescription)required[i].getSupplier(), set);
			}
			ExportPackageDescription[] exported = desc.getResolvedImports();
			for (int i = 0; i < exported.length; i++) {
				addBundleAndDependencies(exported[i].getExporter(), set);
			}
			BundleDescription[] fragments = desc.getFragments();
			for (int i = 0; i < fragments.length; i++) {
				if (!fragments[i].isResolved())
					continue;
				String id = fragments[i].getSymbolicName();
				if (!"org.eclipse.ui.workbench.compatibility".equals(id)) //$NON-NLS-1$
					addBundleAndDependencies(fragments[i], set);
			}
			HostSpecification host = desc.getHost();
			if (host != null)
				addBundleAndDependencies((BundleDescription)host.getSupplier(), set);
		}
	}
	
	
}
