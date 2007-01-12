/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.DependencyManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.tools.IOrganizeManifestsSettings;
import org.eclipse.pde.internal.ui.wizards.tools.OrganizeManifestsOperation;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;

public class LauncherUtils {

	private static final String TIMESTAMP = "timestamp"; //$NON-NLS-1$
	private static final String FILE_NAME = "dep-timestamp.properties"; //$NON-NLS-1$
	private static Properties fLastRun;

	public static Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}
	
	public final static Shell getActiveShell() {
		IWorkbenchWindow window = 
			PDEPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			IWorkbenchWindow[] windows = PDEPlugin.getDefault().getWorkbench().getWorkbenchWindows();
			if (windows.length > 0)
				return windows[0].getShell();
		} else
			return window.getShell();
		return getDisplay().getActiveShell();
	}

	public static boolean clearWorkspace(ILaunchConfiguration configuration,
			String workspace, IProgressMonitor monitor) throws CoreException {
		// If the workspace is not defined, there is no workspace to clear
		// What will happen is that the workspace chooser dialog will be 
		// brought up because no -data parameter will be specified on the 
		// launch
		if (workspace.length() == 0) {
			monitor.done();
			return true;		
		}
		
		File workspaceFile = new Path(workspace).toFile().getAbsoluteFile();
		if (configuration.getAttribute(IPDELauncherConstants.DOCLEAR, false)
				&& workspaceFile.exists()) {
			boolean doClear = !configuration.getAttribute(IPDELauncherConstants.ASKCLEAR,
					true);
			if (!doClear) {
				int result = confirmDeleteWorkspace(workspaceFile);
				if (result == 2 || result == -1) {
					monitor.done();
					return false;
				}
				doClear = result == 0;
			}
			if (doClear) {
				CoreUtility.deleteContent(workspaceFile);
			}
		}
		monitor.done();
		return true;
	}

	private static int confirmDeleteWorkspace(final File workspaceFile) {
		String message = NLS.bind(
				PDEUIMessages.WorkbenchLauncherConfigurationDelegate_confirmDeleteWorkspace,
				workspaceFile.getPath());
		return generateDialog(message);
	}
	
	public static boolean generateConfigIni() {
		String message = PDEUIMessages.LauncherUtils_generateConfigIni;
		return generateDialog(message) == 0;
	}
	
	private static int generateDialog(final String message) {
		final int[] result = new int[1];
		getDisplay().syncExec(new Runnable() {
			public void run() {
				String title = PDEUIMessages.LauncherUtils_title;
				MessageDialog dialog = new MessageDialog(getActiveShell(),
						title, null, message, MessageDialog.QUESTION, new String[] {
								IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL,
								IDialogConstants.CANCEL_LABEL }, 0);
				result[0] = dialog.open();
			}
		});
		return result[0];
	}

	public static void validateProjectDependencies(ILaunchConfiguration launch, final IProgressMonitor monitor) {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		if (!store.getBoolean(IPreferenceConstants.PROP_AUTO_MANAGE))
			return;
		
		String timeStamp, selected, deSelected;
		boolean useDefault, autoAdd;
		try {
			timeStamp = launch.getAttribute(TIMESTAMP, "0"); //$NON-NLS-1$
			selected = launch.getAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS, ""); //$NON-NLS-1$
			deSelected = launch.getAttribute(IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS, ""); //$NON-NLS-1$
			autoAdd = launch.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			useDefault = launch.getAttribute(IPDELauncherConstants.USE_DEFAULT, true);
			useDefault |= launch.getAttribute(IPDELauncherConstants.USEFEATURES, false);
			final ArrayList projects = new ArrayList();
			if (useDefault) 
				handleUseDefault(timeStamp, projects);
			else if (autoAdd) 
				handleDeselectedPlugins(timeStamp, deSelected, projects);
			else 
				handleSelectedPlugins(timeStamp, selected, projects); 

			if (!projects.isEmpty())
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						OrganizeManifestsOperation op = new OrganizeManifestsOperation(projects);
						op.setOperations(getSettings());
						try {
							op.run(monitor);
							// update table for each project with current time stamp
							Properties table = getLastRun();
							String ts = Long.toString(System.currentTimeMillis());
							Iterator it = projects.iterator();
							while (it.hasNext()) 
								table.put(((IProject)it.next()).getName(), ts);
						} catch (InvocationTargetException e) {
						} catch (InterruptedException e) {
						} 
					}
				});

			ILaunchConfigurationWorkingCopy wc = null;
			if (launch.isWorkingCopy())
				wc = (ILaunchConfigurationWorkingCopy)launch;
			else
				wc = launch.getWorkingCopy();
			wc.setAttribute(TIMESTAMP, Long.toString(System.currentTimeMillis()));
			wc.doSave();
		} catch (CoreException e) {
		}
	}
	
	private static IDialogSettings getSettings() {
		IDialogSettings settings = new DialogSettings("");  //$NON-NLS-1$
		settings.put(IOrganizeManifestsSettings.PROP_ADD_MISSING, true);
		settings.put(IOrganizeManifestsSettings.PROP_MARK_INTERNAL, true);
		settings.put(IOrganizeManifestsSettings.PROP_REMOVE_UNRESOLVED_EX, true);
		settings.put(IOrganizeManifestsSettings.PROP_MODIFY_DEP, true);
		settings.put(IOrganizeManifestsSettings.PROP_RESOLVE_IMP_MARK_OPT, true);
		settings.put(IOrganizeManifestsSettings.PROP_REMOVE_LAZY, true);
		settings.put(IOrganizeManifestsSettings.PROP_ADD_DEPENDENCIES, true);
		return settings;
	}
	
	private static String getTimeStamp(IProject project) {
		IJavaProject jp = JavaCore.create(project);
		try {
			long timeStamp = 0;
			IClasspathEntry[] entries = jp.getResolvedClasspath(true);
			for (int i = 0; i < entries.length; i++) {
				if (entries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					File file;
					IPath location = entries[i].getOutputLocation();
					if (location == null) 
						location = jp.getOutputLocation();
					IResource res = project.getWorkspace().getRoot().findMember(location);
					IPath path = res == null ? null : res.getLocation();
					if (path == null)
						continue;
					file = path.toFile();
					Stack files = new Stack();
					files.push(file);
					while(!files.isEmpty()) {
						file = (File) files.pop();
						if (file.isDirectory()) {
							File[] children = file.listFiles();
							for (int j =0 ; j < children.length; j++) 
								files.push(children[j]);
						} else 
							if (file.getName().endsWith(".class") && timeStamp < file.lastModified()) //$NON-NLS-1$
								timeStamp = file.lastModified();
					}
				}
			}
			String[] otherFiles = {"META-INF/MANIFEST.MF", "build.properties"}; //$NON-NLS-1$ //$NON-NLS-2$
			for (int i = 0; i < otherFiles.length; i++) {
				IResource file = project.getFile(otherFiles[i]);
				if (file != null) {
					long fileTimeStamp = file.getRawLocation().toFile().lastModified();
					if (timeStamp < fileTimeStamp)
						timeStamp = fileTimeStamp;
				}
			}
			return Long.toString(timeStamp);
		} catch (JavaModelException e) {
		}
		return "0"; //$NON-NLS-1$
	}
	
	private static void handleUseDefault(String launcherTimeStamp, ArrayList projects) {
		IProject[] projs = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projs.length; i++) {
			if (!WorkspaceModelManager.isPluginProject(projs[i])) 
				continue;
			String timestamp = getTimeStamp(projs[i]);
			if (timestamp.compareTo(launcherTimeStamp) > 0 && 
					shouldAdd(projs[i], launcherTimeStamp, timestamp)) 
				projects.add(projs[i]);
		}
	}
	
	private static void handleSelectedPlugins(String timeStamp, String value, ArrayList projects) {
		StringTokenizer tokenizer = new StringTokenizer(value, ","); //$NON-NLS-1$
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		while(tokenizer.hasMoreTokens()) {
			value = tokenizer.nextToken();
			int index = value.indexOf('@');
			String id = index > 0 ? value.substring(0, index) : value;
			IPluginModelBase base = manager.findModel(id);
			if (base != null) {
				IResource res = base.getUnderlyingResource();
				if (res != null) {
					IProject project = res.getProject();
					String projTimeStamp = getTimeStamp(project);
					if (projTimeStamp.compareTo(timeStamp) > 0 && 
							shouldAdd(project, timeStamp, projTimeStamp)) 
						projects.add(project);
				}
			}
		}
	}
	
	private static void handleDeselectedPlugins(String launcherTimeStamp, String value, ArrayList projects) {
		StringTokenizer tokenizer = new StringTokenizer(value, ","); //$NON-NLS-1$
		HashSet deSelectedProjs = new HashSet();
		while (tokenizer.hasMoreTokens()) 
			deSelectedProjs.add(tokenizer.nextToken());
		
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IProject[] projs = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projs.length; i++) {
			if (!WorkspaceModelManager.isPluginProject(projs[i])) 
				continue;
			IPluginModelBase base = manager.findModel(projs[i]);
			if (base == null || base != null && deSelectedProjs.contains(base.getPluginBase().getId()))
				continue;
			String timestamp = getTimeStamp(projs[i]);
			if (timestamp.compareTo(launcherTimeStamp) > 0 && 
					shouldAdd(projs[i], launcherTimeStamp, timestamp)) 
				projects.add(projs[i]);
		}
	}
	
	public static final void shutdown() {
		if (fLastRun == null)
			return;
		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(new File(getDirectory(), FILE_NAME));
			fLastRun.store(stream, "Cached timestamps"); //$NON-NLS-1$
			stream.flush();
			stream.close();
		} catch (IOException e) {
			PDECore.logException(e);
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e1) {
			}			
		}
	}
	
	private static File getDirectory() {
		IPath path = PDECore.getDefault().getStateLocation().append(".cache"); //$NON-NLS-1$
		File directory = new File(path.toOSString());
		if (!directory.exists() || !directory.isDirectory())
			directory.mkdirs();
		return directory;
	}
	
	private static Properties getLastRun() {
		if (fLastRun == null) {
			fLastRun = new Properties();
			FileInputStream fis = null;
			try { 
				File file = new File(getDirectory(), FILE_NAME);
				if (file.exists()) {
					fis = new FileInputStream(file);
					fLastRun.load(fis);
					fis.close();
				}
			} catch (IOException e) {
				PDECore.logException(e);
			} finally {
				try {
					if (fis != null)
						fis.close();
				} catch (IOException e1) {
				}			
			}
		}
		return fLastRun;
	}
	
	private static boolean shouldAdd(IProject proj, String launcherTS, String fileSystemTS) {
		String projTS = (String)getLastRun().get(proj.getName());
		if (projTS == null)
			return true;
		return ((projTS.compareTo(launcherTS) < 0) || (projTS.compareTo(fileSystemTS) < 0));
	}
	
	public static boolean requiresUI(ILaunchConfiguration configuration) {
		try {
			String projectID = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
			if (projectID.length() > 0) {
				IResource project = PDEPlugin.getWorkspace().getRoot().findMember(projectID);
				if (project instanceof IProject) {
					IPluginModelBase model = PDECore.getDefault().getModelManager().findModel((IProject)project);
					if (model != null) {							
						Set plugins = DependencyManager.getSelfAndDependencies(model);
						return plugins.contains("org.eclipse.swt"); //$NON-NLS-1$
					}
				}
			}
		} catch (CoreException e) {
		}
		return true;
	}


}
