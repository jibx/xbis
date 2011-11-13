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

import java.lang.reflect.Array;

/**
 * Base class for type-specific hash map and hash set classes using
 * object keys. This class uses a single array for keys with <code>null</code>
 * values in unused slots. Subclasses may use additional arrays in parallel to
 * this key array, and the internal operation is designed to allow for this
 * case.<p>
 *
 * Hash classes based on this class are unsynchronized in order to provide the
 * best possible performance for typical usage scenarios, so explicit
 * synchronization must be implemented by the subclass or the application in
 * cases where they are to be modified in a multithreaded environment.<p>
 *
 * This implementation uses the standard naive hash technique of adding a
 * fixed offset to the computed position in the table when a collision occurs,
 * so that several checks may potentially be necessary in order to determine if
 * a particular key value is present in the table (since if the slot calculated
 * by the hash function is occupied by a different entry it's still necessary to
 * check the offset slot for a match, and then the offset slot from that, until
 * an empty slot is found).<p>
 *
 * Each time the number of entries present in the table grows above the
 * specified fraction full the table is expanded to twice its prior size
 * plus one (to ensure the size is always an odd number). The collision
 * offset is set to half the table size rounded down, ensuring that it
 * will cycle through all slots of the table before returning to the
 * original slot.<p>
 *
 * Subclasses need to implement the abstract methods defined by this class
 * for working with the key array, as well as the actual data access methods.
 *
 * @author Dennis M. Sosnoski
 * @version 1.1
 */

public abstract class ObjectHashBase
{
	/** Standard hashing technique specifier. */
	public static final String STANDARD_HASH = "base";

	/** Standard hash with object identity comparison technique specifier. */
	public static final String IDENTITY_COMP = "comp";

	/** Identity hash with object identity comparison technique specifier. */
	public static final String IDENTITY_HASH = "ident";

	/** Default fill fraction allowed before growing table. */
	protected static final double DEFAULT_FILL = 0.3d;

	/** Minimum size used for hash table. */
	protected static final int MINIMUM_SIZE = 31;

	/** Use identity hash flag. */
	protected final boolean m_identHash;

	/** Use identity comparison flag. */
	protected final boolean m_identCompare;

	/** Fill fraction allowed for this hash table. */
	protected final double m_fillFraction;

	/** Number of entries present in table. */
	protected int m_entryCount;

	/** Entries allowed before growing table. */
	protected int m_entryLimit;

	/** Size of array used for keys. */
	protected int m_arraySize;

	/** Offset added (modulo table size) to slot number on collision. */
	protected int m_hitOffset;

	/**
	 * Constructor with full specification.
	 *
	 * @param count number of values to assume in initial sizing of table
	 * @param fill fraction full allowed for table before growing
	 * @param type type of keys contained in table
	 * @param tech hash technique specifier (one of STANDARD_HASH,
	 * IDENTITY_COMP, or IDENTITY_HASH)
	 */

	public ObjectHashBase(int count, double fill, Class type, Object tech) {

		// check the passed in fill fraction
		if (fill <= 0.0d || fill >= 1.0d) {
			throw new IllegalArgumentException("fill value out of range");
		}
		m_fillFraction = fill;

		// set flags for hash technique control
		if (tech == STANDARD_HASH) {
			m_identHash = false;
			m_identCompare = false;
		} else if (tech == IDENTITY_COMP) {
			m_identHash = false;
			m_identCompare = true;
		} else if (tech == IDENTITY_HASH) {
			m_identHash = true;
			m_identCompare = true;
		} else {
			throw new IllegalArgumentException
				("Unknown hash technique specifier");
		}

		// compute initial table size (ensuring odd)
		m_arraySize = Math.max((int) (count / m_fillFraction), MINIMUM_SIZE);
		m_arraySize += (m_arraySize + 1) % 2;

		// initialize the table information
		m_entryLimit = (int) (m_arraySize * m_fillFraction);
		m_hitOffset = m_arraySize / 2;
		setKeyArray(Array.newInstance(type, m_arraySize));
	}

	/**
	 * Copy (clone) constructor.
	 *
	 * @param base instance being copied
	 */

	public ObjectHashBase(ObjectHashBase base) {

		// copy the basic occupancy information
		m_fillFraction = base.m_fillFraction;
		m_entryCount = base.m_entryCount;
		m_entryLimit = base.m_entryLimit;
		m_arraySize = base.m_arraySize;
		m_hitOffset = base.m_hitOffset;
		m_identHash = base.m_identHash;
		m_identCompare = base.m_identCompare;

		// copy table of items
		Class type = base.getKeyArray().getClass().getComponentType();
		Object copy = Array.newInstance(type, m_arraySize);
		System.arraycopy(base.getKeyArray(), 0, copy, 0, m_arraySize);
		setKeyArray(copy);
	}

	/**
	 * Get number of items in table.
	 *
	 * @return item count present
	 */

	public final int size() {
		return m_entryCount;
	}

	/**
	 * Get the backing array of items. This method is used by the type-agnostic
	 * base class code to access the array used for type-specific storage by
	 * the child class.
	 *
	 * @return backing item array object
	 */

	protected abstract Object[] getKeyArray();

	/**
	 * Set the backing array of items. This method is used by the type-agnostic
	 * base class code to set the array used for type-specific storage by the
	 * child class.
	 *
	 * @param array backing item array object
	 */

	protected abstract void setKeyArray(Object array);

	/**
	 * Resize the base arrays after a size change. This is a separate abstract
	 * method so that derived classes can implement the handling appropriate
	 * for their data structures (which may include more than just the array
	 * of key values).
	 *
	 * @param size new size for base arrays
	 */

	protected abstract void reallocate(int size);

	/**
	 * Increase table capacity. This method is called when we actually need
	 * to increase the table size.
	 *
	 * @param min minimum new table capacity
	 */

	protected void growCapacity(int min) {

		// find the array size required
		int size = m_arraySize;
		int limit = m_entryLimit;
		while (limit < min) {
			size = size * 2 + 1;
			limit = (int) (size * m_fillFraction);
		}

		// set parameters for new array size
		m_arraySize = size;
		m_entryLimit = limit;
		m_hitOffset = size / 2;

		// let the subclass handle the adjustments to data
		reallocate(size);
	}

	/**
	 * Ensure that the table has the capacity for at least the specified
	 * number of items.
	 *
	 * @param min minimum capacity to be guaranteed
	 */

	public final void ensureCapacity(int min) {
		if (min > m_entryLimit) {
			growCapacity(min);
		}
	}

	/**
	 * Set the table to the empty state. This method may need to be overridden
	 * in cases where other information associated with the keys also needs to
	 * be cleared.
	 */

	public void clear() {
		Object[] items = getKeyArray();
		for (int i = 0; i < m_arraySize; i++) {
			items[i] = null;
		}
		m_entryCount = 0;
	}

	/**
	 * Step the slot number for an entry. Adds the collision offset (modulo
	 * the table size) to the slot number.
	 *
	 * @param slot slot number to be stepped
	 * @return stepped slot number
	 */

	protected final int stepSlot(int slot) {
		return (slot + m_hitOffset) % m_arraySize;
	}

	/**
	 * Find free slot number for entry. Starts at the slot based directly
	 * on the hashed key value. If this slot is already occupied, it adds
	 * the collision offset (modulo the table size) to the slot number and
	 * checks that slot, repeating until an unused slot is found.
	 *
	 * @param slot initial slot computed from key
	 * @return slot at which entry was added
	 */

	protected final int freeSlot(int slot) {
		Object[] items = getKeyArray();
		while (items[slot] != null) {
			slot = stepSlot(slot);
		}
		return slot;
	}

	/**
	 * Standard base slot computation for a key. This method may be used
	 * directly for key lookup using either the <code>hashCode()</code> method
	 * defined for the key objects or the <code>System.identityHashCode()</code>
	 * method, as selected by the hash technique constructor parameter. To
	 * implement a hash class based on some other methods of hashing and/or
	 * equality testing, define a separate method in the subclass with a
	 * different name and use that method instead. This avoids the overhead
	 * caused by overrides of a very heavily used method.
	 *
	 * @param key key value to be computed
	 * @return base slot for key
	 */

	protected final int standardSlot(Object key) {
		return ((m_identHash ? System.identityHashCode(key) : key.hashCode()) &
			Integer.MAX_VALUE) % m_arraySize;
	}

	/**
	 * Standard find key in table. This method may be used directly for key
	 * lookup using either the <code>hashCode()</code> method defined for the
	 * key objects or the <code>System.identityHashCode()</code> method, and
	 * either the <code>equals()</code> method defined for the key objects or
	 * the <code>==</code> operator, as selected by the hash technique
	 * constructor parameter. To implement a hash class based on some other
	 * methods of hashing and/or equality testing, define a separate method in
	 * the subclass with a different name and use that method instead. This
	 * avoids the overhead caused by overrides of a very heavily used method.
	 *
	 * @param key to be found in table
	 * @return index of matching key, or <code>-index-1</code> of slot to be
	 * used for inserting key in table if not already present (always negative)
	 */

	protected int standardFind(Object key) {

		// find the starting point for searching table
		int slot = standardSlot(key);

		// scan through table to find target key
		Object keys[] = getKeyArray();
		if (m_identCompare) {
			while (keys[slot] != null) {

				// check if we have a match on target key
				if (keys[slot] == key) {
					return slot;
				} else {
					slot = stepSlot(slot);
				}

			}
		} else {
			while (keys[slot] != null) {

				// check if we have a match on target key
				if (keys[slot].equals(key)) {
					return slot;
				} else {
					slot = stepSlot(slot);
				}

			}
		}
		return -slot-1;
	}
}
