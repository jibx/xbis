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

package test;

/**
 * Document summary information. This includes several count values
 * characteristic of a document, allowing simple consistency checks across
 * different representations of the document.
 *
 * @author Dennis M. Sosnoski
 * @version 1.1
 */

public class DocumentSummary
{
	/** Number of elements. */
	private int m_elementCount;

	/** Number of content text segments. */
	private int m_contentCount;

	/** Number of attributes. */
	private int m_attributeCount;

	/** Number of characters of content text. */
	private int m_textCharCount;

	/** Number of characters of attribute data. */
	private int m_attrCharCount;
    
    /** Number of characters of comment text. */
    private int m_commentCharCount;

	/**
	 * Reset count values.
	 */

	public void reset() {
		m_elementCount = 0;
		m_contentCount = 0;
		m_attributeCount = 0;
		m_textCharCount = 0;
		m_attrCharCount = 0;
        m_commentCharCount = 0;
	}

	/**
	 * Get element count.
	 *
	 * @return number of elements
	 */

	public int getElementCount() {
		return m_elementCount;
	}

	/**
	 * Get content segment count.
	 *
	 * @return number of content segments
	 */

	public int getContentCount() {
		return m_contentCount;
	}

	/**
	 * Get attribute count.
	 *
	 * @return number of attributes
	 */

	public int getAttributeCount() {
		return m_attributeCount;
	}

	/**
	 * Get text content character count.
	 *
	 * @return number of text characters
	 */

	public int getTextCharCount() {
		return m_textCharCount;
	}

	/**
	 * Get attribute value character count.
	 *
	 * @return number of attribute value characters
	 */

	public int getAttrCharCount() {
		return m_attrCharCount;
	}

    /**
     * Get comment text character count.
     *
     * @return number of comment text characters
     */

    public int getCommentCharCount() {
        return m_commentCharCount;
    }

	/**
	 * Add to element count.
	 *
	 * @param count value to be added to element count
	 */

	public void addElements(int count) {
		m_elementCount += count;
	}

	/**
	 * Count attribute. Increments the attribute count by one and adds the
	 * supplied character count to the attribute data length.
	 *
	 * @param length attribute value text length
	 */

	public void addAttribute(int length) {
		m_attributeCount++;
		m_attrCharCount += length;
	}

	/**
	 * Count content text segment. Increments the content segment count by one
	 * and adds the supplied character count to the content text length.
	 *
	 * @param length attribute value text length
	 */

	public void addContent(int length) {
		m_contentCount++;
		m_textCharCount += length;
	}

    /**
     * Count comment text characters. Just adds the character count to the
     * total accumulation.
     *
     * @param length comment text length
     */

    public void addComment(int length) {
        m_commentCharCount += length;
    }

	/**
	 * Check if object is equal to this one.
	 *
	 * @param obj object to be compared
	 * @return <code>true</code> if the values match, <code>false</code>
	 * if not
	 */

	public boolean equals(Object obj) {
		if (obj instanceof DocumentSummary) {
			DocumentSummary comp = (DocumentSummary)obj;
			return m_elementCount == comp.m_elementCount &&
				m_contentCount == comp.m_contentCount &&
				m_attributeCount == comp.m_attributeCount &&
				m_textCharCount == comp.m_textCharCount &&
				m_attrCharCount == comp.m_attrCharCount &&
                m_commentCharCount == comp.m_commentCharCount;
		} else {
			return false;
		}
	}

	/**
	 * Check if data and structure is equal to this one. This comparison 
	 * ignores the text content segment count, since that can be changed 
	 * by output formatting.
	 *
	 * @param obj object to be compared
	 * @return <code>true</code> if the values match, <code>false</code>
	 * if not
	 */

	public boolean dataEquals(Object obj) {
		if (obj instanceof DocumentSummary) {
			DocumentSummary comp = (DocumentSummary)obj;
			return m_elementCount == comp.m_elementCount &&
				m_attributeCount == comp.m_attributeCount &&
				m_textCharCount == comp.m_textCharCount &&
				m_attrCharCount == comp.m_attrCharCount &&
                m_commentCharCount == comp.m_commentCharCount;
		} else {
			return false;
		}
	}

	/**
	 * Check if object structure is equal to this one. This comparison ignores
	 * the text content segment count and total character length, since that
	 * can be changed by output formatting.
	 *
	 * @param obj object to be compared
	 * @return <code>true</code> if the values match, <code>false</code>
	 * if not
	 */

	public boolean structureEquals(Object obj) {
		if (obj instanceof DocumentSummary) {
			DocumentSummary comp = (DocumentSummary)obj;
			return m_elementCount == comp.m_elementCount &&
				m_attributeCount == comp.m_attributeCount &&
				m_attrCharCount == comp.m_attrCharCount &&
                m_commentCharCount == comp.m_commentCharCount;
		} else {
			return false;
		}
	}
}