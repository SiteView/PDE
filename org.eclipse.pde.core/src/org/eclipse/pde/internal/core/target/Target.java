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
package org.eclipse.pde.internal.core.target;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ifeature.IEnvironment;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.itarget.IAdditionalLocation;
import org.eclipse.pde.internal.core.itarget.IArgumentsInfo;
import org.eclipse.pde.internal.core.itarget.IEnvironmentInfo;
import org.eclipse.pde.internal.core.itarget.IImplicitDependenciesInfo;
import org.eclipse.pde.internal.core.itarget.ILocationInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetFeature;
import org.eclipse.pde.internal.core.itarget.ITargetJRE;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.itarget.ITargetModelFactory;
import org.eclipse.pde.internal.core.itarget.ITargetObject;
import org.eclipse.pde.internal.core.itarget.ITargetPlugin;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Target extends TargetObject implements ITarget {

	private static final long serialVersionUID = 1L;
	private String fName;
	private TreeMap fPlugins = new TreeMap();
	private TreeMap fFeatures = new TreeMap();
	private IArgumentsInfo fArgsInfo;
	private IEnvironmentInfo fEnvInfo;
	private ITargetJRE fRuntimeInfo;
	private ILocationInfo fLocationInfo;
	private IImplicitDependenciesInfo fImplicitInfo;
	private boolean fUseAllTargetPlatform = false;
	private Set fAdditionalDirectories = new HashSet();
	private String fDescription = null;
	
	public Target(ITargetModel model) {
		super(model);
	}

	public void reset() {
		fArgsInfo = null;
		fEnvInfo = null;
		fRuntimeInfo = null;
		fLocationInfo = null;
		fImplicitInfo = null;
		fPlugins.clear();
		fFeatures.clear();
		fUseAllTargetPlatform = false;
		fAdditionalDirectories.clear();
	}

	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE 
				&& node.getNodeName().equals("target")) { //$NON-NLS-1$
			Element element = (Element)node; 
			fName = element.getAttribute(P_NAME); 
			NodeList children = node.getChildNodes();
			ITargetModelFactory factory = getModel().getFactory();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					String name = child.getNodeName();
					if (name.equals("launcherArgs")) { //$NON-NLS-1$
						fArgsInfo = factory.createArguments();
						fArgsInfo.parse(child);
					} else if (name.equals("content")) { //$NON-NLS-1$
						parseContent((Element)child);
					} else if (name.equals("environment")) { //$NON-NLS-1$
						fEnvInfo = factory.createEnvironment();
						fEnvInfo.parse(child);
					} else if (name.equals("targetJRE")) { //$NON-NLS-1$
						fRuntimeInfo = factory.createJREInfo();
						fRuntimeInfo.parse(child);
					} else if (name.equals("location")) { //$NON-NLS-1$
						fLocationInfo = factory.createLocation();
						fLocationInfo.parse(child);
					} else if (name.equals("implicitDependencies")) { //$NON-NLS-1$
						fImplicitInfo = factory.createImplicitPluginInfo();
						fImplicitInfo.parse(child);
					}
				}
			}
		}
	}
	
	private void parseContent(Element content) {
		fUseAllTargetPlatform =
			"true".equals(content.getAttribute("useAllPlugins")); //$NON-NLS-1$ //$NON-NLS-2$
		NodeList children = content.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if ("plugins".equals(node.getNodeName())) { //$NON-NLS-1$
				parsePlugins(node.getChildNodes());
			} else if ("features".equals(node.getNodeName())) { //$NON-NLS-1$
				parseFeatures(node.getChildNodes());
			} else if ("extraLocations".equals(node.getNodeName())) { //$NON-NLS-1$
				parseLocations(node.getChildNodes());
			}
		}	
	}
	
	private void parsePlugins(NodeList children) {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("plugin")) { //$NON-NLS-1$
					ITargetPlugin plugin = getModel().getFactory().createPlugin();
					plugin.parse(child);
					fPlugins.put(plugin.getId(), plugin);
				}
			}
		}
	}
	
	private void parseFeatures(NodeList children) {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("feature")) { //$NON-NLS-1$
					ITargetFeature feature = getModel().getFactory().createFeature();
					feature.parse(child);
					fFeatures.put(feature.getId(), feature);
				}
			}
		}
	}
	
	private void parseLocations(NodeList children) {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("location")) { //$NON-NLS-1$
					IAdditionalLocation loc = getModel().getFactory().createAdditionalLocation();
					loc.parse(child);
					fAdditionalDirectories.add(loc);
				}
			}
		}
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<target"); //$NON-NLS-1$
		if (fName != null && fName.length() > 0)
			writer.print(" " + P_NAME + "=\"" + getWritableString(fName) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		writer.println(">"); //$NON-NLS-1$
		if (fArgsInfo != null) {
			fArgsInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		if (fEnvInfo != null) {
			fEnvInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		if (fRuntimeInfo != null) {
			fRuntimeInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		if (fLocationInfo != null) {
			fLocationInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		writer.println();
		if (fUseAllTargetPlatform) {
			writer.println(indent + "   <content useAllPlugins=\"true\">"); //$NON-NLS-1$
		} else {
			writer.println(indent + "   <content>"); //$NON-NLS-1$
		}
		
		writer.println(indent + "      <plugins>"); //$NON-NLS-1$
		Iterator iter = fPlugins.values().iterator();
		while (iter.hasNext()) {
			ITargetPlugin plugin = (ITargetPlugin) iter.next();
			plugin.write(indent + "         ", writer); //$NON-NLS-1$
		}
		writer.println(indent + "      </plugins>"); //$NON-NLS-1$		
		writer.println(indent + "      <features>"); //$NON-NLS-1$
		iter = fFeatures.values().iterator();
		while (iter.hasNext()) {
			ITargetFeature feature = (ITargetFeature) iter.next();
			feature.write(indent + "         ", writer); //$NON-NLS-1$
		}
		writer.println(indent + "      </features>"); //$NON-NLS-1$
		if (!fAdditionalDirectories.isEmpty()) {
			writer.println(indent + "      <extraLocations>"); //$NON-NLS-1$
			iter = fAdditionalDirectories.iterator();
			while (iter.hasNext()) {
				IAdditionalLocation location = (IAdditionalLocation) iter.next();
				location.write(indent + "         ", writer); //$NON-NLS-1$
			}
			writer.println(indent + "      </extraLocations>"); //$NON-NLS-1$
		}
		writer.println(indent + "   </content>"); //$NON-NLS-1$
		if (fImplicitInfo != null) {
			fImplicitInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		writer.println();
		writer.println(indent + "</target>"); //$NON-NLS-1$
	}
	
	public IArgumentsInfo getArguments() {
		return fArgsInfo;
	}
	
	public void setArguments(IArgumentsInfo info) {
		fArgsInfo = info;
	}

	public IEnvironmentInfo getEnvironment() {
		return fEnvInfo;
	}

	public void setEnvironment(IEnvironmentInfo info) {
		fEnvInfo = info;
	}

	public ITargetJRE getTargetJREInfo() {
		return fRuntimeInfo;
	}

	public void setTargetJREInfo(ITargetJRE info) {
		fRuntimeInfo = info;
		
	}

	public String getName() {
		return fName;
	}

	public void setName(String name) {
		String oldValue = fName;
		fName = name;
		firePropertyChanged(P_NAME, oldValue, fName);
	}

	public ILocationInfo getLocationInfo() {
		return fLocationInfo;
	}

	public void setLocationInfo(ILocationInfo info) {
		fLocationInfo = info;
	}

	public void addPlugin(ITargetPlugin plugin) {
		addPlugins(new ITargetPlugin[] {plugin});
	}
	
	public void addPlugins(ITargetPlugin[] plugins) {
		ArrayList list = new ArrayList();
		for (int i = 0; i < plugins.length; i ++ ) {
			String id = plugins[i].getId();
			if (fPlugins.containsKey(id))
				continue;
			list.add(plugins[i]);
			plugins[i].setModel(getModel());
			fPlugins.put(id, plugins[i]);
		}
		if (isEditable() && list.size() > 0) {
			fireStructureChanged((ITargetObject[])list.toArray(new ITargetObject[list.size()]), 
								IModelChangedEvent.INSERT);
		}
	}

	public void addFeature(ITargetFeature feature) {
		addFeatures(new ITargetFeature[] {feature});
	}
	
	public void addFeatures(ITargetFeature[] features) {
		ArrayList list = new ArrayList();
		for (int i = 0; i < features.length; i++) {
			String id = features[i].getId();
			if (fFeatures.containsKey(id))
				continue;
			list.add(features[i]);
			features[i].setModel(getModel());
			fFeatures.put(id, features[i]);
		}
		if (isEditable() && list.size() > 0)
			fireStructureChanged((ITargetObject[])list.toArray(new ITargetObject[list.size()]), 
					IModelChangedEvent.INSERT);
	}

	public void removePlugin(ITargetPlugin plugin) {
		removePlugins(new ITargetPlugin[] {plugin});
	}
	
	public void removePlugins(ITargetPlugin[] plugins) {
		boolean modify = false;
		for (int i =0; i < plugins.length; i++) 
			modify = ((fPlugins.remove(plugins[i].getId()) != null) || modify);
		if (isEditable() && modify)
			fireStructureChanged(plugins, IModelChangedEvent.REMOVE);
	}

	public void removeFeature(ITargetFeature feature) {
		removeFeatures(new ITargetFeature[] {feature});
	}
	
	public void removeFeatures(ITargetFeature[] features) {
		boolean modify = false;
		for (int i = 0; i < features.length; i++) {
			modify = ((fFeatures.remove(features[i].getId()) != null) || modify);
		}
		if (isEditable() && modify)
			fireStructureChanged(features, IModelChangedEvent.REMOVE);	
	}

	public ITargetPlugin[] getPlugins() {
		return (ITargetPlugin[]) fPlugins.values().toArray(new ITargetPlugin[fPlugins.size()]);
	}

	public ITargetFeature[] getFeatures() {
		return (ITargetFeature[]) fFeatures.values().toArray(new ITargetFeature[fFeatures.size()]);
	}

	public boolean containsPlugin(String id) {
		return fPlugins.containsKey(id);
	}

	public boolean containsFeature(String id) {
		return fFeatures.containsKey(id);
	}

	public boolean useAllPlugins() {
		return fUseAllTargetPlatform;
	}

	public void setUseAllPlugins(boolean value) {
		boolean oldValue = fUseAllTargetPlatform;
		fUseAllTargetPlatform = value;
		if (isEditable())
			firePropertyChanged(P_ALL_PLUGINS, new Boolean(oldValue), new Boolean(fUseAllTargetPlatform));
	}

	public void setImplicitPluginsInfo(IImplicitDependenciesInfo info) {
		fImplicitInfo = info;
	}

	public IImplicitDependenciesInfo getImplicitPluginsInfo() {
		return fImplicitInfo;
	}

	public IAdditionalLocation[] getAdditionalDirectories() {
		return (IAdditionalLocation[])
			fAdditionalDirectories.toArray(new IAdditionalLocation[fAdditionalDirectories.size()]);
	}

	public void addAdditionalDirectories(IAdditionalLocation[] dirs) {
		for (int i = 0; i < dirs.length; i++) {
			fAdditionalDirectories.add(dirs[i]);
		}
		fireStructureChanged(dirs, IModelChangedEvent.INSERT);
	}
	
	public void removeAdditionalDirectories(IAdditionalLocation[] dirs) {
		for (int i = 0; i < dirs.length; i++) {
			fAdditionalDirectories.remove(dirs[i]);
		}
		fireStructureChanged(dirs, IModelChangedEvent.REMOVE);
	}

	public void setDescription(String desc) {
		fDescription = desc;
	}

	public String getDescription() {
		return fDescription;
	}
	
	//  Would be better implemented if IFeaturePlugin extends IEnvironment.  Look at doing this post 3.2
	public boolean isValidFeatureObject(Object featureObj) {
		IEnvironment env = null;
		IFeaturePlugin plugin = null;
		if (featureObj instanceof IEnvironment)
			env = (IEnvironment) featureObj;
		else
			plugin = (IFeaturePlugin) featureObj;
		boolean result = true;
		
		String value = (env != null) ? env.getArch() : plugin.getArch();
		if (value != null && result) {
			String arch = (fEnvInfo != null) ? fEnvInfo.getArch() : null;
			if (arch == null || arch.length() == 0)
				arch = Platform.getOSArch();
			result = containsProperty(arch, value);
		}
		
		value = (env != null) ? env.getOS() : plugin.getOS();
		if (value != null && result) {
			String os = (fEnvInfo != null) ? fEnvInfo.getOS() : null;
			if (os == null || os.length() == 0)
				os = Platform.getOS();
			result = containsProperty(os, value);
		}
		
		value = (env != null) ? env.getWS() : plugin.getWS();
		if (value != null && result) {
			String ws = (fEnvInfo != null) ? fEnvInfo.getWS() : null;
			if (ws == null || ws.length() == 0)
				ws = Platform.getWS();
			result = containsProperty(ws, value);
		}
		
		value = (env != null) ? env.getNL() : plugin.getNL();
		if (value != null && result) {
			String nl = (fEnvInfo != null) ? fEnvInfo.getNL() : null;
			if (nl == null | nl.length() == 0)
				nl = Platform.getNL();
			result = containsProperty(nl, value);
		}
		return result;
	}
	
	private boolean containsProperty(String property, String value) {
		if (value == null || property == null)
			return false;
		StringTokenizer tokenizer = new StringTokenizer(value, ","); //$NON-NLS-1$
		boolean isFound = false;
		while (tokenizer.hasMoreTokens()) 
			if (property.equals(tokenizer.nextToken().trim()))
				isFound = true;
		return isFound;
	}
}
