/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.*;



public class XMLTagScanner extends RuleBasedScanner {

	public XMLTagScanner(IColorManager manager) {
		IToken string = new Token(new TextAttribute(manager.getColor(IPDEColorConstants.P_STRING)));
		
		IRule[] rules = new IRule[3];
		// Add rule for single and double quotes
		rules[0] = new SingleLineRule("\"", "\"", string, '\\'); //$NON-NLS-1$ //$NON-NLS-2$
		rules[1] = new SingleLineRule("'", "'", string, '\\'); //$NON-NLS-1$ //$NON-NLS-2$
		// Add generic whitespace rule.
		rules[2] = new WhitespaceRule(new XMLWhitespaceDetector());
		setRules(rules);
	}
}
