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

import org.osgi.framework.Constants;

public interface ICoreConstants {
	
	// Target Platform
	String PLATFORM_PATH = "platform_path"; //$NON-NLS-1$
	String SAVED_PLATFORM = "saved_platform"; //$NON-NLS-1$
	String TARGET_MODE = "target_mode"; //$NON-NLS-1$
	String VALUE_USE_THIS = "useThis"; //$NON-NLS-1$
	String VALUE_USE_OTHER = "useOther"; //$NON-NLS-1$
	String CHECKED_PLUGINS = "checkedPlugins"; //$NON-NLS-1$
	String VALUE_SAVED_NONE = "[savedNone]"; //$NON-NLS-1$
	String VALUE_SAVED_ALL = "[savedAll]"; //$NON-NLS-1$
	String VALUE_SAVED_SOME = "savedSome"; //$NON-NLS-1$
	String P_SOURCE_LOCATIONS = "source_locations"; //$NON-NLS-1$
	String P_EXT_LOCATIONS = "ext_locations"; //$NON-NLS-1$
	String PROGRAM_ARGS = "program_args"; //$NON-NLS-1$
	String VM_ARGS = "vm_args"; //$NON-NLS-1$
	String IMPLICIT_DEPENDENCIES = "implicit_dependencies"; //$NON-NLS-1$
	String GROUP_PLUGINS_VIEW = "group_plugins"; //$NON-NLS-1$
	String ADDITIONAL_LOCATIONS = "additional_locations"; //$NON-NLS-1$
	
	// Target JRE
	String TARGET_JRE = "targetJRE"; //$NON-NLS-1$
	
	public final static String TARGET30 = "3.0"; //$NON-NLS-1$
	public final static String TARGET31 = "3.1"; //$NON-NLS-1$
	public final static String TARGET32 = "3.2"; //$NON-NLS-1$
	
	public final static String EQUINOX = "Equinox"; //$NON-NLS-1$

	// project preferences
	public static final String SELFHOSTING_BIN_EXCLUDES = "selfhosting.binExcludes"; //$NON-NLS-1$
	public static final String EQUINOX_PROPERTY = "pluginProject.equinox"; //$NON-NLS-1$
	public static final String EXTENSIONS_PROPERTY = "pluginProject.extensions"; //$NON-NLS-1$
	public static final String RESOLVE_WITH_REQUIRE_BUNDLE = "resolve.requirebundle"; //$NON-NLS-1$
	public static final String TARGET_PROFILE = "target.profile"; //$NON-NLS-1$
	
	// for backwards compatibility with Eclipse 3.0 bundle manifest files
	public final static String PROVIDE_PACKAGE = "Provide-Package"; //$NON-NLS-1$
	public final static String REPROVIDE_ATTRIBUTE = "reprovide"; //$NON-NLS-1$
	public final static String OPTIONAL_ATTRIBUTE = "optional"; //$NON-NLS-1$
	public final static String REQUIRE_PACKAGES_ATTRIBUTE = "require-packages"; //$NON-NLS-1$
	public final static String SINGLETON_ATTRIBUTE = "singleton"; //$NON-NLS-1$
	public final static String PACKAGE_SPECIFICATION_VERSION = "specification-version"; //$NON-NLS-1$
	public final static String EXTENSIBLE_API = "Eclipse-ExtensibleAPI"; //$NON-NLS-1$
	public final static String PATCH_FRAGMENT = "Eclipse-PatchFragment"; //$NON-NLS-1$
	public final static String PLUGIN_CLASS = "Plugin-Class"; //$NON-NLS-1$
	public final static String ECLIPSE_AUTOSTART = "Eclipse-AutoStart"; //$NON-NLS-1$
	public final static String ECLIPSE_LAZYSTART = "Eclipse-LazyStart"; //$NON-NLS-1$
	public final static String ECLIPSE_JREBUNDLE = "Eclipse-JREBundle"; //$NON-NLS-1$

	public static final String INTERNAL_DIRECTIVE = "x-internal"; //$NON-NLS-1$
	public static final String FRIENDS_DIRECTIVE = "x-friends"; //$NON-NLS-1$
	public static final String PLATFORM_FILTER = "Eclipse-PlatformFilter"; //$NON-NLS-1$
	
	public static final String[] TRANSLATABLE_HEADERS = new String[] {
		Constants.BUNDLE_VENDOR, Constants.BUNDLE_NAME,
		Constants.BUNDLE_DESCRIPTION, Constants.BUNDLE_COPYRIGHT
	};
	
}
