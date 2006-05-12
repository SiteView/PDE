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

package org.eclipse.pde.internal.ui.wizards.templates;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.plugin.PluginBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.templates.BooleanOption;
import org.eclipse.pde.ui.templates.OptionTemplateSection;
import org.eclipse.pde.ui.templates.TemplateOption;
import org.osgi.framework.Bundle;

public abstract class PDETemplateSection extends OptionTemplateSection {

	public static final String KEY_PRODUCT_BRANDING = "productBranding"; //$NON-NLS-1$
	public static final String KEY_PRODUCT_ID = "productID"; //$NON-NLS-1$
	public static final String KEY_PRODUCT_NAME = "productName"; //$NON-NLS-1$
	
	protected TemplateOption fPIDOption;
	protected TemplateOption fPNameOption;
	protected BooleanOption fPBrandingOption;
	
	protected ResourceBundle getPluginResourceBundle() {
		Bundle bundle = Platform.getBundle(PDEPlugin.getPluginId());
		return Platform.getResourceBundle(bundle);
	}
	
	protected URL getInstallURL() {
		return PDEPlugin.getDefault().getInstallURL();
	}
	
	public URL getTemplateLocation() {
		try {
			String[] candidates = getDirectoryCandidates();
			for (int i = 0; i < candidates.length; i++) {
				if (PDEPlugin.getDefault().getBundle().getEntry(candidates[i]) != null) {
					URL candidate = new URL(getInstallURL(), candidates[i]);
					return candidate;
				}
			}
		} catch (MalformedURLException e) {
		}
		return null;
	}

	private String[] getDirectoryCandidates() {
		String version = getVersion(model.getPluginBase());
		if ("3.0".equals(version)) //$NON-NLS-1$
			return new String[] { "templates_3.0" + "/" + getSectionId() + "/" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if ("3.1".equals(version) || "3.2".equals(version)) //$NON-NLS-1$ //$NON-NLS-2$
			return new String[] { 
					"templates_3.2" + "/" + getSectionId() + "/", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					"templates_3.1" + "/" + getSectionId() + "/", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					"templates_3.0" + "/" + getSectionId() + "/" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return new String[] { "templates" + "/" + getSectionId() + "/" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
    
    private String getVersion(IPluginBase plugin) {
        // workaround to not introduce new API for IPluginBase
        if (plugin instanceof PluginBase)
            return ((PluginBase)plugin).getTargetVersion();
        return TargetPlatform.getTargetVersionString();
    }
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.ITemplateSection#getFoldersToInclude()
	 */
	public String[] getNewFiles() {
		return new String[0];
	}
	
	protected String getFormattedPackageName(String id){
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < id.length(); i++) {
			char ch = id.charAt(i);
			if (buffer.length() == 0) {
				if (Character.isJavaIdentifierStart(ch))
					buffer.append(Character.toLowerCase(ch));
			} else {
				if (Character.isJavaIdentifierPart(ch) || ch == '.')
					buffer.append(ch);
			}
		}
		return buffer.toString().toLowerCase(Locale.ENGLISH);
	}
	
	protected void generateFiles(IProgressMonitor monitor) throws CoreException {
		super.generateFiles(monitor);
		if (copyBrandingDirectory())
			super.generateFiles(monitor, PDEPlugin.getDefault().getBundle().getEntry("branding/"));
	}
	
	protected boolean copyBrandingDirectory() {
		return getBooleanOption(KEY_PRODUCT_BRANDING);
	}
	
	protected void createBrandingOptions() {
		fPBrandingOption = (BooleanOption)addOption(KEY_PRODUCT_BRANDING, PDEUIMessages.HelloRCPTemplate_productBranding, false, 0);
		fPIDOption = addOption(KEY_PRODUCT_ID, PDEUIMessages.MailTemplate_productID, "product", 0); //$NON-NLS-1$
		fPIDOption.setEnabled(false);
		fPNameOption = addOption(KEY_PRODUCT_NAME, PDEUIMessages.MailTemplate_productName, "RCP Product", 0); //$NON-NLS-1$
		fPNameOption.setEnabled(false);
	}
	
	protected void updateBrandingEnablement() {
		fPIDOption.setEnabled(fPBrandingOption.isSelected());
		fPNameOption.setEnabled(fPBrandingOption.isSelected());
	}
}
