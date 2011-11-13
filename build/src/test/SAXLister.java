/*
Copyright (c) 2003-2004, Dennis M. Sosnoski
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

import org.xbis.eventstore.EventStore;
import org.xbis.eventstore.SAXReader;
import org.xbis.eventstore.SAXWriter;
import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.*;

/**
 * SAX event lister. Just prints out the events generated during a parse, with
 * optional validation.
 *
 * @author Dennis M. Sosnoski
 * @version 1.0
 */

public class SAXLister
{
	/**
	 * Inner class for handling SAX notifications.
	 */

	protected static class InnerHandler extends DefaultHandler
        implements LexicalHandler
	{
		/** Tag nesting depth. */
		private int m_depth;
		
		/**
		 * Indent for nesting of elements.
		 */

		public void indent() {
			for (int i = 0; i < m_depth; i++) {
				System.out.print("  ");
			}
		}
		
		/**
		 * Start of document handler.
		 */

		public void startDocument() {
			System.out.println("startDocument");
		}
		
		/**
		 * End of document handler.
		 */

		public void endDocument() {
			System.out.println("endDocument");
		}

		/**
		 * Start of element handler.
		 *
		 * @param space namespace URI
		 * @param name local name of element
		 * @param raw raw element name
		 * @param atts attributes for element
		 */

		public void startElement(String space, String name,
			String raw, Attributes atts) {
			indent();
			if (space == null || space.length() == 0) {
				System.out.println("startElement " + name + " (" + raw +
					") with " + atts.getLength() + " attributes");
			} else {
				System.out.println("startElement {" + space + "}:" + name +
					" (" + raw + ") with " + atts.getLength() + " attributes");
			}
			m_depth++;
		}

		/**
		 * End of element handler.
		 *
		 * @param space namespace URI
		 * @param name local name of element
		 * @param raw raw element name
		 */

		public void endElement(String space, String name, String raw) {
			m_depth--;
			indent();
			if (space == null || space.length() == 0) {
				System.out.println("endElement " + name + " (" + raw + ")");
			} else {
				System.out.println("endElement {" + space + "}:" + name +
					" (" + raw + ")");
			}
		}

		/**
		 * Character data handler.
		 *
		 * @param ch array supplying character data
		 * @param start starting offset in array
		 * @param length number of characters
		 */

		public void characters(char[] ch, int start, int length) {
			indent();
			StringBuffer text = new StringBuffer(length);
			for (int i = 0; i < length; i++) {
				char chr = ch[start+i];
				if (chr == '\n') {
					text.append("\\n");
				} else if (chr == '\r') {
					text.append("\\r");
				} else {
					text.append(chr);
				}
			}
			System.out.println("characters \"" + text.toString() + "\"");
		}

		/**
		 * Parse error handler. This method is used for reporting error
         * conditions which do not block parsing from continuing.
		 *
		 * @param ex exception for error condition
		 */
        
        public void error(SAXParseException ex) throws SAXException {
            System.err.println("error method called with message: " +
                ex.getMessage());
        }

		/**
		 * Fatal error handler. This method is used for reporting error
         * conditions which prevent any further parsing.
		 *
		 * @param ex exception for error condition
		 */
        
        public void fatalError(SAXParseException ex) throws SAXException {
            System.err.println("fatalError method called with message: " +
                ex.getMessage());
        }

        /* (non-Javadoc)
         * @see org.xml.sax.ext.LexicalHandler#endCDATA()
         */
        public void endCDATA() throws SAXException {
            indent();
            System.out.println("]]>");
        }

        /* (non-Javadoc)
         * @see org.xml.sax.ext.LexicalHandler#endDTD()
         */
        public void endDTD() throws SAXException {
            indent();
            System.out.println("endDTD");
        }

        /* (non-Javadoc)
         * @see org.xml.sax.ext.LexicalHandler#startCDATA()
         */
        public void startCDATA() throws SAXException {
            indent();
            System.out.println("<![CDATA[");
        }

        /* (non-Javadoc)
         * @see org.xml.sax.ext.LexicalHandler#comment(char[], int, int)
         */
        public void comment(char[] ch, int start, int length) throws SAXException {
            indent();
            StringBuffer text = new StringBuffer(length);
            for (int i = 0; i < length; i++) {
                char chr = ch[start+i];
                if (chr == '\n') {
                    text.append("\\n");
                } else if (chr == '\r') {
                    text.append("\\r");
                } else {
                    text.append(chr);
                }
            }
            System.out.println("comment \"" + text.toString() + "\"");
        }

        /* (non-Javadoc)
         * @see org.xml.sax.ext.LexicalHandler#endEntity(java.lang.String)
         */
        public void endEntity(String name) throws SAXException {
            indent();
            System.out.println("endEntity " + name);
        }

        /* (non-Javadoc)
         * @see org.xml.sax.ext.LexicalHandler#startEntity(java.lang.String)
         */
        public void startEntity(String name) throws SAXException {
            indent();
            System.out.println("startEntity " + name);
        }

        /* (non-Javadoc)
         * @see org.xml.sax.ext.LexicalHandler#startDTD(java.lang.String, java.lang.String, java.lang.String)
         */
        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            indent();
            System.out.println("startDTD " + name);
        }
        
        /* (non-Javadoc)
         * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
         */
        public void endPrefixMapping(String prefix) throws SAXException {
            indent();
            System.out.println("endPrefixMapping " + prefix);
        }

        /* (non-Javadoc)
         * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
         */
        public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
            indent();
            System.out.println("startPrefixMapping " + prefix + " for " + uri);
        }
	}

	/**
	 * Print list of parse events for document.
	 */

	public static void main(String[] args) {
		if (args.length >= 1) {
			try {
                
                // check for validate flag given
                boolean validate = false;
                int index = 0;
                if ("-v".equals(args[0])) {
                    validate = true;
                    index++;
                }
				
				// create a parser instance
				XMLReader reader = XMLReaderFactory.createXMLReader();
                reader.setFeature("http://xml.org/sax/features/validation",
                    validate);
                try {
                    reader.setFeature
                        ("http://apache.org/xml/features/validation/schema",
                        validate);
                } catch (SAXNotRecognizedException e) {}
				
				// set up handler to report events
                InnerHandler handler = new InnerHandler();
				reader.setContentHandler(handler);
                reader.setErrorHandler(handler);
                reader.setProperty
                    ("http://xml.org/sax/properties/lexical-handler", handler);
				
				// parse document with output from handler
				InputSource source = new InputSource(args[index]);
				reader.parse(source);
                
                // parse again to generate event stream
                EventStore es = new EventStore();
                SAXWriter saxw = new SAXWriter(es);
                reader.setContentHandler(saxw);
                try {
                    reader.setProperty
                        ("http://xml.org/sax/properties/lexical-handler", saxw);
                } catch (Exception ex) {}
                saxw.reset();
                reader.parse(source);
                
                // feed event stream to lister
                SAXReader saxr = new SAXReader(es);
                saxr.setContentHandler(handler);
                try {
                    saxr.setProperty
                        ("http://xml.org/sax/properties/lexical-handler", handler);
                } catch (Exception ex) {}
                saxr.reset();
                saxr.read();
	
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
				System.exit(0);
			}
			
		} else {
			System.out.println("Usage: test.SAXLister [-v] document-file");
		}
	}
}
