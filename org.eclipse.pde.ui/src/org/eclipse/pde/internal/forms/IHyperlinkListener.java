package org.eclipse.pde.internal.forms;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Label;

public interface IHyperlinkListener {

public void linkActivated(Control linkLabel);
public void linkEntered(Control linkLabel);
public void linkExited(Control linkLabel);
}
