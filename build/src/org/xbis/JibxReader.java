/*
 * Copyright (c) 2007-2008, Dennis M. Sosnoski. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution. Neither the name of
 * XBIS nor the names of its contributors may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.xbis;

import java.io.IOException;

import org.jibx.runtime.IXMLReader;
import org.jibx.runtime.JiBXException;
import org.jibx.runtime.impl.IInByteBuffer;

import com.sosnoski.util.stack.ObjectStack;

/**
 * JiBX reader implementation. This allows JiBX to directly read XBIS representations of documents.
 * 
 * TODO: implement the namespace stack handling? Perhaps not actually needed, with 2.0 hopefully getting started soon
 * 
 * @author Dennis M. Sosnoski
 */
public class JibxReader extends XBISReader implements IXMLReader
{
    private static final NamespaceImpl s_noNamespaceNamespace = new NamespaceImpl("", NO_NAMESPACE);
    
    private static final NamespaceImpl s_xmlNamespace = new NamespaceImpl("xml", XML_NAMESPACE);
    
    private static final int INITIAL_ATTRIBUTE_COUNT = 10;
    
    private int m_state;
    
    private String m_text;
    
    private NameImpl m_element;
    
    private int m_attributeCount;
    
    private NameImpl[] m_attributeNames;
    
    private String[] m_attributeValues;
    
    private ObjectStack m_elementStack;
    
    /**
     * Default constructor. Allocates and initializes instances of the data
     * tables used for storing state information during the serialization
     * process.
     */
    public JibxReader() {
        m_attributeNames = new NameImpl[INITIAL_ATTRIBUTE_COUNT];
        m_attributeValues = new String[INITIAL_ATTRIBUTE_COUNT];
        m_elementStack = new ObjectStack();
    }

    /**
     * Constructor with buffer supplied.
     * 
     * @param buff
     */
    public JibxReader(IInByteBuffer buff) {
        this();
        setBuffer(buff);
    }
    
    /**
     * Build name instance.
     * 
     * @param ns namespace for name
     * @param local local name
     * @return constructed name object
     */
    protected Object buildName(Object ns, String local) {
        return new NameImpl(local, (NamespaceImpl)ns);
    }
    
    /**
     * Build namespace instance.
     * 
     * @param prefix namespace prefix
     * @param uri namespace URI
     * @return constructed namespace object
     */
    protected Object buildNamespace(String prefix, String uri) {
        return new NamespaceImpl(prefix, uri);
    }
    
    /**
     * Check if a namespace is active.
     * 
     * @param obj namespace object
     * @return <code>true</code> if active, <code>false</code> if not
     */
    protected boolean isActiveNamespace(Object obj) {
        return ((NamespaceImpl)obj).isActive();
    }
    
    /**
     * Initialize state information used during the serialization process. This implementation of the abstract base
     * class method is specific to JiBX.
     */
    protected void initState() {
        try {
            NamespaceImpl ns = new NamespaceImpl("", NO_NAMESPACE);
            addNamespace(ns);
            activateNamespace(ns);
            ns = new NamespaceImpl("xml", XML_NAMESPACE);
            addNamespace(ns);
            activateNamespace(ns);
        } catch (XBISException e) {
            // can safely ignore error occurring here
        }
    }

    /**
     * Initialize input, reading and verifying the header information.
     *
     * @throws IOException on error reading from stream
     */
    public void init() throws IOException {
        initInput();
    }
    
    /**
     * Build current parse input position description.
     * 
     * @return placeholder text
     */
    public String buildPositionString() {
        return "unknown location";
    }
    
    /**
     * Advance to next binding component of input document. This is a higher-level operation than {@link #nextToken()},
     * which consolidates text content and ignores parse events for components such as comments and PIs.
     * 
     * @return parse event type code
     * @throws JiBXException if error reading or parsing document
     */
    public int next() throws JiBXException {
        while (true) {
            int event = nextToken();
            switch (event)
            {
                case START_DOCUMENT:
                case END_DOCUMENT:
                case START_TAG:
                case END_TAG:
                case TEXT:
                case CDSECT:
                    return event;
            }
        }
    }
    
    /**
     * Gets the current parse event type, without changing the current parse state.
     * 
     * @return parse event type code
     * @throws JiBXException if error parsing document
     */
    public int getEventType() throws JiBXException {
        return m_state;
    }
    
    /**
     * Get element name from the current start or end tag.
     * 
     * @return local name if namespace handling enabled, full name if namespace handling disabled
     * @throws IllegalStateException if not at a start or end tag (optional)
     */
    public String getName() {
        if (m_state == START_TAG || m_state == END_TAG) {
            return m_element.getLocalName();
        } else {
            throw new IllegalStateException("Internal error - not START_TAG or END_TAG state " + m_state);
        }
    }
    
    /**
     * Get element namespace from the current start or end tag.
     * 
     * @return namespace URI if namespace handling enabled and element is in a namespace, empty string otherwise
     * @throws IllegalStateException if not at a start or end tag (optional)
     */
    public String getNamespace() {
        if (m_state == START_TAG || m_state == END_TAG) {
            return m_element.getNamespace().getUri();
        } else {
            throw new IllegalStateException("Internal error - not START_TAG or END_TAG state " + m_state);
        }
    }
    
    /**
     * Get element prefix from the current start or end tag.
     * 
     * @return prefix text (<code>null</code> if no prefix)
     * @throws IllegalStateException if not at a start or end tag
     */
    public String getPrefix() {
        if (m_state == START_TAG || m_state == END_TAG) {
            String prefix = m_element.getNamespace().getPrefix();
            if (prefix.length() == 0) {
                return null;
            } else {
                return prefix;
            }
        } else {
            throw new IllegalStateException("Internal error - not START_TAG or END_TAG state " + m_state);
        }
    }
    
    /**
     * Get the number of attributes of the current start tag.
     * 
     * @return number of attributes
     * @throws IllegalStateException if not at a start tag (optional)
     */
    public int getAttributeCount() {
        if (m_state == START_TAG) {
            return m_attributeCount;
        } else {
            throw new IllegalStateException("Internal error - not START_TAG state " + m_state);
        }
    }
    
    /**
     * Get an attribute name from the current start tag.
     * 
     * @param index attribute index
     * @return local name if namespace handling enabled, full name if namespace handling disabled
     * @throws IllegalStateException if not at a start tag or invalid index
     */
    public String getAttributeName(int index) {
        if (m_state == START_TAG) {
            if (index < m_attributeCount) {
                return m_attributeNames[index].getLocalName();
            } else {
                throw new IllegalStateException("Internal error - index past maximum attribute");
            }
        } else {
            throw new IllegalStateException("Internal error - not START_TAG state " + m_state);
        }
    }
    
    /**
     * Get an attribute namespace from the current start tag.
     * 
     * @param index attribute index
     * @return namespace URI if namespace handling enabled and attribute is in a namespace, empty string otherwise
     * @throws IllegalStateException if not at a start tag or invalid index
     */
    public String getAttributeNamespace(int index) {
        if (m_state == START_TAG) {
            if (index < m_attributeCount) {
                return m_attributeNames[index].getNamespace().getUri();
            } else {
                throw new IllegalStateException("Internal error - index past maximum attribute");
            }
        } else {
            throw new IllegalStateException("Internal error - not START_TAG state " + m_state);
        }
    }
    
    /**
     * Get an attribute prefix from the current start tag.
     * 
     * @param index attribute index
     * @return prefix for attribute (<code>null</code> if no prefix present)
     * @throws IllegalStateException if not at a start tag or invalid index
     */
    public String getAttributePrefix(int index) {
        if (m_state == START_TAG) {
            if (index < m_attributeCount) {
                String prefix = m_attributeNames[index].getNamespace().getPrefix();
                if (prefix.length() == 0) {
                    return null;
                } else {
                    return prefix;
                }
            } else {
                throw new IllegalStateException("Internal error - index past maximum attribute");
            }
        } else {
            throw new IllegalStateException("Internal error - not START_TAG state " + m_state);
        }
    }
    
    /**
     * Get an attribute value from the current start tag.
     * 
     * @param index attribute index
     * @return value text
     * @throws IllegalStateException if not at a start tag or invalid index
     */
    public String getAttributeValue(int index) {
        if (m_state == START_TAG) {
            if (index < m_attributeCount) {
                return m_attributeValues[index];
            } else {
                throw new IllegalStateException("Internal error - index past maximum attribute");
            }
        } else {
            throw new IllegalStateException("Internal error - not START_TAG state " + m_state);
        }
    }
    
    /**
     * Check if a namespace URI string matches a namespace.
     * 
     * @param uri namespace URI (may be <code>null</code>
     * @param ns
     * @return <code>true</code> if matched, <code>false</code> if not
     */
    private static boolean matchNamespace(String uri, NamespaceImpl ns) {
        if (uri == null || uri.length() == 0) {
            return ns.getUri().length() == 0;
        } else {
            return ns.getUri().equals(uri);
        }
    }
    
    /**
     * Get an attribute value from the current start tag.
     * 
     * @param ns namespace URI for expected attribute (may be <code>null</code> or the empty string for the empty
     * namespace)
     * @param name attribute name expected
     * @return attribute value text, or <code>null</code> if missing
     * @throws IllegalStateException if not at a start tag
     */
    public String getAttributeValue(String ns, String name) {
        if (m_state == START_TAG) {
            for (int i = 0; i < m_attributeCount; i++) {
                NameImpl aname = m_attributeNames[i];
                if (aname.getLocalName().equals(name) && matchNamespace(ns, aname.getNamespace())) {
                    return m_attributeValues[i];
                }
            }
            return null;
        } else {
            throw new IllegalStateException("Internal error - not START_TAG state " + m_state);
        }
    }
    
    /**
     * Get current text. When positioned on a TEXT event this returns the actual text; for CDSECT it returns the text
     * inside the CDATA section; for COMMENT, DOCDECL, or PROCESSING_INSTRUCTION it returns the text inside the
     * structure. TODO: implement extra cases
     * 
     * @return text for current event
     */
    public String getText() {
        if (m_state == TEXT || m_state == CDSECT) {
            return m_text;
        } else {
            throw new IllegalStateException("Internal error - not a text state " + m_state);
        }
    }
    
    /**
     * Get current element nesting depth. The returned depth always includes the current start or end tag (if positioned
     * on a start or end tag).
     * 
     * @return element nesting depth
     */
    public int getNestingDepth() {
        // TODO
        throw new IllegalStateException("Internal error - not yet implemented");
    }
    
    /**
     * Get number of namespace declarations active at depth.
     * 
     * @param depth element nesting depth
     * @return number of namespaces active at depth
     * @throws IllegalArgumentException if invalid depth
     */
    public int getNamespaceCount(int depth) {
        // TODO
        throw new IllegalStateException("Internal error - not yet implemented");
    }
    
    /**
     * Get namespace URI.
     * 
     * @param index declaration index
     * @return namespace URI
     * @throws IllegalArgumentException if invalid index
     */
    public String getNamespaceUri(int index) {
        // TODO
        throw new IllegalStateException("Internal error - not yet implemented");
    }
    
    /**
     * Get namespace prefix.
     * 
     * @param index declaration index
     * @return namespace prefix, <code>null</code> if a default namespace
     * @throws IllegalArgumentException if invalid index
     */
    public String getNamespacePrefix(int index) {
        // TODO
        throw new IllegalStateException("Internal error - not yet implemented");
    }
    
    /**
     * Get document name.
     * 
     * @return <code>null</code> for name not known
     */
    public String getDocumentName() {
        return null;
    }
    
    /**
     * Get current source line number.
     * 
     * @return <code>-1</code> to indicate line number information not available
     */
    public int getLineNumber() {
        return -1;
    }
    
    /**
     * Get current source column number.
     * 
     * @return <code>-1</code> to indicate column number information not available
     */
    public int getColumnNumber() {
        return -1;
    }
    
    /**
     * Get namespace URI associated with prefix.
     * 
     * @param prefix to be found
     * @return associated URI (<code>null</code> if prefix not defined)
     */
    public String getNamespace(String prefix) {
        // TODO
        throw new IllegalStateException("Internal error - not yet implemented");
    }
    
    /**
     * Return the input encoding, if known.
     * 
     * @return always <code>null</code>
     */
    public String getInputEncoding() {
        return null;
    }
    
    /**
     * Return namespace processing flag.
     * 
     * @return always <code>true</code>
     */
    public boolean isNamespaceAware() {
        return true;
    }
    
    /**
     * Advance to next parse event of input document.
     * 
     * @return parse event type code
     * @throws JiBXException if error reading or parsing document
     */
    public int nextToken() throws JiBXException {
        try {
            
            // clear current token state
            if (isReset()) {
                clearReset();
            }
            m_state = -1;
            int lead;
            while (!isEnd() && (lead = readByte()) != 0) {
                
                // check for special types of indicators with bit compares
                if ((lead & NODE_ELEMENT_FLAG) != 0) {
                    
                    // get the basic element information
                    m_element = (NameImpl)readQuickElement(lead);
                    
                    // handle attributes if included
                    m_attributeCount = 0;
                    if ((lead & ELEMENT_HASATTRIBUTES_FLAG) != 0) {
                        while ((lead = readByte()) != 0) {
                            
                            // get attribute name
                            NameImpl aname = (NameImpl)readQuickAttribute(lead);
                            
                            // get attribute value
                            String value;
                            if ((lead & ATTRIBUTE_VALUEREF_FLAG) != 0) {
                                if ((lead & ATTRIBUTE_NEWREF_FLAG) != 0) {
                                    value = readString();
                                    if (m_attrValueTable == null) {
                                        m_attrValueTable = new String[INITIAL_HANDLE_SIZE];
                                        m_attrValueCount = 0;
                                    } else if (m_attrValueCount == m_attrValueTable.length) {
                                        m_attrValueTable = doubleArray(m_attrValueTable);
                                    }
                                    m_attrValueTable[m_attrValueCount++] = value;
                                } else {
                                    value = m_attrValueTable[readValue() - 1];
                                }
                            } else {
                                value = readString();
                            }
                            
                            // add attribute to element
                            if (m_attributeCount >= m_attributeNames.length) {
                                NameImpl[] holdnames = m_attributeNames;
                                String[] holdvalues = m_attributeValues;
                                int size = m_attributeCount * 2;
                                m_attributeNames = new NameImpl[size];
                                m_attributeValues = new String[size];
                                System.arraycopy(holdnames, 0, m_attributeNames, 0, m_attributeCount);
                                System.arraycopy(holdvalues, 0, m_attributeValues, 0, m_attributeCount);
                            }
                            m_attributeNames[m_attributeCount] = aname;
                            m_attributeValues[m_attributeCount++] = value;
                            
                        }
                    }
                    
                    // record and report element
                    m_elementStack.push(m_element);
                    m_state = START_TAG;
                    return m_state;
                    
                } else if ((lead & NODE_PLAINTEXT_FLAG) != 0) {
                    
                    // just read the text directly
                    m_text = readPlainText(lead);
                    m_state = TEXT;
                    return m_state;
                    
                } else if ((lead & NODE_TEXTREF_FLAG) != 0) {
                    
                    // set text from char array
                    m_text = new String(readCharsDef(lead));
                    m_state = TEXT;
                    return m_state;
                    
                } else if ((lead & NODE_NAMESPACEDECL_FLAG) != 0) {
                    readNamespaceDecl(lead);
                } else {
                    
                    // just skip over everything else except CDATA
                    switch (lead)
                    {
                        
                        case NODE_TYPE_CDATA:
                            m_text = readString();
                            m_state = CDSECT;
                            return m_state;
                            
                        case NODE_TYPE_ATTRIBUTEDECL:

                            // discard five strings (with fall-through)
                            readStringChars();
                            
                        case NODE_TYPE_UNPARSEDENTITY:

                            // discard four strings (with fall-through)
                            readStringChars();
                            
                        case NODE_TYPE_DOCTYPE:
                        case NODE_TYPE_NOTATION:
                        case NODE_TYPE_EXTERNALENTITYDECL:

                            // discard three strings (with fall-through)
                            readStringChars();
                            
                        case NODE_TYPE_PI:
                        case NODE_TYPE_ELEMENTDECL:

                            // discard two strings (with fall-through)
                            readStringChars();
                            
                        case NODE_TYPE_COMMENT:
                        case NODE_TYPE_SKIPPEDENTITY:

                            // discard a single string
                            readStringChars();
                            break;
                        
                        default:
                            throw new IllegalArgumentException("Unknown node type " + lead);
                    }
                }
            }
            
            // end of list, either end of element or end of document
            if (m_elementStack.isEmpty()) {
                m_state = END_DOCUMENT;
            } else {
                m_element = (NameImpl)m_elementStack.pop();
                m_state = END_TAG;
            }
            return m_state;
            
        } catch (IOException e) {
            throw new JiBXException("Error reading document", e);
        } catch (XBISException e) {
            throw new JiBXException("Error decoding document", e);
        }
    }
    
    /**
     * Basic name implementation class.
     */
    private static class NameImpl
    {
        /** Local name. */
        private final String m_local;
        
        /** Namespace information. */
        private final NamespaceImpl m_namespace;
        
        /**
         * Constructor.
         * 
         * @param name element or attribute name
         * @param ns namespace for name
         */
        public NameImpl(String name, NamespaceImpl ns) {
            m_local = name;
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
        public NamespaceImpl getNamespace() {
            return m_namespace;
        }
    }
    
    /**
     * Basic namespace implementation class used for SAX2 processing. This handles nested redeclarations of the same
     * namespace by counting.
     */
    private static class NamespaceImpl extends BasicNamespace
    {
        private int m_nestingCount;
        
        private NameImpl m_attribute;
        
        /**
         * Constructor.
         * 
         * @param prefix namespace prefix
         * @param uri namespace URI
         */
        public NamespaceImpl(String prefix, String uri) {
            super(prefix, uri);
        }
        
        /**
         * Increment active nesting level.
         */
        public void incrementNesting() {
            m_nestingCount++;
        }
        
        /**
         * Decrement active nesting level.
         */
        public void decrementNesting() {
            m_nestingCount--;
        }
        
        /**
         * Check if namespace mapping active.
         * 
         * @return <code>true</code> if active, <code>false</code> if not
         */
        public boolean isActive() {
            return m_nestingCount > 0;
        }
    }
}