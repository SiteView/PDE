/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.build;

import java.io.File;
import java.util.regex.Pattern;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.exports.*;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.core.site.WorkspaceSiteModel;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Version;

public class BuildSiteJob extends FeatureExportJob {

	private static FeatureExportInfo getInfo(IFeatureModel[] models, ISiteModel siteModel) {
		FeatureExportInfo info = new FeatureExportInfo();
		info.useJarFormat = true;
		info.toDirectory = true;
		info.destinationDirectory = siteModel.getUnderlyingResource().getParent().getLocation().toOSString();
		info.items = models;
		return info;
	}

	private long fBuildTime;

	private IFeatureModel[] fFeatureModels;
	private ISiteModel fSiteModel;
	private IContainer fSiteContainer;

	public BuildSiteJob(IFeatureModel[] features, ISiteModel site) {
		super(getInfo(features, site), PDEUIMessages.BuildSiteJob_name);
		fFeatureModels = features;
		fSiteModel = site;
		fSiteContainer = site.getUnderlyingResource().getParent();
		setRule(MultiRule.combine(fSiteContainer.getProject(), getRule()));
	}

	protected IStatus run(IProgressMonitor monitor) {
		fBuildTime = System.currentTimeMillis();
		IStatus status = super.run(monitor);
		try {
			fSiteContainer.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			updateSiteFeatureVersions();
		} catch (CoreException ce) {
			PDECore.logException(ce);
		}
		return status;
	}

	protected FeatureExportOperation createOperation() {
		return new SiteBuildOperation(fInfo);
	}

	private void updateSiteFeatureVersions() {
		try {
			for (int i = 0; i < fFeatureModels.length; i++) {
				IFeature feature = fFeatureModels[i].getFeature();
				Version pvi = Version.parseVersion(feature.getVersion());

				if ("qualifier".equals(pvi.getQualifier())) { //$NON-NLS-1$
					String newVersion = findBuiltVersion(feature.getId(), pvi.getMajor(), pvi.getMinor(), pvi.getMicro());
					if (newVersion == null) {
						continue;
					}
					ISiteFeature reVersionCandidate = findSiteFeature(feature, pvi);
					if (reVersionCandidate != null) {
						reVersionCandidate.setVersion(newVersion);
						reVersionCandidate.setURL("features/" + feature.getId() + "_" //$NON-NLS-1$ //$NON-NLS-2$
								+ newVersion + ".jar"); //$NON-NLS-1$
					}
				}
			}
			((WorkspaceSiteModel) fSiteModel).save();
		} catch (CoreException ce) {
			PDEPlugin.logException(ce);
		}
	}

	private ISiteFeature findSiteFeature(IFeature feature, Version pvi) {
		ISiteFeature reversionCandidate = null;
		// first see if version with qualifier being qualifier is present among
		// site features
		ISiteFeature[] siteFeatures = fSiteModel.getSite().getFeatures();
		for (int s = 0; s < siteFeatures.length; s++) {
			if (siteFeatures[s].getId().equals(feature.getId()) && siteFeatures[s].getVersion().equals(feature.getVersion())) {
				return siteFeatures[s];
			}
		}
		String highestQualifier = null;
		// then find feature with the highest qualifier
		for (int s = 0; s < siteFeatures.length; s++) {
			if (siteFeatures[s].getId().equals(feature.getId())) {
				Version candidatePvi = Version.parseVersion(siteFeatures[s].getVersion());
				if (pvi.getMajor() == candidatePvi.getMajor() && pvi.getMinor() == candidatePvi.getMinor() && pvi.getMicro() == candidatePvi.getMicro()) {
					if (reversionCandidate == null || candidatePvi.getQualifier().compareTo(highestQualifier) > 0) {
						reversionCandidate = siteFeatures[s];
						highestQualifier = candidatePvi.getQualifier();
					}
				}
			}
		}
		return reversionCandidate;
	}

	/**
	 * Finds the highest version from feature jars. ID and version components
	 * are constant. Qualifier varies
	 * 
	 * @param builtJars
	 *            candidate jars in format id_version.jar
	 * @param id
	 * @param major
	 * @param minor
	 * @param service
	 */
	private String findBuiltVersion(String id, int major, int minor, int service) {
		IFolder featuresFolder = fSiteContainer.getFolder(new Path("features")); //$NON-NLS-1$
		if (!featuresFolder.exists()) {
			return null;
		}
		IResource[] featureJars = null;
		try {
			featureJars = featuresFolder.members();
		} catch (CoreException ce) {
			return null;
		}
		Pattern pattern = PatternConstructor.createPattern(id + "_" //$NON-NLS-1$
				+ major + "." //$NON-NLS-1$
				+ minor + "." //$NON-NLS-1$
				+ service + "*.jar", true); //$NON-NLS-1$ 
		// finding the newest feature archive
		String newestName = null;
		long newestTime = 0;
		for (int i = 0; i < featureJars.length; i++) {
			File file = new File(featureJars[i].getLocation().toOSString());
			long jarTime = file.lastModified();
			String jarName = featureJars[i].getName();

			if (jarTime < fBuildTime) {
				continue;
			}
			if (jarTime <= newestTime) {
				continue;
			}
			if (pattern.matcher(jarName).matches()) {
				newestName = featureJars[i].getName();
				newestTime = jarTime;
			}
		}
		if (newestName == null) {
			return null;
		}

		return newestName.substring(id.length() + 1, newestName.length() - 4);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.exports.FeatureExportJob#getLogFoundMessage()
	 */
	protected String getLogFoundMessage() {
		return PDEUIMessages.BuildSiteJob_message;
	}
}
