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

package com.sosnoski.util.queue;

import java.util.Iterator;

import com.sosnoski.util.WrappedArrayIterator;

/**
 * Growable circular queue of <code>Object</code>s. This implementation is
 * unsynchronized in order to provide the best possible performance for typical
 * usage scenarios, so explicit synchronization must be implemented by a
 * wrapper class or directly by the application in cases where instances are
 * modified in a multithreaded environment. See the base classes for other
 * details of the implementation.
 *
 * @author Dennis M. Sosnoski
 * @version 1.1
 */

public class ObjectQueue extends QueueBase
{
	/** The underlying array used for storing the data. */
	protected Object[] m_baseArray;

	/**
	 * Constructor with full specification.
	 *
	 * @param size number of elements initially allowed in queue
	 * @param growth maximum size increment for growing queue
	 */

	public ObjectQueue(int size, int growth) {
		super(size, growth, Object.class);
	}

	/**
	 * Constructor with partial specification.
	 *
	 * @param size number of elements initially allowed in queue
	 */

	public ObjectQueue(int size) {
		super(size, Object.class);
	}

	/**
	 * Default constructor.
	 */

	public ObjectQueue() {
		this(DEFAULT_SIZE);
	}

	/**
	 * Copy (clone) constructor.
	 *
	 * @param base instance being copied
	 */

	public ObjectQueue(ObjectQueue base) {
		super(base);
	}

	/**
	 * Get the backing array. This method is used by the type-agnostic base
	 * class code to access the array used for type-specific storage.
	 *
	 * @return backing array object
	 */

	protected Object getArray() {
		return m_baseArray;
	}

	/**
	 * Set the backing array. This method is used by the type-agnostic base
	 * class code to set the array used for type-specific storage.
	 *
	 * @param backing array object
	 */

	protected void setArray(Object array) {
		m_baseArray = (Object[]) array;
	}

	/**
	 * Add an item at the end of the queue. If necessary, the array used for
	 * storing the queue data is grown before storing the added item.
	 *
	 * @param item item to be added
	 */

	public void add(Object item) {
		int index = getAddIndex();
		m_baseArray[index] = item;
	}

	/**
	 * Remove an item from the start of the queue.
	 *
	 * @return item removed from queue
	 * @exception IllegalStateException on attempt to remove an item from an
	 * empty queue
	 */

	public Object remove() {
		int index = getRemoveIndex();
		Object item = m_baseArray[index];
		m_baseArray[index] = null;
		return item;
	}

	/**
	 * Return an iterator for the <code>Object</code>s in this queue. The
	 * iterator returns all values in order, but is not "live". Values
	 * added to the queue during iteration will not be returned by the
	 * iteration, and any other changes to the queue while the iteration is
	 * in progress will give indeterminant results.
	 *
	 * @return iterator for values in queue
	 */

	public final Iterator iterator() {
		int end = m_fillOffset - 1;
		if (end < 0) {
			end = m_countLimit - 1;
		}
		return WrappedArrayIterator.buildIterator(m_baseArray,
			m_emptyOffset, end);
	}

	/**
	 * Constructs and returns a simple array containing the same data as held
	 * in this growable array.
	 *
	 * @return array containing a copy of the data
	 */

	public Object[] toArray() {
		return (Object[]) buildArray(Object.class);
	}

	/**
	 * Constructs and returns a type-specific array containing the same data
	 * as held in this growable generic array. All values in this array must
	 * be assignment compatible with the specified type.
	 *
	 * @param type element type for constructed array
	 * @return array containing a copy of the data
	 */

	public Object[] toArray(Class type) {
		return (Object[]) buildArray(type);
	}

	/**
	 * Duplicates the object with the generic call.
	 *
	 * @return a copy of the object
	 */

	public Object clone() {
		return new ObjectQueue(this);
	}
}
