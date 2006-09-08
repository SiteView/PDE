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
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;

public class JUnitWorkbenchShortcut extends JUnitLaunchShortcut {	
		
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.junit.JUnitLaunchShortcut#getLaunchConfigurationTypeId()
	 */
	protected String getLaunchConfigurationTypeId() {
		return "org.eclipse.pde.ui.JunitLaunchConfig"; //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.junit.JUnitLaunchShortcut#createLaunchConfiguration(org.eclipse.jdt.core.IJavaElement)
	 */
	protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement element) throws CoreException {
		ILaunchConfigurationWorkingCopy wc= super.createLaunchConfiguration(element);
		
		if (TargetPlatform.isRuntimeRefactored2())
			wc.setAttribute("pde.version", "3.2a"); //$NON-NLS-1$ //$NON-NLS-2$
		else if (TargetPlatform.isRuntimeRefactored1())
			wc.setAttribute("pde.version", "3.2"); //$NON-NLS-1$ //$NON-NLS-2$
		wc.setAttribute(IPDELauncherConstants.LOCATION, LaunchArgumentsHelper.getDefaultJUnitWorkspaceLocation());
		setJavaArguments(wc);
		wc.setAttribute(IPDELauncherConstants.USE_DEFAULT, true);
		wc.setAttribute(IPDELauncherConstants.DOCLEAR, true);
		wc.setAttribute(IPDELauncherConstants.ASKCLEAR, false);
		wc.setAttribute(IPDELauncherConstants.TRACING_CHECKED, IPDELauncherConstants.TRACING_NONE);
		wc.setAttribute(IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, true);
		wc.setAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, false);
		wc.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, LaunchArgumentsHelper.getDefaultJUnitConfigurationLocation());
		wc.setAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, true);
		wc.setAttribute(
			IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
			"org.eclipse.pde.ui.workbenchClasspathProvider"); //$NON-NLS-1$

		if (JUnitLaunchConfiguration.requiresUI(wc)) {
			String product = TargetPlatform.getDefaultProduct();
			if (product != null) {
				wc.setAttribute(IPDELauncherConstants.USE_PRODUCT, true);
				wc.setAttribute(IPDELauncherConstants.PRODUCT, product);
			}
		} else {
			wc.setAttribute(IPDELauncherConstants.APPLICATION, JUnitLaunchConfiguration.CORE_APPLICATION);				
		}
		return wc;
	}
		
	private void setJavaArguments(ILaunchConfigurationWorkingCopy wc) {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String programArgs = preferences.getString(ICoreConstants.PROGRAM_ARGS);
		if (programArgs.length() > 0)
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, programArgs);
		String vmArgs = preferences.getString(ICoreConstants.VM_ARGS);
		if (vmArgs.length() > 0)
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
	}
	
}
