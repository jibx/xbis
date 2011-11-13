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

package com.sosnoski.util.hashmap;

import java.lang.reflect.Array;
import java.util.Iterator;

import com.sosnoski.util.ObjectHashBase;
import com.sosnoski.util.SparseArrayIterator;

/**
 * Base class for type-specific hash map classes with object keys. This
 * class builds on the basic structure provided by <code>ObjectHashBase</code>,
 * specializing it for the case of a hash map where data values are associated
 * with the keys. See the base class description for details of the
 * implementation.<p>
 *
 * Hash maps based on this class are unsynchronized in order to provide the
 * best possible performance for typical usage scenarios, so explicit
 * synchronization must be implemented by the subclass or the application in
 * cases where they are to be modified in a multithreaded environment.<p>
 *
 * Subclasses need to implement the abstract methods defined by the base class
 * for working with the key array, and by this class for working with the value
 * array and for restructuring, as well as the actual data access methods (at
 * least the basic <code>add()</code>, <code>containsKey()</code>,
 * <code>get()</code>, and <code>remove()</code> methods).
 *
 * @author Dennis M. Sosnoski
 * @version 1.1
 * @see PrimitiveKeyBase
 */

public abstract class ObjectKeyBase extends ObjectHashBase
{
	/**
	 * Constructor with full specification.
	 *
	 * @param count number of values to assume in initial sizing of table
	 * @param fill fraction full allowed for table before growing
	 * @param ktype type of primitives used for keys
	 * @param vtype type of primitives or objects used for values
	 * @param tech hash technique specifier (one of STANDARD_HASH,
	 * IDENTITY_COMP, or IDENTITY_HASH, inherited from
	 * {@link com.sosnoski.util.ObjectHashBase})
	 */

	public ObjectKeyBase(int count, double fill, Class ktype, Class vtype,
		Object tech) {
		super(count, fill, ktype, tech);
		setValueArray(Array.newInstance(vtype, m_arraySize));
	}

	/**
	 * Copy (clone) constructor.
	 *
	 * @param from instance being copied
	 */

	public ObjectKeyBase(ObjectKeyBase from) {
		super(from);
		Class type = from.getValueArray().getClass().getComponentType();
		Object copy = Array.newInstance(type, m_arraySize);
		System.arraycopy(from.getValueArray(), 0, copy, 0, m_arraySize);
		setValueArray(copy);
	}

	/**
	 * Get the backing array of values. This method is used by the type-agnostic
	 * base class code to access the array used for type-specific storage by
	 * the child class.
	 *
	 * @return backing key array object
	 */

	protected abstract Object getValueArray();

	/**
	 * Set the backing array of values. This method is used by the type-agnostic
	 * base class code to set the array used for type-specific storage by the
	 * child class.
	 *
	 * @param array backing value array object
	 */

	protected abstract void setValueArray(Object array);

	/**
	 * Restructure the table. This abstract method is used when the table
	 * is increasing or decreasing in size, and works directly with the old
	 * table representation arrays. It should insert pairs from the old arrays
	 * directly into the table without adjusting the count present or checking
	 * the table size.
	 *
	 * @param karray array of keys
	 * @param varray array of values
	 */

	protected abstract void restructure(Object karray, Object varray);

	/**
	 * Resize the base arrays after a size change. This implementation of the
	 * abstract base class method allocates the new arrays and then calls
	 * another method for handling the actual transfer of the keys and values
	 * from the old arrays to the new ones.
	 *
	 * @param size new size for base arrays
	 */

	protected void reallocate(int size) {

		// allocate the larger arrays
		Object keys = getKeyArray();;
		Class type = keys.getClass().getComponentType();
		setKeyArray(Array.newInstance(type, size));
		Object values = getValueArray();
		type = values.getClass().getComponentType();
		setValueArray(Array.newInstance(type, size));

		// reinsert all entries into new arrays
		restructure(keys, values);
	}

	/**
	 * Reinsert an entry into the hash map. This abstract method is used
	 * when the table is being directly modified by the base class, and should
	 * not adjust the count present or check the table capacity.
	 *
	 * @param slot position of entry to be reinserted into hash map
	 * @return <code>true</code> if the slot number used by the entry has
	 * has changed, <code>false</code> if not
	 */

	protected abstract boolean reinsert(int slot);

	/**
	 * Internal remove pair from the table. Removes the pair from the table
	 * by setting the key entry to <code>null</code> and adjusting the count
	 * present, then chains through the table to reinsert any other pairs
	 * which may have collided with the removed pair. If the associated value
	 * is an object reference, it should be set to <code>null</code> before
	 * this method is called.
	 *
	 * @param slot index number of pair to be removed
	 */

	protected void internalRemove(int slot) {

		// delete pair from table
		Object[] keys = getKeyArray();
		keys[slot] = null;
		m_entryCount--;
		while (keys[(slot = stepSlot(slot))] != null) {

			// reinsert current entry in table to fill holes
			reinsert(slot);

		}
	}

	/**
	 * Set the table to the empty state. This override of the base class method
	 * first calls the base class method to clear the entry information, then
	 * checks if the hash map values are objects, and if so clears all
	 * references to these objects.
	 */

	public void clear() {
		super.clear();
		Object values = getValueArray();
		if (!values.getClass().getComponentType().isPrimitive()) {
			Object[] objects = (Object[])values;
			for (int i = 0; i < objects.length; i++) {
				objects[i] = null;
			}
		}
	}

	/**
	 * Return an iterator for the key <code>Object</code>s in this map. The
	 * iterator returns all keys in arbitrary order, but is not "live". Any
	 * changes to the map while the iteration is in progress will give
	 * indeterminant results.
	 *
	 * @return iterator for keys in map
	 */

	public final Iterator iterator() {
		return SparseArrayIterator.buildIterator((Object[])getKeyArray());
	}
}
