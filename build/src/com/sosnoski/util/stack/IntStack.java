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

/**
 * Growable <code>int</code> stack with type specific access methods. This
 * implementation is unsynchronized in order to provide the best possible
 * performance for typical usage scenarios, so explicit synchronization must
 * be implemented by a wrapper class or directly by the application in cases
 * where instances are modified in a multithreaded environment. See the base
 * classes for other details of the implementation.
 *
 * @author Dennis M. Sosnoski
 * @version 1.0
 */

public class IntStack extends StackBase
{
	/** The underlying array used for storing the data. */
	protected int[] m_baseArray;

	/**
	 * Constructor with full specification.
	 *
	 * @param size number of <code>int</code> values initially allowed in
	 * stack
	 * @param growth maximum size increment for growing stack
	 */

	public IntStack(int size, int growth) {
		super(size, growth, int.class);
	}

	/**
	 * Constructor with initial size specified.
	 *
	 * @param size number of <code>int</code> values initially allowed in
	 * stack
	 */

	public IntStack(int size) {
		super(size, int.class);
	}

	/**
	 * Default constructor.
	 */

	public IntStack() {
		this(DEFAULT_SIZE);
	}

	/**
	 * Copy (clone) constructor.
	 *
	 * @param base instance being copied
	 */

	public IntStack(IntStack base) {
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
	 * Push a value on the stack.
	 *
	 * @param value value to be added
	 */

	public void push(int value) {
		int index = getAddIndex();
		m_baseArray[index] = value;
	}

	/**
	 * Pop a value from the stack.
	 *
	 * @return value from top of stack
	 * @exception ArrayIndexOutOfBoundsException on attempt to pop empty stack
	 */

	public int pop() {
		if (m_countPresent > 0) {
			return m_baseArray[--m_countPresent];
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

	public int pop(int count) {
		if (count <= 0) {
			throw new IllegalArgumentException("Count must be greater than 0");
		} else if (m_countPresent >= count) {
			m_countPresent -= count;
			return m_baseArray[m_countPresent];
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

	public int peek(int depth) {
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

	public int peek() {
		return peek(0);
	}

	/**
	 * Constructs and returns a simple array containing the same data as held
	 * in this stack. Note that the items will be in reverse pop order, with
	 * the last item to be popped from the stack as the first item in the
	 * array.
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
		return new IntStack(this);
	}
}
