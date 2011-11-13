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

/**
 * Hash map using <code>String</code> values as keys mapped to primitive
 * <code>int</code> values. This implementation is unsynchronized
 * in order to provide the best possible performance for typical usage
 * scenarios, so explicit synchronization must be implemented by a wrapper
 * class or directly by the application in cases where instances are modified
 * in a multithreaded environment. See the base classes for other details of
 * the implementation.
 *
 * @author Dennis M. Sosnoski
 * @version 1.1
 */

public class CharBlockIntHashMap extends ObjectKeyBase
{
	/** Default value returned when key not found in table. */
	public static final int DEFAULT_NOT_FOUND = Integer.MIN_VALUE;

	/** Array of key table slots. */
	protected char[][] m_keyTable;

	/** Array of value table slots. */
	protected int[] m_valueTable;

	/** Value returned when key not found in table. */
	protected int m_notFoundValue;

	/**
	 * Constructor with full specification.
	 *
	 * @param count number of values to assume in initial sizing of table
	 * @param fill fraction full allowed for table before growing
	 * @param miss value returned when key not found in table
	 */

	public CharBlockIntHashMap(int count, double fill, int miss) {
		super(count, fill, char[].class, int.class, STANDARD_HASH);
		m_notFoundValue = miss;
	}

	/**
	 * Constructor with size and fill fraction specified. Uses default hash
	 * technique and value returned when key not found in table.
	 *
	 * @param count number of values to assume in initial sizing of table
	 * @param fill fraction full allowed for table before growing
	 */

	public CharBlockIntHashMap(int count, double fill) {
		this(count, fill, DEFAULT_NOT_FOUND);
	}

	/**
	 * Constructor with only size supplied. Uses default hash technique and
	 * values for fill fraction and value returned when key not found in table.
	 *
	 * @param count number of values to assume in initial sizing of table
	 */

	public CharBlockIntHashMap(int count) {
		this(count, DEFAULT_FILL);
	}

	/**
	 * Default constructor.
	 */

	public CharBlockIntHashMap() {
		this(0, DEFAULT_FILL);
	}

	/**
	 * Copy (clone) constructor.
	 *
	 * @param base instance being copied
	 */

	public CharBlockIntHashMap(CharBlockIntHashMap base) {
		super(base);
		m_notFoundValue = base.m_notFoundValue;
	}

	/**
	 * Get the backing array of keys. This implementation of an abstract
	 * method is used by the type-agnostic base class code to access the
	 * array used for type-specific storage by the child class.
	 *
	 * @return backing key array object
	 */

	protected final Object[] getKeyArray() {
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
		m_keyTable = (char[][])array;
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
		m_valueTable = (int[])array;
	}

	/**
	 * Reinsert an entry into the hash map. This implementation of an abstract
	 * method is used when the table is being directly modified by
	 * the base class, and does not adjust the count present or check the table
	 * capacity.
	 *
	 * @param slot position of entry to be reinserted into hash map
	 * @return <code>true</code> if the slot number used by the entry has
	 * has changed, <code>false</code> if not
	 */

	protected final boolean reinsert(int slot) {
		char[] key = m_keyTable[slot];
		m_keyTable[slot] = null;
		return assignSlot(key, m_valueTable[slot]) != slot;
	}

	/**
	 * Restructure the table. This implementation of an abstract method is
	 * used when the table is increasing or decreasing in size,
	 * and works directly with the old table representation arrays. It
	 * inserts pairs from the old arrays directly into the table without
	 * adjusting the count present or checking the table size.
	 *
	 * @param karray array of keys
	 * @param varray array of values
	 */

	protected void restructure(Object karray, Object varray) {
		char[][] keys = (char[][])karray;
		int[] values = (int[])varray;
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] != null) {
				assignSlot(keys[i], values[i]);
			}
		}
	}

	/**
	 * Assign slot for entry. Starts at the slot found by the hashed key
	 * value. If this slot is already occupied, it steps the slot number and
	 * checks the resulting slot, repeating until an unused slot is found. This
	 * method does not check for duplicate keys, so it should only be used for
	 * internal reordering of the tables.
	 *
	 * @param key to be added to table
	 * @param value associated value for key
	 * @return slot at which entry was added
	 */

	protected int assignSlot(char[] key, int value) {
		int offset = freeSlot(specialSlot(key, 0, key.length));
		m_keyTable[offset] = key;
		m_valueTable[offset] = value;
		return offset;
	}

	/**
	 * Base slot computation for a character array key.
	 *
	 * @param buff array containing characters of key
     * @param offset starting offset in buffer
     * @param length number of characters in key
	 * @return base slot for key
	 */

	protected final int specialSlot(char[] buff, int offset, int length) {
		int accum = 0;
        int limit = offset + length;
        while (offset < limit) {
			accum = (accum + 13) * 43 + buff[offset++];
		}
		return (accum & Integer.MAX_VALUE) % m_arraySize;
	}

	/**
	 * Character array find key in table.
	 *
	 * @param buff array containing characters of key
     * @param offset starting offset in buffer
     * @param length number of characters in key
	 * @return index of matching key, or <code>-index-1</code> of slot to be
	 * used for inserting key in table if not already present (always negative)
	 */

	protected int specialFind(char[] buff, int offset, int length) {

		// find the starting point for searching table
		int slot = specialSlot(buff, offset, length);

		// scan through table to find target key
		while (m_keyTable[slot] != null) {

			// check if we have a match on target key
			if (m_keyTable[slot].length == length) {
				boolean match = true;
				char[] comp = m_keyTable[slot];
				for (int i = 0; i < length; i++) {
					if (comp[i] != buff[offset+i]) {
						match = false;
						break;
					}
				}
				if (match) {
					return slot;
				}
			}
			
			// advance to next slot of table
			slot = stepSlot(slot);

		}
		return -slot-1;
	}

	/**
	 * Add an entry to the table. If the key is already present in the table,
	 * this replaces the existing value associated with the key.
	 *
	 * @param buff array containing characters of key
     * @param offset starting offset in buffer
     * @param length number of characters in key
	 * @param value associated value for key
	 * @return value previously associated with key, or reserved not found
	 * value if key not previously present in table
	 */

	public int add(char[] buff, int offset, int length, int value) {

		// first validate the parameters
		if (value == m_notFoundValue) {
			throw new IllegalArgumentException
				("value matching not found return not supported");
		} else {

			// check space and duplicate key
			ensureCapacity(m_entryCount+1);
			int slot = specialFind(buff, offset, length);
			if (slot >= 0) {

				// replace existing value for key
				int prior = m_valueTable[slot];
				m_valueTable[slot] = value;
				return prior;

			} else {

				// add new pair to table
				m_entryCount++;
				slot = -slot - 1;
                char[] key = new char[length];
                System.arraycopy(buff, offset, key, 0, length);
				m_keyTable[slot] = key;
				m_valueTable[slot] = value;
				return m_notFoundValue;

			}
		}
	}

	/**
	 * Check if an entry is present in the table. This method is supplied to
	 * support the use of values matching the reserved not found value.
	 *
	 * @param buff array containing characters of key
     * @param offset starting offset in buffer
     * @param length number of characters in key
	 * @return <code>true</code> if key found in table, <code>false</code>
	 * if not
	 */

	public final boolean containsKey(char[] buff, int offset, int length) {
		return specialFind(buff, offset, length) >= 0;
	}

	/**
	 * Find an entry in the table.
	 *
	 * @param buff array containing characters of key
     * @param offset starting offset in buffer
     * @param length number of characters in key
	 * @return value for key, or reserved not found value if key not found
	 */

	public final int get(char[] buff, int offset, int length) {
		int slot = specialFind(buff, offset, length);
		if (slot >= 0) {
			return m_valueTable[slot];
		} else {
			return m_notFoundValue;
		}
	}

	/**
	 * Remove an entry from the table. If multiple entries are present with
	 * the same key value, only the first one found will be removed.
	 *
	 * @param buff array containing characters of key
     * @param offset starting offset in buffer
     * @param length number of characters in key
	 * @return value associated with removed key, or reserved not found value
	 * if key not found in table
	 */

	public int remove(char[] buff, int offset, int length) {
		int slot = specialFind(buff, offset, length);
		if (slot >= 0) {
			int value = m_valueTable[slot];
			internalRemove(slot);
			return value;
		} else {
			return m_notFoundValue;
		}
	}

	/**
	 * Construct a copy of the table.
	 *
	 * @return shallow copy of table
	 */

	public Object clone() {
		return new CharBlockIntHashMap(this);
	}
}
