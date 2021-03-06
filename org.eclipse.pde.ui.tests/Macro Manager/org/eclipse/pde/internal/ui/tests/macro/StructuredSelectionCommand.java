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

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableTree;
import org.eclipse.swt.custom.TableTreeItem;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

public class StructuredSelectionCommand extends AbstractStructuredCommand {
	private String type;
	public static final String DEFAULT_SELECT = "default-select";
	public static final String ITEM_SELECT = "item-select";

	public StructuredSelectionCommand(WidgetIdentifier wid, String type) {
		super(wid);
		items = new ArrayList();
		this.type = type;
	}

	public boolean mergeEvent(Event e) {
		if (e.type == SWT.DefaultSelection) {
			this.type = DEFAULT_SELECT;
		}
		return super.mergeEvent(e);
	}

	public String getType() {
		return type;
	}

	protected Widget[] getItemsForEvent(Event event) {
		if (event.widget instanceof Tree) {
			return ((Tree) event.widget).getSelection();
		} else if (event.widget instanceof Table) {
			return ((Table) event.widget).getSelection();
		} else if (event.widget instanceof TableTree) {
			return ((TableTree) event.widget).getSelection();
		}
		return super.getItemsForEvent(event);
	}

	protected void playTreeCommand(Tree tree, TreeItem[] matches) {
		tree.setSelection(matches);
		fireEvent(tree, matches);
	}

	private void fireEvent(Widget widget, Widget[] items) {
		Event e = new Event();
		e.widget = widget;
		e.type = type.equals(ITEM_SELECT) ? SWT.Selection : SWT.DefaultSelection;
		e.item = items.length > 0 ? items[0] : null;
		widget.notifyListeners(e.type, e);
	}

	protected void playTableCommand(Table table, TableItem[] matches) {
		table.setSelection(matches);
		fireEvent(table, matches);
	}

	protected void playTableTreeCommand(TableTree tableTree, TableTreeItem[] matches) {
		tableTree.setSelection(matches);
		fireEvent(tableTree, matches);
	}
}
