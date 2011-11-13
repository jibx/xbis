/*
Copyright (c) 2003, Dennis M. Sosnoski
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

package org.xbis.eventstore;

import java.util.HashMap;
import java.util.Stack;

import org.xbis.*;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import com.sosnoski.util.stack.IntStack;

/**
 * XML event store adapter for SAX2. This reads a document event store to
 * generate a SAX2 parse event stream.
 *
 * @author Dennis M. Sosnoski
 * @version 0.8
 */

public class SAXReader implements XMLReader
{
    private static final int DEFAULT_ATTRIBUTE_COUNT = 10;
    
    private EventStore.StoreReader m_reader;
	private AttributesHolder m_attributes;
	private ContentHandler m_contentHandler;
	private DTDHandler m_dtdHandler;
	private EntityResolver m_entityResolver;
	private ErrorHandler m_errorHandler;
	private LexicalHandler m_lexicalHandler;
    private boolean m_isUsePrefixes;
    private int m_nsDeclCount;
    private Stack m_elementStack;
    private IntStack m_countStack;
    private Stack m_namespaceStack;
    private HashMap m_namespaceAttributeMap;
	
	/**
	 * Constructor.
	 */

	public SAXReader(EventStore es) {
        m_reader = es.createReader();
		m_attributes = new AttributesHolder();
        m_elementStack = new Stack();
        m_countStack = new IntStack();
        m_namespaceStack = new Stack();
	}

	/**
	 * Report start of element to handler.
	 *
	 * @throws SAXException on error in processing XML data
	 */

	private void reportElementStart() throws SAXException {

		// get the basic element information
        NameImpl ename = m_reader.readQName();

		// handle attributes if included
        int acnt = m_reader.readEvent();
        for (int i = 0; i < acnt; i++) {
            
            // read attribute name and value
            NameImpl aname = m_reader.readQName();
            String avalue = m_reader.readString();
            m_attributes.addAttribute(aname, avalue);
        }
        
        // include namespace definitions as attributes if requested
        if (m_isUsePrefixes) {
            int index = m_namespaceStack.size();
            for (int i = 0; i < m_nsDeclCount; i++) {
                BasicNamespace ns =
                    (BasicNamespace)m_namespaceStack.get(--index);
                NameImpl aname = (NameImpl)m_namespaceAttributeMap.get(ns);
                if (aname == null) {
                    String asattr = "xmlns";
                    if (ns.getPrefix().length() > 0) {
                        asattr += ":" + ns.getPrefix();
                    }
                    aname = new NameImpl(m_reader.getNamespace(0),
                        asattr, asattr, -1);
                    m_namespaceAttributeMap.put(ns, aname);
                }
                m_attributes.addAttribute(aname, ns.getUri());
            }
        }
		
		// report element start to content handler
		if (m_contentHandler != null) {
			m_contentHandler.startElement(ename.getNamespace().getUri(),
				ename.getName(), ename.getQName(), m_attributes);
		}
        
        // save element information for close
        m_elementStack.push(ename);
        m_countStack.push(m_nsDeclCount);
        m_nsDeclCount = 0;
		
		// clear accumulated attributes for next element
		m_attributes.clear();
	}

    /**
     * Report start of namespace mapping to handler.
     *
     * @throws SAXException on error in processing XML data
     */

    private void reportNamespaceStart() throws SAXException {

        // report start of prefix mapping
        BasicNamespace ns = m_reader.getNamespace(m_reader.readEvent());
        if (m_contentHandler != null) {
            m_contentHandler.startPrefixMapping(ns.getPrefix(), ns.getUri());
        }
        
        // save namespace information for close
        m_namespaceStack.push(ns);
        m_nsDeclCount++;
    }

    /**
     * Report end of element to handler.
     *
     * @throws SAXException on error in processing XML data
     */

    private void reportElementEnd() throws SAXException {
        
        // report element end to content handler
        NameImpl ename = (NameImpl)m_elementStack.pop();
        if (m_contentHandler != null) {
            m_contentHandler.endElement(ename.getNamespace().getUri(),
                ename.getName(), ename.getQName());
        }
        
        // clean up any namespaces for this element
        int nscnt = m_countStack.pop();
        for (int i = 0; i < nscnt; i++) {
            BasicNamespace ns = (BasicNamespace)m_namespaceStack.pop();
            if (m_contentHandler != null) {
                m_contentHandler.endPrefixMapping(ns.getPrefix());
            }
        }
    }
	
	/**
	 * Report character data.
	 *
	 * @throws SAXException on error in processing XML data
	 */

	protected void reportText() throws SAXException {
        int offset = m_reader.getCharOffset();
        int length = m_reader.readChars();
		if (m_contentHandler != null) {
            m_contentHandler.characters(m_reader.getChars(), offset, length);
		}
	}

	/**
	 * Report CDATA section.
	 *
	 * @throws SAXException on error in processing XML data
	 */

	protected void reportCDATA() throws SAXException {
		if (m_lexicalHandler != null) {
			m_lexicalHandler.startCDATA();
		}
        int offset = m_reader.getCharOffset();
        int length = m_reader.readChars();
		if (m_contentHandler != null) {
            m_contentHandler.characters(m_reader.getChars(), offset, length);
		}
		if (m_lexicalHandler != null) {
			m_lexicalHandler.endCDATA();
		}
	}
    
	/**
	 * Report processing instruction.
	 *
	 * @throws SAXException on error in processing XML data
	 */

	protected void reportProcessingInstruction() throws SAXException {
        String target = m_reader.readString();
        String data = m_reader.readString();
		if (m_contentHandler != null) {
        	m_contentHandler.processingInstruction(target, data);
		}
	}

	/**
	 * Report comment.
	 *
	 * @throws SAXException on error in processing XML data
	 */

	protected void reportComment() throws SAXException {
        int offset = m_reader.getCharOffset();
        int length = m_reader.readChars();
		if (m_lexicalHandler != null) {
			m_lexicalHandler.comment(m_reader.getChars(), offset, length);
		}
	}

	/**
	 * Read document event stream from store.
	 *
	 * @throws SAXException on error in processing XML data
	 */

	public void read() throws SAXException {

		// loop until end of document
        documentReset();
		loop: while (true) {
            switch (m_reader.readEvent()) {
                
                case EventStore.CDSECT:
                    reportCDATA();
                    break;
                
                case EventStore.COMMENT:
                    reportComment();
                    break;
                
                case EventStore.END_DOCUMENT:
                    if (m_contentHandler != null) {
                        m_contentHandler.endDocument();
                    }
                    break loop;
                
                case EventStore.END_TAG:
                    reportElementEnd();
                    break;
                
                case EventStore.PROCESSING_INSTRUCTION:
                    reportProcessingInstruction();
                    break;
                
                case EventStore.START_DOCUMENT:
                    if (m_contentHandler != null) {
                        m_contentHandler.startDocument();
                    }
                    break;
                
                case EventStore.START_NAMESPACE:
                    reportNamespaceStart();
                    break;
                
                case EventStore.START_TAG:
                    reportElementStart();
                    break;
                
                case EventStore.TEXT:
                    reportText();
                    break;

				default:
					throw new IllegalArgumentException("Unknown event type ");
			}
		}
	}

    /**
     * Reset state for processing new document. Clears any internal state
     * information and initializes for processing a new document. This allows
     * instances to be reused, even after an error termination in processing a
     * prior document.
     */
    
    private void documentReset() {
        m_attributes.clear();
        m_elementStack.clear();
        m_countStack.clear();
        m_namespaceStack.clear();
        m_nsDeclCount = 0;
        if (m_namespaceAttributeMap != null) {
            m_namespaceAttributeMap.clear();
        }
    }

	/**
	 * Reset state for processing event store from start.
	 */
	
	public final void reset() {
        m_reader.reset();
	}

	//
	// XMLReader interface methods
	
	public ContentHandler getContentHandler() {
		return m_contentHandler;
	}

	public DTDHandler getDTDHandler() {
		return m_dtdHandler;
	}

	public EntityResolver getEntityResolver() {
		return m_entityResolver;
	}

	public ErrorHandler getErrorHandler() {
		return m_errorHandler;
	}

	public boolean getFeature(String name)
		throws SAXNotRecognizedException, SAXNotSupportedException {
		if ("http://xml.org/sax/features/namespaces".equals(name)) {
			return true;
		} else if ("http://xml.org/sax/features/namespace-prefixes".
			equals(name)) {
			return m_isUsePrefixes;
		} else {
			throw new SAXNotRecognizedException("Unknown feature: " + name);
		}
	}

	public Object getProperty(String name)
		throws SAXNotRecognizedException, SAXNotSupportedException {
		if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
			return m_lexicalHandler;
		} else {
			throw new SAXNotRecognizedException("Unknown property: " + name);
		}
	}

	public void parse(InputSource input) throws SAXException {
//        if (input == null) {
            read();
/*        } else {
            throw new SAXException
                ("Direct parse not supported by stream handler");
        }    */
	}

	public void parse(String systemId) throws SAXException {
        if (systemId == null) {
            read();
        } else {
            throw new SAXException
                ("Direct parse not supported by stream handler");
        }
	}

	public void setContentHandler(ContentHandler handler) {
		m_contentHandler = handler;
	}

	public void setDTDHandler(DTDHandler handler) {
		m_dtdHandler = handler;
	}

	public void setEntityResolver(EntityResolver resolver) {
		m_entityResolver = resolver;
	}

	public void setErrorHandler(ErrorHandler handler) {
		m_errorHandler = handler;
	}

	public void setFeature(String name, boolean value)
		throws SAXNotRecognizedException, SAXNotSupportedException {
		if ("http://xml.org/sax/features/namespaces".equals(name) && !value) {
			throw new SAXNotSupportedException
				("Event store requires namespaces enabled");
		} else if ("http://xml.org/sax/features/namespace-prefixes".
            equals(name)){
            m_isUsePrefixes = value;
            if (value && m_namespaceAttributeMap == null) {
                m_namespaceAttributeMap = new HashMap();
            }
        } else {
			throw new SAXNotRecognizedException("Unknown feature: " + name);
		}
	}

	public void setProperty(String name, Object value)
		throws SAXNotRecognizedException, SAXNotSupportedException {
		if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
			m_lexicalHandler = (LexicalHandler)value;
		} else {
			throw new SAXNotRecognizedException("Unknown property: " + name);
		}
	}
    
    private class AttributesHolder implements Attributes
    {
        private int m_attributeCount;
        private NameImpl[] m_names;
        private String[] m_values;
        
        private AttributesHolder() {
            m_names = new NameImpl[DEFAULT_ATTRIBUTE_COUNT];
            m_values = new String[DEFAULT_ATTRIBUTE_COUNT];
        }
        
        /* (non-Javadoc)
         * @see org.xml.sax.Attributes#getLength()
         */
        public int getLength() {
            return m_attributeCount;
        }

        /* (non-Javadoc)
         * @see org.xml.sax.Attributes#getLocalName(int)
         */
        public String getLocalName(int index) {
            return m_names[index].getName();
        }

        /* (non-Javadoc)
         * @see org.xml.sax.Attributes#getQName(int)
         */
        public String getQName(int index) {
            return m_names[index].getQName();
        }

        /* (non-Javadoc)
         * @see org.xml.sax.Attributes#getType(int)
         */
        public String getType(int index) {
            return "CDATA";
        }

        /* (non-Javadoc)
         * @see org.xml.sax.Attributes#getURI(int)
         */
        public String getURI(int index) {
            return m_names[index].getNamespace().getUri();
        }

        /* (non-Javadoc)
         * @see org.xml.sax.Attributes#getValue(int)
         */
        public String getValue(int index) {
            return m_values[index];
        }

        /* (non-Javadoc)
         * @see org.xml.sax.Attributes#getIndex(java.lang.String)
         */
        public int getIndex(String qname) {
            int split = qname.indexOf(':');
            String prefix = "";
            String lname = qname;
            if (split >= 0) {
                prefix = qname.substring(0, split);
                lname = qname.substring(split+1);
            }
            for (int i = 0; i < m_attributeCount; i++) {
                NameImpl name = m_names[i];
                if (lname.equals(name.getName()) &&
                    prefix.equals(name.getNamespace().getPrefix())) {
                    return i;
                }
            }
            return -1;
        }

        /* (non-Javadoc)
         * @see org.xml.sax.Attributes#getType(java.lang.String)
         */
        public String getType(String qname) {
            int index = getIndex(qname);
            if (index >= 0) {
                return "CDATA";
            } else {
                return null;
            }
        }

        /* (non-Javadoc)
         * @see org.xml.sax.Attributes#getValue(java.lang.String)
         */
        public String getValue(String qname) {
            int index = getIndex(qname);
            if (index >= 0) {
                return m_values[index];
            } else {
                return null;
            }
        }

        /* (non-Javadoc)
         * @see org.xml.sax.Attributes#getIndex(java.lang.String, java.lang.String)
         */
        public int getIndex(String uri, String lname) {
            for (int i = 0; i < m_attributeCount; i++) {
                NameImpl name = m_names[i];
                if (lname.equals(name.getName()) &&
                    uri.equals(name.getNamespace().getUri())) {
                    return i;
                }
            }
            return -1;
        }

        /* (non-Javadoc)
         * @see org.xml.sax.Attributes#getType(java.lang.String, java.lang.String)
         */
        public String getType(String uri, String lname) {
            int index = getIndex(uri, lname);
            if (index >= 0) {
                return "CDATA";
            } else {
                return null;
            }
        }

        /* (non-Javadoc)
         * @see org.xml.sax.Attributes#getValue(java.lang.String, java.lang.String)
         */
        public String getValue(String uri, String lname) {
            int index = getIndex(uri, lname);
            if (index >= 0) {
                return m_values[index];
            } else {
                return null;
            }
        }
        
        public void clear() {
            m_attributeCount = 0;
        }
        
        public void addAttribute(NameImpl name, String value) {
            m_names[m_attributeCount] = name;
            m_values[m_attributeCount++] = value;
        }
    }
} 