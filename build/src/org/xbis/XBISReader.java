/*
Copyright (c) 2000-2008, Dennis M. Sosnoski.
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

import org.jibx.runtime.impl.IInByteBuffer;

/**
 * XML Binary Information Set input handler. This reads a compact representation of
 * the data in an XML document, with the advantages of reduced document size
 * and lower processing overhead as compared to the standard text document
 * representation.<p>
 *
 * This class provides basic input handling, but the driver methods for reading
 * the serial form and constructing the corresponding document representation
 * must be implemented by a subclass specific to the representation used. The
 * serial form is itself independent of the original representation, and may be
 * read by the input handlers for representations other than the one from which
 * it was generated.
 *
 * @author Dennis M. Sosnoski
 */
public abstract class XBISReader implements XBISConstants
{
	/** Default size of input buffer (used by subclasses). */
	public static final int DEFAULT_BUFFER_SIZE = 2048;
	
	/** Initial size of handle value arrays. */
	public static final int INITIAL_HANDLE_SIZE = 32;

	/** Limit for reading String data a byte at a time. */
	public static final int SHORT_STRING_LIMIT = 5;

	/** Initial size of String buffer (must be larger than short limit). */
	public static final int INITIAL_STRING_SIZE = 64;

	/** Serialization reset and ready to begin flag. */
	private boolean m_isReset;
	
	/** Adapter identifier code. */
	private int m_adapterId;

	/** Table of Namespaces corresponding to namespace handle values. */
    private Object[] m_namespaceTable;
	
	/** Number of Namespaces handles currently defined. */
    private int m_namespaceCount;
    
    /** Table of namespace URIs corresponding to namespace URI handle values. */
    private String[] m_namespaceURITable;
    
    /** Number of namespaces URIs currently defined. */
    private int m_namespaceURICount;
    
    /** Table of active namespaces which can be used with names. */
    private Object[] m_activeNamespaces;
    
    /** Number of namespaces currently active. */
    private int m_activeCount;

	/** Table of element names corresponding to element handle values. */
    private Object[] m_elementTable;
	
	/** Number of element name handles currently defined. */
    private int m_elementCount;

	/** Table of attribute names corresponding to attribute handle values. */
    private Object[] m_attributeTable;
	
	/** Number of attribute name handles currently defined. */
	private int m_attributeCount;

    /** Table of char arrays corresponding to text handle values (created when
     * needed). */
    private char[][] m_charsTable;

	/** Table of Strings corresponding to text handle values (created when
	 * needed). */
    private String[] m_stringTable;
	
	/** Number of text handle value strings currently defined. */
    private int m_textCount;

	/** Table of Strings corresponding to attribute handle values (created when
	 * needed). */
    protected String[] m_attrValueTable;
	
	/** Number of attribute handle value Strings currently defined. */
    protected int m_attrValueCount;
    
    /** Byte buffer input source. */
    private IInByteBuffer m_byteBuffer;

	/** Cached byte array buffer for input data. */
	private byte[] m_buffer;

	/** At end of input stream flag. */
	private boolean m_atEnd;

	/** End offset in buffer. */
	private int m_end;

	/** Cached current offset in buffer. */
	private int m_offset;

	/** Buffer for characters read from stream. */
	private char[] m_text;
    
    /** Information on block of characters read. */
    private CharBlock m_charBlock;

	/**
	 * Constructor. Allocates and initializes instances of the data tables used
	 * for storing state information during the serialization process.
	 */
	public XBISReader() {
		m_namespaceTable = new Object[INITIAL_HANDLE_SIZE];
        m_namespaceURITable = new String[INITIAL_HANDLE_SIZE];
        m_activeNamespaces = new Object[INITIAL_HANDLE_SIZE];
		m_elementTable = new Object[INITIAL_HANDLE_SIZE];
		m_attributeTable = new Object[INITIAL_HANDLE_SIZE];
		m_text = new char[INITIAL_STRING_SIZE];
        m_charBlock = new CharBlock();
		initState();
		m_isReset = true;
	}

	/**
	 * Doubles the size of an array of <code>Object</code> values. Constructs 
	 * and returns the resized array, copying values from the old array across 
	 * to the new one.
	 *
	 * @param base array to be resized
	 * @return replacement array with values copied
	 */
	protected final Object[] doubleArray(Object[] base) {
		Object[] grown = new Object[base.length*2];
		System.arraycopy(base, 0, grown, 0, base.length);
		return grown;
	}

    /**
     * Doubles the size of an array of <code>char[]</code> values. Constructs 
     * and returns the resized array, copying values from the old array across 
     * to the new one.
     *
     * @param base array to be resized
     * @return replacement array with values copied
     */
    protected final char[][] doubleArray(char[][] base) {
        char[][] grown = new char[base.length*2][];
        System.arraycopy(base, 0, grown, 0, base.length);
        return grown;
    }

	/**
	 * Doubles the size of an array of <code>String</code> values. Constructs 
	 * and returns the resized array, copying values from the old array across 
	 * to the new one.
	 *
	 * @param base array to be resized
	 * @return replacement array with values copied
	 */
	protected final String[] doubleArray(String[] base) {
		String[] grown = new String[base.length*2];
		System.arraycopy(base, 0, grown, 0, base.length);
		return grown;
	}

	/**
	 * Clears an array of <code>Object</code>s.
	 *
	 * @param count number of references present in array
	 * @param array array to be cleared (may be <code>null</code>)
	 */
	protected final void clearArray(int count, Object[] array) {
		if (array != null) {
			for (int i = 0; i < count; i++) {
				array[i] = null;
			}
		}
	}
    
    /**
     * Set the input buffer.
     *
     * @param buff
     */
    public void setBuffer(IInByteBuffer buff) {
        m_byteBuffer = buff;
    }
    
    /**
     * Get input buffer.
     *
     * @return buffer, <code>null</code> if none set
     */
    public IInByteBuffer getBuffer() {
        return m_byteBuffer;
    }

    /**
     * Initialize input, reading and verifying the header information.
     *
     * @return output adapter identifier
     * @throws IOException on error reading from stream
     */
    public int initInput() throws IOException {
        m_byteBuffer.require(1);
        m_buffer = m_byteBuffer.getBuffer();
        m_offset = m_byteBuffer.getOffset();
        m_end = m_byteBuffer.getLimit();
        m_atEnd = false;
        if (isReset()) {
            clearReset();
            if (readByte() == HEADER_VERSION_ID) {
                m_adapterId = readByte();
                readByte();
                readByte();
                return m_adapterId;
            } else {
                throw new IOException("Invalid format header in stream");
            }
        } else {
            return m_adapterId;
        }
    }

	/**
	 * Checks if end of input reached.
	 *
	 * @return <code>true</code> if end of input, <code>false</code> if data
	 * remaining
	 * @throws IOException on error reading from stream
	 */
	protected final boolean isEnd() throws IOException {

		// check if buffer is now empty
		if (m_offset >= m_end) {

			// read more data to buffer (no need to check flag, just try it)
            m_byteBuffer.setOffset(m_offset);
		    if (m_byteBuffer.require(1)) {
                m_buffer = m_byteBuffer.getBuffer();
		        m_end = m_byteBuffer.getLimit();
		        m_offset = m_byteBuffer.getOffset();
		        return false;
		    } else {
		        m_end = 0;
				m_offset = 0;
				m_atEnd = true;
				return true;
			}
		}
		return false;
	}

	/**
	 * Read next byte from buffer. Reads another block of data from the input
	 * stream if the buffer is empty.
	 *
	 * @return byte value from buffer
	 * @throws IOException on error reading from stream
	 */
	protected final int readByte() throws IOException {

		// check for byte of data available in buffer
		if (m_offset < m_end) {
			return m_buffer[m_offset++];
		} else {

			// read more data to buffer (no need to check flag, just try it)
		    m_byteBuffer.setOffset(m_offset);
            if (m_byteBuffer.require(1)) {
                m_buffer = m_byteBuffer.getBuffer();
                m_end = m_byteBuffer.getLimit();
                m_offset = m_byteBuffer.getOffset();
                return m_buffer[m_offset++];
            } else {
                m_end = 0;
                m_offset = 0;
                m_atEnd = true;
                throw new EOFException("End of file with data expected.");
            }
		}
	}

	/**
	 * Read non-negative integer value from stream. This handles decoding of
     * the variable-length representation used in the serial form, which gives
     * seven bits of value per serialized byte by using the high-order bit of
     * each byte as a continuation flag.
	 *
	 * @return integer value read from stream
	 * @throws IOException on error reading from stream
	 */
	protected final int readValue() throws IOException {
		int acc = readByte();
		if (acc < 0) {
			int byt;
			acc = acc & 0x7F;
			while ((byt = readByte()) < 0) {
				acc = (acc << 7) | (byt & 0x7F);
			}
			return (acc << 7) + byt;
		} else {
			return acc;
		}
	}

	/**
	 * Read non-negative integer value with partial lead byte from stream. This
	 * handles decoding of the variable-length representation used in the serial
	 * form, with a partial leading byte. This format is used when a handle
	 * value or length is combined with flags. If the value from the initial
	 * byte is zero, the value is encoded in standard form in the following
	 * byte(s).
	 *
	 * @param initial byte containing value or continuation
	 * @param mask value mask within first byte (right justified)
	 * @return integer value read from stream
	 * @throws IOException on error reading from stream
	 */
	protected final int readQuickValue(int initial, int mask)
		throws IOException {
		int acc = initial & mask;
		if (acc == 0) {
			return readValue()-1;
		} else {
			return acc-1;
		}
	}

	/**
	 * Read string of characters from stream. This method uses a dual approach
	 * to building the array of characters, depending on the number of
	 * characters. If the count is low, byte at a time reads are used to collect
	 * the characters (with between one and three bytes per character). If the
	 * count is high, we take as many characters as possible from the buffer
	 * before checking for end conditions. This approach creates some duplicate
	 * code, but gives the best performance.
	 *
	 * @param length number of characters to read
	 * @return array containing characters read (starting at position 0, to the
	 * requested length), only valid until the next call to a method in this
	 * class
	 * @throws IOException on error reading from stream
	 */
	protected final char[] readChars(int length) throws IOException {

		// check if short enough to read a byte at a time
		if (length < SHORT_STRING_LIMIT) {

			// collect all the characters in buffer
			for (int i = 0; i < length; i++) {
				int value = readByte();
				if (value < 0) {
					int byt = readByte();
					if (byt < 0) {
						value = value << 14 + ((byt & 0x7F) << 7) + readByte();
					} else {
						value = ((value & 0x7F) << 7) + byt;
					}
				}
				m_text[i] = (char)value;
			}

		} else {

			// make sure character buffer has space for all data
			if (length > m_text.length) {
				m_text = new char[length];
			}

			// fill buffer with required number of characters
			int fill = 0;
			while (true) {

				// fill characters until done or data exhausted
				int offset = m_offset;
				int limit = m_end - 2;
				while (fill < length && offset < limit) {
					int value = m_buffer[offset++];
					if (value < 0) {
						int byt = m_buffer[offset++];
						if (byt < 0) {
							value = value << 14 + ((byt & 0x7F) << 7) +
								m_buffer[offset++];
						} else {
							value = ((value & 0x7F) << 7) + byt;
						}
					}
					m_text[fill++] = (char)value;
				}
				m_offset = offset;
				if (fill >= length) {
				    break;
				}

                // check if we're already at end of stream
                if (m_atEnd) {
                    
                    // just fill last characters with byte at a time
                    while (fill < length) {
                        m_text[fill++] = (char)readValue();
                    }
                    break;

                } else {

                    // fill buffer with more data from input stream
                    m_byteBuffer.setOffset(m_offset);
                    if (!m_byteBuffer.require(m_buffer.length/2)) {
                        m_atEnd = true;
                    }
                    m_buffer = m_byteBuffer.getBuffer();
                    m_end = m_byteBuffer.getLimit();
                    m_offset = m_byteBuffer.getOffset();
                    
                }
			}
		}

		// return buffer containing characters
        return m_text;
	}

    /**
     * Read <code>String</code> data from stream. Just uses {@link #readChars}
     * to read the characters, then turns them into a string to be returned.
     *
     * @param length number of character to read
     * @return string read from input
     * @throws IOException on error reading from stream
     */
    protected final String readStringData(int length) throws IOException {
        return new String(readChars(length), 0, length);
    }

	/**
	 * Read characters in a string directly from stream. This handles decoding
	 * of the string value representation used in the serial form, consisting of
	 * a leading character count plus one (with the value zero used to indicate
	 * a <code>null</code> string), in the standard integer value encoding,
	 * followed by the specified number of characters. Character data uses a
	 * separate encoding scheme. It is an error if this method is called for a
	 * <code>null</code> string.
	 *
	 * @return information for string of characters read from stream (only valid
	 * until the next call to a method in this class)
	 * @throws IOException on error reading from stream
	 * @see #readChars
	 */
	protected final CharBlock readStringChars() throws IOException {
		int length = readValue();
        if (length > 0) {
            m_charBlock.m_length = length-1;
            m_charBlock.m_buffer = readChars(length-1);
            m_charBlock.m_offset = 0;
            return m_charBlock;
        } else {
            throw new IllegalStateException("Null string not allowed");
        }
	}

    /**
     * Read <code>String</code> directly from stream. This handles decoding of
     * the <code>String</code> value representation used in the serial form,
     * consisting of a leading character count plus one (with the value zero
     * used to indicate a <code>null</code> <code>String</code>), in the
     * standard integer value encoding, followed by the specified number of
     * characters. Character data uses a separate encoding scheme.
     *
     * @return String read from stream (may be <code>null</code>)
     * @throws IOException on error reading from stream
     * @see #readStringData
     */
    protected final String readString() throws IOException {
        int length = readValue();
        if (length == 0) {
            return null;
        } else {
            return readStringData(length-1);
        }
    }

    /**
     * Read characters in a plain text node directly from stream. This first
     * gets the length of the text either from the lead byte or from the
     * following byte(s), as encoded. It then reads the required number of
     * characters and returns them as a <code>String</code>.
     *
     * @param lead node definition byte
     * @return information for string of characters read from stream (only valid
     * until the next call to a method in this class)
     * @throws IOException on error reading from stream
     * @see #readChars
     */
    protected final CharBlock readTextChars(int lead) throws IOException {
        int length = readQuickValue(lead, PLAINTEXT_LENGTH_MASK);
        m_charBlock.m_length = length;
        if (length != 0) {
            m_charBlock.m_buffer = readChars(length);
            m_charBlock.m_offset = 0;
        }
        return m_charBlock;
    }

	/**
	 * Read plain text node from stream. This first gets the length of the text
	 * either from the lead byte or from the following byte(s), as encoded. It
	 * then reads the required number of characters and returns them as a
	 * <code>String</code>.
	 *
	 * @param lead node definition byte
	 * @return text read from stream
	 * @throws IOException on error reading from stream
	 * @see #readStringData
	 */
	protected final String readPlainText(int lead) throws IOException {
        CharBlock block = readTextChars(lead);
        return new String(block.m_buffer, block.m_offset, block.m_length);
	}

    /**
     * Read reusable text node from stream. If a handle is given in the input
     * this returns the array of characters with that handle. Otherwise, it
     * reads a string definition as an array of characters, assigning it the
     * next consecutive handle.
     *
     * @param lead node definition byte
     * @return text read from stream
     * @throws IOException on error reading from stream
     */
    protected final char[] readCharsDef(int lead) throws IOException {
        int handle = readQuickValue(lead, TEXTREF_HANDLE_MASK);
        if (handle > 0) {
            if (m_charsTable == null) {
                if (m_stringTable == null) {
                    System.out.println();
                }
                m_charsTable = new char[m_stringTable.length][];
                for (int i = 0; i < m_textCount; i++) {
                    m_charsTable[i] = m_stringTable[i].toCharArray();
                }
            }
            return m_charsTable[handle-1];
        } else {
            if (m_charsTable == null) {
                m_charsTable = new char[INITIAL_HANDLE_SIZE][];
            } else if (m_textCount == m_charsTable.length) {
                m_charsTable = doubleArray(m_charsTable);
                if (m_stringTable != null) {
                    m_stringTable = doubleArray(m_stringTable);
                }
            }
            CharBlock block = readStringChars();
            char[] chars = new char[block.m_length];
            System.arraycopy(block.m_buffer, block.m_offset, chars,
                0, block.m_length);
            if (m_stringTable != null) {
                m_stringTable[m_textCount] = new String(chars);
            }
            m_charsTable[m_textCount++] = chars;
            return chars;
        }
    }

	/**
	 * Read reusable text node from stream. If a handle is given in the input
	 * this returns the <code>String</code> with that handle. Otherwise, it
	 * reads a <code>String</code> definition, assigning it the next consecutive
	 * handle.
	 *
	 * @param lead node definition byte
	 * @return text read from stream
	 * @throws IOException on error reading from stream
	 */
	protected final String readStringDef(int lead) throws IOException {
        int handle = readQuickValue(lead, TEXTREF_HANDLE_MASK);
        if (handle > 0) {
            if (m_stringTable == null) {
                m_stringTable = new String[m_charsTable.length];
                for (int i = 0; i < m_textCount; i++) {
                    m_stringTable[i] = new String(m_charsTable[i]);
                }
            }
            return m_stringTable[handle-1];
        } else {
            if (m_stringTable == null) {
                m_stringTable = new String[INITIAL_HANDLE_SIZE];
            } else if (m_textCount == m_stringTable.length) {
                m_stringTable = doubleArray(m_stringTable);
                if (m_charsTable != null) {
                    m_charsTable = doubleArray(m_charsTable);
                }
            }
            String string = readString();
            if (m_charsTable != null) {
                m_charsTable[m_textCount] = string.toCharArray();
            }
            m_stringTable[m_textCount++] = string;
            return string;
        }
	}

	/**
	 * Add namespace definition to table.
	 *
	 * @param ns namespace information
	 * @return index number of added namespace
	 */
	protected final int addNamespace(Object ns) {
		if (m_namespaceCount == m_namespaceTable.length) {
			m_namespaceTable = doubleArray(m_namespaceTable);
		}
		m_namespaceTable[m_namespaceCount] = ns;
		return m_namespaceCount++;
	}

	/**
	 * Add namespace URI definition to table.
	 *
	 * @param uri namespace URI
	 * @return index number of added namespace URI
	 */
	protected final int addNamespaceURI(String uri) {
		if (m_namespaceURICount == m_namespaceURITable.length) {
			m_namespaceURITable = doubleArray(m_namespaceURITable);
		}
		m_namespaceURITable[m_namespaceURICount] = uri;
		return m_namespaceURICount++;
	}

    /**
     * Activate a namespace. This assigns an active namespace handle to the
     * namespace, and also adds it to the table of active namespaces.
     *
     * @param ns namespace to be activated
     * @throws XBISException on error in processing XML data
     */
    protected void activateNamespace(Object ns) throws XBISException {
        if (!isActiveNamespace(ns)) {
            if (m_activeCount >= m_activeNamespaces.length) {
                m_activeNamespaces = doubleArray(m_activeNamespaces);
            }
            m_activeNamespaces[m_activeCount++] = ns;
        }
        declareNamespace(ns);
    }

    /**
     * Read namespace definition from stream. This is used to define a new
     * namespace (prefix+URI pair). The format is a single byte with a handle
     * value for the namespace URI and a quick length field for the prefix. This
     * also assigns an active namespace handle to the new namespace and adds it
     * to the active namespace stack for automatic cleanup at the end of the
     * element.
     *
     * @return name object reconstructed from stream
     * @throws IOException on error reading from stream
     * @throws XBISException on error in processing XML data
     */
    protected final Object readNamespaceDef()
        throws IOException, XBISException {

        // check for handle continued to following byte(s)
        int lead = readByte();
        int handle = readQuickValue(lead >> NSDEF_URIHANDLE_SHIFT,
            NSDEF_URIHANDLE_MASK);

        // check for existing namespace URI reference
        String uri;
        if (handle > 0) {

            // get existing namespace URI from table
            uri = m_namespaceURITable[handle-1];

        } else {

            // read new namespace and add to table
            uri = readString();
            addNamespaceURI(uri);

        }
        
        // read prefix to associate with URI
        int length = readQuickValue(lead, NSDEF_PRELENGTH_MASK);
        String prefix = readStringData(length);
        
        // create the namespace and add to tables
        Object ns = buildNamespace(prefix, uri);
        addNamespace(ns);
        activateNamespace(ns);
        return ns;
    }

    /**
     * Read namespace declaration from stream. If a previously defined namespace
     * handle is supplied this reactivates that namespace for the following
     * element. Otherwise, it reads a new namespace definition and assigns it a
     * new namespace handle. Either way, the namespace is assigned an active
     * namespace handle and is added to the active namespace stack for automatic
     * cleanup at the end of the element.
     *
     * @param initial byte containing handle or continuation
     * @return name object reconstructed from stream
     * @throws IOException on error reading from stream
     * @throws XBISException on error in processing XML data
     */
    protected final Object readNamespaceDecl(int initial)
        throws IOException, XBISException {

        // check for existing namespace reference
        int handle = readQuickValue(initial, NAMESPACEDECL_HANDLE_MASK);
        if (handle > 0) {

            // activate and return existing namespace from table
            Object ns = m_namespaceTable[handle-1];
            activateNamespace(ns);
            return ns;

        } else {

            // construct and return new namespace
            return readNamespaceDef();

        }
    }

	/**
	 * Read active namespace reference from stream. This method interprets the
	 * namespace handle value from the lead byte of a name definition, if
	 * necessary reading additional bytes for the handle or defining a new
	 * namespace with the next consecutive handle value.
	 *
	 * @param initial byte containing handle or continuation
	 * @param mask value mask within first byte (right justified)
	 * @return name object reconstructed from stream
	 * @throws IOException on error reading from stream
     * @throws XBISException on error in processing XML data
	 */
	protected final Object readNamespaceRef(int initial, int mask)
		throws IOException, XBISException {
        
        // check for existing namespace reference
        int handle = readQuickValue(initial, mask);
        if (handle == 0) {

            // construct and return new namespace
            return readNamespaceDef();
            
        } else {

            // return existing namespace from table
            return m_activeNamespaces[handle-1];
        }
	}

	/**
	 * Read attribute name definition from stream with partial lead byte. If a 
	 * valid handle is given in the input this returns the existing name with 
	 * that handle. Otherwise, it reads a namespace definition and local name 
	 * from the stream to construct the new name, assigning the name the next 
	 * consecutive handle value.
	 *
	 * @param initial byte containing value or continuation
	 * @return name object reconstructed from stream
	 * @throws IOException on error reading from stream
     * @throws XBISException on error in processing XML data
	 */
	protected final Object readQuickAttribute(int initial)
        throws IOException, XBISException {

		// get full handle value
		int handle = readQuickValue(initial, ATTRIBUTE_HANDLE_MASK);
		if (handle > 0) {

			// return existing name from table
            try {
                return m_attributeTable[handle-1];
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }

		} else {

			// read new name definition
			int lead = readByte();
			Object ns = readNamespaceRef(lead>>NAME_NS_SHIFT, NAME_NS_MASK);
			int length = readQuickValue(lead, NAME_LENGTH_MASK);
			Object name = buildName(ns, readStringData(length));
			if (m_attributeCount == m_attributeTable.length) {
				m_attributeTable = doubleArray(m_attributeTable);
			}
			m_attributeTable[m_attributeCount++] = name;
			return name;
		}
	}

	/**
	 * Read element name definition from stream with partial lead byte. If a 
	 * valid handle is given in the input this returns the existing name with 
	 * that handle. Otherwise, it reads a namespace definition and local name 
	 * from the stream to construct the new name, assigning the name the next 
	 * consecutive handle value.
	 *
	 * @param initial byte containing value or continuation
	 * @return name object reconstructed from stream
	 * @throws IOException on error reading from stream
     * @throws XBISException on error in processing XML data
	 */
	protected final Object readQuickElement(int initial)
        throws IOException, XBISException {

		// get full handle value
		int handle = readQuickValue(initial, ELEMENT_HANDLE_MASK);
		if (handle > 0) {

			// return existing name from table
			return m_elementTable[handle-1];

		} else {

			// read new name definition
			int lead = readByte();
			Object ns = readNamespaceRef(lead>>NAME_NS_SHIFT, NAME_NS_MASK);
			int length = readQuickValue(lead, NAME_LENGTH_MASK);
			Object name = buildName(ns, readStringData(length));
			if (m_elementCount == m_elementTable.length) {
				m_elementTable = doubleArray(m_elementTable);
			}
			m_elementTable[m_elementCount++] = name;
			return name;
		}
	}

	/**
	 * Clear reset state. This method must be called before beginning any
	 * operations which read from the output stream.
	 */
	protected void clearReset() {
		m_isReset = false;
	}

	/**
	 * Check reset state.
	 *
	 * @return <code>true</code> if reset, <code>false</code> if not
	 */
	protected boolean isReset() {
		return m_isReset;
	}

	/**
	 * Reset state information used during the serialization process. This
	 * reinitializes the serialization state so that an instance of this class
	 * can be reused to unserialize multiple independent documents. Subclasses
	 * overriding this method to perform their own reinitialization must call
	 * the base class method before returning to the caller.
	 */
	public void reset() {
		if (!m_isReset) {
			clearArray(m_namespaceCount, m_namespaceTable);
			m_namespaceCount = 0;
            clearArray(m_namespaceURICount, m_namespaceURITable);
            m_namespaceURICount = 0;
			clearArray(m_activeNamespaces.length, m_activeNamespaces);
			m_activeCount = 0;
			clearArray(m_elementCount, m_elementTable);
			m_elementCount = 0;
			clearArray(m_attributeCount, m_attributeTable);
			m_attributeCount = 0;
			clearArray(m_textCount, m_stringTable);
            clearArray(m_textCount, m_charsTable);
			m_textCount = 0;
			clearArray(m_attrValueCount, m_attrValueTable);
			m_attrValueCount = 0;
			initState();
			m_isReset = true;
		}
	}

	/**
	 * Initialize state information used during the serialization process. This
	 * abstract method must be implemented by each subclass to set up predefined
	 * state information in the tables.
	 */
	protected abstract void initState();

	/**
	 * Build namespace instance. This abstract method must be implemented by
	 * each subclass to build a namespace object of the appropriate type.
	 *
	 * @param prefix namespace prefix
	 * @param uri namespace URI
	 * @return constructed namespace object
	 */
	protected abstract Object buildNamespace(String prefix, String uri);

    /**
     * Check if a namespace is active. This method must be overridden by
     * subclasses to get namespace status.
     *
     * @param obj namespace object
     */
    protected abstract boolean isActiveNamespace(Object obj);

    /**
     * Declare namespace. This method may be overridden by subclasses to handle
     * the beginning of a namespace scope.
     *
     * @param obj namespace object
     * @throws XBISException on error in processing XML data
     */
    protected void declareNamespace(Object obj) throws XBISException {}

    /**
     * Undeclare namespace. This method may be overridden by subclasses to
     * handle the ending of a namespace scope, but they must call this base
     * class method anytime a namespace is closed.
     * 
     * @param obj namespace object
     */
    protected void undeclareNamespace(Object obj) {
        if (!isActiveNamespace(obj)) {
            m_activeCount--;
        }
    }

	/**
	 * Build name instance. This abstract method must be implemented by each
	 * subclass to build a name object of the appropriate type.
	 *
	 * @param ns namespace for name
	 * @param local local name
	 * @return constructed name object
	 */
	protected abstract Object buildName(Object ns, String local);

    /**
     * Data block describing a block of characters.
     */
    public static class CharBlock
    {
        public char[] m_buffer;
        public int m_offset;
        public int m_length;
    }
}