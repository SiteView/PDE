/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <zx@us.ibm.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.spy.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.pde.internal.runtime.spy.dialogs.SpyDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class SpyHandler extends AbstractHandler {

	public SpyHandler() {}

	public Object execute(ExecutionEvent event) throws ExecutionException {		
		if(event != null) {
		    Shell shell = HandlerUtil.getActiveShell(event);
			SpyDialog dialog = new SpyDialog(shell, event, shell.getDisplay().getCursorLocation());
			dialog.create();
			dialog.open();
		}
		
		return null;
	}
}