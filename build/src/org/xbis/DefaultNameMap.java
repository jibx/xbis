/*
Copyright (c) 2000-2003, Dennis M. Sosnoski
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.
 * Neither the name of XBIS nor the names of its contributors may be used
   to endorse or promote products derived from this software without specific
   prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.xbis;

import com.sosnoski.util.hashmap.ObjectObjectHashMap;

/**
 * XML Binary Information Set name mapping to handles. This class is used for models
 * which do not include support for unique element and attribute qnames. It
 * tracks and looks up handles for previously defined names, and allows new
 * names to be added to the known set.
 *
 * @author Dennis M. Sosnoski
 * @version 0.8
 */

public class DefaultNameMap
{
	/** Map of heads of name lists corresponding to local names. */
	protected ObjectObjectHashMap m_nameMap;

	/** Number of name handles defined. */
	protected int m_nameCount;

	/**
	 * Constructor.
	 */

	public DefaultNameMap() {
		m_nameMap = new ObjectObjectHashMap();
	}

	/**
	 * Get name handle.
	 *
	 * @param local local name
	 * @param ns namespace for name
	 * @return handle value (always strictly positive) if known, negative value
	 * if unknown name
	 */

	public int getHandle(String local, Object ns) {
		LinkedQName name = (LinkedQName)m_nameMap.get(local);
		while (name != null && name.getNamespace() != ns) {
			name = name.getNext();
		}
		if (name == null) {
			return -1;
		} else {
			return name.getHandle();
		}
	}

	/**
	 * Add name to known set.
	 *
	 * @param local local name
	 * @param ns namespace for name
	 */

	public int addDefined(String local, Object ns) {
		LinkedQName name = new LinkedQName(local, ns, ++m_nameCount);
		name.setNext((LinkedQName)m_nameMap.add(local, name));
		return m_nameCount;
	}

	/**
	 * Empty the known set of names. Clears all state information so that the
	 * instance can be reused.
	 */

	public void clear() {
		m_nameMap.clear();
		m_nameCount = 0;
	}

	/**
	 * Linkable form of name information. This extension of the basic name
	 * information is used to construct a singly-linked list of names.
	 */

	protected static class LinkedQName extends DefaultQName
	{
		/** Link to next in list. */
		private LinkedQName m_next;

		/** Handle assigned to name. */
		private final int m_handle;

		/**
		 * Constructor.
		 *
		 * @param local local name
		 * @param ns namespace for name
		 * @param handle handle assigned to name
		 */

		protected LinkedQName(String local, Object ns, int handle) {
			super(local, ns);
			m_handle = handle;
		}

		/**
		 * Get next in list.
		 *
		 * @return next instance in list, or <code>null</code> if end of list
		 */

		protected LinkedQName getNext() {
			return m_next;
		}

		/**
		 * Set next in list.
		 *
		 * @param next next instance in list (<code>null</code> if end of list)
		 */

		protected void setNext(LinkedQName next) {
			m_next = next;
		}

		/**
		 * Get handle assigned to name.
		 *
		 * @return handle value assigned to name
		 */

		protected int getHandle() {
			return m_handle;
		}
	}
}
