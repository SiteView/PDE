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
package org.eclipse.pde.internal.core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.builders.FeatureRebuilder;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class PDECore extends Plugin implements IEnvironmentVariables {
	public static final String PLUGIN_ID = "org.eclipse.pde.core"; //$NON-NLS-1$
	
	public static final String BINARY_PROJECT_VALUE = "binary"; //$NON-NLS-1$
	public static final String BINARY_REPOSITORY_PROVIDER =
		PLUGIN_ID + "." + "BinaryRepositoryProvider"; //$NON-NLS-1$ //$NON-NLS-2$

	public static final String CLASSPATH_CONTAINER_ID =PLUGIN_ID + ".requiredPlugins"; //$NON-NLS-1$
	public static final String JAVA_SEARCH_CONTAINER_ID = PLUGIN_ID + ".externalJavaSearch"; //$NON-NLS-1$

	public static final String ECLIPSE_HOME_VARIABLE = "ECLIPSE_HOME"; //$NON-NLS-1$
	public static final QualifiedName EXTERNAL_PROJECT_PROPERTY =
		new QualifiedName(PLUGIN_ID, "imported"); //$NON-NLS-1$
	public static final String EXTERNAL_PROJECT_VALUE = "external"; //$NON-NLS-1$

	// Shared instance
	private static PDECore inst;
	private static IPluginModelBase[] registryPlugins;

	private static boolean isDevLaunchMode = false;

	public static PDECore getDefault() {
		return inst;
	}
	public static String getPluginId() {
		return getDefault().getBundle().getSymbolicName();
	}
	
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	public static void log(IStatus status) {
		ResourcesPlugin.getPlugin().getLog().log(status);
	}

	public static void log(Throwable e) {
		if (e instanceof InvocationTargetException)
			e = ((InvocationTargetException) e).getTargetException();
		IStatus status = null;
		if (e instanceof CoreException)
			status = ((CoreException) e).getStatus();
		else if (e.getMessage() != null)
			status =
				new Status(
					IStatus.ERROR,
					getPluginId(),
					IStatus.OK,
					e.getMessage(),
					e);
		log(status);
	}

	public static void logErrorMessage(String message) {
		log(
			new Status(
				IStatus.ERROR,
				getPluginId(),
				IStatus.ERROR,
				message,
				null));
	}

	public static void logException(Throwable e) {
		logException(e, null);
	}

	public static void logException(Throwable e, String message) {
		if (e instanceof InvocationTargetException) {
			e = ((InvocationTargetException) e).getTargetException();
		}
		IStatus status = null;
		if (e instanceof CoreException)
			status = ((CoreException) e).getStatus();
		else {
			if (message == null)
				message = e.getMessage();
			if (message == null)
				message = e.toString();
			status =
				new Status(
					IStatus.ERROR,
					getPluginId(),
					IStatus.OK,
					message,
					e);
		}
		ResourcesPlugin.getPlugin().getLog().log(status);
	}
	
	public static boolean isDevLaunchMode() {
		String[] args = Platform.getApplicationArgs();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-pdelaunch")) //$NON-NLS-1$
				isDevLaunchMode = true;			
		}
		return isDevLaunchMode;
	}

	private PluginModelManager fModelManager;
	private ExternalModelManager fExternalModelManager;
	private WorkspaceModelManager fWorkspacePluginModelManager;
	private FeatureModelManager fFeatureModelManager;
	private TargetDefinitionManager fTargetProfileManager;

	// Schema registry
	private SchemaRegistry fSchemaRegistry;

	private SourceLocationManager fSourceLocationManager;
	private JavadocLocationManager fJavadocLocationManager;

	// Tracing options manager
	private TracingOptionsManager fTracingOptionsManager;
	private BundleContext fBundleContext;
	private ServiceTracker fTracker;
	private JavaElementChangeListener fJavaElementChangeListener;

	private FeatureRebuilder fFeatureRebuilder;

	public PDECore() {
		inst = this;
	}

	public URL getInstallURL() {
		try {
			return FileLocator.resolve(getDefault().getBundle().getEntry("/")); //$NON-NLS-1$
		} catch (IOException e) {
			return null;
		}
	}
	
	public IPluginExtensionPoint findExtensionPoint(String fullID) {
		if (fullID == null || fullID.length() == 0)
			return null;
		// separate plugin ID first
		int lastDot = fullID.lastIndexOf('.');
		if (lastDot == -1)
			return null;
		String pluginID = fullID.substring(0, lastDot);
		IPlugin plugin = findPlugin(pluginID);
		if (plugin == null)
			return PDEManager.findExtensionPoint(fullID);
		String pointID = fullID.substring(lastDot + 1);
		IPluginExtensionPoint[] points = plugin.getExtensionPoints();
		for (int i = 0; i < points.length; i++) {
			IPluginExtensionPoint point = points[i];
			if (point.getId().equals(pointID))
				return point;
		}
		IFragmentModel[] fragments = PDEManager.findFragmentsFor(plugin.getPluginModel());
		for (int i = 0; i < fragments.length; i++) {
			points = fragments[i].getPluginBase().getExtensionPoints();
			for (int j = 0; j < points.length; j++)
				if (points[j].getId().equals(pointID))
					return points[j];
		}
		return PDEManager.findExtensionPoint(fullID);
	}

	private IFeature findFeature(
		IFeatureModel[] models,
		String id,
		String version,
		int match) {

		for (int i = 0; i < models.length; i++) {
			IFeatureModel model = models[i];

			IFeature feature = model.getFeature();
			String pid = feature.getId();
			String pversion = feature.getVersion();
			if (VersionUtil.compare(id, version, pid, pversion, match))
				return feature;
		}
		return null;
	}

	/**
	 * Finds a feature with the given ID, any version
	 * @param id
	 * @return IFeature or null
	 */
	public IFeature findFeature(String id) {
		IFeatureModel[] models = getFeatureModelManager().findFeatureModels(id);
		if (models.length > 0)
			return models[0].getFeature();
		return null;
	}

	/**
	 * Finds a feature with the given ID and satisfying constraints
	 * of the version and the match.
	 * @param id
	 * @param version
	 * @param match
	 * @return IFeature or null
	 */
	public IFeature findFeature(String id, String version, int match) {
		IFeatureModel[] models = getFeatureModelManager().findFeatureModels(id);
		return findFeature(models, id, version, match);
	}

	public IPlugin findPlugin(String id) {
		return findPlugin(id, null, 0);
	}
	
	public IPluginModelBase findPluginInHost(String id) {
		if (registryPlugins == null) {
			URL[] pluginPaths = ConfiguratorUtils.getCurrentPlatformConfiguration().getPluginPath();
			PDEState state = new PDEState(pluginPaths, false, new NullProgressMonitor());
			registryPlugins = state.getTargetModels();
		}

		for (int i = 0; i < registryPlugins.length; i++) {
			if (registryPlugins[i].getPluginBase().getId().equals(id))
				return registryPlugins[i];
		}
		return null;
	}

	public IPlugin findPlugin(String id, String version, int match) {
		IPluginModel model = getModelManager().findPluginModel(id);
		return (model != null && model.isEnabled()) ? model.getPlugin() : null;
	}

	public ExternalModelManager getExternalModelManager() {
		initializeModels();
		return fExternalModelManager;
	}
	public PluginModelManager getModelManager() {
		initializeModels();
		return fModelManager;
	}
	
	public TargetDefinitionManager getTargetProfileManager() {
		if (fTargetProfileManager == null)
			fTargetProfileManager = new TargetDefinitionManager();
		return fTargetProfileManager;
	}
	
	public FeatureModelManager getFeatureModelManager() {
		initializeModels();
		return fFeatureModelManager;
	}
	
	public JavaElementChangeListener getJavaElementChangeListener() {
		return fJavaElementChangeListener;
	}
	
	public SchemaRegistry getSchemaRegistry() {
		if (fSchemaRegistry == null)
			fSchemaRegistry = new SchemaRegistry();
		return fSchemaRegistry;
	}

	public SourceLocationManager getSourceLocationManager() {
		if (fSourceLocationManager == null)
			fSourceLocationManager = new SourceLocationManager();
		return fSourceLocationManager;
	}
	
	public JavadocLocationManager getJavadocLocationManager() {
		if (fJavadocLocationManager == null)
			fJavadocLocationManager = new JavadocLocationManager();
		return fJavadocLocationManager;
	}

	public TracingOptionsManager getTracingOptionsManager() {
		if (fTracingOptionsManager == null)
			fTracingOptionsManager = new TracingOptionsManager();
		return fTracingOptionsManager;
	}
	public WorkspaceModelManager getWorkspaceModelManager() {
		initializeModels();
		return fWorkspacePluginModelManager;
	}
	
	public boolean areModelsInitialized() {
		return fModelManager != null;
	}

	private synchronized void initializeModels() {
		if (fModelManager != null && fExternalModelManager != null && fWorkspacePluginModelManager != null)
			return;
		fExternalModelManager = new ExternalModelManager();
		fWorkspacePluginModelManager = new WorkspacePluginModelManager();
		
		fModelManager = new PluginModelManager(fWorkspacePluginModelManager, fExternalModelManager);
		fFeatureModelManager = new FeatureModelManager(fWorkspacePluginModelManager);
		fFeatureRebuilder = new FeatureRebuilder();
		fFeatureRebuilder.start();
	}

	public void releasePlatform() {
		if (fTracker == null)
			return;
		fTracker.close();
		fTracker = null;
	}
	
	public PlatformAdmin acquirePlatform() {
		if (fTracker==null) {
			fTracker = new ServiceTracker(fBundleContext, PlatformAdmin.class.getName(), null);
			fTracker.open();
		}
		PlatformAdmin result = (PlatformAdmin)fTracker.getService();
		while (result == null) {
			try {
				fTracker.waitForService(1000);
				result = (PlatformAdmin) fTracker.getService();
			} catch (InterruptedException ie) {
			}
		}
		return result;
	}
	
	public void start(BundleContext context) throws Exception {
		super.start(context);
		fBundleContext = context;
		CompilerFlags.initializeDefaults();
		fJavaElementChangeListener = new JavaElementChangeListener();
		fJavaElementChangeListener.start();
	}

	public BundleContext getBundleContext() {
		return this.fBundleContext;
	}

	public void stop(BundleContext context) throws CoreException {
		PDECore.getDefault().savePluginPreferences();
		if (fJavaElementChangeListener != null) {
			fJavaElementChangeListener.shutdown();
			fJavaElementChangeListener = null;
		}
		if (fFeatureRebuilder != null) {
			fFeatureRebuilder.stop();
			fFeatureRebuilder = null;
		}
		if (fSchemaRegistry != null) {
			fSchemaRegistry.shutdown();
			fSchemaRegistry = null;
		}
		if (fModelManager != null) {
			fModelManager.shutdown();
			fModelManager = null;
		}
		if (fFeatureModelManager!=null) {
			fFeatureModelManager.shutdown();
			fFeatureModelManager = null;
		}
		if (fExternalModelManager!=null) {
			fExternalModelManager.shutdown();
			fExternalModelManager=null;
		}
		if (fWorkspacePluginModelManager!=null) {
			fWorkspacePluginModelManager.shutdown();
			fWorkspacePluginModelManager = null;
		}
		if (fTargetProfileManager!=null) {
			fTargetProfileManager.shutdown();
			fTargetProfileManager = null;
		}
	}
}
