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
package org.eclipse.pde.internal.core.text.bundle;

import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class ImportPackageObject extends PackageObject {
    
    private static final long serialVersionUID = 1L;
       
    private static String getVersion(ExportPackageDescription desc) {
        String version = desc.getVersion().toString();
        if (!version.equals(Version.emptyVersion.toString()))
            return desc.getVersion().toString();
        return null;
    }
    
    public ImportPackageObject(ManifestHeader header, ManifestElement element, String versionAttribute) {
        super(header, element, versionAttribute);
    }
    
    public ImportPackageObject(ManifestHeader header, ExportPackageDescription desc, String versionAttribute) {       
        super(header, desc.getName(), getVersion(desc), versionAttribute);
    }
   
    public boolean isOptional() {
    	int manifestVersion = BundlePluginBase.getBundleManifestVersion(getHeader().getBundle());
    	if (manifestVersion > 1)
    		return Constants.RESOLUTION_OPTIONAL.equals(getDirective(Constants.RESOLUTION_DIRECTIVE));
    	return "true".equals(getAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE));
    }

    public void setOptional(boolean optional) {
    	int manifestVersion = BundlePluginBase.getBundleManifestVersion(getHeader().getBundle());
    	if (optional) {
    		if (manifestVersion > 1)
    			setDirective(Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL);
    		else
    			setAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE, "true");
    	} else {
    		setDirective(Constants.RESOLUTION_DIRECTIVE, null);
    		setAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE, null);
    	}
    }

}
