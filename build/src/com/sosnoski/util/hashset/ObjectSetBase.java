/*
 * Copyright (c) 2000-2001 Sosnoski Software Solutions, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.sosnoski.util.hashset;

import java.lang.reflect.Array;
import java.util.Iterator;

import com.sosnoski.util.ObjectHashBase;
import com.sosnoski.util.SparseArrayIterator;

/**
 * Base class for type-specific hash set classes with object keys. This
 * class builds on the basic structure provided by <code>ObjectHashBase</code>,
 * specializing it for the case of a hash set where the keys are the only data.
 * See the base class description for details of the implementation.<p>
 *
 * Hash sets based on this class are unsynchronized in order to provide the
 * best possible performance for typical usage scenarios, so explicit
 * synchronization must be implemented by the subclass or the application in
 * cases where they are to be modified in a multithreaded environment.<p>
 *
 * Subclasses need to implement the abstract methods defined by the base class
 * for working with the key array, and the abstract restructuring method
 * defined by this class, as well as the actual data access methods (at least
 * the basic <code>add</code>, <code>contains</code>, and <code>remove</code>
 * methods).
 *
 * @author Dennis M. Sosnoski
 * @version 1.1
 */

public abstract class ObjectSetBase extends ObjectHashBase
{
	/**
	 * Constructor with full specification.
	 *
	 * @param count number of values to assume in initial sizing of table
	 * @param fill fraction full allowed for table before growing
	 * @param type type of objects contained in table
	 * @param tech hash technique specifier (one of STANDARD_HASH,
	 * IDENTITY_COMP, or IDENTITY_HASH, inherited from
	 * {@link com.sosnoski.util.hashset.ObjectSetBase})
	 */

	public ObjectSetBase(int count, double fill, Class type, Object tech) {
		super(count, fill, type, tech);
	}

	/**
	 * Copy (clone) constructor.
	 *
	 * @param base instance being copied
	 */

	public ObjectSetBase(ObjectSetBase base) {
		super(base);
	}

	/**
	 * Reinsert an entry into the table. This method is used when the table is
	 * being directly modified by the base class, and should not adjust the
	 * count present or check the table capacity.
	 *
	 * @param slot position of entry to be reinserted into table
	 * @return <code>true</code> if the slot number used by the entry has
	 * has changed, <code>false</code> if not
	 */

	protected abstract boolean reinsert(int slot);

	/**
	 * Restructure the table. This abstract method is used when the table
	 * is increasing or decreasing in size, and works directly with the old
	 * table representation array. It should insert keys from the old array
	 * directly into the table without adjusting the count present or checking
	 * the table size.
	 *
	 * @param karray array of keys
	 */

	protected abstract void restructure(Object karray);

	/**
	 * Resize the base array after a size change. This implementation of the
	 * abstract base class method allocates the new array and then calls
	 * another method for handling the actual transfer of the key set from the
	 * old array to the new one.
	 *
	 * @param size new size for base arrays
	 */

	protected void reallocate(int size) {

		// allocate the larger array
		Object keys = getKeyArray();
		Class type = keys.getClass().getComponentType();
		setKeyArray(Array.newInstance(type, size));

		// reinsert all entries into new arrays
		restructure(keys);
	}

	/**
	 * Internal remove item from the table. Removes the item from the table
	 * by setting the key entry to <code>null</code> and adjusting the count
	 * present, then chains through the table to reinsert any other items
	 * which may have collided with the removed item.
	 *
	 * @param slot index number of item to be removed
	 */

	protected void internalRemove(int slot) {
		Object[] items = getKeyArray();
		items[slot] = null;
		m_entryCount--;
		while (items[(slot = stepSlot(slot))] != null) {
			reinsert(slot);
		}
	}

	/**
	 * Return an iterator for the <code>Object</code>s in this set. The
	 * iterator returns all items in arbitrary order, but is not "live". Any
	 * changes to the set while the iteration is in progress will give
	 * indeterminant results.
	 *
	 * @return iterator for values in set
	 */

	public final Iterator iterator() {
		return SparseArrayIterator.buildIterator(getKeyArray());
	}
}
