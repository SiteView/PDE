/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.cheatsheet.simple;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * SimpleCS
 *
 */
public class SimpleCS extends SimpleCSObject implements ISimpleCS {

	/**
	 * Element:  intro
	 */
	private ISimpleCSIntro fIntro;
	
	/**
	 * Attribute:  title
	 */
	private String fTitle;
	
	/**
	 * Element:  item
	 */
	private ArrayList fItems;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCS(ISimpleCSModel model) {
		super(model, null);
		reset();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#getIntro()
	 */
	public ISimpleCSIntro getIntro() {
		return fIntro;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#getItems()
	 */
	public ISimpleCSItem[] getItems() {
		return (ISimpleCSItem[])fItems.toArray(new ISimpleCSItem[fItems.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#getTitle()
	 */
	public String getTitle() {
		return fTitle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#reset()
	 */
	public void reset() {
		fIntro = null;
		fTitle = null;
		fItems = new ArrayList();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#setIntro(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro)
	 */
	public void setIntro(ISimpleCSIntro intro) {
		ISimpleCSObject old = fIntro;
		fIntro = intro;

		if (isEditable()) {
			fireStructureChanged(intro, old);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#setTitle(java.lang.String)
	 */
	public void setTitle(String title) {
		String old = fTitle;
		fTitle = title;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_TITLE, old, fTitle);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Element element) {
		// Process cheatsheet element
		if (element.getNodeName().equals(ELEMENT_CHEATSHEET)) {
			// Process title attribute
			// Trim leading and trailing whitespace
			fTitle = element.getAttribute(ATTRIBUTE_TITLE).trim(); 
			// Process children
			NodeList children = element.getChildNodes();
			ISimpleCSModelFactory factory = getModel().getFactory();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					String name = child.getNodeName();
					Element childElement = (Element)child;
					if (name.equals(ELEMENT_INTRO)) {
						fIntro = factory.createSimpleCSIntro(this);
						fIntro.parse(childElement);
					} else if (name.equals(ELEMENT_ITEM)) { 
						ISimpleCSItem item = factory.createSimpleCSItem(this);
						fItems.add(item);
						item.parse(childElement);
					}
				}
			}
		}
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {

		StringBuffer buffer = new StringBuffer();
		String newIndent = indent + XMLPrintHandler.XML_INDENT;
		
		try {
			// Print XML decl
			XMLPrintHandler.printHead(writer, ATTRIBUTE_VALUE_ENCODING);
			// Print cheatsheet element
			buffer.append(ELEMENT_CHEATSHEET);
			// Print title attribute
			if ((fTitle != null) && 
					(fTitle.length() > 0)) {
				// Trim leading and trailing whitespace
				// Encode characters
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_TITLE, 
						PDETextHelper.translateWriteText(
								fTitle.trim(), SUBSTITUTE_CHARS)));
			}
			// Start element
			XMLPrintHandler.printBeginElement(writer, buffer.toString(),
					indent, false);
			// Print intro element
			if (fIntro != null) {
				fIntro.write(newIndent, writer);
			}
			// Print item elements
			Iterator iterator = fItems.iterator();
			while (iterator.hasNext()) {
				ISimpleCSItem item = (ISimpleCSItem)iterator.next();
				item.write(newIndent, writer);
			}			
			// End element
			XMLPrintHandler.printEndElement(writer, ELEMENT_CHEATSHEET, indent);
			
		} catch (IOException e) {
			// Suppress
			//e.printStackTrace();
		} 		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#addItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public void addItem(ISimpleCSItem item) {
		fItems.add(item);
		
		if (isEditable()) {
			fireStructureChanged(item, IModelChangedEvent.INSERT);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#removeItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public void removeItem(ISimpleCSItem item) {
		fItems.remove(item);
		
		if (isEditable()) {
			fireStructureChanged(item, IModelChangedEvent.REMOVE);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_CHEAT_SHEET;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getName()
	 */
	public String getName() {
		return fTitle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getChildren()
	 */
	public List getChildren() {
		ArrayList list = new ArrayList();
		// Add intro
		if (fIntro != null) {
			list.add(fIntro);
		}
		// Add items
		if (fItems.size() > 0) {
			list.addAll(fItems);
		}
		return list;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#isFirstItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public boolean isFirstItem(ISimpleCSItem item) {
		int position = fItems.indexOf(item);
		if (position == 0) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#isLastItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public boolean isLastItem(ISimpleCSItem item) {
		int position = fItems.indexOf(item);
		int lastPosition = fItems.size() - 1;
		if (position == lastPosition) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#addItem(int, org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public void addItem(int index, ISimpleCSItem item) {
		
		if (index < 0) {
			return;
		}
		if (index >= fItems.size()) {
			fItems.add(item);
		} else {
			fItems.add(index, item);
		}
		
		if (isEditable()) {
			fireStructureChanged(item, IModelChangedEvent.INSERT);
		}
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#indexOfItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public int indexOfItem(ISimpleCSItem item) {
		return fItems.indexOf(item);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#removeItem(int, org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public void removeItem(int index) {
		
		if ((index < 0) ||
				(index > (fItems.size() - 1))) {
			return;
		}		
		
		ISimpleCSItem item = (ISimpleCSItem)fItems.remove(index);
		
		if (isEditable()) {
			fireStructureChanged(item, IModelChangedEvent.REMOVE);
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#getItemCount()
	 */
	public int getItemCount() {
		return fItems.size();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#hasItems()
	 */
	public boolean hasItems() {
		if (fItems.isEmpty()) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#getNextSibling(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public ISimpleCSItem getNextSibling(ISimpleCSItem item) {
		int position = fItems.indexOf(item);
		int lastIndex = fItems.size() - 1;
		if ((position == -1) ||
				(position == lastIndex)) {
			// Either the item was not found or the item was found but it is 
			// at the last index
			return null;
		}
		return (ISimpleCSItem)fItems.get(position + 1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#getPreviousSibling(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public ISimpleCSItem getPreviousSibling(ISimpleCSItem item) {
		int position = fItems.indexOf(item);
		if ((position == -1) ||
				(position == 0)) {
			// Either the item was not found or the item was found but it is 
			// at the first index
			return null;
		}
		return (ISimpleCSItem)fItems.get(position - 1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#moveItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem, int)
	 */
	public void moveItem(ISimpleCSItem item, int newRelativeIndex) {
		// Get the current index of the task object
		int currentIndex = fItems.indexOf(item);
		// Ensure the object is found
		if (currentIndex == -1) {
			return;
		}
		// Calculate the new index
		int newIndex = newRelativeIndex + currentIndex;
		// Validate the new index
		if ((newIndex < 0) ||
				(newIndex >= fItems.size())) {
			return;
		}
		// Remove the task object
		fItems.remove(item);
		// Add the task object back at the specified index
		fItems.add(newIndex, item);
		// Send an insert event
		if (isEditable()) {
			fireStructureChanged(item, IModelChangedEvent.INSERT);
		}	
	}

}