/*
Copyright (c) 2004, Dennis M. Sosnoski
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

import org.xml.sax.Attributes;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX2 handler for generating summary of document parse event stream.
 */

public class SummaryHandler extends DefaultHandler implements LexicalHandler
{
    /** Summary information accumulated for document. */
    private DocumentSummary m_summary;

    /**
     * Getter for document summary information.
     *
     * @return document summary information
     */

    public DocumentSummary getSummary() {
        return m_summary;
    }

    /**
     * Setter for document summary information.
     *
     * @param summary document summary information
     */

    public void setSummary(DocumentSummary summary) {
        m_summary = summary;
    }

    /**
     * Start of document handler. Clears the accumulated document
     * summary information.
     */

    public void startDocument() {
//          m_summary.reset();
    }

    /**
     * Start of element handler. Counts the element and attributes.
     *
     * @param space namespace URI
     * @param name local name of element
     * @param raw raw element name
     * @param atts attributes for element
     */

    public void startElement(String space, String name,
        String raw, Attributes atts) {
        m_summary.addElements(1);
        for (int i = 0; i < atts.getLength(); i++) {
            m_summary.addAttribute(atts.getValue(i).length());
        }
    }

    /**
     * Character data handler. Counts the characters in total for
     * document.
     *
     * @param ch array supplying character data
     * @param start starting offset in array
     * @param length number of characters
     */

    public void characters(char[] ch, int start, int length) {
        m_summary.addContent(length);
    }

    /**
     * Ignorable whitespace handler. Counts the characters in total for
     * document.
     *
     * @param ch array supplying character data
     * @param start starting offset in array
     * @param length number of characters
     */

    public void ignorableWhitespace(char[] ch, int start, int length) {
        m_summary.addContent(length);
    }

    /**
     * Comment handler. Counts the characters in total for document.
     *
     * @param ch array supplying character data
     * @param start starting offset in array
     * @param length number of characters
     */

    public void comment(char[] ch, int start, int length) {
        m_summary.addComment(length);
    }

    //
    // Unused methods required by LexicalHandler interface

    public void startCDATA() {}

    public void endCDATA() {}

    public void startDTD(String name, String publicId, String systemId) {}

    public void endDTD() {}

    public void startEntity(String name) {}

    public void endEntity(String name) {}
}
