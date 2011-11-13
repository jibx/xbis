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

package com.sosnoski.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator class for values contained in a possibly wrapped array range. This
 * type of iterator can be used for items in an object array which are
 * contiguous.modulo the array size.
 *
 * @author Dennis M. Sosnoski
 * @version 1.1
 */

public class WrappedArrayIterator implements Iterator
{
	/** Array supplying values for iteration. */
	protected Object[] m_array;

	/** Offset of next iteration value. */
	protected int m_offset;

	/** Ending offset for values. */
	protected int m_last;

	/**
	 * Internal constructor.
	 *
	 * @param array array containing values to be iterated
	 * @param start offset of first value in array
	 * @param end offset of last value in array
	 */

	private WrappedArrayIterator(Object[] array, int start, int end) {
		m_array = array;
		m_offset = start;
		m_last = end;
	}

	/**
	 * Check for iteration element available.
	 *
	 * @return <code>true</code> if element available, <code>false</code> if
	 * not
	 */

	public boolean hasNext() {
		return m_offset != m_last;
	}

	/**
	 * Get next iteration element.
	 *
	 * @return next iteration element
	 * @exception NoSuchElementException if past end of iteration
	 */

	public Object next() {
		if (m_offset != m_last) {
			Object result = m_array[m_offset];
			m_offset = (m_offset+1) % m_array.length;
			return result;
		} else {
			throw new NoSuchElementException();
		}
	}

	/**
	 * Remove element from iteration. This optional operation is not supported
	 * and always throws an exception.
	 *
	 * @exception UnsupportedOperationException for unsupported operation
	 */

	public void remove() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Build iterator.
	 *
	 * @param array array containing values to be iterated (may be
	 * <code>null</code>)
	 * @param start offset of first value in array
	 * @param end offset of last value in array (the same as start
	 * offset, if the array is empty)
	 * @return constructed iterator
	 */

	public static Iterator buildIterator(Object[] array, int start, int end) {
		if (array == null || start == end) {
			return ArrayRangeIterator.EMPTY_ITERATOR;
		} else {
			return new WrappedArrayIterator(array, start, end);
		}
	}
}
