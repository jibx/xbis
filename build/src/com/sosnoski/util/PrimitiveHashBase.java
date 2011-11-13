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
 * Base class for type-specific hash map and hash set classes using keys of
 * primitive types. This base class implementation is based on a design using
 * parallel arrays for "slot in use" flags and the actual key values, with
 * type-specific subclasses implementing the key value hashing and comparisons.
 * Subclasses may use additional parallel arrays beyond these two, and the
 * internal operation is designed to allow for this case.<p>
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
 * @see com.sosnoski.util.ObjectHashBase
 * @author Dennis M. Sosnoski
 * @version 1.0
 */

public abstract class PrimitiveHashBase
{
	/** Default fill fraction allowed before growing table. */
	protected static final double DEFAULT_FILL = 0.3d;

	/** Minimum size used for table. */
	protected static final int MINIMUM_SIZE = 31;

	/** Hash value multiplier to scramble bits before calculating slot. */
	protected static final int KEY_MULTIPLIER = 517;

	/** Fill fraction allowed for table. */
	protected double m_fillFraction;

	/** Number of entries present in table. */
	protected int m_entryCount;

	/** Entries allowed before growing table. */
	protected int m_entryLimit;

	/** Offset added (modulo table size) to slot number on collision. */
	protected int m_hitOffset;

	/** Array of slot occupied flags. */
	protected boolean[] m_flagTable;

	/**
	 * Constructor with full specification.
	 *
	 * @param count number of values to assume in initial sizing of table
	 * @param fill fraction full allowed for table before growing
	 * @param ktype type of primitives used for keys
	 */

	public PrimitiveHashBase(int count, double fill, Class ktype) {

		// check the passed in fill fraction
		if (fill <= 0.0d || fill >= 1.0d) {
			throw new IllegalArgumentException("fill value out of range");
		}
		m_fillFraction = fill;

		// compute initial table size (ensuring odd)
		int size = Math.max((int) (count / m_fillFraction), MINIMUM_SIZE);
		size += (size + 1) % 2;

		// initialize the table information
		m_entryLimit = (int) (size * m_fillFraction);
		m_hitOffset = size / 2;
		m_flagTable = new boolean[size];
		setKeyArray(Array.newInstance(ktype, size));
	}

	/**
	 * Copy (clone) constructor.
	 *
	 * @param base instance being copied
	 */

	public PrimitiveHashBase(PrimitiveHashBase base) {

		// copy the basic occupancy information
		m_fillFraction = base.m_fillFraction;
		m_entryCount = base.m_entryCount;
		m_entryLimit = base.m_entryLimit;
		m_hitOffset = base.m_hitOffset;

		// copy table of slot occupied flags
		int size = base.m_flagTable.length;
		m_flagTable = new boolean[size];
		System.arraycopy(base.m_flagTable, 0, m_flagTable, 0,
			m_flagTable.length);

		// copy table of keys
		Class type = base.getKeyArray().getClass().getComponentType();
		Object copy = Array.newInstance(type, size);
		System.arraycopy(base.getKeyArray(), 0, copy, 0, size);
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
	 * Get the backing array of keys. This method is used by the type-agnostic
	 * base class code to access the array used for type-specific storage by
	 * the child class.
	 *
	 * @return backing key array object
	 */

	protected abstract Object getKeyArray();

	/**
	 * Set the backing array of keys. This method is used by the type-agnostic
	 * base class code to set the array used for type-specific storage by the
	 * child class.
	 *
	 * @param array backing key array object
	 */

	protected abstract void setKeyArray(Object array);

	/**
	 * Resize the base arrays after a size change. This is a separate abstract
	 * method so that derived classes can implement the handling appropriate
	 * for their data structures (which may include more than just the arrays
	 * of flags and key values).
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
		int size = m_flagTable.length;
		int limit = m_entryLimit;
		while (limit < min) {
			size = size * 2 + 1;
			limit = (int) (size * m_fillFraction);
		}

		// set parameters for new array size
		m_entryLimit = limit;
		m_hitOffset = size / 2;

		// let the subclass handle the adjustments to data
		reallocate(size);
	}

	/**
	 * Ensure that the table has the capacity for at least the specified
	 * number of keys.
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
		for (int i = 0; i < m_flagTable.length; i++) {
			m_flagTable[i] = false;
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
		return (slot + m_hitOffset) % m_flagTable.length;
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
		while (m_flagTable[slot]) {
			slot = stepSlot(slot);
		}
		return slot;
	}
}
