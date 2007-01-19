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
package org.eclipse.pde.internal.ui.editor.plugin;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SourceLocationManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.extension.NewExtensionPointWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ExtensionPointsSection extends TableSection {
	private TableViewer pointTable;

	class TableContentProvider extends DefaultContentProvider implements
			IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			IPluginModelBase model = (IPluginModelBase) getPage().getModel();
			IPluginBase pluginBase = model.getPluginBase();
			if (pluginBase != null)
				return pluginBase.getExtensionPoints();
			return new Object[0];
		}
	}

	public ExtensionPointsSection(PDEFormPage page, Composite parent) {
		super(page, parent, ExpandableComposite.TITLE_BAR | Section.DESCRIPTION, new String[] { PDEUIMessages.ManifestEditor_DetailExtensionPointSection_new });
		getSection().setText(PDEUIMessages.ManifestEditor_DetailExtensionPointSection_title);
		getSection().setDescription(PDEUIMessages.ExtensionPointsSection_sectionDescAllExtensionPoints);
		fHandleDefaultButton = false;
		getTablePart().setEditable(false);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		TablePart tablePart = getTablePart();
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		pointTable = tablePart.getTableViewer();
		pointTable.setContentProvider(new TableContentProvider());
		pointTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		toolkit.paintBordersFor(container);
		section.setClient(container);
		pointTable.setInput(getPage());
		selectFirstExtensionPoint();
		IBaseModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).addModelChangedListener(this);
		tablePart.setButtonEnabled(0, model.isEditable());
	}

	private void selectFirstExtensionPoint() {
		Table table = pointTable.getTable();
		TableItem[] items = table.getItems();
		if (items.length == 0)
			return;
		TableItem firstItem = items[0];
		Object obj = firstItem.getData();
		pointTable.setSelection(new StructuredSelection(obj));
	}

	void fireSelection() {
		pointTable.setSelection(pointTable.getSelection());
	}

	public void dispose() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).removeModelChangedListener(this);
		super.dispose();
	}

	public boolean doGlobalAction(String actionId) {
		
		if (!isEditable()) { return false; }
		
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleDelete();
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		return false;
	}

	public void refresh() {
		pointTable.refresh();
		getManagedForm().fireSelectionChanged(this, pointTable.getSelection());
		super.refresh();
	}

	public boolean setFormInput(Object object) {
		if (object instanceof IPluginExtensionPoint) {
			pointTable.setSelection(new StructuredSelection(object), true);
			return true;
		}
		return false;
	}

	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		super.selectionChanged(selection);
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IPluginExtensionPoint) {
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				pointTable.add(changeObject);
				pointTable.setSelection(
					new StructuredSelection(changeObject),
					true);
				pointTable.getTable().setFocus();
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				pointTable.remove(changeObject);
			} else {
				pointTable.update(changeObject, null);
			}
		}
	}

	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = pointTable.getSelection();

		Action newAction = new Action(PDEUIMessages.ManifestEditor_DetailExtensionPointSection_newExtensionPoint) {
			public void run() {
				handleNew();
			}
		};
		newAction.setEnabled(isEditable());
		manager.add(newAction);
		
		if (selection.isEmpty()) {
			getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
			return;
		}
		manager.add(new Separator());
		IBaseModel model = getPage().getPDEEditor().getAggregateModel();
		PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
		actionGroup.setBaseModel(model);
		actionGroup.setContext(new ActionContext(selection));
		actionGroup.fillContextMenu(manager);
		manager.add(new Separator());
		
		Action deleteAction = new Action(PDEUIMessages.Actions_delete_label) {
			public void run() {
				handleDelete();
			}
		};
		deleteAction.setEnabled(isEditable());
		manager.add(deleteAction);
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}

	protected void buttonSelected(int index) {
		if (index == 0)
			handleNew();
	}

	private void handleDelete() {
		Object[] selection = ((IStructuredSelection) pointTable
				.getSelection()).toArray();
		for (int i = 0; i < selection.length; i++) {
			Object object = selection[i];
			if (object != null && object instanceof IPluginExtensionPoint) {
				IPluginExtensionPoint ep = (IPluginExtensionPoint) object;
				IPluginBase plugin = ep.getPluginBase();
				try {
					plugin.remove(ep);
					String schema = ep.getSchema();
					IProject project = ep.getModel().getUnderlyingResource()
							.getProject();
					IFile schemaFile = project.getFile(schema);
					if (schemaFile.exists())
						if (MessageDialog.openQuestion(getSection().getShell(),
								PDEUIMessages.ExtensionPointsSection_title,
								PDEUIMessages.ExtensionPointsSection_message1 + " " //$NON-NLS-1$
								+ schemaFile.getProjectRelativePath().toString() + "?")) //$NON-NLS-1$
							schemaFile.delete(true, false,
									new NullProgressMonitor());

				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		}
	}

	private void handleNew() {
		IFile file = ((IFileEditorInput) getPage().getPDEEditor()
				.getEditorInput()).getFile();
		final IProject project = file.getProject();
		BusyIndicator.showWhile(pointTable.getTable().getDisplay(),
				new Runnable() {
					public void run() {
						NewExtensionPointWizard wizard = new NewExtensionPointWizard(
								project, (IPluginModelBase) getPage()
										.getModel(), (ManifestEditor) getPage()
										.getPDEEditor());
						WizardDialog dialog = new WizardDialog(PDEPlugin
								.getActiveWorkbenchShell(), wizard);
						dialog.create();
						SWTUtil.setDialogSize(dialog, 400, 450);
						dialog.open();
					}
				});
	}

	public static IEditorInput getSchemaFromSourceExtension(IPluginBase plugin, IPath path) {
		SourceLocationManager mgr = PDECore.getDefault().getSourceLocationManager();
		File file = mgr.findSourceFile(plugin, path);
		return (file != null && file.exists() && file.isFile())
				 ? new SystemFileEditorInput(file)
				 : null;
	}
	
	protected void doPaste(Object target, Object[] objects) {
		/*
		 * IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		 * IPluginBase plugin = model.getPluginBase(); try { for (int i = 0; i <
		 * objects.length; i++) { Object obj = objects[i]; if (obj instanceof
		 * IPluginExtensionPoint) { PluginExtensionPoint point =
		 * (PluginExtensionPoint) obj; point.setModel(model);
		 * point.setParent(plugin); plugin.add(point); } } } catch
		 * (CoreException e) { PDEPlugin.logException(e); }
		 */
	}

	protected boolean canPaste(Object target, Object[] objects) {
		if (objects[0] instanceof IPluginExtensionPoint)
			return true;
		return false;
	}
	
	protected void selectExtensionPoint(ISelection selection) {
		pointTable.setSelection(selection, true);
	}
}
