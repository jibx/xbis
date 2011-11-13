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

import java.lang.reflect.Array;
import com.sosnoski.util.PrimitiveHashBase;

/**
 * Base class for type-specific hash map classes with primitive keys. This
 * class builds on the basic structure provided by
 * <code>PrimitiveHashBase</code>, specializing it for the case of a hash map
 * where a data value is associated with each key. See the base class 
 * description for details of the implementation.<p>
 *
 * Hash sets based on this class are unsynchronized in order to provide the
 * best possible performance for typical usage scenarios, so explicit
 * synchronization must be implemented by the subclass or the application in
 * cases where they are to be modified in a multithreaded environment.<p>
 *
 * Subclasses need to implement the abstract methods defined by the base class
 * for working with the key array, and by this class for working with the value 
 * array and for restructuring, as well as the actual data access methods (at 
 * least the basic <code>add()</code>, <code>containsKey()</code>, 
 * <code>get()</code>, and <code>remove()</code> methods).
 * 
 * @see com.sosnoski.util.PrimitiveHashBase
 * @author Dennis M. Sosnoski
 * @version 1.0
 */

public abstract class PrimitiveKeyBase extends PrimitiveHashBase
{
	/**
	 * Constructor with full specification.
	 * 
	 * @param count number of values to assume in initial sizing of table
	 * @param fill fraction full allowed for table before growing
	 * @param ktype type of primitives used for keys
	 * @param vtype type of primitives or objects used for values
	 */
	
	public PrimitiveKeyBase(int count, double fill, Class ktype, Class vtype) {
		super(count, fill, ktype);
		setValueArray(Array.newInstance(vtype, m_flagTable.length));
	}

	/**
	 * Copy (clone) constructor.
	 * 
	 * @param base instance being copied
	 */
	
	public PrimitiveKeyBase(PrimitiveKeyBase base) {
		super(base);
		int size = base.m_flagTable.length;
		Class type = base.getValueArray().getClass().getComponentType();
		Object copy = Array.newInstance(type, size);
		System.arraycopy(base.getValueArray(), 0, copy, 0, size);
		setValueArray(copy);
	}

	/**
	 * Get the backing array of values. This method is used by the type-agnostic
	 * base class code to access the array used for type-specific storage by
	 * the child class.
	 * 
	 * @return backing key array object
	 */
	
	protected abstract Object getValueArray();

	/**
	 * Set the backing array of values. This method is used by the type-agnostic
	 * base class code to set the array used for type-specific storage by the
	 * child class.
	 * 
	 * @param array backing value array object
	 */
	
	protected abstract void setValueArray(Object array);

	/**
	 * Restructure the table. This abstract method is used when the table
	 * is increasing or decreasing in size, and works directly with the old
	 * table representation arrays. It should insert pairs from the old arrays
	 * directly into the table without adjusting the count present or checking
	 * the table size.
	 * 
	 * @param flags array of flags for array slots used
	 * @param karray array of keys
	 * @param varray array of values
	 */
	
	protected abstract void restructure(boolean[] flags, Object karray,
		Object varray);

	/**
	 * Resize the base arrays after a size change. This implementation of the
	 * abstract base class method allocates the new arrays and then calls
	 * another method for handling the actual transfer of the keys and values
	 * from the old arrays to the new ones.
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
		Object values = getValueArray();
		type = values.getClass().getComponentType();
		setValueArray(Array.newInstance(type, size));
		
		// reinsert all entries into new arrays
		restructure(flags, keys, values);
	}

	/**
	 * Set the table to the empty state. This override of the base class method 
	 * first calls the base class method to clear the entry information, then
	 * checks if the hash map values are objects, and if so clears all
	 * references to these objects.
	 */
	
	public void clear() {
		super.clear();
		Object values = getValueArray();
		if (!values.getClass().getComponentType().isPrimitive()) {
			Object[] objects = (Object[])values;
			for (int i = 0; i < objects.length; i++) {
				objects[i] = null;
			}
		}
	}
}
