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

import com.sosnoski.util.*;

/**
 * Base class for type-specific stack classes with any type of values 
 * (including primitive types). This class builds on the basic structure 
 * provided by <code>GrowableBase</code>, specializing it for usage as a
 * stack. See the base class description for details of the implementation.<p>
 *
 * Stacks based on this class are unsynchronized in order to provide the
 * best possible performance for typical usage scenarios, so explicit
 * synchronization must be implemented by the subclass or the application in
 * cases where they are to be modified in a multithreaded environment.<p>
 *
 * Subclasses need to implement the abstract methods defined by the base class
 * for working with the data array, as well as the actual data access methods
 * (at least the basic <code>push()</code>, <code>pop()</code>, and
 * <code>peek()</code> methods).
 * 
 * @author Dennis M. Sosnoski
 * @version 1.0
 */

public abstract class StackBase extends GrowableBase
{
	/** The number of values currently present in the stack. */
	protected int m_countPresent;

	/**
	 * Constructor with full specification.
	 * 
	 * @param size number of elements initially allowed in stack
	 * @param growth maximum size increment for growing stack
	 * @param type stack element type
	 */
	
	public StackBase(int size, int growth, Class type) {
		super(size, growth, type);
	}

	/**
	 * Constructor with partial specification.
	 * 
	 * @param size number of elements initially allowed in stack
	 * @param type stack element type
	 */
	
	public StackBase(int size, Class type) {
		this(size, Integer.MAX_VALUE, type);
	}

	/**
	 * Copy (clone) constructor.
	 * 
	 * @param base instance being copied
	 */
	
	public StackBase(StackBase base) {
		super(base);
		System.arraycopy(base.getArray(), 0, getArray(), 0, 
			base.m_countPresent);
		m_countPresent = base.m_countPresent;
	}

	/**
	 * Gets the array offset for appending a value to those in the stack.
	 * If the underlying array is full, it is grown by the appropriate size
	 * increment so that the index value returned is always valid for the 
	 * array in use by the time of the return.
	 * 
	 * @return index position for added element
	 */
	
	protected int getAddIndex() {
		int index = m_countPresent++;
		if (m_countPresent > m_countLimit) {
			growArray(m_countPresent);
		}
		return index;
	}

	/**
	 * Get the number of values currently present in the stack.
	 * 
	 * @return count of values present
	 */
	
	public int size() {
		return m_countPresent;
	}

	/**
	 * Check if stack is empty.
	 * 
	 * @return <code>true</code> if stack empty, <code>false</code> if not
	 */
	
	public boolean isEmpty() {
		return m_countPresent == 0;
	}

	/**
	 * Set the stack to the empty state.
	 */
	
	public void clear() {
		discardValues(0, m_countPresent);
		m_countPresent = 0;
	}

	/**
	 * Constructs and returns a simple array containing the same data as held 
	 * in this stack. Note that the items will be in reverse pop order, with 
	 * the last item to be popped from the stack as the first item in the 
	 * array.
	 * 
	 * @param type element type for constructed array
	 * @return array containing a copy of the data
	 */
	
	protected Object buildArray(Class type) {
		return super.buildArray(type, 0, m_countPresent);
	}
}
