/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.tests.macro;

import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;

public class ViewCommandTarget extends CommandTarget {
	public ViewCommandTarget(Widget widget, IViewPart view) {
		super(widget, view);
	}

	public IViewPart getView() {
		return (IViewPart) getContext();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.macro.CommandTarget#ensureVisible()
	 */
	public void ensureVisible() {
		IViewPart view = getView();
		IWorkbenchPage page = view.getViewSite().getPage();
		page.activate(view);
	}
}
