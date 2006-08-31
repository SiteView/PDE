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
package org.eclipse.pde.internal.core.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.exports.FeatureExportOperation;

public abstract class BaseExportTask extends Task {
	
	protected String fDestination;
	protected String fZipFilename;
	protected boolean fToDirectory;
	protected boolean fUseJarFormat;
	protected boolean fExportSource;
	protected String fQualifier;

	public BaseExportTask() {
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		if (fDestination == null)
			throw new BuildException("No destination is specified"); //$NON-NLS-1$
		
		if (!fToDirectory && fZipFilename == null)
			throw new BuildException("No zip file is specified"); //$NON-NLS-1$
		
		Job job = new Job(PDECoreMessages.BaseExportTask_pdeExport) {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					getExportOperation().run(new NullProgressMonitor());
				} catch (CoreException e) {
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule(2000);
	}
	
	public void setExportType(String type) {
		fToDirectory = !"zip".equals(type); //$NON-NLS-1$
	}
	
	public void setUseJARFormat(String useJarFormat) {
		fUseJarFormat = "true".equals(useJarFormat); //$NON-NLS-1$
	}
	
	public void setExportSource(String doExportSource) {
		fExportSource = "true".equals(doExportSource); //$NON-NLS-1$
	}
	
	public void setDestination(String destination) {
		fDestination = destination;
	}
	
	public void setFilename(String filename) {
		fZipFilename = filename;
	}
	
	public void setQualifier(String qualifier) {
		fQualifier = qualifier;
	}
	
	protected abstract FeatureExportOperation getExportOperation();
}
