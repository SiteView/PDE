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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.builders.DependencyLoop;
import org.eclipse.pde.internal.core.builders.DependencyLoopFinder;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.search.FindDeclarationsAction;
import org.eclipse.pde.internal.ui.search.FindReferencesAction;
import org.eclipse.pde.internal.ui.search.dependencies.UnusedDependenciesAction;
import org.eclipse.pde.internal.ui.views.dependencies.OpenDependenciesAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DependencyAnalysisSection extends PDESection {
	private FormText formText;

	public DependencyAnalysisSection(PDEFormPage page, Composite parent, int style) {
		super(page, parent, ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | style);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	private String getFormText() {
		boolean editable = getPage().getModel().isEditable();
		if (getPage().getModel() instanceof IPluginModel) {
			if (editable)
				return PDEUIMessages.DependencyAnalysisSection_plugin_editable;  
			return PDEUIMessages.DependencyAnalysisSection_plugin_notEditable; 
		}
		if (editable)
			return PDEUIMessages.DependencyAnalysisSection_fragment_editable;  
		return PDEUIMessages.DependencyAnalysisSection_fragment_notEditable; 
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.DependencyAnalysisSection_title); 

		Composite container = toolkit.createComposite(section, SWT.NONE);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 1));
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
		
		formText = toolkit.createFormText(container, true);
		formText.setText(getFormText(), true, false);		
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		formText.setImage("loops", lp.get(PDEPluginImages.DESC_LOOP_OBJ)); //$NON-NLS-1$
		formText.setImage("search", lp.get(PDEPluginImages.DESC_PSEARCH_OBJ)); //$NON-NLS-1$
		formText.setImage("hierarchy", lp.get(PDEPluginImages.DESC_HIERARCHICAL_LAYOUT)); //$NON-NLS-1$
		formText.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				if (e.getHref().equals("unused")) //$NON-NLS-1$
					doFindUnusedDependencies();
				else if (e.getHref().equals("loops")) //$NON-NLS-1$
					doFindLoops();
				else if (e.getHref().equals("references")) //$NON-NLS-1$
					doFindReferences();
				else if (e.getHref().equals("hierarchy")) //$NON-NLS-1$
					OpenDependenciesAction.openDependencies((IPluginModelBase)getPage().getModel());
			}
		});
		
		section.setClient(container);
	}

	protected void doFindLoops() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IPluginModel) {
			IPlugin plugin = ((IPluginModel)model).getPlugin();
			DependencyLoop[] loops = DependencyLoopFinder.findLoops(plugin);
			if (loops.length == 0)
				MessageDialog.openInformation(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.DependencyAnalysisSection_loops, PDEUIMessages.DependencyAnalysisSection_noCycles); // 
			else {
				LoopDialog dialog = new LoopDialog(PDEPlugin.getActiveWorkbenchShell(), loops);
				dialog.open();
			}
		}	
	}

	protected void doFindUnusedDependencies() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IPluginModelBase) {
			new UnusedDependenciesAction((IPluginModelBase)model, false).run();
		}		
	}
	
	private void doFindReferences() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IPluginModel) {
			new FindReferencesAction(((IPluginModel)model).getPlugin()).run();
		} else if (model instanceof IFragmentModel){
			IFragment fragment = ((IFragmentModel)model).getFragment();
			String id = fragment.getPluginId();
			ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(id);
			if (entry != null) {
				IPluginModelBase pluginModel = entry.getActiveModel();
				new FindDeclarationsAction(pluginModel.getPluginBase()).run();
			} else {
				MessageDialog.openInformation(
						PDEPlugin.getActiveWorkbenchShell(), 
						PDEUIMessages.DependencyAnalysisSection_references,  
						PDEUIMessages.DependencyAnalysisSection_noReferencesFound);  
			}
		}
	}

}
