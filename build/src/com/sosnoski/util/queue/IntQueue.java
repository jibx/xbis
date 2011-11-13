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

/**
 * Growable circular queue of <code>int</code>s. This implementation is 
 * unsynchronized in order to provide the best possible performance for typical 
 * usage scenarios, so explicit synchronization must be implemented by a 
 * wrapper class or directly by the application in cases where instances are 
 * modified in a multithreaded environment. See the base classes for other 
 * details of the implementation.
 * 
 * @author Dennis M. Sosnoski
 * @version 1.0
 */

public class IntQueue extends QueueBase
{
	/** The underlying array used for storing the data. */
	protected int[] m_baseArray;

	/**
	 * Constructor with full specification.
	 * 
	 * @param size number of elements initially allowed in queue
	 * @param growth maximum size increment for growing queue
	 */
	
	public IntQueue(int size, int growth) {
		super(size, growth, int.class);
	}

	/**
	 * Constructor with partial specification.
	 * 
	 * @param size number of elements initially allowed in queue
	 */
	
	public IntQueue(int size) {
		super(size, int.class);
	}

	/**
	 * Default constructor.
	 */
	
	public IntQueue() {
		this(DEFAULT_SIZE);
	}

	/**
	 * Copy (clone) constructor.
	 * 
	 * @param base instance being copied
	 */
	
	public IntQueue(IntQueue base) {
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
		m_baseArray = (int[]) array;
	}

	/**
	 * Add an item at the end of the queue. If necessary, the array used for 
	 * storing the queue data is grown before storing the added item.
	 * 
	 * @param item item to be added
	 */
	
	public void add(int item) {
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
	
	public int remove() {
		return m_baseArray[getRemoveIndex()];
	}

	/**
	 * Constructs and returns a simple array containing the same data as held 
	 * in this growable array.
	 * 
	 * @return array containing a copy of the data
	 */
	
	public int[] toArray() {
		return (int[]) buildArray(int.class);
	}

	/**
	 * Duplicates the object with the generic call.
	 * 
	 * @return a copy of the object
	 */
	
	public Object clone() {
		return new IntQueue(this);
	}
}
