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

/**
 * XML Binary Information Set name information. This class is used for models
 * which do not include support for unique element and attribute qnames. It
 * holds the pair consisting of local name and namespace information, which
 * together make up the unique name identification used by XMLS.
 *
 * @author Dennis M. Sosnoski
 * @version 0.8
 */

public class DefaultQName
{
	/** Local name. */
	private final String m_local;

	/** Namespace information. */
	private final Object m_namespace;

	/**
	 * Constructor.
	 *
	 * @param local local name
	 * @param ns namespace for name
	 */

	public DefaultQName(String local, Object ns) {
		m_local = local;
		m_namespace = ns;
	}

	/**
	 * Get local name.
	 *
	 * @return local name
	 */

	public String getLocalName() {
		return m_local;
	}

	/**
	 * Get namespace.
	 *
	 * @return namespace for name
	 */

	public Object getNamespace() {
		return m_namespace;
	}
}