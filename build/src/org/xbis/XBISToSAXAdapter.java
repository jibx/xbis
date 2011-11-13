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

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import org.jibx.runtime.impl.InByteBuffer;
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
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;

/**
 * XML Binary Information Set input adapter for SAX2. This reads the XBIS
 * representation of a document and generates a SAX2 event stream as output.
 * It only supports operation with namespaces enabled. It really shouldn't need
 * to support reporting qualified (with namespace prefix) versions of names,
 * since this is an optional SAX2 feature, but <i>does</i> support this so that
 * it can be used as a source for XSLT transformations in JAXP (the JAXP code
 * included in JDK 1.4.1 sets this feature and assumes it'll be accepted,
 * silently dropping any exceptions in the process - somebody should do a proper
 * QA check on this garbage code being shipped as part of the core Java
 * libraries).
 *
 * @author Dennis M. Sosnoski
 */
public class XBISToSAXAdapter extends XBISReader implements XMLReader
{
    private static final int DEFAULT_ATTRIBUTE_COUNT = 10;
    
    private static final NamespaceImpl s_noNamespaceNamespace
        = new NamespaceImpl("", NO_NAMESPACE);
    private static final NamespaceImpl s_xmlNamespace =
        new NamespaceImpl("xml", XML_NAMESPACE);
    
    private final InByteBuffer m_byteBuffer;
    
	private AttributesHolder m_attributes;
	private ContentHandler m_contentHandler;
	private DTDHandler m_dtdHandler;
	private EntityResolver m_entityResolver;
	private ErrorHandler m_errorHandler;
	private LexicalHandler m_lexicalHandler;
    private DeclHandler m_declHandler;
    private boolean m_outsideDocument;
    private boolean m_isUsePrefixes;
    private int m_nsDeclCount;
    private Stack m_namespaceStack;

    /**
     * Constructor. Allocates and initializes instances of the data tables used
     * for storing state information during the serialization process.
     * 
     * @param buff
     */
    public XBISToSAXAdapter(InByteBuffer buff) {
        super();
        setBuffer(buff);
        m_byteBuffer = buff;
        m_attributes = new AttributesHolder();
    }
	
    /**
     * Default constructor. This creates a new input buffer of the default size.
     */
	public XBISToSAXAdapter() {
        this(new InByteBuffer(DEFAULT_BUFFER_SIZE));
	}

	/**
	 * Initialize state information used during the serialization process. This
	 * implementation of the abstract base class method is specific to SAX.
	 */
	protected void initState() {
        try {
            m_outsideDocument = true;
            m_namespaceStack = new Stack();
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
     * Set input stream. This first resets the state to clear any data that
     * came from another stream, then sets the new input stream and reads and
     * verifies the XBIS header information.
     *
     * @param is serialization input stream
     * @return output adapter identifier
     * @throws IOException on error reading from stream
     */
    public int setStream(InputStream is) throws IOException {
        try {
            m_byteBuffer.finish();
        } catch (IOException e) { /* deliberately empty */ }
        reset();
        m_byteBuffer.setInput(is);
        return initInput();
    }

	/**
	 * Build namespace instance. This implementation of the abstract base class
	 * method is specific to SAX.
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
     * @return <code>true</code> if namespace active, <code>false</code> if not
     */
    protected boolean isActiveNamespace(Object obj) {
        return ((NamespaceImpl)obj).isActive();
    }

    /**
     * Declare namespace. This implementation of the abstract base class method
     * is specific to SAX.
     *
     * @param obj namespace object
     * @throws XBISException on error in processing XML data
     */
    protected void declareNamespace(Object obj) throws XBISException {
        m_namespaceStack.push(obj);
        if (!m_outsideDocument) {
            m_nsDeclCount++;
            if (m_contentHandler != null) {
                NamespaceImpl ns = (NamespaceImpl)obj;
                ns.incrementNesting();
                try {
                    m_contentHandler.startPrefixMapping(ns.getPrefix(), ns.getUri());
                } catch (SAXException ex) {
                    throw new XBISException("SAX processing error", ex);
                }
            }
        }
    }

    /**
     * Undeclare namespace. This handles the processing at this level, then
     * calls the corresponding base class method.
     *
     * @param ns namespace information
     * @throws XBISException on error in processing XML data
     */
    private void undeclareNamespace(NamespaceImpl ns) throws XBISException {
        if (m_contentHandler != null) {
            try {
                m_contentHandler.endPrefixMapping(ns.getPrefix());
            } catch (SAXException ex) {
                throw new XBISException("SAX processing error", ex);
            }
        }
        ns.decrementNesting();
        super.undeclareNamespace(ns);
    }

	/**
	 * Build name instance. This implementation of the abstract base class
	 * method is specific to SAX.
	 *
	 * @param ns namespace for name
	 * @param local local name
	 * @return constructed name object
	 */
	protected Object buildName(Object ns, String local) {
		return new NameImpl(local, (NamespaceImpl)ns);
	}

	/**
	 * Report element to handler.
	 *
	 * @param initial node information byte for element
	 * @throws IOException on error reading from stream
	 * @throws XBISException on error in processing XML data
	 */
	protected void reportElement(int initial)
		throws IOException, XBISException {

		// get the basic element information
		NameImpl name = (NameImpl)readQuickElement(initial);

		// handle attributes if included
		if ((initial & ELEMENT_HASATTRIBUTES_FLAG) != 0) {
			int lead;
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
						} else if (m_attrValueCount ==
                            m_attrValueTable.length) {
							m_attrValueTable = doubleArray(m_attrValueTable);
						}
						m_attrValueTable[m_attrValueCount++] = value;
					} else {
						value = m_attrValueTable[readValue()-1];
					}
				} else {
					value = readString();
				}

				// add attribute to element
				m_attributes.addAttribute(aname, value);
			}
		}
		
		// report element start to content handler
		if (m_contentHandler != null) {
        
            // check for prefix reporting requested
            if (m_isUsePrefixes) {
        
                // include namespace definitions as attributes
                int index = m_namespaceStack.size();
                for (int i = 0; i < m_nsDeclCount; i++) {
                    NamespaceImpl ns =
                        (NamespaceImpl)m_namespaceStack.get(--index);
                    m_attributes.addAttribute(ns.getAsAttribute(), ns.getUri());
                }
            
                // report element with raw qualified name
                try {
                    NamespaceImpl nsimpl = name.getNamespace();
                    m_contentHandler.startElement(nsimpl.getUri(),
                        name.getLocalName(), name.getRawQName(), m_attributes);
                } catch (SAXException ex) {
                    throw new XBISException("SAX processing error", ex);
                }
                
            } else {
            
                // just report the element without raw qualifed name
                try {
                    NamespaceImpl nsimpl = name.getNamespace();
                    m_contentHandler.startElement(nsimpl.getUri(),
                        name.getLocalName(), "", m_attributes);
                } catch (SAXException ex) {
                    throw new XBISException("SAX processing error", ex);
                }
            }
		}
		
		// clear accumulated attributes and namespaces for next element
        int nscount = m_nsDeclCount;
        m_nsDeclCount = 0;
		m_attributes.clear();
		
		// process children of this element
		readChildren();
		
		// report element end to content handler
		if (m_contentHandler != null) {
			try {
                if (m_isUsePrefixes) {
                    m_contentHandler.endElement(name.getNamespace().getUri(),
                        name.getLocalName(), name.getRawQName());
                } else {
                    m_contentHandler.endElement(name.getNamespace().getUri(),
                        name.getLocalName(), "");
                }
			} catch (SAXException ex) {
				throw new XBISException("SAX processing error", ex);
			}
		}
        
        // close off namespaces for element
        while (--nscount >= 0) {
            NamespaceImpl ns = (NamespaceImpl)m_namespaceStack.pop();
            undeclareNamespace(ns);
        }
	}
	
	/**
	 * Report character data.
	 *
	 * @param ch array supplying character data
	 * @param start starting offset in array
	 * @param length number of characters
	 * @throws XBISException on processing error
	 */
	protected void reportText(char[] ch, int start, int length)
		throws XBISException {
		if (m_contentHandler != null) {
			try {
				m_contentHandler.characters(ch, start, length);
			} catch (SAXException ex) {
				throw new XBISException("SAX processing error", ex);
			}
		}
	}

	/**
	 * Report CDATA section.
	 *
	 * @param ch array supplying character data
	 * @param start starting offset in array
	 * @param length number of characters
	 * @throws XBISException on processing error
	 */
	protected void reportCDATA(char[] ch, int start, int length)
		throws XBISException {
		try {
			if (m_lexicalHandler != null) {
				m_lexicalHandler.startCDATA();
			}
			if (m_contentHandler != null) {
				m_contentHandler.characters(ch, start, length);
			}
			if (m_lexicalHandler != null) {
				m_lexicalHandler.endCDATA();
			}
		} catch (SAXException ex) {
			throw new XBISException("SAX processing error", ex);
		}
	}
    
	/**
	 * Report processing instruction.
	 *
	 * @param target processing instruction target
	 * @param data processing instruction data
	 * @throws XBISException on processing error
	 */
	protected void reportProcessingInstruction(String target, String data)
		throws XBISException {
		if (m_contentHandler != null) {
			try {
				m_contentHandler.processingInstruction(target, data);
			} catch (SAXException ex) {
				throw new XBISException("SAX processing error", ex);
			}
		}
	}

	/**
	 * Report comment.
	 *
	 * @param ch array supplying character data
	 * @param start starting offset in array
	 * @param length number of characters
	 * @throws XBISException on processing error
	 */
	protected void reportComment(char[] ch, int start, int length)
		throws XBISException {
		if (m_lexicalHandler != null) {
			try {
				m_lexicalHandler.comment(ch, start, length);
			} catch (SAXException ex) {
				throw new XBISException("SAX processing error", ex);
			}
		}
	}
    
    /**
     * Report document type information.
     *
     * @param name element name
     * @param pubid public id
     * @param sysid system id
     * @throws XBISException on processing error
     */
    protected void reportDocType(String name, String pubid, String sysid)
        throws XBISException {
        if (m_lexicalHandler != null) {
            try {
                m_lexicalHandler.startDTD(name, pubid, sysid);
            } catch (SAXException ex) {
                throw new XBISException("SAX processing error", ex);
            }
        }
    }
    
    /**
     * Report notation information.
     *
     * @param name notation name
     * @param pubid public id
     * @param sysid system id
     * @throws XBISException on processing error
     */
    protected void reportNotation(String name, String pubid, String sysid)
        throws XBISException {
        if (m_dtdHandler != null) {
            try {
                m_dtdHandler.notationDecl(name, pubid, sysid);
            } catch (SAXException ex) {
                throw new XBISException("SAX processing error", ex);
            }
        }
    }

    /**
     * Report unparsed entity information.
     *
     * @param name entity name
     * @param pubid public id
     * @param sysid system id
     * @param notation notation name
     * @throws XBISException on processing error
     */
    public void reportUnparsedEntity(String name, String pubid, String sysid,
        String notation) throws XBISException {
        if (m_dtdHandler != null) {
            try {
                m_dtdHandler.unparsedEntityDecl(name, pubid, sysid, notation);
            } catch (SAXException ex) {
                throw new XBISException("SAX processing error", ex);
            }
        }
    }
    
    /**
     * Report skipped entity information.
     *
     * @param name entity name
     * @throws XBISException on processing error
     */
    protected void reportSkippedEntity(String name) throws XBISException {
        if (m_contentHandler != null) {
            try {
                m_contentHandler.skippedEntity(name);
            } catch (SAXException ex) {
                throw new XBISException("SAX processing error", ex);
            }
        }
    }
    
    /**
     * Report element declaration information.
     *
     * @param name element name
     * @param model content model
     * @throws XBISException on processing error
     */
    protected void reportElementDecl(String name, String model)
        throws XBISException {
        if (m_declHandler != null) {
            try {
                m_declHandler.elementDecl(name, model);
            } catch (SAXException ex) {
                throw new XBISException("SAX processing error", ex);
            }
        }
    }
    
    /**
     * Report attribute declaration information.
     *
     * @param ename owning element name
     * @param aname attribute name
     * @param type attribute type
     * @param deftype defaulting type
     * @param dflt default value
     * @throws XBISException on processing error
     */
    protected void reportAttributeDecl(String ename, String aname, String type,
        String deftype, String dflt) throws XBISException {
        if (m_declHandler != null) {
            try {
                m_declHandler.attributeDecl(ename, aname, type, deftype, dflt);
            } catch (SAXException ex) {
                throw new XBISException("SAX processing error", ex);
            }
        }
    }
    
    /**
     * Report external entity declaration information.
     *
     * @param name external entity name
     * @param pubid public id
     * @param sysid system id
     * @throws XBISException on processing error
     */
    protected void reportExternalEntityDecl(String name, String pubid,
        String sysid) throws XBISException {
        if (m_declHandler != null) {
            try {
                m_declHandler.externalEntityDecl(name, pubid, sysid);
            } catch (SAXException ex) {
                throw new XBISException("SAX processing error", ex);
            }
        }
    }

	/**
	 * Read children of node from stream.
	 *
	 * @throws IOException on error reading from stream
	 * @throws XBISException on error in processing XML data
	 */
	protected void readChildren() throws IOException, XBISException {

		// loop until end of child nodes
		int lead;
		while ((lead = readByte()) != 0) {

			// check for special types with bit compares
			if ((lead & NODE_ELEMENT_FLAG) != 0) {
				reportElement(lead);
			} else if ((lead & NODE_PLAINTEXT_FLAG) != 0) {
                XBISReader.CharBlock block = readTextChars(lead);
				reportText(block.m_buffer, block.m_offset, block.m_length);
			} else if ((lead & NODE_TEXTREF_FLAG) != 0) {
                char[] chars = readCharsDef(lead);
                reportText(chars, 0, chars.length);
			} else if ((lead & NODE_NAMESPACEDECL_FLAG) != 0) {
				readNamespaceDecl(lead);
			} else {

				// handle everything else
                XBISReader.CharBlock block;
				switch (lead) {

					case NODE_TYPE_COMMENT:
                        block = readStringChars();
						reportComment(block.m_buffer, block.m_offset,
                            block.m_length);
						break;

					case NODE_TYPE_CDATA:
                        block = readStringChars();
                        reportCDATA(block.m_buffer, block.m_offset,
                            block.m_length);
						break;

					case NODE_TYPE_PI:
						{
							String name = readString();
							String text = readString();
							reportProcessingInstruction(name, text);
						}
						break;

					case NODE_TYPE_DOCTYPE:
						{
							String name = readString();
							String pub = readString();
							String sys = readString();
							reportDocType(name, pub, sys);
						}
						break;

                    case NODE_TYPE_NOTATION:
                        {
                            String name = readString();
                            String pub = readString();
                            String sys = readString();
                            reportNotation(name, pub, sys);
                        }
                        break;

                    case NODE_TYPE_UNPARSEDENTITY:
                        {
                            String name = readString();
                            String pub = readString();
                            String sys = readString();
                            String notation = readString();
                            reportUnparsedEntity(name, pub, sys, notation);
                        }
                        break;

                    case NODE_TYPE_SKIPPEDENTITY:
                        {
                            String name = readString();
                            reportSkippedEntity(name);
                        }
                        break;

                    case NODE_TYPE_ELEMENTDECL:
                        {
                            String name = readString();
                            String model = readString();
                            reportElementDecl(name, model);
                        }
                        break;

                    case NODE_TYPE_ATTRIBUTEDECL:
                        {
                            String ename = readString();
                            String aname = readString();
                            String type = readString();
                            String dtype = readString();
                            String dflt = readString();
                            reportAttributeDecl(ename, aname, type,
                                dtype, dflt);
                        }
                        break;

                    case NODE_TYPE_EXTERNALENTITYDECL:
                        {
                            String name = readString();
                            String pub = readString();
                            String sys = readString();
                            reportExternalEntityDecl(name, pub, sys);
                        }
                        break;

					default:
						throw new IllegalArgumentException
							("Unknown node type " + lead);
				}
			}
		}
	}

	/**
	 * Read document from stream.
	 *
	 * @throws IOException on error reading from stream
	 * @throws XBISException on error in processing XML data
	 */
	public void readDocument() throws IOException, XBISException {
		clearReset();
		if (readByte() == NODE_TYPE_DOCUMENT) {
			if (m_contentHandler != null) {
				try {
					m_contentHandler.startDocument();
				} catch (SAXException ex) {
					throw new XBISException("SAX processing error", ex);
				}
			}
            m_outsideDocument = false;
			readChildren();
			if (m_contentHandler != null) {
				try {
					m_contentHandler.endDocument();
				} catch (SAXException ex) {
					throw new XBISException("SAX processing error", ex);
				}
			}
		} else {
			throw new IOException("Document not found at start of input");
		}
	}

	/**
	 * Reset state for processing new document. Clears any internal state
	 * information and initializes for processing a new document. This allows
	 * instances to be reused, even after an error termination in processing a
	 * prior document.
	 */
	public final void reset() {
        m_outsideDocument = true;
		super.reset();
		if (m_attributes != null) {
			m_attributes.clear();
		}
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
			return false;
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
		InputStream is = input.getByteStream();
        if (is == null) {
            throw new SAXException("XBIS stream handler requires input stream");
        } else {
            try {
                setStream(is);
                readDocument();
            } catch (IOException e) {
                throw new SAXException("Error reading XBIS input stream");
            } catch (XBISException e) {
                throw new SAXException("Error in XBIS input stream");
            }
        }
	}

	public void parse(String systemId) throws SAXException {
		throw new SAXException
			("Direct parse not supported by XBIS stream handler");
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
		if ("http://xml.org/sax/features/namespaces".equals(name)) {
			throw new SAXNotSupportedException
				("Stream converter requires namespaces enabled");
		} else if ("http://xml.org/sax/features/namespace-prefixes".
            equals(name)){
            m_isUsePrefixes = value;
		} else {
			throw new SAXNotRecognizedException("Unknown feature: " + name);
		}
	}

	public void setProperty(String name, Object value)
		throws SAXNotRecognizedException, SAXNotSupportedException {
		if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
			m_lexicalHandler = (LexicalHandler)value;
		} else if ("http://xml.org/sax/properties/declaration-handler".
            equals(name)) {
			m_declHandler = (DeclHandler)value;
        } else {
            throw new SAXNotRecognizedException("Unknown property: " + name);
		}
	}

	/**
	 * Name implementation class used for SAX2 processing. This supports the
     * option of using raw qualified names (prefix:lname format), generating
     * them the first time they're needed and caching them for reuse.
	 */
	private static class NameImpl
	{
        /** Local name. */
        private final String m_local;

        /** Namespace information. */
        private final NamespaceImpl m_namespace;
        
        /** Raw qualified name (lazy create, only if necessary). */
		private String m_rawQName;
        
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
        
        /**
         * Get raw qualified name text.
         *
         * @return qualified name in raw form
         */
		public String getRawQName() {
            if (m_rawQName == null) {
                if (m_namespace.getPrefix().length() == 0) {
                    m_rawQName = m_local;
                } else {
                    m_rawQName = m_namespace.getPrefix() + ':' + m_local;
                }
            }
            return m_rawQName;
		}
	}

	/**
	 * Namespace implementation class used for SAX2 processing. This adds the
     * option of accessing the namespace declaration as though it were an
     * attribute, with lazy creation and caching of the result for reuse, and
     * also handles nested redeclarations of the same namespace by counting.
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
        
        /**
         * Get namespace declaration as an attribute.
         *
         * @return namespace declaration as attribute
         */
		public NameImpl getAsAttribute() {
            if (m_attribute == null) {
                String asattr = "xmlns";
                if (getPrefix().length() > 0) {
                    asattr += ":" + getPrefix();
                }
                m_attribute = new NameImpl(asattr, s_noNamespaceNamespace);
            }
            return m_attribute;
		}
	}

	/**
	 * Holder class for SAX2 attribute data.
	 */
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
            return m_names[index].getLocalName();
        }

        /* (non-Javadoc)
         * @see org.xml.sax.Attributes#getQName(int)
         */
        public String getQName(int index) {
            return m_names[index].getRawQName();
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
                if (lname.equals(name.getLocalName()) &&
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
                if (lname.equals(name.getLocalName()) &&
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