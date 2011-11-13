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

import java.lang.reflect.Array;

import com.sosnoski.util.PrimitiveHashBase;

/**
 * Base class for type-specific hash set classes with primitive keys. This
 * class builds on the basic structure provided by
 * <code>PrimitiveHashBase</code>, specializing it for the case of a hash set
 * where the keys are the only data. See the base class description for
 * details of the implementation.<p>
 *
 * Hash sets based on this class are unsynchronized in order to provide the
 * best possible performance for typical usage scenarios, so explicit
 * synchronization must be implemented by the subclass or the application in
 * cases where they are to be modified in a multithreaded environment.<p>
 *
 * Subclasses need to implement the abstract methods defined by the base class
 * for working with the key array, and the abstract restructuring method 
 * defined by this class, as well as the actual data access methods (at least 
 * the basic <code>add</code>, <code>contains</code>, and <code>remove</code> 
 * methods).
 * 
 * @see com.sosnoski.util.PrimitiveHashBase
 * @author Dennis M. Sosnoski
 * @version 1.0
 */

public abstract class PrimitiveSetBase extends PrimitiveHashBase
{
	/**
	 * Constructor with full specification.
	 * 
	 * @param count number of values to assume in initial sizing of table
	 * @param fill fraction full allowed for table before growing
	 * @param ktype type of primitives used for keys
	 */
	
	public PrimitiveSetBase(int count, double fill, Class ktype) {
		super(count, fill, ktype);
	}

	/**
	 * Copy (clone) constructor.
	 * 
	 * @param base instance being copied
	 */
	
	public PrimitiveSetBase(PrimitiveSetBase base) {
		super(base);
	}

	/**
	 * Restructure the table. This abstract method is used when the table
	 * is increasing or decreasing in size, and works directly with the old
	 * table representation arrays. It should insert keys from the old array
	 * directly into the table without adjusting the count present or checking
	 * the table size.
	 * 
	 * @param flags array of flags for array slots used
	 * @param karray array of keys
	 */
	
	protected abstract void restructure(boolean[] flags, Object karray);

	/**
	 * Resize the base arrays after a size change. This implementation of the
	 * abstract base class method allocates the new arrays and then calls
	 * another method for handling the actual transfer of the key set from the
	 * old arrays to the new ones.
	 * 
	 * @param size new size for base arrays
	 */
	
	protected void reallocate(int size) {
	
		// allocate the larger arrays
		boolean[] flags = m_flagTable;
		m_flagTable = new boolean[size];
		Object keys = getKeyArray();;
		Class type = keys.getClass().getComponentType();
		setKeyArray(Array.newInstance(type, size));
		
		// reinsert all entries into new arrays
		restructure(flags, keys);
	}
}
