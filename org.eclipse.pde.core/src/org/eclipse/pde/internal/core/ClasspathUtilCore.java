/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.bundle.BundlePlugin;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.plugin.Plugin;

public class ClasspathUtilCore {
	
	private static boolean ENABLE_RESTRICTIONS = false;
	
	public static IClasspathEntry createContainerEntry() {
		return JavaCore.newContainerEntry(new Path(PDECore.CLASSPATH_CONTAINER_ID));
	}

	public static IClasspathEntry createJREEntry() {
		return JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER")); //$NON-NLS-1$
	}
	
	public static void addLibraries(IPluginModelBase model, boolean isExported, boolean useInclusionPatterns, Vector result) throws CoreException {
		if (new File(model.getInstallLocation()).isFile()) {
			addJARdPlugin(model, isExported, useInclusionPatterns, result);
		} else {
			IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
			for (int i = 0; i < libraries.length; i++) {
				if (IPluginLibrary.RESOURCE.equals(libraries[i].getType()))
					continue;
				IClasspathEntry entry = createLibraryEntry(libraries[i], isExported, useInclusionPatterns);
				if (entry != null && !result.contains(entry)) {
					result.add(entry);
				}
			}		
		}
	}

	protected static void addProjectEntry(IPluginModelBase model, boolean isExported, boolean useinclusionPatterns, Vector result) throws CoreException {
		IProject project = model.getUnderlyingResource().getProject();
		if (project.hasNature(JavaCore.NATURE_ID)) {
			IClasspathEntry entry = null;
			if (ENABLE_RESTRICTIONS && useinclusionPatterns) {
				IPath[] inclusionPatterns = model.isFragmentModel()
												? getInclusionPatterns((IFragmentModel)model)
											    : getInclusions(model);
				IAccessRule[] accessRules = getAccessRules(inclusionPatterns);
				entry = JavaCore.newProjectEntry(
							project.getFullPath(), 
							accessRules, 
							false, 
							new IClasspathAttribute[0], 
							isExported);
			} else {
				entry = JavaCore.newProjectEntry(project.getFullPath(), isExported);		
			}
			if (entry != null && !result.contains(entry))
				result.add(entry);
		}
	}
	
	private static void addJARdPlugin(IPluginModelBase model,
			boolean isExported, boolean useInclusionPatterns, Vector result)
			throws CoreException {
		
		IPath sourcePath = getSourceAnnotation(model, "."); //$NON-NLS-1$
		if (sourcePath == null)
			sourcePath = new Path(model.getInstallLocation());
		
		IClasspathEntry entry = null;
		if (ENABLE_RESTRICTIONS && useInclusionPatterns) {
			IPath[] inclusionPatterns = model.isFragmentModel()
											? getInclusionPatterns((IFragmentModel)model)
										    : getInclusions(model);
			IAccessRule[] accessRules = getAccessRules(inclusionPatterns);
			entry = JavaCore.newLibraryEntry(
						new Path(model.getInstallLocation()), 
						sourcePath, 
						null,
						accessRules,
						new IClasspathAttribute[0],
						isExported);
		} else {
			entry = JavaCore.newLibraryEntry(
						new Path(model.getInstallLocation()), 
						sourcePath, 
						null, 
						isExported);
		}
		if (entry != null && !result.contains(entry)) {
			result.add(entry);
		}
	}

	protected static IClasspathEntry createLibraryEntry(
		IPluginLibrary library,
		boolean exported,
		boolean useInclusionPatterns) {
		
		IClasspathEntry entry = null;
		try {
			String name = library.getName();
			String expandedName = expandLibraryName(name);

			IPluginModelBase model = library.getPluginModel();
			IPath path = getPath(model, expandedName);
			if (path == null) {
				if (model.isFragmentModel() || !containsVariables(name))
					return null;
				model = resolveLibraryInFragments(library, expandedName);
				if (model == null)
					return null;
				path = getPath(model, expandedName);
			}
			
			if (ENABLE_RESTRICTIONS && useInclusionPatterns) {
				IPath[] inclusionPatterns = getInclusionPatterns(library);
				IAccessRule[] accessRules = getAccessRules(inclusionPatterns);
				entry = JavaCore.newLibraryEntry(
							path, 
							getSourceAnnotation(model, expandedName), 
							null, 
							accessRules,
							new IClasspathAttribute[0], 
							exported);
			} else {
				entry = JavaCore.newLibraryEntry(
						path, 
						getSourceAnnotation(model, expandedName),
						null, 
						exported);
			}
		} catch (CoreException e) {
		}
		return entry;
	}
	
	private static IPath[] getInclusionPatterns(IFragmentModel model) throws CoreException{
		IPath[] direct = getInclusions(model);
		IPlugin plugin = PDECore.getDefault().findPlugin(model.getFragment().getPluginId());
		if (plugin == null || !hasExtensibleAPI(plugin))
			return direct;
		
		IPath[] indirect = getInclusions((IPluginModelBase)plugin.getModel());		
		IPath[] all = new IPath[direct.length + indirect.length];
		System.arraycopy(direct, 0, all, 0, direct.length);
		System.arraycopy(indirect, 0, all, direct.length, indirect.length);
		return all;
	}
	
	public static boolean hasExtensibleAPI(IPlugin plugin) {
		if (plugin instanceof Plugin)
			return ((Plugin) plugin).hasExtensibleAPI();
		if (plugin instanceof BundlePlugin)
			return ((BundlePlugin) plugin).hasExtensibleAPI();
		return false;
	}
	
	private static IPath[] getInclusions(IPluginModelBase model) throws CoreException {
		ArrayList list = new ArrayList();
		if (isBundle(model)) {
			BundleDescription desc = model.getBundleDescription();
			if (desc != null) {
				ExportPackageDescription[] exports = desc.getExportPackages();
				for (int i = 0; i < exports.length; i++) {
					list.add(new Path(exports[i].getName().replaceAll("\\.", "/") + "/*")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
		} else {
			IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
			for (int i = 0; i < libraries.length; i++) {
				IPath[] paths = getInclusionPatterns(libraries[i]);
				for (int j = 0; j < paths.length; j++) {
					if (!list.contains(paths[j]))
						list.add(paths[j]);
				}
			}
		}
		return (IPath[])list.toArray(new IPath[list.size()]);
	}
	
	public static boolean isBundle(IPluginModelBase model) {
		if (model instanceof IBundlePluginModelBase)
			return true;
		if (model.getUnderlyingResource() == null) {
			File file = new File(model.getInstallLocation());
			if (file.isDirectory())
				return new File(file, "META-INF/MANIFEST.MF").exists();
			ZipFile jarFile = null;
			try {
				jarFile = new ZipFile(file, ZipFile.OPEN_READ);
				return jarFile.getEntry("META-INF/MANIFEST.MF") != null;
			} catch (IOException e) {
			} finally {
				try {
					if (jarFile != null)
						jarFile.close();
				} catch (IOException e) {
				}
			}
		}
		return false;
	}
	
	private static IAccessRule[] getAccessRules(IPath[] inclusionPatterns) {
		int length = inclusionPatterns.length;
		IAccessRule[] accessRules;
		if (length == 0) {
			accessRules = new IAccessRule[] {JavaCore.newAccessRule(new Path("**/*"), IAccessRule.K_NON_ACCESSIBLE)}; //$NON-NLS-1$
		} else {
			accessRules = new IAccessRule[length];
			for (int i = 0; i < length; i++) {
				accessRules[i] = JavaCore.newAccessRule(inclusionPatterns[i], IAccessRule.K_ACCESSIBLE);
			}
		}
		return accessRules;
	}
	
	private static IPath[] getInclusionPatterns(IPluginLibrary library) {		
		String[] exports = library.getContentFilters();
		ArrayList list = new ArrayList();
		for (int i = 0; i < exports.length; i++) {
			String export = exports[i].replaceAll("\\.", "/"); //$NON-NLS-1$ //$NON-NLS-2$
			if (!export.endsWith("/*")) //$NON-NLS-1$
				export = export + "/*"; //$NON-NLS-1$
			list.add(new Path(export)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return (IPath[])list.toArray(new IPath[list.size()]);
	}
	
	public static boolean containsVariables(String name) {
		return name.indexOf("$os$") != -1 //$NON-NLS-1$
			|| name.indexOf("$ws$") != -1 //$NON-NLS-1$
			|| name.indexOf("$nl$") != -1 //$NON-NLS-1$
			|| name.indexOf("$arch$") != -1; //$NON-NLS-1$
	}

	public static String expandLibraryName(String source) {
		if (source == null || source.length() == 0)
			return ""; //$NON-NLS-1$
		if (source.indexOf("$ws$") != -1) //$NON-NLS-1$
			source =
				source.replaceAll(
					"\\$ws\\$", //$NON-NLS-1$
					"ws" + IPath.SEPARATOR + TargetPlatform.getWS()); //$NON-NLS-1$
		if (source.indexOf("$os$") != -1) //$NON-NLS-1$
			source =
				source.replaceAll(
					"\\$os\\$", //$NON-NLS-1$
					"os" + IPath.SEPARATOR + TargetPlatform.getOS()); //$NON-NLS-1$
		if (source.indexOf("$nl$") != -1) //$NON-NLS-1$
			source =
				source.replaceAll(
						"\\$nl\\$", //$NON-NLS-1$
						"nl" + IPath.SEPARATOR + TargetPlatform.getNL()); //$NON-NLS-1$
		if (source.indexOf("$arch$") != -1) //$NON-NLS-1$
			source =
				source.replaceAll(
						"\\$arch\\$", //$NON-NLS-1$
						"arch" + IPath.SEPARATOR + TargetPlatform.getOSArch()); //$NON-NLS-1$
		return source;
	}

	public static IPath getSourceAnnotation(IPluginModelBase model, String libraryName)
		throws CoreException {
		IPath path = null;
		int dot = libraryName.lastIndexOf('.');
		if (dot != -1) {
			String zipName = libraryName.substring(0, dot) + "src.zip"; //$NON-NLS-1$
			path = getPath(model, zipName);
			if (path == null) {
				SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
				path = manager.findSourcePath(model.getPluginBase(), new Path(zipName));
			}
		}
		return path;
	}

	private static IPluginModelBase resolveLibraryInFragments(
		IPluginLibrary library,
		String libraryName) {
		IFragment[] fragments =
			PDECore.getDefault().findFragmentsFor(
				library.getPluginBase().getId(),
				library.getPluginBase().getVersion());

		for (int i = 0; i < fragments.length; i++) {
			IPath path = getPath(fragments[i].getPluginModel(), libraryName);
			if (path != null)
				return fragments[i].getPluginModel();
			
			// the following case is to account for cases when a plugin like org.eclipse.swt.win32 is checked out from
			// cvs (i.e. it has no library) and org.eclipse.swt is not in the workspace.
			// we have to find the external swt.win32 fragment to locate the $ws$/swt.jar.
			if (fragments[i].getModel().getUnderlyingResource() != null) {
				ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(fragments[i].getId());
				IPluginModelBase model = entry.getExternalModel();
				if (model != null && model instanceof IFragmentModel) {
					path = getPath(model, libraryName);
					if (path != null)
						return model;
				}
			}
		}
		return null;
	}

	private static IPath getPath(IPluginModelBase model, String libraryName) {
		IResource resource = model.getUnderlyingResource();
		if (resource != null) {
			IResource jarFile = resource.getProject().findMember(libraryName);
			if (jarFile != null)
				return jarFile.getFullPath();
		} else {
			File file = new File(model.getInstallLocation(), libraryName);
			if (file.exists())
				return new Path(file.getAbsolutePath());
		}
		return null;
	}
	
	protected static IBuild getBuild(IPluginModelBase model)
			throws CoreException {
		IBuildModel buildModel = model.getBuildModel();
		if (buildModel == null) {
			IProject project = model.getUnderlyingResource().getProject();
			IFile buildFile = project.getFile("build.properties"); //$NON-NLS-1$
			if (buildFile.exists()) {
				buildModel = new WorkspaceBuildModel(buildFile);
				buildModel.load();
			}
		}
		return (buildModel != null) ? buildModel.getBuild() : null;
	}

	protected static void addExtraClasspathEntries(IPluginModelBase model,
			Vector result) throws CoreException {
		IBuild build = ClasspathUtilCore.getBuild(model);
		IBuildEntry entry = (build == null) ? null : build
				.getEntry(IBuildEntry.JARS_EXTRA_CLASSPATH);
		if (entry == null)
			return;

		String[] tokens = entry.getTokens();
		for (int i = 0; i < tokens.length; i++) {
			String device = new Path(tokens[i]).getDevice();
			IPath path = null;
			if (device == null) {
				path = new Path(model.getUnderlyingResource().getProject()
						.getName());
				path = path.append(tokens[i]);
			} else if (device.equals("platform:")) { //$NON-NLS-1$
				path = new Path(tokens[i]);
				if (path.segmentCount() > 1 && path.segment(0).equals("plugin")) { //$NON-NLS-1$
					path = path.setDevice(null);
					path = path.removeFirstSegments(1);
				}
			}
			if (path != null) {
				IResource resource = PDECore.getWorkspace().getRoot()
						.findMember(path);
				if (resource != null && resource instanceof IFile) {
					IClasspathEntry newEntry = JavaCore.newLibraryEntry(
							resource.getFullPath(), null, null);
					IProject project = resource.getProject();
					if (project.hasNature(JavaCore.NATURE_ID)) {
						IJavaProject jProject = JavaCore.create(project);
						IClasspathEntry[] entries = jProject.getRawClasspath();
						for (int j = 0; j < entries.length; j++) {
							if (entries[j].getEntryKind() == IClasspathEntry.CPE_LIBRARY
									&& entries[j].getContentKind() == IPackageFragmentRoot.K_BINARY
									&& entries[j].getPath().equals(
											resource.getFullPath())) {
								newEntry = JavaCore.newLibraryEntry(entries[j]
										.getPath(), entries[j]
										.getSourceAttachmentPath(), entries[j]
										.getSourceAttachmentRootPath());
								break;
							}
						}
					}
					if (!result.contains(newEntry))
						result.add(newEntry);
				}
			}
		}
	}



}
