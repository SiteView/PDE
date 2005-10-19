/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.model.bundle;

import java.util.TreeMap;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.Constants;

public abstract class BasePackageHeader extends ManifestHeader {

    private static final long serialVersionUID = 1L;
    
    protected TreeMap fPackages = new TreeMap();
    
	public BasePackageHeader(String name, String value, IBundle bundle,
			String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
		processValue();
	}

    protected String getVersionAttribute() {
        int manifestVersion = BundlePluginBase.getBundleManifestVersion(getBundle());
        return (manifestVersion < 2) ? ICoreConstants.PACKAGE_SPECIFICATION_VERSION : Constants.VERSION_ATTRIBUTE;
    }
    
    protected abstract void processValue();

    public void addPackage(PackageObject object) {
        fPackages.put(object.getName(), object);
        fireStructureChanged(object, IModelChangedEvent.INSERT);
    }
    
    public Object removePackage(PackageObject object) {
        Object value = fPackages.remove(object.getName());
        fireStructureChanged(object, IModelChangedEvent.REMOVE);
        return value;
    }
    
    public boolean hasPackage(String packageName) {
        return fPackages.containsKey(packageName);
    }
    
    public Object removePackage(String name) {
    	PackageObject object = (PackageObject) fPackages.remove(name);
    	fireStructureChanged(object, IModelChangedEvent.REMOVE);
    	return object;
    }
    
    public boolean isEmpty() {
    	return fPackages.size() == 0;
    }
    
    public boolean renamePackage(String oldName, String newName) {
    	if (hasPackage(oldName)) {
    		PackageObject object = (PackageObject)fPackages.remove(oldName);
    		object.setName(newName);
    		fPackages.put(newName, object);
    		return true;
    	}
    	return false;
    }
  

}
