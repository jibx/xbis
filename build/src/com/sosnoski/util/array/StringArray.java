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

package com.sosnoski.util.array;

import java.util.Iterator;

import com.sosnoski.util.ArrayRangeIterator;

/**
 * Growable <code>String</code> array with type specific access methods. This
 * implementation is unsynchronized in order to provide the best possible
 * performance for typical usage scenarios, so explicit synchronization must
 * be implemented by a wrapper class or directly by the application in cases
 * where instances are modified in a multithreaded environment. See the base
 * classes for other details of the implementation.
 *
 * @author Dennis M. Sosnoski
 * @version 1.1
 */

public class StringArray extends ArrayBase
{
	/** The underlying array used for storing the data. */
	protected String[] m_baseArray;

	/**
	 * Constructor with full specification.
	 *
	 * @param size number of <code>String</code> values initially allowed in
	 * array
	 * @param growth maximum size increment for growing array
	 */

	public StringArray(int size, int growth) {
		super(size, growth, String.class);
	}

	/**
	 * Constructor with initial size specified.
	 *
	 * @param size number of chars to size array for initially
	 */

	public StringArray(int size) {
		super(size, String.class);
	}

	/**
	 * Default constructor.
	 */

	public StringArray() {
		this(DEFAULT_SIZE);
	}

	/**
	 * Copy (clone) constructor.
	 *
	 * @param base instance being copied
	 */

	public StringArray(StringArray base) {
		super(base);
	}

	/**
	 * Get the backing array. This method is used by the type-agnostic base
	 * class code to access the array used for type-specific storage.
	 *
	 * @return backing array object
	 */

	protected final Object getArray() {
		return m_baseArray;
	}

	/**
	 * Set the backing array. This method is used by the type-agnostic base
	 * class code to set the array used for type-specific storage.
	 *
	 * @param backing array object
	 */

	protected final void setArray(Object array) {
		m_baseArray = (String[])array;
	}

	/**
	 * Add a value to the array, appending it after the current values.
	 *
	 * @param value value to be added
	 * @return index number of added element
	 */

	public final int add(String value) {
		int index = getAddIndex();
		m_baseArray[index] = value;
		return index;
	}

	/**
	 * Add a value at a specified index in the array.
	 *
	 * @param index index position at which to insert element
	 * @param value value to be inserted into array
	 */

	public void add(int index, String value) {
		makeInsertSpace(index);
		m_baseArray[index] = (String)value;
	}

	/**
	 * Retrieve the value present at an index position in the array.
	 *
	 * @param index index position for value to be retrieved
	 * @return value from position in the array
	 */

	public final String get(int index) {
		if (index < m_countPresent) {
			return m_baseArray[index];
		} else {
			throw new ArrayIndexOutOfBoundsException("Invalid index value");
		}
	}

	/**
	 * Set the value at an index position in the array. Note that this
	 * method can only be used for modifying an existing value in the
	 * array, not for appending to the end of the array.
	 *
	 * @param index index position to be set
	 * @param value value to be set
	 * @see #add
	 */

	public final void set(int index, String value) {
		if (index < m_countPresent) {
			m_baseArray[index] = value;
		} else {
			throw new ArrayIndexOutOfBoundsException("Invalid index value");
		}
	}

	/**
	 * Return an iterator for the <code>String</code>s in this array. The
	 * iterator returns all values in order, but is not "live". Values
	 * added to the array during iteration will not be returned by the
	 * iteration, and any other changes to the array while the iteration is
	 * in progress will give indeterminant results.
	 *
	 * @return iterator for values in array
	 */

	public final Iterator iterator() {
		return ArrayRangeIterator.buildIterator(m_baseArray, 0, m_countPresent);
	}

	/**
	 * Constructs and returns a simple array containing the same data as held
	 * in this growable array.
	 *
	 * @return array containing a copy of the data
	 */

	public String[] toArray() {
		return (String[])buildArray(String.class, 0, m_countPresent);
	}

	/**
	 * Constructs and returns a simple array containing the same data as held
	 * in a portion of this growable array.
	 *
	 * @param offset start offset in array
	 * @param length number of characters to use
	 * @return array containing a copy of the data
	 */

	public String[] toArray(int offset, int length) {
		return (String[])buildArray(String.class, offset, length);
	}

	/**
	 * Duplicates the object with the generic call.
	 *
	 * @return a copy of the object
	 */

	public Object clone() {
		return new StringArray(this);
	}
}
