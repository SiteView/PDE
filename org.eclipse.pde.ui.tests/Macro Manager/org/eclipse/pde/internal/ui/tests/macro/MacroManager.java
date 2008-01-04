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
package org.eclipse.pde.internal.ui.tests.macro;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MacroManager {
	public static final String IGNORE = "__macro_ignore__";
	public static final int IDLE = 0;

	public static final int RUNNING = 1;

	public static final int DONE = 2;

	private Macro currentMacro;

	private IIndexHandler indexHandler;

	class DisplayListener implements Listener {
		public void handleEvent(Event event) {
			onEvent(event);
		}
	}

	class JobListener extends JobChangeAdapter {
		private int state = IDLE;

		public void running(IJobChangeEvent event) {
			if (!event.getJob().isSystem() && state == IDLE)
				state = RUNNING;
		}

		public void done(IJobChangeEvent event) {
			if (!event.getJob().isSystem() && state == RUNNING)
				state = DONE;
		}

		public void reset() {
			state = IDLE;
		}

		public int getState() {
			return state;
		}
	}

	private DisplayListener listener;

	private JobListener jobListener;

	private Vector listeners;

	private ArrayList widgetResolvers;
	private SAXParser parser;

	public MacroManager() {
		listener = new DisplayListener();
		jobListener = new JobListener();
		listeners = new Vector();
	}

	public void addRecorderListener(IRecorderListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}

	public void addIndex(String indexId) {
		if (currentMacro != null) {
			currentMacro.addIndex(indexId);
		}
	}

	public void removeRecorderListener(IRecorderListener listener) {
		if (listeners.contains(listener))
			listeners.remove(listener);
	}

	public boolean isRecording() {
		return currentMacro != null;
	}

	public void startRecording() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		hookListeners(display);
		currentMacro = new Macro();
		currentMacro.initializeForRecording(display);
		IRecorderListener[] array = (IRecorderListener[]) listeners.toArray(new IRecorderListener[listeners.size()]);
		for (int i = 0; i < array.length; i++) {
			array[i].recordingStarted();
		}
	}

	public String[] getExistingIndices() {
		if (currentMacro != null) {
			return currentMacro.getExistingIndices();
		}
		return new String[0];
	}

	public Macro stopRecording() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		unhookListeners(display);
		currentMacro.stopRecording();
		Macro newMacro = currentMacro;
		currentMacro = null;
		IRecorderListener[] array = (IRecorderListener[]) listeners.toArray(new IRecorderListener[listeners.size()]);
		for (int i = 0; i < array.length; i++) {
			array[i].recordingStopped();
		}
		return newMacro;
	}

	public void hookListeners(Display display) {
		display.addFilter(SWT.KeyDown, listener);
		display.addFilter(SWT.Selection, listener);
		display.addFilter(SWT.DefaultSelection, listener);
		display.addFilter(SWT.Expand, listener);
		display.addFilter(SWT.Collapse, listener);
		display.addFilter(SWT.Modify, listener);
		display.addFilter(SWT.Activate, listener);
		display.addFilter(SWT.Close, listener);
		display.addFilter(SWT.FocusIn, listener);
		IJobManager jobManager = Platform.getJobManager();
		jobManager.addJobChangeListener(jobListener);
	}

	public void unhookListeners(Display display) {
		display.removeFilter(SWT.KeyDown, listener);
		display.removeFilter(SWT.Selection, listener);
		display.removeFilter(SWT.DefaultSelection, listener);
		display.removeFilter(SWT.Expand, listener);
		display.removeFilter(SWT.Collapse, listener);
		display.removeFilter(SWT.Modify, listener);
		display.removeFilter(SWT.Activate, listener);
		display.removeFilter(SWT.Close, listener);
		display.removeFilter(SWT.FocusIn, listener);
		IJobManager jobManager = Platform.getJobManager();
		jobManager.removeJobChangeListener(jobListener);
	}

	public void shutdown() {
		if (currentMacro != null) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			unhookListeners(display);
			currentMacro.stopRecording();
			currentMacro = null;
		}
	}

	/**
	 * Plays a provided macro stream. The method will close the input stream
	 * upon parsing.
	 * 
	 * @param is
	 * @throws CoreException
	 */
	public boolean play(final Display display, IRunnableContext context, String scriptName, InputStream is) throws CoreException {
		XMLDefaultHandler handler = createMacroDocument(is);
		Node root = handler.getDocumentElement();
		NodeList children = root.getChildNodes();

		final Macro macro = new Macro(scriptName);
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeName().equals("shell")) {
				macro.addShell(child, handler.getLineTable());
			}
		}
		// discard the DOM
		handler = null;

		macro.setIndexHandler(getIndexHandler());

		final boolean[] result = new boolean[1];

		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					//System.out.println("Start macro: "+macro.getName());
					result[0] = macro.playback(display, null, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} catch (ClassCastException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
					//System.out.println("Stop macro: "+macro.getName());
				}
			}
		};
		try {
			context.run(true, true, op);
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			MacroPlugin.logException(e);
			return false;
		}
		return result[0];
	}

	private XMLDefaultHandler createMacroDocument(InputStream is) throws CoreException {
		XMLDefaultHandler handler = null;
		try {
			SAXParser parser = getParser();
			handler = new XMLDefaultHandler();
			parser.parse(is, handler);
		} catch (SAXException e) {
			MacroUtil.throwCoreException("Error parsing the macro file", 0, e);
		} catch (IOException e) {
			MacroUtil.throwCoreException("Error parsing the macro file", 0, e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
			}
		}
		return handler;
	}

	private SAXParser getParser() throws CoreException {
		if (parser == null) {
			try {
				return SAXParserFactory.newInstance().newSAXParser();
			} catch (ParserConfigurationException e) {
				MacroUtil.throwCoreException("Error parsing the macro file", 0, e);
			} catch (SAXException e) {
				MacroUtil.throwCoreException("Error parsing the macro file", 0, e);
			}
		}
		return parser;
	}

	private void onEvent(Event event) {
		try {
			if (event.type == SWT.KeyDown) {
				if ((event.stateMask & SWT.SHIFT) != 0 && (event.stateMask & SWT.CTRL) != 0) {
					int key = event.keyCode & SWT.KEY_MASK;
					if (key == SWT.F11)
						notifyInterrupt(IRecorderListener.STOP);
					else if (key == SWT.F10)
						notifyInterrupt(IRecorderListener.INDEX);
				}
				return;
			}
			if ((event.type == SWT.Close || event.type == SWT.Activate) && !(event.widget instanceof Shell))
				return;
			if (jobListener.getState() == RUNNING || jobListener.getState() == DONE)
				currentMacro.addPause();
			jobListener.reset();
			boolean stop = currentMacro.addEvent(event);
			if (stop) {
				notifyInterrupt(IRecorderListener.STOP);
			}
		} catch (Exception e) {
			MacroPlugin.logException(e);
			stopRecording();
		}
	}

	private void notifyInterrupt(int type) {
		IRecorderListener[] array = (IRecorderListener[]) listeners.toArray(new IRecorderListener[listeners.size()]);
		for (int i = 0; i < array.length; i++) {
			array[i].recordingInterrupted(type);
		}
	}

	public String resolveWidget(Widget widget) {
		if (widgetResolvers == null)
			loadWidgetResolvers();
		for (int i = 0; i < widgetResolvers.size(); i++) {
			IWidgetResolver resolver = (IWidgetResolver) widgetResolvers.get(i);
			String id = resolver.getUniqueId(widget);
			if (id != null)
				return id;
		}
		return null;
	}

	private void loadWidgetResolvers() {
		widgetResolvers = new ArrayList();
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.pde.ui.tests.macroSupport");
		for (int i = 0; i < elements.length; i++) {
			if (elements[i].getName().equals("widgetResolver")) {
				try {
					Object obj = elements[i].createExecutableExtension("class");
					if (obj instanceof IWidgetResolver)
						widgetResolvers.add(obj);
				} catch (CoreException e) {
					System.out.println(e);
				}
			}
		}
	}

	public IIndexHandler getIndexHandler() {
		return indexHandler;
	}

	public void setIndexHandler(IIndexHandler indexHandler) {
		this.indexHandler = indexHandler;
	}

}