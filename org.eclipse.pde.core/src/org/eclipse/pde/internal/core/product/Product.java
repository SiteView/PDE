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
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.TreeMap;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.iproduct.IAboutInfo;
import org.eclipse.pde.internal.core.iproduct.IArgumentsInfo;
import org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo;
import org.eclipse.pde.internal.core.iproduct.IIntroInfo;
import org.eclipse.pde.internal.core.iproduct.IJVMInfo;
import org.eclipse.pde.internal.core.iproduct.ILauncherInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductModelFactory;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.core.iproduct.ISplashInfo;
import org.eclipse.pde.internal.core.iproduct.IWindowImages;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class Product extends ProductObject implements IProduct {

	private static final long serialVersionUID = 1L;
	private String fId;
	private String fName;
	private String fApplication;
	private IAboutInfo fAboutInfo;
	
	private TreeMap fPlugins = new TreeMap();
	private TreeMap fFeatures = new TreeMap();
	private IConfigurationFileInfo fConfigIniInfo;
	private IJVMInfo fJVMInfo;
	private boolean fUseFeatures;
	private IWindowImages fWindowImages;
	private ISplashInfo fSplashInfo;
	private ILauncherInfo fLauncherInfo;
	private IArgumentsInfo fLauncherArgs;
	private IIntroInfo fIntroInfo;

	public Product(IProductModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getId()
	 */
	public String getId() {
		return fId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getName()
	 */
	public String getName() {
		return fName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getApplication()
	 */
	public String getApplication() {
		return fApplication;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getDefiningPluginId()
	 */
	public String getDefiningPluginId() {
		if (fId == null)
			return null;
		int dot = fId.lastIndexOf('.');
		return (dot != -1) ? fId.substring(0, dot) : null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#setId(java.lang.String)
	 */
	public void setId(String id) {
		String old = fId;
		fId = id;
		if (isEditable())
			firePropertyChanged(P_ID, old, fId);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#setName(java.lang.String)
	 */
	public void setName(String name) {
		String old = fName;
		fName = name;
		if (isEditable())
			firePropertyChanged(P_NAME, old, fName);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#setAboutInfo(org.eclipse.pde.internal.core.iproduct.IAboutInfo)
	 */
	public void setAboutInfo(IAboutInfo info) {
		fAboutInfo = info;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#setApplication(java.lang.String)
	 */
	public void setApplication(String application) {
		String old = fApplication;
		fApplication = application;
		if (isEditable())
			firePropertyChanged(P_APPLICATION, old, fApplication);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.product.ProductObject#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<product"); //$NON-NLS-1$
		if (fName != null && fName.length() > 0)
			writer.print(" " + P_NAME + "=\"" + getWritableString(fName) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (fId != null && fId.length() > 0)
			writer.print(" " + P_ID + "=\"" + fId + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (fApplication != null && fApplication.length() > 0)
			writer.print(" " + P_APPLICATION + "=\"" + fApplication + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		writer.print(" " + P_USEFEATURES + "=\"" + Boolean.toString(fUseFeatures) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		writer.println(">"); //$NON-NLS-1$

		if (fAboutInfo != null) {
			writer.println();
			fAboutInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		if (fConfigIniInfo != null) {
			writer.println();
			fConfigIniInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		if (fLauncherArgs != null) {
			writer.println();
			fLauncherArgs.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		if (fWindowImages != null) {
			writer.println();
			fWindowImages.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		if (fSplashInfo != null) {
			writer.println();
			fSplashInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		if (fLauncherInfo != null) {
			writer.println();
			fLauncherInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		if (fIntroInfo != null) {
			writer.println();
			fIntroInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		if (fJVMInfo != null) {
			writer.println();
			fJVMInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		writer.println();
		writer.println(indent + "   <plugins>"); //$NON-NLS-1$  
		Iterator iter = fPlugins.values().iterator();
		while (iter.hasNext()) {
			IProductPlugin plugin = (IProductPlugin)iter.next();
			plugin.write(indent + "      ", writer); //$NON-NLS-1$
		}
		writer.println(indent + "   </plugins>"); //$NON-NLS-1$
		
		if (fFeatures.size() > 0) {
			writer.println();
			writer.println(indent + "   <features>"); //$NON-NLS-1$
			iter = fFeatures.values().iterator();
			while (iter.hasNext()) {
				IProductFeature feature = (IProductFeature)iter.next();
				feature.write(indent + "      ", writer); //$NON-NLS-1$
			}
			writer.println(indent + "   </features>"); //$NON-NLS-1$
		}
		
		writer.println();
		writer.println("</product>"); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getAboutInfo()
	 */
	public IAboutInfo getAboutInfo() {
		return fAboutInfo;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#reset()
	 */
	public void reset() {
		fAboutInfo = null;
		fApplication = null;
		fId = null;
		fName = null;
		fIntroInfo = null;
		fJVMInfo = null;
		fPlugins.clear();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE 
				&& node.getNodeName().equals("product")) { //$NON-NLS-1$
			Element element = (Element)node;
			fApplication = element.getAttribute(P_APPLICATION); 
			fId = element.getAttribute(P_ID); 
			fName = element.getAttribute(P_NAME); 
			fUseFeatures = "true".equals(element.getAttribute(P_USEFEATURES)); //$NON-NLS-1$
			NodeList children = node.getChildNodes();
			IProductModelFactory factory = getModel().getFactory();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					String name = child.getNodeName();
					if (name.equals("aboutInfo")) { //$NON-NLS-1$
						fAboutInfo = factory.createAboutInfo();
						fAboutInfo.parse(child);
					} else if (name.equals("plugins")) { //$NON-NLS-1$
						parsePlugins(child.getChildNodes());
					} else if (name.equals("features")) { //$NON-NLS-1$
						parseFeatures(child.getChildNodes());
					} else if (name.equals("configIni")) { //$NON-NLS-1$
						fConfigIniInfo = factory.createConfigFileInfo();
						fConfigIniInfo.parse(child);
					} else if (name.equals("windowImages")) { //$NON-NLS-1$
						fWindowImages = factory.createWindowImages();
						fWindowImages.parse(child);
					} else if (name.equals("splash")) { //$NON-NLS-1$
						fSplashInfo = factory.createSplashInfo();
						fSplashInfo.parse(child);
					} else if (name.equals("launcher")) { //$NON-NLS-1$
						fLauncherInfo = factory.createLauncherInfo();
						fLauncherInfo.parse(child);
					} else if (name.equals("launcherArgs")) { //$NON-NLS-1$
						fLauncherArgs = factory.createLauncherArguments();
						fLauncherArgs.parse(child);
					} else if (name.equals("intro")) { //$NON-NLS-1$
						fIntroInfo = factory.createIntroInfo();
						fIntroInfo.parse(child);
					} else if (name.equals("vm")) { //$NON-NLS-1$
						fJVMInfo = factory.createJVMInfo();
						fJVMInfo.parse(child);
					}
				}
			}
		}
	}
	
	private void parsePlugins(NodeList children) {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("plugin")) { //$NON-NLS-1$
					IProductPlugin plugin = getModel().getFactory().createPlugin();
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
					IProductFeature feature = getModel().getFactory().createFeature();
					feature.parse(child);
					fFeatures.put(feature.getId(), feature);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#addPlugin(org.eclipse.pde.internal.core.iproduct.IProductPlugin)
	 */
	public void addPlugins(IProductPlugin[] plugins) {
		boolean modified = false;
		for (int i = 0; i < plugins.length; i++) {
			if (plugins[i] == null)
				continue;
			String id = plugins[i].getId();
			if (id == null || fPlugins.containsKey(id)) {
				plugins[i] = null;
				continue;
			}
			
			plugins[i].setModel(getModel());
			fPlugins.put(id, plugins[i]);
			modified = true;
		}
		if (modified && isEditable())
			fireStructureChanged(plugins, IModelChangedEvent.INSERT);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#removePlugins(org.eclipse.pde.internal.core.iproduct.IProductPlugin[])
	 */
	public void removePlugins(IProductPlugin[] plugins) {
		boolean modified = false;
		for (int i = 0; i < plugins.length; i++) {
			if (fPlugins.remove(plugins[i].getId()) != null)
				modified = true;
		}
		if (modified && isEditable())
			fireStructureChanged(plugins, IModelChangedEvent.REMOVE);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getPlugins()
	 */
	public IProductPlugin[] getPlugins() {
		return (IProductPlugin[])fPlugins.values().toArray(new IProductPlugin[fPlugins.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getConfigurationFileInfo()
	 */
	public IConfigurationFileInfo getConfigurationFileInfo() {
		return fConfigIniInfo;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#setConfigurationFileInfo(org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo)
	 */
	public void setConfigurationFileInfo(IConfigurationFileInfo info) {
		fConfigIniInfo = info;
	}

	public boolean useFeatures() {
		return fUseFeatures;
	}

	public void setUseFeatures(boolean use) {
		boolean old = fUseFeatures;
		fUseFeatures = use;
		if (isEditable())
			firePropertyChanged(P_USEFEATURES, Boolean.toString(old), Boolean.toString(fUseFeatures));
	}

	public boolean containsPlugin(String id) {
		return fPlugins.containsKey(id);
	}
	
	public boolean containsFeature(String id) {
		return fFeatures.containsKey(id);
	}

	public IWindowImages getWindowImages() {
		return fWindowImages;
	}

	public void setWindowImages(IWindowImages images) {
		fWindowImages = images;
	}

	public ISplashInfo getSplashInfo() {
		return fSplashInfo;
	}

	public void setSplashInfo(ISplashInfo info) {
		fSplashInfo = info;
	}

	public ILauncherInfo getLauncherInfo() {
		return fLauncherInfo;
	}

	public void setLauncherInfo(ILauncherInfo info) {
		fLauncherInfo = info;
	}

	public void addFeatures(IProductFeature[] features) {
		boolean modified = false;
		for (int i = 0; i < features.length; i++) {
			if (features[i] == null)
				continue;
			String id = features[i].getId();
			if (fFeatures.containsKey(id)) {
				features[i] = null;
				continue;
			}
			
			features[i].setModel(getModel());
			fFeatures.put(id, features[i]);
			modified = true;
		}
		
		if (modified && isEditable())
			fireStructureChanged(features, IModelChangedEvent.INSERT);
	}

	public void removeFeatures(IProductFeature[] features) {
		boolean modified = false;
		for (int i = 0; i < features.length; i++) {
			if (fFeatures.remove(features[i].getId()) != null)
				modified = true;
		}
		if (modified && isEditable())
			fireStructureChanged(features, IModelChangedEvent.REMOVE);
	}

	public IProductFeature[] getFeatures() {
		return (IProductFeature[])fFeatures.values().toArray(new IProductFeature[fFeatures.size()]);
	}

	public IArgumentsInfo getLauncherArguments() {
		return fLauncherArgs;
	}

	public void setLauncherArguments(IArgumentsInfo info) {
		fLauncherArgs = info;
	}

	public IIntroInfo getIntroInfo() {
		return fIntroInfo;
	}

	public void setIntroInfo(IIntroInfo introInfo) {
		fIntroInfo = introInfo;
	}

	public IJVMInfo getJVMLocations() {
		return fJVMInfo;
	}

	public void setJVMLocations(IJVMInfo info) {
		fJVMInfo = info;
	}

}
