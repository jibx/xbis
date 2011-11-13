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

/**
 * Hash set using primitive <code>double</code> values. This implementation is 
 * unsynchronized in order to provide the best possible performance for typical 
 * usage scenarios, so explicit synchronization must be implemented by a wrapper 
 * class or directly by the application in cases where instances are modified 
 * in a multithreaded environment. See the base classes for other details of 
 * the hash set implementation.<p>
 *
 * Note that the hashing function implemented in this class has not been tested
 * extensively and may be unsuitable for use with some patterns of data values.
 * To change the hashing function, modify the <code>computeSlot()</code>
 * method. 
 * 
 * @author Dennis M. Sosnoski
 * @version 1.0
 */

public class DoubleHashSet extends PrimitiveSetBase
{
	/** Array of key table slots. */
	protected double[] m_keyTable;

	/**
	 * Constructor with full specification.
	 * 
	 * @param count number of values to assume in initial sizing of table
	 * @param fill fraction full allowed for table before growing
	 */
	
	public DoubleHashSet(int count, double fill) {
		super(count, fill, double.class);
	}

	/**
	 * Constructor with only size supplied. Uses default value for fill
	 * fraction.
	 * 
	 * @param count number of values to assume in initial sizing of table
	 */
	
	public DoubleHashSet(int count) {
		this(count, DEFAULT_FILL);
	}

	/**
	 * Default constructor.
	 */
	
	public DoubleHashSet() {
		this(0, DEFAULT_FILL);
	}

	/**
	 * Copy (clone) constructor.
	 * 
	 * @param base instance being copied
	 */
	
	public DoubleHashSet(DoubleHashSet base) {
		super(base);
	}

	/**
	 * Get the backing array of keys. This implementation of an abstract
	 * method is used by the type-agnostic base class code to access the
	 * array used for type-specific storage by the child class.
	 * 
	 * @return backing key array object
	 */
	
	protected Object getKeyArray() {
		return m_keyTable;
	}

	/**
	 * Set the backing array of keys. This implementation of an abstract
	 * method is used by the type-agnostic base class code to set the
	 * array used for type-specific storage by the child class.
	 * 
	 * @param array backing key array object
	 */
	
	protected void setKeyArray(Object array) {
		m_keyTable = (double[])array;
	}

	/**
	 * Reinsert a key into the hash set. This method is designed for 
	 * internal use when the table is being modified, and does not adjust
	 * the count present or check the table capacity.
	 * 
	 * @param slot position of key to be reinserted into hash set
	 * @return <code>true</code> if the slot number used by the key has
	 * has changed, <code>false</code> if not
	 */
	
	protected final boolean reinsert(int slot) {
		m_flagTable[slot] = false;
		return assignSlot(m_keyTable[slot]) != slot;
	}

	/**
	 * Restructure the table. This implementation of an abstract method is 
	 * used when the table is increasing or decreasing in size,
	 * and works directly with the old table representation arrays. It
	 * inserts keys from the old array directly into the table without
	 * adjusting the count present or checking the table size.
	 * 
	 * @param flags array of flags for array slots used
	 * @param karray array of keys
	 */
	
	protected void restructure(boolean[] flags, Object karray) {
		double[] keys = (double[])karray;
		for (int i = 0; i < flags.length; i++) {
			if (flags[i]) {
				assignSlot(keys[i]);
			}
		}
	}

	/**
	 * Compute the base slot for a key.
	 * 
	 * @param key key value to be computed
	 * @return base slot for key
	 */
	
	protected final int computeSlot(double key) {
		long bits = Double.doubleToRawLongBits(key);
		int reduced = ((int)(bits >> 48)) + ((int)(bits >> 24)) + (int)bits;
		return (reduced * KEY_MULTIPLIER & Integer.MAX_VALUE) % 
			m_flagTable.length;
	}

	/**
	 * Assign slot for key. Starts at the slot found by the hashed key
	 * value. If this slot is already occupied, it steps the slot number and
	 * checks the resulting slot, repeating until an unused slot is found. This
	 * method does not check for duplicate keys, so it should only be used for
	 * internal reordering of the tables.
	 * 
	 * @param key key to be added to table
	 * @return slot at which key was added
	 */
	
	protected int assignSlot(double key) {
		int offset = freeSlot(computeSlot(key));
		m_flagTable[offset] = true;
		m_keyTable[offset] = key;
		return offset;
	}

	/**
	 * Add a key to the table.
	 * 
	 * @param key key to be added to table
	 * @return <code>true</code> if key added to set, <code>false</code> if
	 * already present in set
	 */
	
	public boolean add(double key) {
		ensureCapacity(m_entryCount+1);
		int offset = -internalFind(key) - 1;
		if (offset >= 0) {
			m_entryCount++;
			m_flagTable[offset] = true;
			m_keyTable[offset] = key;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Internal find key in table.
	 * 
	 * @param key to be found in table
	 * @return index of matching key, or <code>-index-1</code> of slot to be
	 * used for inserting key in table if not already present (always negative)
	 */
	
	protected final int internalFind(double key) {
		int slot = computeSlot(key);
		while (m_flagTable[slot]) {
			if (key == m_keyTable[slot]) {
				return slot;
			}
			slot = stepSlot(slot);
		}
		return -slot - 1;
	}

	/**
	 * Check if a key is present in the set.
	 * 
	 * @param key key to be found
	 * @return <code>true</code> if key found in table, <code>false</code>
	 * if not
	 */
	
	public boolean contains(double key) {
		return internalFind(key) >= 0;
	}

	/**
	 * Remove a key from the table.
	 * 
	 * @param key key to be removed from table
	 * @return <code>true</code> if key successfully removed from set,
	 * <code>false</code> if key not found in set
	 */
	
	public boolean remove(double key) {
		int slot = internalFind(key);
		if (slot >= 0) {
			m_flagTable[slot] = false;
			m_entryCount--;
			while (m_flagTable[(slot = stepSlot(slot))]) {
				reinsert(slot);
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Construct a copy of the table.
	 * 
	 * @return copy of table
	 */
	
	public Object clone() {
		return new DoubleHashSet(this);
	}
}
