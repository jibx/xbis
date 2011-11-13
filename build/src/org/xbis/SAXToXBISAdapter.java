/*
Copyright (c) 2003-2008, Dennis M. Sosnoski.
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

import java.io.*;
import java.util.HashMap;
import java.util.Stack;

import org.jibx.runtime.impl.OutByteBuffer;
import org.xml.sax.*;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML Binary Information Set output adapter for SAX2 documents. This writes a compact
 * representation of the data in an XML document, with the advantages of
 * reduced document size and lower processing overhead as compared to the
 * standard text document representation.
 *
 * @author Dennis M. Sosnoski
 */
public class SAXToXBISAdapter extends DefaultHandler
implements LexicalHandler, DeclHandler
{
	/** Actual encoding output handler. */
	private SAX2EventWriter m_output;
	
	/** Flag for next character data content is CDATA. */
	private boolean m_isNextCData;

	/**
	 * Default constructor.
	 */
	public SAXToXBISAdapter() {
		m_output = new SAX2EventWriter();
	}

	/**
	 * Reset state information used during the serialization process. This
	 * reinitializes the serialization state so that an instance of this class
	 * can be reused to serialize multiple independent documents.
	 */
	public void reset() {
		m_output.reset();
	}

	/**
	 * Set output stream.
	 * 
	 * @param os output stream for writing document data
	 * @throws IOException on error writing to stream
	 */
	public void setStream(OutputStream os) throws IOException {
		m_output.setStream(os);
	}

    /**
     * Flush output. This forces out all buffered data.
     *
     * @throws IOException on error writing to stream
     */
    public final void flush() throws IOException {
        m_output.flush();
    }

	/**
	 * Start of document handler. Writes event stream header and start of
	 * document to event stream.
	 * 
	 * @throws SAXException on error writing to event stream
	 */
	public void startDocument() throws SAXException {
		try {
			m_output.writeDocumentStart();
		} catch (IOException ex) {
			throw new SAXException("Error writing to stream", ex);
		}
	}

	/**
	 * End of document handler. Writes end of document to event stream.
	 * 
	 * @throws SAXException on error writing to event stream
	 */
	public void endDocument() throws SAXException {
		try {
			m_output.writeDocumentEnd();
		} catch (IOException ex) {
			throw new SAXException("Error writing to stream", ex);
		}
	}

	/**
	 * Start of element handler. Writes element and attributes to event stream.
	 *
	 * @param uri namespace URI
	 * @param name local name of element
	 * @param raw raw element name
	 * @param atts attributes for element
	 * @throws SAXException on error writing to event stream
	 */
	public void startElement(String uri, String name,
		String raw, Attributes atts) throws SAXException {
		try {
            
            // find prefix for element namespace qualification
            String prefix = null;
            if (uri == null || uri.equals("")) {
                uri = null;
            } else if (raw != null && raw.length() > 0) {
                int split = raw.indexOf(':');
                if (split > 0) {
                    prefix = raw.substring(0, split);
                }
            } else {
                prefix = m_output.getElementPrefix(uri);
            }
            
            // write element with attributes flag
			boolean hasa = atts.getLength() > 0;
			m_output.writeElementStart(prefix, uri, name, hasa);
			if (hasa) {
                
                // write all attributes of element
				for (int i = 0; i < atts.getLength(); i++) {
                    
                    // get the attribute name information
					String auri = atts.getURI(i);
					String aname = atts.getLocalName(i);
                    String aprefix = null;
					if (auri.equals("")) {
						auri = null;
					} else {
                        String araw = atts.getQName(i);
                        if (araw != null && araw.length() > 0) {
                           int split = araw.indexOf(':');
                           if (split > 0) {
                               aprefix = araw.substring(0, split);
                           }
                       } else {
                           aprefix = m_output.getAttributePrefix(uri);
                       }
                    }
                    
                    // write attribute information
					m_output.writeElementAttribute(aprefix, auri, aname,
                        atts.getValue(i));
				}
                
                // finish with attribute end marker
				m_output.writeEndAttribute();
                
			}
		} catch (IOException ex) {
			throw new SAXException("Error writing to stream", ex);
		} catch (XBISException ex) {
            throw new SAXException("Error in parse event stream", ex);
        }
	}

	/**
	 * End of element handler. Writes element end to event stream.
	 *
	 * @param uri namespace URI
	 * @param name local name of element
	 * @param raw raw element name
	 * @throws SAXException on error writing to event stream
	 */
	public void endElement(String uri, String name, String raw)
		throws SAXException {
		try {
			m_output.writeElementEnd();
		} catch (IOException ex) {
			throw new SAXException("Error writing to stream", ex);
		}
	}

	/**
	 * Character data handler. Writes data to event stream.
	 *
	 * @param ch array supplying character data
	 * @param start starting offset in array
	 * @param length number of characters
	 * @throws SAXException on error writing to event stream
	 */
	public void characters(char[] ch, int start, int length)
		throws SAXException {
		try {
			if (m_isNextCData) {
				m_output.writeCDATA(ch, start, length);
			} else {
				m_output.writeCharData(ch, start, length);
			}
		} catch (IOException ex) {
			throw new SAXException("Error writing to stream", ex);
		}
	}

	/**
	 * Start of CDATA section notification handler. Just sets a flag to indicate
	 * that we're in CDATA.
	 */
	public void startCDATA() {
		m_isNextCData = true;
	}

	/**
	 * End of CDATA section notification handler. Just clears a flag to indicate
	 * that we're in normal character data.
	 */
	public void endCDATA() throws SAXException {
		m_isNextCData = false;
	}

	/**
	 * Processing instruction handler. Writes data to event stream.
	 *
	 * @param target processing instruction target
	 * @param data processing instruction data
	 * @throws SAXException on error writing to event stream
	 */
	public void processingInstruction(String target, String data)
		throws SAXException {
		try {
			m_output.writeProcessingInstruction(target, data);
		} catch (IOException ex) {
			throw new SAXException("Error writing to stream", ex);
		}
	}

	/**
	 * Comment handler. Writes data to event stream.
	 *
	 * @param ch array supplying character data
	 * @param start starting offset in array
	 * @param length number of characters
	 * @throws SAXException on error writing to event stream
	 */
	public void comment(char[] ch, int start, int length) throws SAXException {
		try {
			m_output.writeComment(ch, start, length);
		} catch (IOException ex) {
			throw new SAXException("Error writing to stream", ex);
		}
	}

	/**
	 * Start of namespace prefix mapping.
	 *
	 * @param prefix prefix used for namespace
	 * @param uri defining URI for namespace
     * @throws SAXException on error in XML handling
	 */
	public void startPrefixMapping(String prefix, String uri)
        throws SAXException {
    	try {
            m_output.startPrefixMapping(prefix, uri);
        } catch (XBISException ex) {
            throw new SAXException("XML processing error", ex);
        }
	}

	/**
	 * End of namespace prefix mapping.
	 *
	 * @param prefix prefix used for namespace
	 */
	public void endPrefixMapping(String prefix) {
		m_output.endPrefixMapping(prefix);
	}

	//
	// Supplementary method that are either ignored or passed on directly

	public void startDTD(String name, String publicId, String systemId)
        throws SAXException {
        try {
            m_output.writeDocumentType(name, publicId, systemId);
        } catch (IOException ex) {
            throw new SAXException("Error writing to stream", ex);
        }
    }

	public void endDTD() {}

	public void startEntity(String name) {}

	public void endEntity(String name) {}

    public void elementDecl(String name, String model) throws SAXException {
        try {
            m_output.writeElementDecl(name, model);
        } catch (IOException ex) {
            throw new SAXException("Error writing to stream", ex);
        }
    }

    public void internalEntityDecl(String name, String value) {}

    public void externalEntityDecl(String name, String publicId,
        String systemId) throws SAXException {
        try {
            m_output.writeExternalEntityDecl(name, publicId, systemId);
        } catch (IOException ex) {
            throw new SAXException("Error writing to stream", ex);
        }
    }

    public void attributeDecl(String eName, String aName, String type,
        String valueDefault, String value) throws SAXException {
        try {
            m_output.writeAttributeDecl(eName, aName, type, valueDefault, value);
        } catch (IOException ex) {
            throw new SAXException("Error writing to stream", ex);
        }
    }
    
    public void fatalError(SAXParseException e) throws SAXException {
        throw new SAXException("Fatal error", e);
    }

    public void ignorableWhitespace(char[] ch, int start, int length)
        throws SAXException {
        try {
            m_output.writeCharData(ch, start, length);
        } catch (IOException ex) {
            throw new SAXException("Error writing to stream", ex);
        }
    }

    public void notationDecl(String name, String publicId, String systemId)
        throws SAXException {
        try {
            m_output.writeNotation(name, publicId, systemId);
        } catch (IOException ex) {
            throw new SAXException("Error writing to stream", ex);
        }
    }

    public void skippedEntity(String name) throws SAXException {
        try {
            m_output.writeSkippedEntity(name);
        } catch (IOException ex) {
            throw new SAXException("Error writing to stream", ex);
        }
    }

    public void unparsedEntityDecl(String name, String publicId,
        String systemId, String notationName) throws SAXException {
        try {
            m_output.writeUnparsedEntity(name, publicId, systemId,
                notationName);
        } catch (IOException ex) {
            throw new SAXException("Error writing to stream", ex);
        }
    }

    /**
     * Event writer with extended namespace processing for SAX2. This is part of
     * the infrastructure required to compensate for SAX2's weaknesses in
     * namespace handling, which forces a choice between having namespaces
     * resolved by the parser (in which case you get namespace URIs, but not
     * necessarily prefix information) and doing all namespace processing in the
     * application. 
     */
    private static class SAX2EventWriter extends XBISEventWriter
    {
        /** No namespace namespace. */
        private final SAX2Namespace m_noNamespace;
    
        /** XML namespace. */
        private final SAX2Namespace m_xmlNamespace;
        
        /** Actual output buffer passed to base class. */
        private final OutByteBuffer m_byteBuffer;
    
        /** Map of URI to namespace entries. */
        private HashMap m_uriMap;
        
        /** Map of prefixes to current active namespace entries. */
        private HashMap m_prefixMap;
        
        /** Stack of active namespace mappings. */
        private Stack m_namespaceStack;
        
        /** Stack of namespace mappings with masked prefixes. */
        private Stack m_maskedStack;
        
        /**
         * Constructor.
         * 
         * @param buff 
         */
        public SAX2EventWriter(OutByteBuffer buff) {
            super(buff);
            m_noNamespace = new SAX2InitialNamespace("", NO_NAMESPACE);
            m_xmlNamespace = new SAX2InitialNamespace("xml", XML_NAMESPACE);
            m_byteBuffer = buff;
            m_uriMap = new HashMap();
            m_uriMap.put(NO_NAMESPACE, m_noNamespace);
            m_uriMap.put(XML_NAMESPACE, m_xmlNamespace);
            m_prefixMap = new HashMap();
            m_prefixMap.put("", m_noNamespace);
            m_prefixMap.put("xml", m_xmlNamespace);
            m_namespaceStack = new Stack();
            m_maskedStack = new Stack();
        }

        /**
         * Default constructor. This creates a new output buffer of the default
         * size.
         */
        public SAX2EventWriter() {
            this(new OutByteBuffer(DEFAULT_BUFFER_SIZE));
        }
        
        /**
         * Reset state information used during the serialization process. This
         * reinitializes the serialization state so that an instance of this
         * class can be reused to serialize multiple independent documents.
         */
        public void reset() {
            m_uriMap.clear();
            m_uriMap.put(NO_NAMESPACE, m_noNamespace);
            m_uriMap.put(XML_NAMESPACE, m_xmlNamespace);
            m_prefixMap.clear();
            m_prefixMap.put("", m_noNamespace);
            m_prefixMap.put("xml", m_xmlNamespace);
            m_namespaceStack.clear();
            m_maskedStack.clear();
            super.reset();
        }

        /**
         * Set output stream. This first flushes any pending writes and resets the
         * state to clear any data intended for another stream, then sets the new
         * output stream and writes the XBIS header information.
         *
         * @param os serialization output stream
         * @throws IOException on error writing to stream
         */
        public final void setStream(OutputStream os) throws IOException {
            updateBuffer();
            m_byteBuffer.finish();
            reset();
            m_byteBuffer.setOutput(os);
            initWrite(SAX2_SOURCE_ID);
        }
    
        /**
         * Get prefix for use with an element with a particular namespace URI.
         *
         * @param uri element namespace URI
         * @return prefix to use for element
         * @throws XBISException if no active definition
         */
        public String getElementPrefix(String uri) throws XBISException {
            SAX2InitialNamespace nsi = (SAX2InitialNamespace)m_uriMap.get(uri);
            return nsi.getElementNamespace().getPrefix();
        }
    
        /**
         * Get prefix for use with an attribute with a particular namespace URI.
         *
         * @param uri attribute namespace URI
         * @return prefix to use for attribute
         * @throws XBISException if no active definition
         */
        public String getAttributePrefix(String uri) throws XBISException {
            SAX2InitialNamespace nsi = (SAX2InitialNamespace)m_uriMap.get(uri);
            return nsi.getAttributeNamespace().getPrefix();
        }

        /**
         * Write namespace mapping. This declares the namespace and makes it
         * usable.
         *
         * @param prefix element namespace prefix
         * @param uri element namespace URI
         * @throws XBISException if namespace error
         */
        public void startPrefixMapping(String prefix, String uri)
            throws XBISException {
        
            // create namespace URI if not already defined
            Object obj = m_uriMap.get(uri);
            if (obj == null) {
        
                // first use of namespace, add tracking object to table
                obj = new SAX2InitialNamespace(prefix, uri);
                m_uriMap.put(uri, obj);
            }
    
            // get entry for this prefix with URI
            SAX2InitialNamespace ins = (SAX2InitialNamespace)obj;
            SAX2Namespace ns = ins.get(prefix);
    
            // set namespace mapping active
            ns.setActive(true);
            ns.startMapping();
            m_namespaceStack.push(ns);
        
            // handle prefix masking
            SAX2Namespace masked = (SAX2Namespace)m_prefixMap.get(prefix);
            m_maskedStack.push(masked);
            if (masked != null) {
                masked.setActive(false);
            }
            m_prefixMap.put(prefix, ns);
            
            // handle XBIS namespace processing
            beginNamespaceMapping(prefix, uri);
        }

        /**
         * End of namespace prefix mapping. Finds the namespace information and
         * marks it as inactive.
         *
         * @param prefix prefix used for namespace
         */
        public void endPrefixMapping(String prefix) {
            SAX2Namespace ns = (SAX2Namespace)m_namespaceStack.pop();
            ns.setActive(false);
            ns.endMapping();
            ns = (SAX2Namespace)m_maskedStack.pop();
            m_prefixMap.put(prefix, ns);
            if (ns != null) {
                ns.setActive(true);
            }
        }

        /**
         * Flush output. This forces out all buffered data.
         *
         * @throws IOException on error writing to stream
         */
        public final void flush() throws IOException {
            
            // update fill position and flush the stream buffer
            updateBuffer();
            m_byteBuffer.flush();
            
            // set the buffer again, to force update of cached information
            setBuffer(m_byteBuffer);
        }
    }

    /**
     * Extended namespace information for SAX2 processing. This is part of the
     * infrastructure required to compensate for SAX2's weaknesses in namespace
     * handling, which forces a choice between having namespaces resolved by the
     * parser (in which case you get namespace URIs, but not necessarily prefix
     * information) and doing all namespace processing in the application. 
     */
    private static class SAX2Namespace extends BasicNamespace
    {
        /** Flag for namespace is usable. */
        private boolean m_isActive;
        
        /** Owning initial namespace for URI. */
        private final SAX2InitialNamespace m_owner;
        
        /**
         * Constructor.
         *
         * @param prefix namespace prefix
         * @param uri namespace URI
         * @param owner owning initial namespace for URI
         */
        public SAX2Namespace(String prefix, String uri,
            SAX2InitialNamespace owner) {
            super(prefix, uri);
            m_owner = (owner == null) ? (SAX2InitialNamespace)this : owner;
        }
    
        /**
         * Start mapping for this namespace definition.
         */
        public void startMapping() {
            m_owner.pushDefine(this);
        }
    
        /**
         * End mapping for this namespace definition.
         */
        public void endMapping() {
            m_owner.popDefine(this);
        }
    
        /**
         * Set namespace active state.
         *
         * @param active <code>true</code> if active, <code>false</code> if not
         */
        protected void setActive(boolean active) {
            m_isActive = active;
        }
    
        /**
         * Check if namespace mapping active.
         *
         * @return <code>true</code> if active, <code>false</code> if not
         */
        public boolean isActive() {
            return m_isActive;
        }
    
        /**
         * Get owning URI information.
         *
         * @return information for the URI used by this namespace
         */
        public SAX2InitialNamespace getOwner() {
            return m_owner;
        }
    }

    /**
     * Initial extended namespace information for output processing. This is the
     * form used for the first namespace referencing a particular URI. It
     * provides extension information for access to other namespaces that
     * reference the same URI with other prefixes, including tracking for the
     * current active prefix.
     */
    private static class SAX2InitialNamespace extends SAX2Namespace
    {
        /** Map based on prefix used for this URI (lazy contruction, only if
         multiple definitions present). */
        private HashMap m_prefixMap;
        
        /** Stack of namespace definitions for this URI in current scope (lazy
         construction, only if multiple definitions present). */
        private Stack m_definitionStack;
        
        /** Current active namespace with or without prefix for this URI. */
        private SAX2Namespace m_activeNoPrefix;
        
        /** Current active namespace with prefix for this URI. */
        private SAX2Namespace m_activePrefix;
    
        /**
         * Constructor.
         *
         * @param prefix namespace prefix
         * @param uri namespace URI
         */
        public SAX2InitialNamespace(String prefix, String uri) {
            super(prefix, uri, null);
            m_definitionStack = new Stack();
        }
    
        /**
         * Get the namespace information for a particular prefix of this
         * namespace. If the prefix has already been defined for this URI the
         * existing information is returned, otherwise a new pairing is
         * constructed and returned.
         *
         * @param prefix 
         * @return namespace information
         */
        public SAX2Namespace get(String prefix) {
            if (getPrefix().equals(prefix)) {
                return this;
            } else {
                SAX2Namespace ns = null;
                if (m_prefixMap != null) {
                    ns = (SAX2Namespace)m_prefixMap.get(prefix);
                }
                if (ns == null) {
                    ns = new SAX2Namespace(prefix, getUri(), this);
                    if (m_prefixMap == null) {
                        m_prefixMap = new HashMap();
                    }
                    m_prefixMap.put(prefix, ns);
                }
                return ns;
            }
        }
    
        /**
         * Push namespace definition for this URI.
         *
         * @param ns definition to be pushed
         */
        public void pushDefine(SAX2Namespace ns) {
            m_definitionStack.push(ns);
            if (ns.getPrefix().length() == 0) {
                m_activeNoPrefix = ns;
            } else {
                m_activePrefix = ns;
            }
        }
    
        /**
         * Pop namespace definition from stack.
         */
        public void popDefine(SAX2Namespace ns) {
            m_definitionStack.pop();
        }
    
        /**
         * Get namespace for use with an element with this namespace URI.
         *
         * @return namespace for use with element
         * @throws XBISException if no active definition
         */
        public SAX2Namespace getElementNamespace() throws XBISException {
            if (m_activeNoPrefix != null && m_activeNoPrefix.isActive()) {
                return m_activeNoPrefix;
            } else if (m_activePrefix != null && m_activePrefix.isActive()) {
                return m_activePrefix;
            } else {
                for (int i = m_definitionStack.size()-1; i >= 0; i--) {
                    SAX2Namespace ns =
                        (SAX2Namespace)m_definitionStack.get(i);
                    if (ns.isActive()) {
                        if (ns.getPrefix().length() == 0) {
                            m_activeNoPrefix = ns;
                        } else {
                            m_activePrefix = ns;
                        }
                        return ns;
                    }
                }
                throw new XBISException("No usable namespace definition");
            }
        }
    
        /**
         * Get namespace for use with an attribute with this namespace URI.
         *
         * @return namespace for use with attribute
         * @throws XBISException if no active definition
         */
        public SAX2Namespace getAttributeNamespace() throws XBISException {
            if (m_activePrefix != null && m_activePrefix.isActive()) {
                return m_activePrefix;
            } else {
                for (int i = m_definitionStack.size()-1; i >= 0; i--) {
                    SAX2Namespace ns =
                        (SAX2Namespace)m_definitionStack.get(i);
                    if (ns.isActive() && ns.getPrefix().length() > 0) {
                        m_activePrefix = ns;
                        return ns;
                    }
                }
                throw new XBISException("No usable namespace definition");
            }
        }
    }
}