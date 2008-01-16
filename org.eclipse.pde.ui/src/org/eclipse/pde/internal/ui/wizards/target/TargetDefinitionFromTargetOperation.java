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
package org.eclipse.pde.internal.ui.wizards.target;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetDefinitionManager;
import org.eclipse.pde.internal.core.itarget.ITargetModel;

public class TargetDefinitionFromTargetOperation extends BaseTargetDefinitionOperation {

	private String fTargetId;

	public TargetDefinitionFromTargetOperation(IFile file, String id) {
		super(file);
		fTargetId = id;
	}

	protected void initializeTarget(ITargetModel model) {
		IConfigurationElement elem = PDECore.getDefault().getTargetProfileManager().getTarget(fTargetId);
		String path = elem.getAttribute("definition"); //$NON-NLS-1$
		String symbolicName = elem.getDeclaringExtension().getNamespaceIdentifier();
		URL url = TargetDefinitionManager.getResourceURL(symbolicName, path);
		if (url != null) {
			try {
				model.load(new BufferedInputStream(url.openStream()), false);
			} catch (CoreException e) {
			} catch (IOException e) {
			}
		}
	}

}
