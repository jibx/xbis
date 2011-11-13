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
 * Hash set of <code>Object</code>s. This implementation is unsynchronized
 * in order to provide the best possible performance for typical usage
 * scenarios, so explicit synchronization must be implemented by a wrapper
 * class or directly by the application in cases where instances are modified
 * in a multithreaded environment. See the base classes for other details of
 * the implementation.
 *
 * @author Dennis M. Sosnoski
 * @version 1.1
 */

public class ObjectHashSet extends ObjectSetBase
{
	/** Array of value table slots. */
	protected Object[] m_keyTable;

	/**
	 * Constructor with full specification.
	 *
	 * @param count number of values to assume in initial sizing of set
	 * @param fill fraction full allowed for set before growing
	 * @param tech hash technique specifier (one of STANDARD_HASH,
	 * IDENTITY_COMP, or IDENTITY_HASH, inherited from
	 * {@link com.sosnoski.util.ObjectHashBase})
	 */

	public ObjectHashSet(int count, double fill, Object tech) {
		super(count, fill, Object.class, tech);
	}

	/**
	 * Constructor with size and fill fraction supplied. Uses default value for
	 * hash technique.
	 *
	 * @param count number of values to assume in initial sizing of set
	 * @param fill fraction full allowed for set before growing
	 */

	public ObjectHashSet(int count, double fill) {
		this(count, fill, STANDARD_HASH);
	}

	/**
	 * Constructor with size and technique supplied. Uses default value for fill
	 * fraction.
	 *
	 * @param count number of values to assume in initial sizing of set
	 * @param tech hash technique specifier (one of STANDARD_HASH,
	 * IDENTITY_COMP, or IDENTITY_HASH, inherited from
	 * {@link com.sosnoski.util.ObjectHashBase})
	 */

	public ObjectHashSet(int count, Object tech) {
		this(count, DEFAULT_FILL, tech);
	}

	/**
	 * Constructor with only size supplied. Uses default value for fill
	 * fraction and hash technique.
	 *
	 * @param count number of values to assume in initial sizing of set
	 */

	public ObjectHashSet(int count) {
		this(count, DEFAULT_FILL, STANDARD_HASH);
	}

	/**
	 * Constructor with only technique supplied. Uses default value for fill
	 * fraction.
	 *
	 * @param tech hash technique specifier (one of STANDARD_HASH,
	 * IDENTITY_COMP, or IDENTITY_HASH, inherited from
	 * {@link com.sosnoski.util.ObjectHashBase})
	 */

	public ObjectHashSet(Object tech) {
		this(0, DEFAULT_FILL, tech);
	}

	/**
	 * Default constructor.
	 */

	public ObjectHashSet() {
		this(0, DEFAULT_FILL, STANDARD_HASH);
	}

	/**
	 * Copy (clone) constructor.
	 *
	 * @param base instance being copied
	 */

	public ObjectHashSet(ObjectHashSet base) {
		super(base);
	}

	/**
	 * Get the backing array of keys. This implementation of an abstract
	 * method is used by the type-agnostic base class code to access the
	 * array used for type-specific storage by the child class.
	 *
	 * @return backing key array object
	 */

	protected Object[] getKeyArray() {
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
		m_keyTable = (Object[])array;
	}

	/**
	 * Reinsert a key into the hash set. This implementation of an abstract
	 * method is used when the set is being directly modified by
	 * the base class, and does not adjust the count present or check the set
	 * capacity.
	 *
	 * @param slot position of key to be reinserted into hash set
	 * @return <code>true</code> if the slot number used by the key has
	 * has changed, <code>false</code> if not
	 */

	protected boolean reinsert(int slot) {
		Object key = m_keyTable[slot];
		m_keyTable[slot] = null;
		return assignSlot(key) != slot;
	}

	/**
	 * Restructure the set. This implementation of an abstract method is
	 * used when the set is increasing or decreasing in size,
	 * and works directly with the old set representation array. It
	 * inserts keys from the old array directly into the set without
	 * adjusting the count present or checking the set size.
	 *
	 * @param karray array of keys
	 */

	protected void restructure(Object karray) {
		Object[] keys = (Object[])karray;
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] != null) {
				assignSlot(keys[i]);
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
	 * @param key key to be added to set
	 * @return slot at which key was added
	 */

	protected int assignSlot(Object key) {
		int offset = freeSlot(standardSlot(key));
		m_keyTable[offset] = key;
		return offset;
	}

	/**
	 * Add a key to the set.
	 *
	 * @param key key to be added to set
	 * @return <code>true</code> if key added to set, <code>false</code> if
	 * already present in set
	 */

	public boolean add(Object key) {
		ensureCapacity(m_entryCount+1);
		int offset = -standardFind(key) - 1;
		if (offset >= 0) {
			m_entryCount++;
			m_keyTable[offset] = key;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Check if key is in set.
	 *
	 * @param key key to be found in set
	 * @return <code>true</code> if key found in set, <code>false</code>
	 * if not
	 */

	public boolean contains(Object key) {
		return standardFind(key) >= 0;
	}

	/**
	 * Remove an entry from the set.
	 *
	 * @param key key to be removed from set
	 * @return <code>true</code> if key successfully removed from set,
	 * <code>false</code> if key not found in set
	 */

	public boolean remove(Object key) {
		int slot = standardFind(key);
		if (slot >= 0) {
			internalRemove(slot);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Construct a copy of the set.
	 *
	 * @return shallow copy of set
	 */

	public Object clone() {
		return new ObjectHashSet(this);
	}
}
