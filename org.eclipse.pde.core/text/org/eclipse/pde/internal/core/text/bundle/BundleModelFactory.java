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

import org.eclipse.jface.text.TextUtilities;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundleModelFactory;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.osgi.framework.Constants;

public class BundleModelFactory implements IBundleModelFactory {
	
	private IBundleModel fModel;

	public BundleModelFactory(IBundleModel model) {
		fModel = model;
	}
	

	public IManifestHeader createHeader() {
		return null;
	}

    public IManifestHeader createHeader(String key, String value) {
        ManifestHeader header = null;
        IBundle bundle = fModel.getBundle();
        String newLine;
        if (fModel instanceof BundleModel) 
        	newLine = TextUtilities.getDefaultLineDelimiter(((BundleModel)fModel).getDocument());
        else
        	newLine = System.getProperty("line.separator");
        
        if (key.equals(Constants.EXPORT_PACKAGE) || key.equals(ICoreConstants.PROVIDE_PACKAGE)) {
 			header = new ExportPackageHeader(key, value, bundle, newLine);
        } else if (key.equals(Constants.IMPORT_PACKAGE)){
 			header = new ImportPackageHeader(key, value, bundle, newLine);
        } else if (key.equals(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT)) {
        	header = new RequiredExecutionEnvironmentHeader(key, value, bundle, newLine);
        } else if (key.equals(ICoreConstants.ECLIPSE_LAZYSTART) || key.equals(ICoreConstants.ECLIPSE_AUTOSTART)) {
        	header = new LazyStartHeader(key, value, bundle, newLine);
        } else {
            header = new ManifestHeader(key, value, bundle, newLine);
        }
        return header;
    }

}
