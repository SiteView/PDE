/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.pde.internal.core.plugin;

import org.w3c.dom.Node;

public interface IDocumentModelFactory {

	public IDocumentNode createXMLNode(IDocumentNode[] children, Node domNode);

}
