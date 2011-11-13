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

package org.xbis.eventstore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.xbis.*;
import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Handler for feeding XML parse event store from a SAX parser.
 *
 * @author Dennis M. Sosnoski
 * @version 1.0
 */

public class SAXWriter extends DefaultHandler implements LexicalHandler
{
    //
    // Fixed namespaces

	private static final String NO_NAMESPACE = "";
	private static final String XML_NAMESPACE =
		"http://www.w3.org/XML/1998/namespace";
	
	/** No namespace namespace. */
	private SAXNamespace m_noNamespace;
	
	/** XML namespace. */
	private SAXNamespace m_xmlNamespace;
	
	/** Map of URI to namespace entries. */
	private HashMap m_uriMap;
	
	/** Most-nested prefix set for each URI. */
	private ArrayList m_uriPrefixes;
	
	/** Map of prefixes to current active namespace entries. */
	private HashMap m_prefixMap;
	
	/** Stack of current active namespace mappings. */
	private Stack m_namespaceStack;
	
	/** Stack of namespace mappings with masked prefixes. */
	private Stack m_maskedStack;
    
    /** Flag for next text is CDATA section. */
    private boolean m_isNextCData;
    
    /** Event store for document. */
    private EventStore m_eventStore;

	/**
	 * Default constructor.
     * 
     * @param es event store for parse events
	 */

	public SAXWriter(EventStore es) {
		SAXInitialNamespace inn = new SAXInitialNamespace("", NO_NAMESPACE);
        inn.setIndex(0);
		m_noNamespace = inn.get("");
		inn = new SAXInitialNamespace("xml", XML_NAMESPACE);
        inn.setIndex(1);
		m_xmlNamespace = inn.get("xml");
		m_uriMap = new HashMap();
		m_uriMap.put("", m_noNamespace);
		m_uriPrefixes = new ArrayList();
		m_uriPrefixes.add(m_noNamespace);
		m_prefixMap = new HashMap();
		m_prefixMap.put("", m_noNamespace);
        m_prefixMap.put("xml", m_xmlNamespace);
		m_namespaceStack = new Stack();
		m_maskedStack = new Stack();
        m_eventStore = es;
	}

	/**
	 * Reset state information used during the serialization process. This
	 * reinitializes the serialization state so that an instance of this class
	 * can be reused to serialize multiple independent documents.
	 */

	public void reset() {
		m_uriMap.clear();
		m_uriMap.put("", m_noNamespace.getOwner());
		m_uriPrefixes.clear();
		m_prefixMap.clear();
		m_prefixMap.put("", m_noNamespace);
		m_noNamespace.setActive(true);
		m_prefixMap.put("xml", m_xmlNamespace);
		m_xmlNamespace.setActive(true);
		m_namespaceStack.clear();
		m_maskedStack.clear();
        m_eventStore.reset();
	}

	/**
	 * Start of document handler. Writes event stream header and start of
	 * document to event store.
	 */

	public void startDocument() throws SAXException {
        m_eventStore.appendEvent(EventStore.START_DOCUMENT);
        m_eventStore.appendNs(m_noNamespace);
        m_eventStore.appendNs(m_xmlNamespace);
	}

	/**
	 * End of document handler. Writes end of document to event store.
	 */

	public void endDocument() {
        m_eventStore.appendEvent(EventStore.END_DOCUMENT);
	}

	/**
	 * Start of element handler. Writes element and attributes to event store.
	 *
	 * @param uri namespace URI
	 * @param name local name of element
	 * @param raw raw element name
	 * @param atts attributes for element
	 * @throws SAXException on error writing to event stream
	 */

	public void startElement(String uri, String name,
		String raw, Attributes atts) throws SAXException {
		
		// find namespace for element
		SAXNamespace ns;
		if (uri == null || uri.equals("")) {
			ns = m_noNamespace;
		} else if (raw != null && raw.length() > 0) {
			int split = raw.indexOf(':');
			if (split < 0) {
				split = 0;
			}
			ns = (SAXNamespace)m_prefixMap.get(raw.substring(0, split));
		} else {
			SAXInitialNamespace ins = (SAXInitialNamespace)m_uriMap.get(uri);
			ns = ins.getElementNamespace();
		}
		
        // write element start followed by attribute count and attributes
        m_eventStore.appendElementStart(ns, name, atts.getLength());
		for (int i = 0; i < atts.getLength(); i++) {
			String auri = atts.getURI(i);
			String aname = atts.getLocalName(i);
			String aqname = atts.getQName(i);
			if (auri == null || auri.equals("")) {
				ns = m_noNamespace;
			} else if (aqname != null && aqname.length() > 0) {
				int split = aqname.indexOf(':');
				ns = (SAXNamespace)m_prefixMap.get
					(aqname.substring(0, split));
			} else {
				SAXInitialNamespace ins =
					(SAXInitialNamespace)m_uriMap.get(uri);
				ns = ins.getAttributeNamespace();
			}
			m_eventStore.appendAttribute(ns, aname, atts.getValue(i));
		}
	}

	/**
	 * End of element handler. Writes element end to event store.
	 *
	 * @param uri namespace URI
	 * @param name local name of element
	 * @param raw raw element name
	 */

	public void endElement(String uri, String name, String raw) {
        m_eventStore.appendEvent(EventStore.END_TAG);
	}

	/**
	 * Character data handler. Writes data to event store.
	 *
	 * @param ch array supplying character data
	 * @param start starting offset in array
	 * @param length number of characters
	 */

	public void characters(char[] ch, int start, int length) {
        m_eventStore.appendEvent(m_isNextCData ?
            EventStore.CDSECT : EventStore.TEXT);
        m_eventStore.appendEvent((short)length);
        m_eventStore.appendChars(ch, start, length);
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
	 * Processing instruction handler. Writes data to event store.
	 *
	 * @param target processing instruction target
	 * @param data processing instruction data
	 */

	public void processingInstruction(String target, String data) {
        m_eventStore.appendEvent(EventStore.PROCESSING_INSTRUCTION);
        m_eventStore.appendString(target);
        m_eventStore.appendString(data);
	}

	/**
	 * Comment handler. Writes data to event store.
	 *
	 * @param ch array supplying character data
	 * @param start starting offset in array
	 * @param length number of characters
	 */

	public void comment(char[] ch, int start, int length) throws SAXException {
        m_eventStore.appendEvent(EventStore.COMMENT);
        m_eventStore.appendEvent((short)length);
        m_eventStore.appendChars(ch, start, length);
	}

	/**
	 * Start of namespace prefix mapping. Finds or adds the namespace
	 * information and sets it as active, then writes the mapping start to the
	 * store.
	 *
	 * @param prefix prefix used for namespace
	 * @param uri defining URI for namespace
	 */

	public void startPrefixMapping(String prefix, String uri) {
		
		// create namespace URI if not already defined
        SAXInitialNamespace ins = (SAXInitialNamespace)m_uriMap.get(uri);
		if (ins == null) {
			
			// first use of namespace, add tracking object to table
			ins = new SAXInitialNamespace(prefix, uri);
			m_uriMap.put(uri, ins);
		}
		
		// get entry for this prefix with URI
		SAXNamespace ns = ins.get(prefix);
        if (ns.getIndex() < 0) {
            ns.setIndex(m_eventStore.appendNs(ns));
        }
		
		// set namespace mapping active
		ns.setActive(true);
		ns.startMapping();
		m_namespaceStack.push(ns);
		
		// handle prefix masking
		SAXNamespace masked = (SAXNamespace)m_prefixMap.get(prefix);
		m_maskedStack.push(masked);
		if (masked != null) {
			masked.setActive(false);
		}
		m_prefixMap.put(prefix, ns);
		
		// write namespace mapping to store
        m_eventStore.appendEvent(EventStore.START_NAMESPACE);
        m_eventStore.appendEvent((short)ns.getIndex());
	}

	/**
	 * End of namespace prefix mapping. Finds the namespace information and
	 * marks it as inactive.
	 *
	 * @param prefix prefix used for namespace
	 * @throws SAXException on error writing to event stream
	 */

	public void endPrefixMapping(String prefix) throws SAXException {
		SAXNamespace ns = (SAXNamespace)m_namespaceStack.pop();
		ns.setActive(false);
		ns.endMapping();
		ns = (SAXNamespace)m_maskedStack.pop();
		m_prefixMap.put(prefix, ns);
		if (ns != null) {
			ns.setActive(true);
		}
	}

	//
	// Unused methods required by LexicalHandler interface

	public void startDTD(String name, String publicId, String systemId) {}

	public void endDTD() {}

	public void startEntity(String name) {}

	public void endEntity(String name) {}

    /**
     * Extended namespace information for SAX2 processing. This adds tracking of
     * the current active state of a namespace to the basic namespace
     * definition.
     */
    
    private static class SAXNamespace extends BasicNamespace
    {
        /** Owning initial namespace for URI. */
        private final SAXInitialNamespace m_owner;
        
        /** Flag for namespace mapping active. */
        private boolean m_isActive;
        
        /** Index number for this namespace. */
        private int m_index;
        
        /**
         * Constructor.
         *
         * @param prefix namespace prefix
         * @param uri namespace URI
         * @param owner owning initial namespace for URI
         */
        
        public SAXNamespace(String prefix, String uri,
            SAXInitialNamespace owner) {
            super(prefix, uri);
            m_owner = (owner == null) ? (SAXInitialNamespace)this : owner;
            m_isActive = true;
            m_index = -1;
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
         * Check if namespace mapping active.
         *
         * @return <code>true</code> if active, <code>false</code> if not
         */
        
        public boolean isActive() {
            return m_isActive;
        }
    
        /**
         * Set namespace mapping active state.
         *
         * @param active <code>true</code> if active, <code>false</code> if not
         */
        
        public void setActive(boolean active) {
            m_isActive = active;
        }
    
        /**
         * Get owning URI information.
         *
         * @return information for the URI used by this namespace
         */
        
        public SAXInitialNamespace getOwner() {
            return m_owner;
        }
    
        /**
         * Set the namespace index.
         * 
         * @param index number of this namespace
         */
        
        public void setIndex(int index) {
            m_index = index;
        }
    
        /**
         * Get the namespace index.
         * 
         * @return index number of this namespace
         */
        
        public int getIndex() {
            return m_index;
        }
    }

    /**
     * Initial extended namespace information for SAX2 processing. This is the
     * form used for the first namespace referencing a particular URI. It
     * provides extension information for access to other namespaces that
     * reference the same URI with other prefixes, including tracking for the
     * current active prefix.
     */
    
    private static class SAXInitialNamespace extends SAXNamespace
    {
        /** Map based on prefix used for this URI (lazy contruction, only if
         multiple definitions present). */
        private HashMap m_prefixMap;
        
        /** Stack of namespace definitions for this URI in current scope (lazy
         construction, only if multiple definitions present). */
        private Stack m_definitionStack;
        
        /** Current active namespace with or without prefix for this URI. */
        private SAXNamespace m_activeNoPrefix;
        
        /** Current active namespace with prefix for this URI. */
        private SAXNamespace m_activePrefix;
    
        /**
         * Constructor.
         *
         * @param prefix namespace prefix
         * @param uri namespace URI
         */
        
        public SAXInitialNamespace(String prefix, String uri) {
            super(prefix, uri, null);
            m_definitionStack = new Stack();
        }
    
        /**
         * Get the namespace information for a particular prefix of this
         * namespace. If the prefix has already been defined for this URI the
         * existing information is returned, otherwise a new pairing is
         * constructed and returned.
         *
         * @return namespace information
         */
        
        public SAXNamespace get(String prefix) {
            if (getPrefix().equals(prefix)) {
                return this;
            } else {
                SAXNamespace ns = null;
                if (m_prefixMap != null) {
                    ns = (SAXNamespace)m_prefixMap.get(prefix);
                }
                if (ns == null) {
                    ns = new SAXNamespace(prefix, getUri(), this);
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
         * @param namespace definition to be pushed
         */
        
        public void pushDefine(SAXNamespace ns) {
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
        
        public void popDefine(SAXNamespace ns) {
            m_definitionStack.pop();
        }
    
        /**
         * Get namespace for use with an element with this namespace URI.
         *
         * @return namespace for use with element (<code>null</code> if no
         * active definition of namespace)
         * @throws SAXException if no active definition
         */
        
        public SAXNamespace getElementNamespace() {
            if (m_activeNoPrefix != null && m_activeNoPrefix.isActive()) {
                return m_activeNoPrefix;
            } else if (m_activePrefix != null && m_activePrefix.isActive()) {
                return m_activePrefix;
            } else {
                for (int i = m_definitionStack.size()-1; i >= 0; i--) {
                    SAXNamespace ns = (SAXNamespace)m_definitionStack.get(i);
                    if (ns.isActive()) {
                        if (ns.getPrefix().length() == 0) {
                            m_activeNoPrefix = ns;
                        } else {
                            m_activePrefix = ns;
                        }
                        return ns;
                    }
                }
                return null;
            }
        }
    
        /**
         * Get namespace for use with an attribute with this namespace URI.
         *
         * @return namespace for use with attribute (<code>null</code> if no
         * active definition of namespace)
         */
        
        public SAXNamespace getAttributeNamespace() {
            if (m_activePrefix != null && m_activePrefix.isActive()) {
                return m_activePrefix;
            } else {
                for (int i = m_definitionStack.size()-1; i >= 0; i--) {
                    SAXNamespace ns = (SAXNamespace)m_definitionStack.get(i);
                    if (ns.isActive() && ns.getPrefix().length() > 0) {
                        m_activePrefix = ns;
                        return ns;
                    }
                }
                return null;
            }
        }
    }
}