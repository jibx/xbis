/*
Copyright (c) 2000-2004, Dennis M. Sosnoski
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
 * Basic namespace information. It holds the pair consisting of mapped prefix
 * and namespace URI, which together make up a namespace reference.
 *
 * @author Dennis M. Sosnoski
 * @version 0.8
 */

public class BasicNamespace
{
	/** Prefix used for namespace. */
	private final String m_prefix;
	
	/** Namespace URI */
	private final String m_uri;

	/**
	 * Constructor.
	 *
	 * @param prefix namespace prefix
	 * @param uri namespace URI
	 */
	
	public BasicNamespace(String prefix, String uri) {
		m_prefix = prefix;
		m_uri = uri;
	}

	/**
	 * Get namespace prefix.
	 *
	 * @return namespace prefix
	 */
	
	public String getPrefix() {
		return m_prefix;
	}

	/**
	 * Get namespace URI.
	 *
	 * @return namespace URI
	 */
	
	public String getUri() {
		return m_uri;
	}

	/**
	 * Compute hash code for object. This uses the combination of hashes from
	 * the component values.
	 *
	 * @return hash code
	 */
	
	public int hashCode() {
		return m_prefix.hashCode() + m_uri.hashCode();
	}

	/**
	 * Check equality with another object. This tests type and component
	 * equality.
	 *
	 * @return <code>true</code> if equal, <code>false</code> if not
	 */
	
	public boolean equals(Object obj) {
		if (obj instanceof BasicNamespace) {
			BasicNamespace comp = (BasicNamespace)obj;
			return m_prefix.equals(comp.getPrefix()) &&
				m_uri.equals(comp.getUri());
		} else {
			return false;
		}
	}
}