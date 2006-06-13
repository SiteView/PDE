/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.nls;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class ExternalizeStringsOperation extends WorkspaceModifyOperation {

	private Object[] fChangeFiles;
	
	public ExternalizeStringsOperation(Object[] changeFiles) {
		fChangeFiles = changeFiles;
	}
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		for (int i = 0; i < fChangeFiles.length; i++) {
			if (fChangeFiles[i] instanceof ModelChangeFile) {
				ModelChangeFile changeFile = (ModelChangeFile)fChangeFiles[i];
				ModelChange change = changeFile.getModel();
				IFile pFile = change.getPropertiesFile();
				checkPropertiesFile(pFile);
				if (!change.localizationSet())
					addBundleLocalization(change, monitor);
				
				ITextFileBufferManager pManager = FileBuffers.getTextFileBufferManager();
				try {
					pManager.connect(pFile.getFullPath(), monitor);
					ITextFileBuffer pBuffer = pManager.getTextFileBuffer(pFile.getFullPath());
					IDocument pDoc = pBuffer.getDocument();
					MultiTextEdit pEdit = new MultiTextEdit();
					
					doReplace(changeFile, pDoc, pEdit, monitor);
					
					pEdit.apply(pDoc);
					pBuffer.commit(monitor, true);
					
				} catch (MalformedTreeException e) {
				} catch (BadLocationException e) {
				} finally {
					pManager.disconnect(pFile.getFullPath(), monitor);
				}
			}
		}
	}
	private void doReplace(ModelChangeFile changeFile, IDocument pDoc, MultiTextEdit pEdit, IProgressMonitor monitor) throws CoreException {
		IFile uFile = changeFile.getFile();
		ITextFileBufferManager uManager = FileBuffers.getTextFileBufferManager();
		try {
			uManager.connect(uFile.getFullPath(), monitor);
			ITextFileBuffer uBuffer = uManager.getTextFileBuffer(uFile.getFullPath());
			IDocument uDoc = uBuffer.getDocument();
			MultiTextEdit uEdit = new MultiTextEdit();
			
			Iterator iter = changeFile.getChanges().iterator();
			
			while (iter.hasNext()) {
				ModelChangeElement changeElement = (ModelChangeElement)iter.next();
				if (changeElement.isExternalized()) {
					uEdit.addChild(new ReplaceEdit(changeElement.getOffset(),
							changeElement.getLength(), 
							changeElement.getExternKey()));
					pEdit.addChild(getPropertiesInsertEdit(pDoc, changeElement));
				}
			}
			uEdit.apply(uDoc);
			uBuffer.commit(monitor, true);
			
		} catch (MalformedTreeException e) {
		} catch (BadLocationException e) {
		} finally {
			uManager.disconnect(uFile.getFullPath(), monitor);
		}
 	}
	
	private void addBundleLocalization(ModelChange change, IProgressMonitor mon) {
		IPluginModelBase base = change.getParentModel();
		IFile manifest = base.getUnderlyingResource().getProject().getFile(PDEModelUtility.F_MANIFEST_FP);
		final String localiz = change.getBundleLocalization();
		PDEModelUtility.modifyModel(new ModelModification(manifest) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBundlePluginModel) {
					IBundlePluginModel bundleModel = (IBundlePluginModel) model;
					IBundle bundle = bundleModel.getBundleModel().getBundle();
					bundle.setLocalization(localiz);
				}
			}
		}, mon);
	}
	
	public static InsertEdit getPropertiesInsertEdit(IDocument doc, ModelChangeElement element) {
		String nl = TextUtilities.getDefaultLineDelimiter(doc);
		StringBuffer sb = new StringBuffer(nl);
		sb.append(element.getKey());
		sb.append(" = "); //$NON-NLS-1$
		sb.append(StringHelper.preparePropertiesString(element.getValue(), nl.toCharArray()));
		return new InsertEdit(doc.getLength(), sb.toString());
	}
	
	public static void checkPropertiesFile(IFile file) {
		if (!file.exists()) {
			String propertiesFileComment = NLS.bind(PDEUIMessages.ExternalizeStringsOperation_propertiesComment, file.getProject().getName());
			ByteArrayInputStream pStream = new ByteArrayInputStream(propertiesFileComment.getBytes());
			try {
				file.create(pStream, true, new NullProgressMonitor());
				pStream.close();
			} catch (CoreException e1) {
			} catch (IOException e) {
			}
		}
	}
 }
