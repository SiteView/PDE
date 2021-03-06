/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.tests.macro;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.CoolBarManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPluginContribution;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.WorkbenchPartReference;
import org.eclipse.ui.keys.SWTKeySupport;
import org.w3c.dom.Node;

public class MacroUtil {
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	/**
	 * Returns the path where counters of the given event are stored, or null if
	 * we are not keeping records of the given event.
	 * 
	 * @param event
	 * @return
	 */
	public static WidgetIdentifier getWidgetIdentifier(Widget widget) {
		if (widget instanceof MenuItem) {

			MenuItem menuItem = (MenuItem) widget;

			if (onMenubar(menuItem)) {
				return new WidgetIdentifier(new Path("menus"), new Path(getActionId(menuItem))); //$NON-NLS-1$
			}
			Control c = widget.getDisplay().getFocusControl();
			WidgetIdentifier ci = getControlIdentifier(c);
			if (ci == null)
				return null;
			return new WidgetIdentifier(new Path("popup").append(ci.getFullyQualifiedPath()), new Path(getActionId(menuItem))); //$NON-NLS-1$
		} else if (widget instanceof ToolItem) {
			ToolItem toolItem = (ToolItem) widget;

			if (onToolbar(toolItem))
				return new WidgetIdentifier(new Path("toolbar"), new Path(getActionId(toolItem))); //$NON-NLS-1$
			// local toolbar somewhere - locate the parent
			// first
			ToolBar toolBar = toolItem.getParent();
			WidgetIdentifier controlId = getControlIdentifier(toolBar);
			IPath localPath = controlId.getFullyQualifiedPath();
			return new WidgetIdentifier(new Path("local-toolbar").append(localPath), new Path(getActionId(toolItem)));
		} else if (widget instanceof Shell) {
			return new WidgetIdentifier(new Path("shell"), getShellId((Shell) widget));
		} else if (widget instanceof Control) {
			return getControlIdentifier((Control) widget);
		} else if (widget instanceof Menu) {
			return new WidgetIdentifier(new Path("menu"), new Path(getActionId((Menu) widget)));
		}
		return null;
	}

	public static IPath getShellId(Shell shell) {
		Object data = shell.getData();
		String id = "";
		if (data instanceof WizardDialog) {
			id = data.getClass().getName().toString();
		} else if (data instanceof Window) {
			id = data.getClass().getName().toString();
		}
		return new Path(id);
	}

	public static WidgetIdentifier getControlIdentifier(Control control) {
		Shell shell = control.getShell();
		Object data = shell.getData();
		if (data instanceof WizardDialog) {
			// in wizard
			WizardDialog wd = (WizardDialog) data;
			IWizardPage page = wd.getCurrentPage();
			if (page == null)
				return null;
			Control pageControl = page.getControl();
			String relativePath = computeRelativePath((Composite) pageControl, null, control);
			if (relativePath != null) {
				IPath path = new Path("wizard-page").append(page.getName());
				return new WidgetIdentifier(path, new Path(relativePath));
			}
			// check for wizard buttons
			if (control instanceof Button) {
				relativePath = computeRelativePath(shell, (Composite) pageControl, control);
				return new WidgetIdentifier(new Path("wizard"), new Path(relativePath));
			}
			return null;
		} else if (data instanceof IWorkbenchWindow) {
			IWorkbenchWindow window = (IWorkbenchWindow) data;
			IWorkbenchPage page = window.getActivePage();
			IWorkbenchPart part = page.getActivePart();
			IWorkbenchPartSite site = part.getSite();
			IPath path;
			if (part instanceof IViewPart)
				path = new Path("view").append(site.getId());
			else if (part instanceof IEditorPart) {
				String inputName = ((IEditorPart) part).getEditorInput().getName();
				path = new Path("editor").append(site.getId()).append(inputName);
			} else
				return null;
			Composite paneComposite = (Composite) getPartControl(part);
			// If the control we are looking for is a local tool bar,
			// go up one level
			if (part instanceof IViewPart && control instanceof ToolBar)
				paneComposite = paneComposite.getParent();
			String relativePath = computeRelativePath(paneComposite, null, control);
			if (relativePath != null) {
				return new WidgetIdentifier(path, new Path(relativePath));
			}
		} else {
			// unknown shell - fetch controls starting from the shell
			String relativePath = computeRelativePath(shell, null, control);
			return new WidgetIdentifier(new Path("shell"), new Path(relativePath));
		}
		return null;
	}

	private static String computeRelativePath(Composite parent, Composite skip, Control control) {
		int[] counter = new int[1];
		counter[0] = 0;
		boolean result = computeControlToken(parent, skip, control, counter);
		if (!result && skip == null)
			return null;
		int index = result ? counter[0] : 0;
		return getControlId(control, index);
	}

	private static String getControlId(Control control, int index) {
		MacroManager recorder = MacroPlugin.getDefault().getMacroManager();
		String controlId = recorder.resolveWidget(control);
		if (controlId == null)
			controlId = index + "";
		return control.getClass().getName() + "#" + controlId;
	}

	private static boolean computeControlToken(Composite parent, Composite skip, Control control, int[] counter) {
		Control[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control child = children[i];

			if (!child.isVisible())
				continue;

			if (child.getClass().equals(control.getClass())) {
				// same type - increment counter
				counter[0]++;
				if (control.equals(child)) {
					// bingo
					return true;
				}
			} else if (child instanceof Composite) {
				if (skip != null && child.equals(skip))
					continue;
				boolean status = computeControlToken((Composite) child, skip, control, counter);
				if (status)
					return true;
			}
		}
		return false;
	}

	public static boolean isInputControl(Control control) {
		return true;
	}

	/**
	 * @param menuItem
	 * @return
	 */
	private static boolean onMenubar(MenuItem menuItem) {
		Menu parent = menuItem.getParent();
		MenuItem parentItem = parent.getParentItem();

		if (parentItem != null) {
			return onMenubar(parentItem);
		}

		Shell theShell = parent.getShell();

		return parent == theShell.getMenuBar();
	}

	private static boolean onToolbar(ToolItem toolItem) {
		ToolBar toolBar = toolItem.getParent();
		Shell shell = toolBar.getShell();
		Object data = shell.getData();
		if (data instanceof ApplicationWindow) {
			ApplicationWindow window = (ApplicationWindow) data;
			ToolBarManager mng = window.getToolBarManager();
			if (mng != null) {
				if (mng.getControl() != null && mng.getControl() == toolBar)
					return true;
			}
			CoolBarManager cmng = window.getCoolBarManager();
			if (cmng != null) {
				CoolBar cbar = cmng.getControl();
				Composite parent = toolBar.getParent();
				while (parent != null) {
					if (parent == cbar)
						return true;
					parent = parent.getParent();
				}
			}
		}
		return false;
	}

	/**
	 * @param toolItem
	 * @return
	 */
	private static String getActionId(ToolItem toolItem) {
		Object data = toolItem.getData();
		if (data != null && (data instanceof IContributionItem)) {
			String result = getActionId((IContributionItem) data);
			if (!result.equals(EMPTY_STRING)) {
				return result;
			}
		}

		return "readablename/" + getDisplayName(toolItem); //$NON-NLS-1$
	}

	/**
	 * @param toolItem
	 * @return
	 */
	private static String getDisplayName(ToolItem toolItem) {
		String name = toolItem.getText();

		if (name != null && !name.equals(EMPTY_STRING)) {
			return name;
		}

		name = toolItem.getToolTipText();

		if (name != null) {
			return name;
		}

		return "unknown"; //$NON-NLS-1$
	}

	/**
	 * Returns an identifier for the given MenuItem, based on its user-readable
	 * strings
	 * 
	 * @param menuItem
	 * @return
	 */
	private static String getDisplayName(MenuItem menuItem) {

		if (menuItem.getParent() == null || menuItem.getParent().getParentItem() == null) {
			return removeChar(menuItem.getText(), '&');
		}

		return getDisplayName(menuItem.getParent()) + "/" //$NON-NLS-1$
				+ removeChar(menuItem.getText(), '&');
	}

	/**
	 * Returns an identifier for the given Menu, based on its user-readable
	 * strings
	 * 
	 * @param menu
	 * @return
	 */
	private static String getDisplayName(Menu menu) {

		MenuItem parentItem = menu.getParentItem();

		if (parentItem == null) {
			return EMPTY_STRING;
		}

		return getDisplayName(parentItem);
	}

	protected String getContribId(MenuItem menuItem) {
		Object data = menuItem.getData();
		if (data != null && (data instanceof IContributionItem)) {
			String result = ((IContributionItem) data).getId();

			if (result != null) {
				return result;
			}
		}

		return EMPTY_STRING;
	}

	/**
	 * @param menuItem
	 * @return
	 */
	private static String getActionId(MenuItem menuItem) {
		Object data = menuItem.getData();
		if (data != null && (data instanceof IContributionItem)) {
			String result = getActionId((IContributionItem) data);
			if (!result.equals(EMPTY_STRING)) {
				return result;
			}
		}

		// return EMPTY_STRING;

		return "readablename/" + getDisplayName(menuItem); //$NON-NLS-1$
	}

	private static String getActionId(Menu menu) {
		Object data = menu.getData();
		if (data != null && (data instanceof IContributionItem)) {
			String result = getActionId((IContributionItem) data);
			if (!result.equals(EMPTY_STRING)) {
				return result;
			}
		}

		// return EMPTY_STRING;

		return "readablename/" + getDisplayName(menu); //$NON-NLS-1$
	}

	private static String getActionId(IContributionItem contrib) {
		String id = null;

		if (contrib instanceof IPluginContribution) {
			id = ((IPluginContribution) contrib).getLocalId();
		}
		if (id == null)
			id = contrib.getId();

		if (id != null) {
			return "contribid/" + id; //$NON-NLS-1$
		}

		if (contrib instanceof ActionContributionItem) {
			ActionContributionItem actionItem = (ActionContributionItem) contrib;

			id = actionItem.getId();

			if (id != null) {
				return "actionid/" + id; //$NON-NLS-1$
			}

			IAction action = actionItem.getAction();

			id = action.getActionDefinitionId();

			if (id != null) {
				return "defid/" + id; //$NON-NLS-1$
			}

			return "actionclass/" + action.getClass().getName(); //$NON-NLS-1$
		}
		return "contribclass/" + contrib.getClass().getName(); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.instrumentation.IDataProvider#getDefaultValue(org.eclipse.core.runtime.IPath)
	 */
	public Object getDefaultValue(IPath node) throws CoreException {
		return null;
	}

	private MenuItem getMenuItem(Menu menu, IPath menuPath) {
		if (menuPath.isEmpty()) {
			return null;
		}

		String toFind = menuPath.segment(0);
		MenuItem[] items = menu.getItems();

		for (int idx = 0; idx < items.length; idx++) {
			MenuItem item = items[idx];

			String itemName = removeChar(item.getText(), '&');

			if (itemName.equals(toFind)) {
				return getMenuItem(item, menuPath.removeFirstSegments(1));
			}
		}

		return null;
	}

	private MenuItem getMenuItem(MenuItem menu, IPath menuPath) {
		if (menuPath.isEmpty()) {
			return menu;
		}

		Menu subMenu = menu.getMenu();
		if (subMenu == null) {
			return null;
		}

		return getMenuItem(subMenu, menuPath);

	}

	public static String removeChar(String input, char toRemove) {
		StringBuffer buf = new StringBuffer(input.length());

		int last = 0;
		for (int pos = input.indexOf(toRemove); pos != -1; pos = input.indexOf(toRemove, last)) {
			buf.append(input.substring(last, pos));
			last = pos + 1;
		}

		buf.append(input.substring(last, input.length()));

		return buf.toString();
	}

	public static String getAttribute(Node node, String name) {
		Node value = node.getAttributes().getNamedItem(name);
		if (value != null)
			return value.getNodeValue();
		return null;
	}

	public static String getNormalizedText(String source) {
		if (source == null)
			return "";
		//String result = source.replace('\t', ' ');
		String result = source;
		result = result.trim();
		return result;
	}

	public static String getWritableText(String input) {
		String result = input.trim();
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < result.length(); i++) {
			char c = result.charAt(i);
			switch (c) {
				case '<' :
					buf.append("&lt;"); //$NON-NLS-1$
					break;
				case '>' :
					buf.append("&gt;"); //$NON-NLS-1$
					break;
				case '&' :
					buf.append("&amp;"); //$NON-NLS-1$
					break;
				case '\'' :
					buf.append("&apos;"); //$NON-NLS-1$
					break;
				case '\"' :
					buf.append("&quot;"); //$NON-NLS-1$
					break;
				default :
					buf.append(c);
			}
		}
		return buf.toString();
	}

	public static CommandTarget locateCommandTarget(Composite parent, WidgetIdentifier wid, int line) throws CoreException {
		return locateCommandTarget(parent, wid, null, line);
	}

	public static CommandTarget locateCommandTarget(Composite parent, WidgetIdentifier wid, ArrayList parents, int line) throws CoreException {
		Shell shell = (Shell) parent;

		String firstToken = wid.contextPath.segment(0);
		IPath wpath = wid.widgetPath;
		Iterator iter = parents != null ? parents.iterator() : null;
		if (firstToken.equals("menus"))
			return locateMenuBarItem(shell, wpath, iter, line);
		if (firstToken.equals("popup"))
			return locatePopupMenuItem(shell, wid, iter, line);
		if (firstToken.equals("toolbar"))
			return locateToolItem(shell, wpath, line);
		if (firstToken.equals("local-toolbar"))
			return locateLocalToolItem(shell, wid, line);
		if (firstToken.equals("wizard"))
			return locateWizardControl(shell, wpath, line);
		if (firstToken.equals("shell"))
			return locateShellControl(shell, wpath, line);

		String id = wid.contextPath.segment(1);
		if (firstToken.equals("wizard-page"))
			return locateWizardPageControl(shell, id, wpath, line);
		if (firstToken.equals("view"))
			return locateViewControl(shell, id, wpath, line);
		if (firstToken.equals("editor")) {
			String inputName = wid.contextPath.segment(2);
			return locateEditorControl(shell, id, inputName, wpath, line);
		}
		return null;
	}

	private static CommandTarget locateMenuBarItem(Shell shell, IPath path, Iterator parents, int line) throws CoreException {
		MenuItem item = null;
		Object data = shell.getData();
		Menu menuBar = shell.getMenuBar();

		if (data instanceof ApplicationWindow && parents != null) {
			ApplicationWindow window = (ApplicationWindow) data;
			MenuManager manager = window.getMenuBarManager();
			item = locateMenuItem(manager, path.toString(), parents, line);
		} else {
			item = locateMenuItem(menuBar, path.toString(), line);
		}
		if (item != null)
			return new CommandTarget(item, menuBar);
		throwCoreException("Cannot locate menu item: " + path.toString(), line);
		return null;
	}

	private static MenuItem locateMenuItem(Menu menu, String id, int line) {
		MenuItem[] items = menu.getItems();

		for (int i = 0; i < items.length; i++) {
			MenuItem item = items[i];

			Menu submenu = item.getMenu();
			if (submenu != null) {
				MenuItem hit = locateMenuItem(submenu, id, line);
				if (hit != null)
					return hit;
			} else {
				String itemId = getActionId(item);
				if (itemId != null && id.equals(itemId))
					return item;
			}
		}
		return null;
	}

	private static MenuItem locateMenuItem(MenuManager mng, String id, Iterator parents, int line) {
		IContributionItem[] items = mng.getItems();

		String parentId = null;
		if (parents.hasNext())
			parentId = (String) parents.next();

		for (int i = 0; i < items.length; i++) {
			IContributionItem citem = items[i];

			if (citem instanceof MenuManager) {
				MenuManager submenu = (MenuManager) citem;
				String subId = submenu.getId();

				if (subId.equals(parentId)) {
					// show this menu to force dynamic items
					// to show
					Menu menu = submenu.getMenu();
					forceMenuOpen(null, menu);

					MenuItem hit = locateMenuItem(submenu, id, parents, line);
					forceMenuClosed(menu);
					if (hit != null)
						return hit;
				}
			} else {
				String itemId = getActionId(citem);
				if (itemId != null && id.equals(itemId)) {
					MenuItem hit = locateMenuItem(mng.getMenu(), id, line);
					if (hit != null)
						return hit;
				}
			}
		}
		return null;
	}

	private static void forceMenuOpen(Control c, Menu menu) {
		Event e = new Event();
		e.type = SWT.Show;
		e.widget = menu;
		/*
		if (c!=null) {
			Point midpoint = c.getSize();
			midpoint.x /= 2;
			midpoint.y /= 2;
			midpoint = c.toDisplay(midpoint);
			menu.setLocation(midpoint);
		}
		*/
		//menu.setVisible(true);
		menu.notifyListeners(e.type, e);
		processDisplayEvents(menu.getDisplay());
	}

	private static void forceMenuClosed(Menu menu) {
		Event e = new Event();
		e.type = SWT.Hide;
		//menu.setVisible(false);
		e.widget = menu;
		menu.notifyListeners(e.type, e);
		processDisplayEvents(menu.getDisplay());
	}

	public static void processDisplayEvents(Display display) {
		for (;;) {
			if (!display.readAndDispatch())
				break;
		}
	}

	private static CommandTarget locatePopupMenuItem(Shell shell, WidgetIdentifier wid, Iterator parents, int line) throws CoreException {
		IPath contextPath = wid.contextPath.removeFirstSegments(1);
		IPath wpath = new Path(contextPath.lastSegment());
		contextPath = contextPath.removeLastSegments(1);
		CommandTarget target = locateCommandTarget(shell, new WidgetIdentifier(contextPath, wpath), line);
		if (target != null) {
			Control control = (Control) target.getWidget();
			Menu popupMenu = control.getMenu();
			if (popupMenu != null) {
				forceMenuOpen(control, popupMenu);
				MenuItem menuItem = locateMenuItem(popupMenu, wid.getWidgetId(), line);
				forceMenuClosed(popupMenu);
				if (menuItem != null) {
					return new CommandTarget(menuItem, control);
				}
			}
		}
		throwCoreException("Cannot locate pop-up menu item: " + wid.getWidgetId(), line);
		return null;
	}

	private static CommandTarget locateToolItem(Shell shell, IPath path, int line) throws CoreException {
		Object data = shell.getData();
		CommandTarget target = null;
		if (data instanceof ApplicationWindow) {
			ApplicationWindow window = (ApplicationWindow) data;
			CoolBarManager coolMng = window.getCoolBarManager();
			if (coolMng != null) {
				target = locateToolItem(coolMng, path.toString(), line);
			}
			ToolBarManager toolMng = window.getToolBarManager();
			if (toolMng != null) {
				target = locateToolItem(toolMng, path.toString(), line);
			}
		}
		if (target == null)
			throwCoreException("Cannot locate pop-up menu item: " + path.toString(), line);
		return target;
	}

	private static CommandTarget locateToolItem(ICoolBarManager coolMng, String id, int line) {
		IContributionItem[] items = coolMng.getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i] instanceof ToolBarContributionItem) {
				ToolBarContributionItem item = (ToolBarContributionItem) items[i];
				IToolBarManager toolMng = item.getToolBarManager();
				CommandTarget target = locateToolItem((ToolBarManager) toolMng, id, line);
				if (target != null)
					return target;
			}
		}
		return null;
	}

	private static CommandTarget locateToolItem(ToolBarManager toolMng, String id, int line) {
		return locateToolItem(toolMng.getControl(), id, line);
	}

	private static CommandTarget locateToolItem(ToolBar toolBar, String id, int line) {
		ToolItem[] items = toolBar.getItems();
		for (int i = 0; i < items.length; i++) {
			ToolItem item = items[i];
			String itemId = getActionId(item);
			if (itemId != null && itemId.equals(id))
				return new CommandTarget(item, toolBar);
		}
		return null;
	}

	private static CommandTarget locateLocalToolItem(Shell shell, WidgetIdentifier wid, int line) throws CoreException {
		IPath wpath = wid.contextPath.removeFirstSegments(1);
		String firstToken = wpath.segment(0);

		if (firstToken.equals("view")) {
			String id = wpath.segment(1);
			IViewPart view = locateView(shell, id, line);
			if (view != null) {
				Control control = getPartControl(view);
				processDisplayEvents(shell.getDisplay());
				Composite parent = control.getParent();
				Control c = locateVisibleChild(parent, null, wpath.removeFirstSegments(2));
				if (c != null) {
					//TODO bad cast
					ToolBarManager mng = (ToolBarManager) view.getViewSite().getActionBars().getToolBarManager();
					CommandTarget target = locateToolItem(mng, wid.getWidgetId(), line);
					if (target != null)
						return target;
				}
			}
		}
		throwCoreException("Cannot locate local tool bar item: " + wid.getFullyQualifiedId().toString(), line);
		return null;
	}

	private static WizardCommandTarget locateWizardControl(Shell shell, IPath wpath, int line) throws CoreException {
		WizardDialog wdialog = (WizardDialog) shell.getData();
		IWizardPage page = wdialog.getCurrentPage();
		Composite pparent = (Composite) page.getControl();
		Control control = locateVisibleChild(shell, pparent, wpath);
		if (control == null)
			throwCoreException("Cannot locate wizard control: " + wpath.toString(), line);
		if (control.isDisposed())
			throwCoreException("Wizard control is disposed: " + wpath.toString(), line);
		return new WizardCommandTarget(control, wdialog);
	}

	private static WindowCommandTarget locateShellControl(Shell shell, IPath wpath, int line) throws CoreException {
		Window window = (Window) shell.getData();
		Control control = locateVisibleChild(shell, null, wpath);
		if (control == null)
			throwCoreException("Cannot locate shell control: " + wpath.toString(), line);
		if (control.isDisposed())
			throwCoreException("Shell control is disposed: " + wpath.toString(), line);
		return new WindowCommandTarget(control, window);
	}

	private static WizardCommandTarget locateWizardPageControl(Shell shell, String id, IPath wpath, int line) throws CoreException {
		Control control = null;
		Object data = shell.getData();
		if (data instanceof WizardDialog) {
			WizardDialog wdialog = (WizardDialog) data;
			IWizardPage page = wdialog.getCurrentPage();
			String pname = page.getName();
			// assert page
			if (pname.equals(id) == false)
				throwCoreException("Unexpected wizard page: " + pname + ", expected " + id, line);
			Composite pparent = (Composite) page.getControl();
			control = locateVisibleChild(pparent, null, wpath);
			if (control != null)
				return new WizardCommandTarget(control, wdialog);
		}
		if (control == null)
			throwCoreException("Cannot locate wizard page control: " + wpath.toString(), line);
		return null;
	}

	private static IViewPart locateView(Shell shell, String id, int line) throws CoreException {
		Object data = shell.getData();

		if (data instanceof IWorkbenchWindow) {
			IWorkbenchWindow window = (IWorkbenchWindow) data;
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				IViewPart view = page.showView(id);
				return view;
			}
		}
		throwCoreException("Cannot locate view: " + id, line);
		return null;
	}

	/**
	 * Retrieves the primary SWT control of the given workbench part.
	 * <p>
	 * <b>Note:</b> This method uses workbench internals that are not part of
	 * the public Eclipse API. Changing the code contained within this method
	 * may cause breakage across Eclipse 3.x and Eclipse 4.x.
	 * </p>
	 */
	private static Control getPartControl(IWorkbenchPart part) {
		IWorkbenchPartSite site = part.getSite();
		return ((WorkbenchPartReference) site.getPage().getReference(part)).getPane().getControl();
	}

	private static ViewCommandTarget locateViewControl(Shell shell, String id, IPath wpath, int line) throws CoreException {
		Control control = null;

		IViewPart view = locateView(shell, id, line);
		if (view != null) {
			Control c = getPartControl(view);
			control = locateVisibleChild((Composite) c, null, wpath);
			if (control != null) {
				return new ViewCommandTarget(control, view);
			}
		}
		throwCoreException("Cannot locate view control: " + wpath.toString(), line);
		return null;
	}

	private static EditorCommandTarget locateEditorControl(Shell shell, String id, String inputName, IPath wpath, int line) throws CoreException {
		Control control = null;

		Object data = shell.getData();

		if (data instanceof IWorkbenchWindow) {
			IWorkbenchWindow window = (IWorkbenchWindow) data;
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				IEditorReference[] erefs = page.getEditorReferences();
				IEditorPart editor = null;
				for (int i = 0; i < erefs.length; i++) {
					IEditorReference eref = erefs[i];
					if (eref.getId().equals(id)) {
						// check the input
						IEditorPart part = eref.getEditor(true);
						if (part.getEditorInput().getName().equals(inputName)) {
							editor = part;
							break;
						}
					}
				}
				if (editor != null) {
					Control c = getPartControl(editor);
					control = locateVisibleChild((Composite) c, null, wpath);
					if (control != null) {
						return new EditorCommandTarget(control, editor);
					}
				}
			}
		}
		if (control == null)
			throwCoreException("Cannot locate editor control: " + wpath.toString(), line);
		return null;
	}

	private static Control locateVisibleChild(Composite parent, Composite skip, IPath wpath) {
		int[] counter = new int[1];
		counter[0] = 0;
		String wid = wpath.toString();
		int sloc = wid.lastIndexOf('#');
		if (sloc == -1)
			return null;
		String wclassName = wid.substring(0, sloc);
		return locateVisibleChild(parent, skip, wid, wclassName, counter);
	}

	private static Control locateVisibleChild(Composite parent, Composite skip, String id, String wclassName, int[] counter) {
		Control[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control child = children[i];

			if (child.getClass().getName().equals(wclassName)) {
				// same type - increment counter
				if (child.isVisible() == false)
					continue;
				counter[0]++;
				String cid = getControlId(child, counter[0]);
				if (cid.equals(id)) {
					// bingo
					return child;
				}
			} else if (child instanceof Composite) {
				if (skip != null && child.equals(skip))
					continue;
				if (!child.isVisible())
					continue;
				Control c = locateVisibleChild((Composite) child, skip, id, wclassName, counter);
				if (c != null)
					return c;
			}
		}
		return null;
	}

	public static void throwCoreException(String message, int line) throws CoreException {
		throwCoreException(message, line, null);
	}

	public static void throwCoreException(String message, int line, Throwable t) throws CoreException {
		if (line > 0)
			message = "Line " + line + ": " + message;
		Status s = new Status(IStatus.ERROR, "org.eclipse.ui.macro", IStatus.OK, message, t);
		throw new CoreException(s);
	}

	public static java.util.List generatePossibleKeyStrokes(Event event) {
		final java.util.List keyStrokes = new ArrayList(3);

		/*
		 * If this is not a keyboard event, then there are no key strokes. This
		 * can happen if we are listening to focus traversal events.
		 */
		if ((event.stateMask == 0) && (event.keyCode == 0) && (event.character == 0)) {
			return keyStrokes;
		}

		// Add each unique key stroke to the list for consideration.
		final int firstAccelerator = SWTKeySupport.convertEventToUnmodifiedAccelerator(event);
		keyStrokes.add(SWTKeySupport.convertAcceleratorToKeyStroke(firstAccelerator));

		// We shouldn't allow delete to undergo shift resolution.
		if (event.character == SWT.DEL) {
			return keyStrokes;
		}

		final int secondAccelerator = SWTKeySupport.convertEventToUnshiftedModifiedAccelerator(event);
		if (secondAccelerator != firstAccelerator) {
			keyStrokes.add(SWTKeySupport.convertAcceleratorToKeyStroke(secondAccelerator));
		}

		final int thirdAccelerator = SWTKeySupport.convertEventToModifiedAccelerator(event);
		if ((thirdAccelerator != secondAccelerator) && (thirdAccelerator != firstAccelerator)) {
			keyStrokes.add(SWTKeySupport.convertAcceleratorToKeyStroke(thirdAccelerator));
		}

		return keyStrokes;
	}
}