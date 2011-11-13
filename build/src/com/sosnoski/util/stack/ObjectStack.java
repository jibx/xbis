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

package com.sosnoski.util.stack;

import java.util.Iterator;

import com.sosnoski.util.ArrayRangeIterator;

/**
 * Growable <code>Object</code> stack with type specific access methods. This
 * implementation is unsynchronized in order to provide the best possible
 * performance for typical usage scenarios, so explicit synchronization must
 * be implemented by a wrapper class or directly by the application in cases
 * where instances are modified in a multithreaded environment. See the base
 * classes for other details of the implementation.
 *
 * @author Dennis M. Sosnoski
 * @version 1.1
 */

public class ObjectStack extends StackBase
{
	/** The underlying array used for storing the data. */
	protected Object[] m_baseArray;

	/**
	 * Constructor with full specification.
	 *
	 * @param size number of <code>Object</code> values initially allowed in
	 * stack
	 * @param growth maximum size increment for growing stack
	 */

	public ObjectStack(int size, int growth) {
		super(size, growth, Object.class);
	}

	/**
	 * Constructor with initial size specified.
	 *
	 * @param size number of <code>Object</code> values initially allowed in
	 * stack
	 */

	public ObjectStack(int size) {
		super(size, Object.class);
	}

	/**
	 * Default constructor.
	 */

	public ObjectStack() {
		this(DEFAULT_SIZE);
	}

	/**
	 * Copy (clone) constructor.
	 *
	 * @param base instance being copied
	 */

	public ObjectStack(ObjectStack base) {
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
	 * Push a value on the stack.
	 *
	 * @param value value to be added
	 */

	public void push(Object value) {
		int index = getAddIndex();
		m_baseArray[index] = value;
	}

	/**
	 * Pop a value from the stack.
	 *
	 * @return value from top of stack
	 * @exception ArrayIndexOutOfBoundsException on attempt to pop empty stack
	 */

	public Object pop() {
		if (m_countPresent > 0) {
			Object value = m_baseArray[--m_countPresent];
			m_baseArray[m_countPresent] = null;
			return value;
		} else {
			throw new ArrayIndexOutOfBoundsException
				("Attempt to pop empty stack");
		}
	}

	/**
	 * Pop multiple values from the stack. The last value popped is the
	 * one returned.
	 *
	 * @param count number of values to pop from stack (must be strictly
	 * positive)
	 * @return value from top of stack
	 * @exception ArrayIndexOutOfBoundsException on attempt to pop past end of
	 * stack
	 */

	public Object pop(int count) {
		if (count <= 0) {
			throw new IllegalArgumentException("Count must be greater than 0");
		} else if (m_countPresent >= count) {
			m_countPresent -= count;
			Object value = m_baseArray[m_countPresent];
			discardValues(m_countPresent, m_countPresent + count);
			return value;
		} else {
			throw new ArrayIndexOutOfBoundsException
				("Attempt to pop past end of stack");
		}
	}

	/**
	 * Copy a value from the stack. This returns a value from within
	 * the stack without modifying the stack.
	 *
	 * @param depth depth of value to be returned
	 * @return value from stack
	 * @exception ArrayIndexOutOfBoundsException on attempt to peek past end of
	 * stack
	 */

	public Object peek(int depth) {
		if (m_countPresent > depth) {
			return m_baseArray[m_countPresent - depth - 1];
		} else {
			throw new ArrayIndexOutOfBoundsException
				("Attempt to peek past end of stack");
		}
	}

	/**
	 * Copy top value from the stack. This returns the top value without
	 * removing it from the stack.
	 *
	 * @return value at top of stack
	 * @exception ArrayIndexOutOfBoundsException on attempt to peek empty stack
	 */

	public Object peek() {
		return peek(0);
	}

	/**
	 * Return an iterator for the <code>Object</code>s in this stack. The
	 * iterator returns all values in order the order they were added, but is
	 * not "live". Values added to the stack during iteration will not be
	 * returned by the iteration, and any other changes to the stack while the
	 * iteration is in progress will give indeterminant results.
	 *
	 * @return iterator for values in stack
	 */

	public final Iterator iterator() {
		return ArrayRangeIterator.buildIterator(m_baseArray, 0, m_countPresent);
	}

	/**
	 * Constructs and returns a simple array containing the same data as held
	 * in this stack. Note that the items will be in reverse pop order, with
	 * the last item to be popped from the stack as the first item in the
	 * array.
	 *
	 * @return array containing a copy of the data
	 */

	public Object[] toArray() {
		return (Object[]) buildArray(Object.class);
	}

	/**
	 * Constructs and returns a type-specific array containing the same data
	 * as held in this growable generic array. All values in this array must
	 * be assignment compatible with the specified type. Note that the items
	 * will be in reverse pop order, with the last item to be popped from the
	 * stack as the first item in the array.
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
		return new ObjectStack(this);
	}
}
