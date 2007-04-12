/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ArchiveSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaClassType;
import org.eclipse.jdt.debug.core.IJavaFieldVariable;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDEClasspathContainer;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class PDESourceLookupQuery implements ISafeRunnable {
	
	protected static String OSGI_CLASSLOADER = "org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader"; //$NON-NLS-1$
	private static String LEGACY_ECLIPSE_CLASSLOADER = "org.eclipse.core.runtime.adaptor.EclipseClassLoader"; //$NON-NLS-1$
	private static String MAIN_CLASS = "org.eclipse.core.launcher.Main"; //$NON-NLS-1$
	private static String MAIN_PLUGIN = "org.eclipse.platform"; //$NON-NLS-1$
	
	private Object fElement;
	private Object fResult;
	
	public PDESourceLookupQuery(Object object) {
		fElement = object;		
	}

	public void handleException(Throwable exception) {
	}

	public void run() throws Exception {
		IJavaObject classLoaderObject = null;
		String declaringTypeName = null;
		String sourcePath = null;
		if (fElement instanceof IJavaStackFrame) {
			IJavaStackFrame stackFrame = (IJavaStackFrame)fElement;
			classLoaderObject = stackFrame.getReferenceType().getClassLoaderObject();
			declaringTypeName = stackFrame.getDeclaringTypeName();
			sourcePath = generateSourceName(declaringTypeName);
		} else if (fElement instanceof IJavaObject){
			IJavaObject object = (IJavaObject)fElement;
			IJavaReferenceType type = (IJavaReferenceType)object.getJavaType();
			classLoaderObject = type.getClassLoaderObject();
			if (object.getJavaType() != null){
				declaringTypeName = object.getJavaType().getName();
			}
			if (declaringTypeName != null){
				sourcePath = generateSourceName(declaringTypeName);
			}	
		} else if (fElement instanceof IJavaReferenceType){
			IJavaReferenceType type = (IJavaReferenceType)fElement;
			classLoaderObject = type.getClassLoaderObject();
			declaringTypeName = type.getName();
			sourcePath = generateSourceName(declaringTypeName);
		}
			
		if (classLoaderObject != null) {
			IJavaClassType type = (IJavaClassType)classLoaderObject.getJavaType();
			if (OSGI_CLASSLOADER.equals(type.getName())) {
				fResult = findSourceElement(classLoaderObject, sourcePath);
			} else if (LEGACY_ECLIPSE_CLASSLOADER.equals(type.getName())) {
				fResult = findSourceElement_legacy(classLoaderObject, sourcePath);		
			} else if (MAIN_CLASS.equals(declaringTypeName)){
				IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(MAIN_PLUGIN);
				if (model != null)
					fResult = getSourceElement(model.getInstallLocation(), MAIN_PLUGIN, sourcePath);
			}
		}
	}
	
	protected Object getResult() {
		return fResult;
	}
	
	private String getValue(IJavaObject object, String variable) throws DebugException {
		IJavaFieldVariable var = object.getField(variable, false);
		return var == null ? null : var.getValue().getValueString();
	}
	
	protected Object findSourceElement(IJavaObject object, String typeName) throws CoreException {
		IJavaObject manager = getObject(object, "manager", false); //$NON-NLS-1$
		if (manager != null) {
			IJavaObject data = getObject(manager, "data", false); //$NON-NLS-1$
			if (data != null) {
				String location = getValue(data, "fileName"); //$NON-NLS-1$
				String id = getValue(data, "symbolicName"); //$NON-NLS-1$
				return getSourceElement(location, id, typeName);			
			}
		}	
		return null;
	}
	
	private IJavaObject getObject(IJavaObject object, String field, boolean superfield) throws DebugException {
		IJavaFieldVariable variable = object.getField(field, superfield);
		if (variable != null) {
			IValue value = variable.getValue();
			if (value instanceof IJavaObject) 
				return (IJavaObject)value;
		}
		return null;
	}
	
	private Object findSourceElement_legacy(IJavaObject object, String typeName) throws CoreException {
		IJavaObject hostdata = getObject(object, "hostdata", true); //$NON-NLS-1$
		if (hostdata != null) {
			String location = getValue(hostdata, "fileName"); //$NON-NLS-1$
			String id = getValue(hostdata, "symbolicName"); //$NON-NLS-1$
			return getSourceElement(location, id, typeName);
		}
		return null;
	}
	
	private Object getSourceElement(String location, String id, String typeName) throws CoreException {
		if (location != null && id != null) {
			Object result = findSourceElement(getSourceContainers(location, id), typeName);
			if (result != null)
				return result;
			
			// don't give up yet, search fragments attached to this host
			State state = TargetPlatformHelper.getState();
			BundleDescription desc = state.getBundle(id, null);
			if (desc != null) {
				BundleDescription[] fragments = desc.getFragments();
				for (int i = 0; i < fragments.length; i++) {
					location = fragments[i].getLocation();
					id = fragments[i].getSymbolicName();
					result = findSourceElement(getSourceContainers(location, id), typeName);
					if (result != null)
						return result;
				}
			}
		}
		return null;
	}
	
	private Object findSourceElement(ISourceContainer[] containers, String typeName) throws CoreException {
		for (int i = 0; i < containers.length; i++) {
			Object[] result = containers[i].findSourceElements(typeName);
			if (result.length > 0)
				return result[0];
		}
		return null;
	}
	
	protected ISourceContainer[] getSourceContainers(String location, String id) throws CoreException {
		ArrayList result = new ArrayList();		
		ModelEntry entry = PluginRegistry.findEntry(id);
		
		boolean match = false;

		IPluginModelBase[] models = entry.getWorkspaceModels();		
		for (int i = 0; i < models.length; i++) {
			if (isPerfectMatch(models[i], new Path(location))) {
				IResource resource = models[i].getUnderlyingResource();
				// if the plug-in matches a workspace model,
				// add the project and any libraries not coming via a container
				// to the list of source containers, in that order
				if (resource != null) {
					addProjectSourceContainers(resource.getProject(), result);
				}
				match = true;
				break;
			}
		}

		if (!match) {
			File file = new File(location);
			if (file.isFile()) {
				// in case of linked plug-in projects that map to an external JARd plug-in,
				// use source container that maps to the library in the linked project.
				ISourceContainer container = getArchiveSourceContainer(location);
				if (container != null)
					return new ISourceContainer[] {container};				
			} 
			
			models = entry.getExternalModels();
			for (int i = 0; i < models.length; i++) {
				if (isPerfectMatch(models[i], new Path(location))) {
					// try all source zips found in the source code locations
					IClasspathEntry[] entries = PDEClasspathContainer.getExternalEntries(models[i]);
					for (int j = 0; j < entries.length; j++) {
						IRuntimeClasspathEntry rte = convertClasspathEntry(entries[j]);
						if (rte != null)
							result.add(rte);
					}
					break;
				}
			}
		}
		
		IRuntimeClasspathEntry[] entries = (IRuntimeClasspathEntry[])
			 				result.toArray(new IRuntimeClasspathEntry[result.size()]);
		return JavaRuntime.getSourceContainers(entries);
	}
	
	private void addProjectSourceContainers(IProject project, ArrayList result) throws CoreException {
		if (project == null || !project.hasNature(JavaCore.NATURE_ID))
			return;
		
		IJavaProject jProject = JavaCore.create(project);
		result.add(JavaRuntime.newProjectRuntimeClasspathEntry(jProject));
		
		IClasspathEntry[] entries = jProject.getRawClasspath();
		for (int i = 0; i < entries.length; i++) {
			IClasspathEntry entry = entries[i];
			if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
				IRuntimeClasspathEntry rte = convertClasspathEntry(entry);
				if (rte != null)
					result.add(rte);
			}
		}	
	}
	
	private IRuntimeClasspathEntry convertClasspathEntry(IClasspathEntry entry) {
		if (entry == null)
			return null;
		
		IPath srcPath = entry.getSourceAttachmentPath();
		if (srcPath != null && srcPath.segmentCount() > 0) {
			IRuntimeClasspathEntry rte = 
				JavaRuntime.newArchiveRuntimeClasspathEntry(entry.getPath());
			rte.setSourceAttachmentPath(srcPath);
			rte.setSourceAttachmentRootPath(entry.getSourceAttachmentRootPath());
			return rte;
		}
		return null;
	}
	
	private boolean isPerfectMatch(IPluginModelBase model, IPath path) {
		return model == null ? false : path.equals(new Path(model.getInstallLocation()));
	}
	
	private ISourceContainer getArchiveSourceContainer(String location) throws JavaModelException {
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		IFile[] containers = root.findFilesForLocation(new Path(location));
		for (int i = 0; i < containers.length; i++) {
			IJavaElement element = JavaCore.create(containers[i]);
			if (element instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot archive = (IPackageFragmentRoot)element;
				IPath path = archive.getSourceAttachmentPath();
				if (path == null || path.segmentCount() == 0)
					continue;
				
				IPath rootPath = archive.getSourceAttachmentRootPath();
				boolean detectRootPath = rootPath != null && rootPath.segmentCount() > 0;
				
				IFile archiveFile = root.getFile(path);
				if (archiveFile.exists()) 
					return new ArchiveSourceContainer(archiveFile, detectRootPath);
				
				File file = path.toFile();
				if (file.exists()) 
					return new ExternalArchiveSourceContainer(file.getAbsolutePath(), detectRootPath);		
			}
		}
		return null;
	}
	/**
	 * Generates and returns a source file path based on a qualified type name.
	 * For example, when <code>java.lang.String</code> is provided,
	 * the returned source name is <code>java/lang/String.java</code>.
	 * 
	 * @param qualifiedTypeName fully qualified type name that may contain inner types
	 *  denoted with <code>$</code> character
	 * @return a source file path corresponding to the type name
	 */
	private static String generateSourceName(String qualifiedTypeName) {
		int index = qualifiedTypeName.indexOf('$');
		if (index >= 0) 
			qualifiedTypeName = qualifiedTypeName.substring(0, index);	
		return qualifiedTypeName.replace('.', File.separatorChar) + ".java"; //$NON-NLS-1$
	}      

}