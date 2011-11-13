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

import java.util.Iterator;

import com.sosnoski.util.SparseArrayIterator;

/**
 * Hash map using primitive <code>int</code> values as keys mapped to
 * <code>String</code> values. This implementation is unsynchronized in order
 * to provide the best possible performance for typical usage scenarios, so
 * explicit synchronization must be implemented by a wrapper class or directly
 * by the application in cases where instances are modified in a multithreaded
 * environment. See the base classes for other details of the
 * hash map implementation.
 *
 * @author Dennis M. Sosnoski
 * @version 1.1
 */

public class IntStringHashMap extends PrimitiveKeyBase
{
	/** Array of key table slots. */
	protected int[] m_keyTable;

	/** Array of value table slots. */
	protected String[] m_valueTable;

	/**
	 * Constructor with full specification.
	 *
	 * @param count number of values to assume in initial sizing of table
	 * @param fill fraction full allowed for table before growing
	 */

	public IntStringHashMap(int count, double fill) {
		super(count, fill, int.class, String.class);
	}

	/**
	 * Constructor with only size supplied. Uses default value for fill
	 * fraction.
	 *
	 * @param count number of values to assume in initial sizing of table
	 */

	public IntStringHashMap(int count) {
		this(count, DEFAULT_FILL);
	}

	/**
	 * Default constructor.
	 */

	public IntStringHashMap() {
		this(0, DEFAULT_FILL);
	}

	/**
	 * Copy (clone) constructor.
	 *
	 * @param base instance being copied
	 */

	public IntStringHashMap(IntStringHashMap base) {
		super(base);
	}

	/**
	 * Get the backing array of keys. This implementation of an abstract
	 * method is used by the type-agnostic base class code to access the
	 * array used for type-specific storage by the child class.
	 *
	 * @return backing key array object
	 */

	protected final Object getKeyArray() {
		return m_keyTable;
	}

	/**
	 * Set the backing array of keys. This implementation of an abstract
	 * method is used by the type-agnostic base class code to set the
	 * array used for type-specific storage by the child class.
	 *
	 * @param array backing key array object
	 */

	protected final void setKeyArray(Object array) {
		m_keyTable = (int[])array;
	}

	/**
	 * Get the backing array of values. This implementation of an abstract
	 * method is used by the type-agnostic base class code to access the
	 * array used for type-specific storage by the child class.
	 *
	 * @return backing key array object
	 */

	protected final Object getValueArray() {
		return m_valueTable;
	}

	/**
	 * Set the backing array of values. This implementation of an abstract
	 * method is used by the type-agnostic base class code to set the
	 * array used for type-specific storage by the child class.
	 *
	 * @param array backing value array object
	 */

	protected final void setValueArray(Object array) {
		m_valueTable = (String[])array;
	}

	/**
	 * Reinsert an entry into the hash map. This method is designed for
	 * internal use when the table is being modified, and does not adjust
	 * the count present or check the table capacity.
	 *
	 * @param slot position of entry to be reinserted into hash map
	 * @return <code>true</code> if the slot number used by the entry has
	 * has changed, <code>false</code> if not
	 */

	protected final boolean reinsert(int slot) {
		m_flagTable[slot] = false;
		return assignSlot(m_keyTable[slot], m_valueTable[slot]) != slot;
	}

	/**
	 * Restructure the table. This implementation of an abstract method is
	 * used when the table is increasing or decreasing in size,
	 * and works directly with the old table representation arrays. It
	 * inserts pairs from the old arrays directly into the table without
	 * adjusting the count present or checking the table size.
	 *
	 * @param flags array of flags for array slots used
	 * @param karray array of keys
	 * @param varray array of values
	 */

	protected void restructure(boolean[] flags, Object karray, Object varray) {
		int[] keys = (int[])karray;
		String[] values = (String[])varray;
		for (int i = 0; i < flags.length; i++) {
			if (flags[i]) {
				assignSlot(keys[i], values[i]);
			}
		}
	}

	/**
	 * Compute the base slot for a key.
	 *
	 * @param key key value to be computed
	 * @return base slot for key
	 */

	protected final int computeSlot(int key) {
		return (key * KEY_MULTIPLIER & Integer.MAX_VALUE) % m_flagTable.length;
	}

	/**
	 * Assign slot for entry. Starts at the slot found by the hashed key
	 * value. If this slot is already occupied, it steps the slot number and
	 * checks the resulting slot, repeating until an unused slot is found. This
	 * method does not check for duplicate keys, so it should only be used for
	 * internal reordering of the tables.
	 *
	 * @param key key to be added to table
	 * @param value associated value for key
	 * @return slot at which entry was added
	 */

	protected int assignSlot(int key, String value) {
		int offset = freeSlot(computeSlot(key));
		m_flagTable[offset] = true;
		m_keyTable[offset] = key;
		m_valueTable[offset] = value;
		return offset;
	}

	/**
	 * Add an entry to the table. If the key is already present in the table,
	 * this replaces the existing value associated with the key.
	 *
	 * @param key key to be added to table
	 * @param value associated value for key
	 * @return value previously associated with key, <code>null</code> if
	 * key not previously present in table
	 */

	public String add(int key, String value) {
		ensureCapacity(m_entryCount+1);
		int offset = internalFind(key);
		if (offset >= 0) {
			String prior = m_valueTable[offset];
			m_valueTable[offset] = value;
			return prior;
		} else {
			m_entryCount++;
			offset = -offset - 1;
			m_flagTable[offset] = true;
			m_keyTable[offset] = key;
			m_valueTable[offset] = value;
			return null;
		}
	}

	/**
	 * Internal find key in table.
	 *
	 * @param key to be found in table
	 * @return index of matching key, or <code>-index-1</code> of slot to be
	 * used for inserting key in table if not already present (always negative)
	 */

	protected final int internalFind(int key) {
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
	 * Check if an entry is present in the table. This method is supplied to
	 * support the use of <code>null</code> values in the table.
	 *
	 * @param key key for entry to be found
	 * @return <code>true</code> if key found in table, <code>false</code>
	 * if not
	 */

	public final boolean containsKey(int key) {
		return internalFind(key) >= 0;
	}

	/**
	 * Find an entry in the table.
	 *
	 * @param key key for entry to be returned
	 * @return value for key, or <code>null</code> if key not found
	 */

	public final String get(int key) {
		int slot = internalFind(key);
		if (slot >= 0) {
			return m_valueTable[slot];
		} else {
			return null;
		}
	}

	/**
	 * Remove an entry from the table.
	 *
	 * @param key key to be removed from table
	 * @return value associated with removed key, <code>null</code> if key
	 * not found in table
	 */

	public String remove(int key) {
		int slot = internalFind(key);
		if (slot >= 0) {
			String value = m_valueTable[slot];
			m_flagTable[slot] = false;
			m_entryCount--;
			while (m_flagTable[(slot = stepSlot(slot))]) {
				reinsert(slot);
			}
			return value;
		} else {
			return null;
		}
	}

	/**
	 * Return an iterator for the value <code>Object</code>s in this map. The
	 * iterator returns all values in arbitrary order, but is not "live". Any
	 * changes to the map while the iteration is in progress will give
	 * indeterminant results.
	 *
	 * @return iterator for values in array
	 */

	public final Iterator valueIterator() {
		return SparseArrayIterator.buildIterator(m_valueTable);
	}

	/**
	 * Construct a copy of the table.
	 *
	 * @return shallow copy of table
	 */

	public Object clone() {
		return new IntStringHashMap(this);
	}
}
