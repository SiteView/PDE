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
package org.eclipse.pde.internal.core.plugin;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.util.PDEXMLHelper;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PluginLibrary extends PluginObject implements IPluginLibrary {

	private static final long serialVersionUID = 1L;
	private String[] fContentFilters;
	private boolean fExported = false;
	private String fType;

	public PluginLibrary() {
	}
	
	public boolean isValid() {
		return fName != null;
	}
	
	public String[] getContentFilters() {
		IPluginModelBase model = (IPluginModelBase)getModel();
		if (ClasspathUtilCore.hasBundleStructure(model)) {
			BundleDescription desc = model.getBundleDescription();
			if (desc != null) {
				ArrayList list = new ArrayList();
				ExportPackageDescription[] exports = desc.getExportPackages();
				for (int i = 0; i < exports.length; i++) {
					list.add(exports[i].getName());
				}
				return (String[])list.toArray(new String[list.size()]);
			}
		}
		if (!isExported())
			return new String[0];
		return isFullyExported() ? new String[] {"**"} : fContentFilters; //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#addContentFilter(java.lang.String)
	 */
	public void addContentFilter(String filter) throws CoreException {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#removeContentFilter(java.lang.String)
	 */
	public void removeContentFilter(String filter) throws CoreException {
	}
	
	public String[] getPackages() {
		return new String[0];
	}
	
	public boolean isExported() {
		return fExported;
	}
	
	public boolean isFullyExported() {
		return fExported
			&& (fContentFilters == null || fContentFilters.length == 0);
	}

	public String getType() {
		return fType;
	}

	
	public void load(String name) {
		fName = name;
		fExported = true;
	}
	
	void load(Node node) {
		fName = getNodeAttribute(node, "name"); //$NON-NLS-1$
		fType = getNodeAttribute(node, "type"); //$NON-NLS-1$
		NodeList children = node.getChildNodes();
		Vector exports = new Vector();
		boolean all = false;
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String tag = child.getNodeName().toLowerCase(Locale.ENGLISH);
				if (tag.equals("export")) { //$NON-NLS-1$
					String ename = getNodeAttribute(child, "name"); //$NON-NLS-1$
					if (ename != null) {
						ename = ename.trim();
						if (ename.equals("*")) { //$NON-NLS-1$
							all = true;
						} else {
							exports.add(ename);
						}
					}
				} 
			}
		}
		if (exports.size() > 0) {
			fContentFilters = new String[exports.size()];
			exports.copyInto(fContentFilters);
		}
		fExported = all || exports.size() > 0;
	}
	public void setContentFilters(String[] filters) throws CoreException {
		ensureModelEditable();
		ArrayList oldValue = createArrayList(fContentFilters);
		fContentFilters = filters;
		firePropertyChanged(
			P_CONTENT_FILTERS,
			oldValue,
			createArrayList(filters));
	}

	public void setPackages(String[] packages) throws CoreException {
	}

	public void setExported(boolean value) throws CoreException {
		ensureModelEditable();
		Boolean oldValue = new Boolean(fExported);
		fExported = value;
		firePropertyChanged(P_EXPORTED, oldValue, new Boolean(value));
	}

	public void setType(String type) throws CoreException {
		ensureModelEditable();
		String oldValue = fType;
		fType = type;
		firePropertyChanged(P_TYPE, oldValue, type);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_CONTENT_FILTERS)) {
			ArrayList list = (ArrayList) newValue;
			if (list != null)
				setContentFilters(
					(String[]) list.toArray(new String[list.size()]));
			else
				setContentFilters(null);
			return;
		}
		if (name.equals(P_EXPORTED)) {
			setExported(((Boolean) newValue).booleanValue());
			return;
		}
		if (name.equals(P_TYPE)) {
			setType(newValue != null ? newValue.toString() : null);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	private ArrayList createArrayList(String[] array) {
		if (array == null)
			return null;
		ArrayList list = new ArrayList();
		for (int i = 0; i < array.length; i++) {
			list.add(array[i]);
		}
		return list;
	}

	public void write(String indent, PrintWriter writer) {
		// TODO: MP: CCP TOUCH

		writer.println(PDEXMLHelper.getWritableString(getName()));
	}
	
	public void reconnect(ISharedPluginModel model, IPluginObject parent) {
		// TODO: MP: CCP TOUCH
		
		super.reconnect(model, parent);
		// No transient fields
	}	
	
}
