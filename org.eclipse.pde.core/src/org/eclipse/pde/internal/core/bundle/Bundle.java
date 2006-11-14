/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bundle;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.ManifestHeader;
import org.osgi.framework.Constants;

public class Bundle extends BundleObject implements IBundle {
    private static final long serialVersionUID = 1L;
    private Dictionary fProperties;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundle#setHeader(java.lang.String, java.lang.String)
	 */
	public void setHeader(String key, String value) {
        if (fProperties == null)
            fProperties = new Properties();
		Object oldValue = fProperties.get(key);
		if (value == null || value.trim().length() == 0)
			fProperties.remove(key);
		else 
			fProperties.put(key, value);
		getModel().fireModelObjectChanged(this, key, oldValue, value);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundle#getHeader(java.lang.String)
	 */
	public String getHeader(String key) {
		if (fProperties == null) {
			return null;
		}
		return (String)fProperties.get(key);
	}
	
	public void load(Dictionary properties) {
		// Passed dictionary is read-only
		fProperties = new Properties();
		Enumeration keys = properties.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement().toString();
			fProperties.put(key, properties.get(key));
		}
	}
	
	public String getLocalization() {
		return getHeader(Constants.BUNDLE_LOCALIZATION);
	}
	
	public void setLocalization(String localization) {
		setHeader(Constants.BUNDLE_LOCALIZATION, localization);
	}
	
	public void renameHeader(String key, String newKey) {
		if (fProperties == null)
			fProperties = new Properties();
		if (fProperties.get(key) != null) {
			fProperties.put(newKey, fProperties.remove(key));
		}
	}
	
	public IManifestHeader getManifestHeader(String key) {
		return new ManifestHeader(key, getHeader(key), this, System.getProperty("line.separator")); //$NON-NLS-1$
	}
	
	protected Dictionary getHeaders() {
		return fProperties;
	}
}
