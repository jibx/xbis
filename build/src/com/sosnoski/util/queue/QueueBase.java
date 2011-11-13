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

import java.lang.reflect.Array;

import com.sosnoski.util.*;

/**
 * Base class for type-specific growable circular queue classes with any type 
 * of values (including primitive types). This class builds on the basic 
 * structure provided by <code>GrowableBase</code>, specializing it for usage 
 * as a queue. See the base class description for details of the 
 * implementation.<p>
 *
 * Queues based on this class are unsynchronized in order to provide the
 * best possible performance for typical usage scenarios, so explicit
 * synchronization must be implemented by the subclass or the application in
 * cases where they are to be modified in a multithreaded environment.<p>
 *
 * Subclasses need to implement the abstract methods defined by the base class
 * for working with the data array, as well as the actual data access methods
 * (at least the basic <code>add()</code> and <code>remove()</code> methods).
 * 
 * @author Dennis M. Sosnoski
 * @version 1.0
 */

public abstract class QueueBase extends GrowableBase
{
	/** Offset for adding next item to queue. */
	protected int m_fillOffset;
	
	/** Offset for removing next item from queue. */
	protected int m_emptyOffset;

	/**
	 * Constructor with full specification.
	 * 
	 * @param size number of elements initially allowed in queue
	 * @param growth maximum size increment for growing queue
	 * @param type queue element type
	 */
	
	public QueueBase(int size, int growth, Class type) {
		super(size, growth, type);
	}

	/**
	 * Constructor with partial specification.
	 * 
	 * @param size number of elements initially allowed in queue
	 * @param type queue element type
	 */
	
	public QueueBase(int size, Class type) {
		this(size, Integer.MAX_VALUE, type);
	}

	/**
	 * Copy (clone) constructor.
	 * 
	 * @param from instance being copied
	 */
	
	public QueueBase(QueueBase base) {
		super(base);
		System.arraycopy(base.getArray(), 0, getArray(), 0, m_countLimit);
		m_fillOffset = base.m_fillOffset;
		m_emptyOffset = base.m_emptyOffset;
	}

	/**
	 * Copy data after array resize. This override of the default
	 * implementation rearranges the data present in the queue when the
	 * data was wrapped in the original array.
	 * 
	 * @param base original array containing data
	 * @param grown resized array for data
	 */
	
	protected void resizeCopy(Object base, Object grown) {
		
		// check if we have a wrapped queue
		if (m_fillOffset < m_emptyOffset) {
			
			// copy wrapped queue data to new array
			int size = Array.getLength(base);
			int length = size - m_emptyOffset;
			System.arraycopy(base, m_emptyOffset, grown, 0, length);
			System.arraycopy(base, 0, grown, length, m_fillOffset);
			m_fillOffset += size;
			
		} else {
			
			// copy single block of queue data to new array
			int length = m_fillOffset - m_emptyOffset;
			System.arraycopy(base, m_emptyOffset, grown, 0, length);
		}
		
		// set data limits in new array
		m_fillOffset -= m_emptyOffset;
		m_emptyOffset = 0;
	}

	/**
	 * Make space for adding a value to those in the queue.
	 * If the underlying array is full, it is grown by the appropriate size
	 * increment so that the index value returned is always valid for the 
	 * array in use by the time of the return.
	 * 
	 * @return index position to store added element
	 */
	
	protected int getAddIndex() {
		
		// increment fill offset in queue
		int next = (m_fillOffset + 1) % m_countLimit;
		
		// check for queue full condition
		if (next == m_emptyOffset) {
			
			// grow the underlying array
			growArray(m_countLimit + 1);
			next = m_fillOffset + 1;
		}
		
		// set next offset and return this
		int index = m_fillOffset;
		m_fillOffset = next;
		return index;
	}

	/**
	 * Removes the next item from the queue.
	 * 
	 * @return index position of removed element
	 * @exception IllegalStateException on attempt to remove an item from an
	 * empty queue
	 */
	
	protected int getRemoveIndex() {
		if (m_fillOffset == m_emptyOffset) {
			throw new IllegalStateException
				("Attempted remove from empty queue");
		} else {
			int offset = m_emptyOffset;
			m_emptyOffset = (m_emptyOffset + 1) % m_countLimit;
			return offset;
		}
	}

	/**
	 * Get the number of values currently present in the queue.
	 * 
	 * @return count of values present
	 */
	
	public int size() {
		int diff = m_fillOffset - m_emptyOffset;
		return (diff >= 0) ? diff : (diff + m_countLimit);
	}

	/**
	 * Check if queue is empty.
	 * 
	 * @return <code>true</code> if queue empty, <code>false</code> if not
	 */
	
	public boolean isEmpty() {
		return m_fillOffset == m_emptyOffset;
	}

	/**
	 * Set the queue to the empty state.
	 */
	
	public void clear() {
		if (m_fillOffset >= m_emptyOffset) {
			discardValues(m_emptyOffset, m_fillOffset);
		} else {
			discardValues(m_emptyOffset, m_countLimit);
			discardValues(0, m_fillOffset);
		}
		m_fillOffset = m_emptyOffset = 0;
	}

	/**
	 * Discard items from queue.
	 *
	 * @param count number of items to be discarded
	 * @exception IllegalStateException if attempt to discard more items than
	 * present on queue
	 */
	
	public void discard(int count) {
		if (count > size()) {
			throw new IllegalStateException
				("Attempted to discard more items than present on queue");
		} else {
			int limit = m_emptyOffset + count;
			if (limit > m_countLimit) {
				limit -= m_countLimit;
				discardValues(m_emptyOffset, m_countLimit);
				discardValues(0, limit);
			} else {
				discardValues(m_emptyOffset, limit);
			}
			m_emptyOffset = limit;
		}
	}

	/**
	 * Constructs and returns a simple array containing the same data as held 
	 * in this queue.
	 * 
	 * @param type element type for constructed array
	 * @return array containing a copy of the data
	 */
	
	protected Object buildArray(Class type) {
		int length = size();
		Object copy = Array.newInstance(type, length);
		if (m_fillOffset >= m_emptyOffset) {
			System.arraycopy(getArray(), m_emptyOffset, copy, 0, length);
		} else {
			int offset = m_countLimit - m_emptyOffset;
			System.arraycopy(getArray(), m_emptyOffset, copy, 0, offset);
			System.arraycopy(getArray(), 0, copy, offset, m_fillOffset);
		}
		return copy;
	}
}
