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
import java.util.HashMap;

import org.jibx.runtime.impl.IOutByteBuffer;

import com.sosnoski.util.hashmap.*;

/**
 * XML Binary Information Set output handler. This writes a compact representation of
 * the data in an XML document, with the advantages of reduced document size
 * and lower processing overhead as compared to the standard text document
 * representation.<p>
 *
 * This class provides basic output handling, but the driver methods for walking
 * a document representation and writing the corresponding serial form must be
 * implemented by a subclass specific to the model used. The generated serial
 * form is independent of the original model, and may be read by the input
 * handlers for models other than the one from which it was generated.
 *
 * @author Dennis M. Sosnoski
 */
public abstract class XBISWriter implements XBISConstants
{
    /** Margin for space to be available in buffer when setting mark. */
	private static final int MARK_BUFFER_MARGIN = 100;

    /** Default size of output buffer (used by subclasses). */
	public static final int DEFAULT_BUFFER_SIZE = 2048;

	/** Limit for accessing String data a byte at a time. */
	public static final int SHORT_STRING_LIMIT = 5;

	/** Size of String character buffer (must be larger than short limit, but
	 * less than one third of the buffer size). */
	public static final int STRING_ARRAY_SIZE = 64;

	/** Minimum size for output buffer. */
	public static final int MINIMUM_BUFFER_SIZE = STRING_ARRAY_SIZE*3;

    /** Number of namespace declarations allowed for in default allocation. */
    public static final int INITIAL_NAMESPACE_COUNT = 8;

    /** Serialization reset and ready to begin flag. */
    private boolean m_isReset;
        
    /** No namespace namespace. */
    private final OutputNamespace m_noNamespace;
    
    /** XML namespace. */
    private final OutputNamespace m_xmlNamespace;
    
    /** Number of namespaces written to output or predefined (next handle). */
    private int m_namespaceCount;
    
    /** Number of URIs written to output (next handle). */
    private int m_uriCount;
    
    /** Current active namespace count. */
    private int m_activeCount;
    
    /** Active namespace table. */
    private OutputNamespace[] m_activeNamespaces;
    
    /** Map of URI to namespace entries. */
    private HashMap m_uriMap;

    /** Maximum size for shared content text Strings. */
    private int m_sharedTextLimit;
    
    /** Map of text handle values corresponding to char blocks (optional). */
    private CharBlockIntHashMap m_charsMap;

	/** Map of text handle values corresponding to Strings (optional). */
	private StringIntHashMap m_textMap;

	/** Maximum size for shared attribute value text Strings. */
	private int m_sharedAttrLimit;

	/** Map of attribute value handle corresponding to Strings (optional). */
	private StringIntHashMap m_attrValueMap;
    
    /** Byte buffer output sink. */
    private IOutByteBuffer m_byteBuffer;

    /** Cached byte array buffer for output data. */
    private byte[] m_buffer;

    /** Cached current offset in buffer. */
    private int m_offset;

	/** Marked offset in buffer (negative if none). */
	private int m_markOffset;

	/** Buffer for characters of String. */
	private char[] m_text;

	/**
	 * Constructor. Allocates and initializes instances of the data tables used
	 * for storing state information during the serialization process.
	 */
	public XBISWriter() {
		m_text = new char[STRING_ARRAY_SIZE];
		m_markOffset = -1;
        m_noNamespace = new OutputInitialNamespace("", NO_NAMESPACE);
        m_xmlNamespace = new OutputInitialNamespace("xml", XML_NAMESPACE);
        m_uriMap = new HashMap();
        m_uriMap.put(NO_NAMESPACE, m_noNamespace);
        m_uriMap.put(XML_NAMESPACE, m_xmlNamespace);
        m_noNamespace.setActiveHandle(0);
        m_noNamespace.incrementNesting();
        m_xmlNamespace.setActiveHandle(1);
        m_xmlNamespace.incrementNesting();
        m_namespaceCount = 2;
        m_uriCount = 0;
        m_activeNamespaces = new OutputNamespace[INITIAL_NAMESPACE_COUNT];
        m_activeNamespaces[0] = m_noNamespace;
        m_activeNamespaces[1] = m_xmlNamespace;
        m_activeCount = 2;
        initState();
		m_isReset = true;
	}

    /**
     * Initialize output, writing the header information.
     * 
     * @param id output adapter identifier
     * @throws IOException on error writing to stream
     */
	protected final void initWrite(int id) throws IOException {
	    if (isReset()) {
	        clearReset();
            m_buffer = m_byteBuffer.getBuffer();
            m_offset = m_byteBuffer.getOffset();
            writeByte(HEADER_VERSION_ID);
            writeByte(id);
            writeByte(0);
            writeByte(0);
	    }
    }
    
    /**
     * Set the byte buffer.
     *
     * @param buff
     */
    public void setBuffer(IOutByteBuffer buff) {
        m_byteBuffer = buff;
        m_buffer = buff.getBuffer();
        if (m_buffer.length < MINIMUM_BUFFER_SIZE) {
            throw new IllegalArgumentException("Buffer size must be at least " +
                MINIMUM_BUFFER_SIZE);
        }
        m_offset = buff.getOffset();
    }

	/**
	 * Set shared content text limit. This method enables or disables
	 * content text sharing, with associated control information. If enabled,
	 * text content <code>String</code>s of no more than the limit length are
	 * assigned handles and checked for duplication so the same content will
	 * only be sent once.
	 *
	 * @param limit maximum number of characters for sharing (zero to disable)
	 */
	public final void setSharedContent(int limit) {
		m_sharedTextLimit = limit;
		if (limit > 0) {
			if (m_textMap == null) {
				m_textMap = new StringIntHashMap();
			}
            if (m_charsMap == null) {
                m_charsMap = new CharBlockIntHashMap();
            }
		} else {
			m_textMap = null;
		}
	}

	/**
	 * Set shared attribute text parameter. This method enables or disables
	 * attribute value text sharing, with associated control information. If
	 * enabled, attribute value <code>String</code>s of no more than the limit
	 * length are assigned handles and checked for duplication so the same
	 * value will only be sent once.
	 *
	 * @param limit maximum number of characters for sharing (zero to disable)
	 */
	public final void setSharedAttributes(int limit) {
		m_sharedAttrLimit = limit;
		if (limit > 0) {
			if (m_attrValueMap == null) {
				m_attrValueMap = new StringIntHashMap();
			}
		} else {
			m_attrValueMap = null;
		}
	}

	/**
	 * Set buffer mark for later access. Marks the current byte position (next
	 * byte to be written), allowing it to be read and/or written later. It is
	 * an error to call this method while a mark is set.
	 *
	 * @throws IOException on error writing to stream
	 */
	protected final void setMark() throws IOException {
		if (m_markOffset >= 0) {
			throw new IllegalStateException("Internal error - mark already set");
		} else {
			if (m_offset >= m_buffer.length - MARK_BUFFER_MARGIN) {
			    m_byteBuffer.setOffset(m_offset);
			    m_byteBuffer.free(m_offset, m_buffer.length / 2);
		        m_buffer = m_byteBuffer.getBuffer();
		        m_offset = m_byteBuffer.getOffset();
			}
			m_markOffset = m_offset;
		}
	}

	/**
	 * Read marked byte. Reads the previously marked byte. It is an error to
	 * call this method if a mark is not set, or before the marked byte has
	 * been written normally.
	 *
	 * @return current value of marked byte
	 */
	protected final int readMarked() {
		if (m_markOffset < 0) {
			throw new IllegalStateException("Internal error - mark not set");
		} else {
			return m_buffer[m_markOffset];
		}
	}

	/**
	 * Write marked byte. Writes the previously marked byte. It is an error to
	 * call this method if a mark is not set, or before the marked byte has
	 * been written normally.
	 *
	 * @param value value to be written to byte
	 */
	protected final void writeMarked(int value) {
		if (m_markOffset < 0) {
			throw new IllegalStateException("mark not set");
		} else {
			m_buffer[m_markOffset] = (byte)value;
		}
	}

	/**
	 * Clear buffer mark. It is an error to call this method if a
	 * mark is not set.
	 */
	protected final void clearMark() {
		if (m_markOffset < 0) {
			throw new IllegalStateException("mark not set");
		} else {
			m_markOffset = -1;
		}
	}

	/**
	 * Update buffer with output state.
	 */
	public void updateBuffer() {
		m_byteBuffer.setOffset(m_offset);
	}

	/**
	 * Check space available in buffer.
	 *
	 * @return number of bytes available
	 */
	private final int checkSpace() {
        return m_buffer.length - m_offset;
	}

    /**
     * Make space for more data in buffer. This makes at least the requested
     * number of bytes available.
     *
     * @param bytes minimum number of bytes of space required
     * @throws IOException on error writing to stream
     */
    private final void makeSpace(int bytes) throws IOException {
        m_byteBuffer.setOffset(m_offset);
        if (m_markOffset >= 0) {
            m_byteBuffer.free(m_markOffset, bytes);
        } else {
            m_byteBuffer.free(m_offset, bytes);
        }
        m_buffer = m_byteBuffer.getBuffer();
        m_offset = m_byteBuffer.getOffset();
    }

	/**
	 * Write byte to buffer. Writes the block of data to the output
	 * stream if the buffer is full.
	 *
	 * @param value byte to be stored to buffer
	 * @throws IOException on error writing to stream
	 */
	protected final void writeByte(int value) throws IOException {
		if (m_offset >= m_buffer.length) {
			makeSpace(1);
		}
		m_buffer[m_offset++] = (byte)value;
	}

	/**
	 * Write the leading bytes for a multiple-byte integer value to stream. This
	 * handles encoding of the variable-length representation used in the serial
	 * form, which gives seven bits of value per serialized byte by using the
	 * high-order bit of each byte as a continuation flag. Only the leading
	 * bytes containing the high-order bits of the value are written by this
	 * method; the final byte must be written by the caller. Space for the
	 * leading bytes must have been assured prior to the call to this method.
	 *
	 * @param value value to be written to stream (greater than
	 * <code>0x7F</code>)
	 */
	private void writeLeadBytes(int value) {
		if (value > 0x3FFF) {
			if (value > 0x1FFFFF) {
				if (value > 0xFFFFFFF) {
					m_buffer[m_offset++] = (byte)(0x80 | (value >> 28));
				}
				m_buffer[m_offset++] =
					(byte)(0x80 | ((value >> 21) & 0x7F));
			}
			m_buffer[m_offset++] = (byte)(0x80 | ((value >> 14) & 0x7F));
		}
		m_buffer[m_offset++] = (byte)(0x80 | ((value >> 7) & 0x7F));
	}

	/**
	 * Write non-negative integer value to stream. This handles encoding of
	 * the variable-length representation used in the serial form, which gives
	 * seven bits of value per serialized byte by using the high-order bit of
	 * each byte as a continuation flag.
	 *
	 * @param value value to be written to stream
	 * @throws IOException on error writing to stream
	 */
	protected void writeValue(int value) throws IOException {

		// check if we need to dump the buffer
		if (m_offset+5 > m_buffer.length) {
			makeSpace(5);
		}

		// store encoded value to buffer
		if (value > 0x7F) {
			writeLeadBytes(value);
			value &= 0x7F;
		}
		m_buffer[m_offset++] = (byte)value;
	}

	/**
	 * Write non-negative integer value with partial lead byte to stream. This
	 * handles encoding of the variable-length representation used in the serial
	 * form, with a partial leading byte. This format is used when a handle
	 * value is combined with flags. If the value cannot fit in the initial byte
	 * a zero value is used to indicate a continuation and the standard encoding
	 * is used in the following byte(s).
	 *
	 * @param value value to be written to stream
	 * @param initial byte containing high bits of value
	 * @param mask value mask within first byte (right justified)
	 * @throws IOException on error writing to stream
	 */
	protected final void writeQuickValue(int value, int initial, int mask)
		throws IOException {

		// check if value fits in initial byte
		value++;
		if ((value & mask) == value) {
			writeByte(initial | value);
		} else {
			writeByte(initial);
			writeValue(value);
		}
	}

    /**
     * Write string of characters to stream. This method just encodes the
     * characters in chunks guaranteed to fit in the buffer.
     *
     * @param buff array containing characters to be written
     * @param offset starting offset in buffer
     * @param length number of characters in string
     * @throws IOException on error writing to stream
     */
    protected final void writeChars(char[] buff, int offset, int length)
        throws IOException {

        // loop for as many blocks as necessary
        int position = offset;
        int end = offset + length;
        int bcnt = Math.min(length, m_buffer.length / 2);
        while (position < end) {
            
            // make sure at least the minimum amount of space is free
            if (checkSpace() < bcnt) {
                makeSpace(bcnt);
            }

            // get character count for this block
            int count = end - position;
            int space = checkSpace() / 3;
            if (count > space) {
                count = space;
            }

            // encode characters directly to buffer
            int limit = position + count;
            while (position < limit) {
                int chr = buff[position++];
                if (chr > 0x7F) {
                    writeLeadBytes(chr);
                    chr &= 0x7F;
                }
                m_buffer[m_offset++] = (byte)chr;
            }
        }
    }

	/**
	 * Write <code>String</code> data to stream. This method uses a dual
	 * approach to retrieving the array of characters in the
	 * <code>String</code>, depending on the number of characters. If the count
	 * is low, the code first makes sure there's enough space in the buffer to
	 * hold all the encoded characters and then accesses them one at a time for
	 * conversion. If the count is high, we retrieve the entire array of
	 * characters and then encode them in chunks guaranteed to fit in the
	 * buffer. This approach creates some duplicate code, but gives the best
	 * performance.
	 *
	 * @param text <code>String</code> to be written
	 * @throws IOException on error writing to stream
	 */
	protected final void writeStringData(String text) throws IOException {

		// check if small enough to access bytes directly
		int length = text.length();
		if (length < SHORT_STRING_LIMIT) {

			// check if we need to dump the buffer
			if (m_offset+length*3 > m_buffer.length) {
				makeSpace(length*3);
			}

			// encode characters directly to buffer
			for (int i = 0; i < length; i++) {
				int chr = text.charAt(i);
				if (chr > 0x7F) {
					writeLeadBytes(chr);
					chr &= 0x7F;
				}
				m_buffer[m_offset++] = (byte)chr;
			}
			
		} else {

			// loop for as many blocks as necessary
			int start = 0;
			while (start < length) {

				// get character count for this block
				int count = length - start;
				if (count > m_text.length) {
					count = m_text.length;
				}

				// copy characters out of String
				text.getChars(start, start+count, m_text, 0);
				start += count;

				// check if we need to dump the buffer
				if (m_offset + count*3 > m_buffer.length) {
					makeSpace(count*3);
				}

				// encode characters directly to buffer
				for (int i = 0; i < count; i++) {
					int chr = m_text[i];
					if (chr > 0x7F) {
						writeLeadBytes(chr);
						chr &= 0x7F;
					}
					m_buffer[m_offset++] = (byte)chr;
				}
			}
		}
	}

    /**
     * Write character string directly to stream. This handles encoding of the
     * character string value representation used in the serial form. The
     * character count plus one is first encoded in the standard integer value
     * format, followed by the specified number of characters.
     *
     * @param buff array containing characters to be written
     * (non-<code>null</code>)
     * @param offset starting offset in buffer
     * @param length number of characters in string
     * @throws IOException on error writing to stream
     * @see #writeChars
     */
    protected final void writeStringChars(char[] buff, int offset, int length)
        throws IOException {
        writeValue(length+1);
        writeChars(buff, offset, length);
    }

	/**
	 * Write <code>String</code> directly to stream. This handles encoding of
	 * the <code>String</code> value representation used in the serial form.
	 * The character count plus one is first encoded in the standard integer
	 * value format (with the value zero reserved for use with <code>null</code>
	 * <code>String</code>s), followed by the specified number of characters.
	 *
	 * @param text <code>String</code> to be written (may be <code>null</code>)
	 * @throws IOException on error writing to stream
	 * @see #writeStringData
	 */
	protected final void writeString(String text)
		throws IOException {
		if (text == null) {
			writeByte(0);
		} else {
			writeValue(text.length()+1);
			writeStringData(text);
		}
	}

	/**
	 * Write content text to stream. If shared content text is enabled and this
	 * text meets the qualifications for sharing, this first checks if the text
	 * has previously been output. If so, the existing handle is used to
	 * reference the text; if not, the value is output and assigned the next
	 * consecutive handle value. If shared content text is not enabled or this
	 * text does not qualify, the text is output as a plain text value.
	 *
	 * @param buff array containing characters to be written
     * @param offset starting offset in buffer
     * @param length number of characters in string
	 * @throws IOException on error writing to stream
	 */
	protected final void writeText(char[] buff, int offset, int length)
        throws IOException {

		// check if a shared value
        new String(buff, offset, length);
		if (m_charsMap != null && length <= m_sharedTextLimit) {

			// check if already defined
			int handle = m_charsMap.get(buff, offset, length);
			if (handle > 0) {

				// write handle for previously defined value
				writeQuickValue(handle, NODE_TEXTREF_FLAG,
					TEXTREF_HANDLE_MASK);

			} else {

				// write new handle definition
				writeByte(NODE_TEXTREF_FLAG + 1);
				writeStringChars(buff, offset, length);
				m_charsMap.add(buff, offset, length, m_charsMap.size()+1);

			}
			return;
		}

		// write plain text content
		writeQuickValue(length, NODE_PLAINTEXT_FLAG, PLAINTEXT_LENGTH_MASK);
		writeChars(buff, offset, length);
	}

    /**
     * Write content text to stream. If shared content text is enabled and this
     * text meets the qualifications for sharing, this first checks if the text
     * has previously been output. If so, the existing handle is used to
     * reference the text; if not, the value is output and assigned the next
     * consecutive handle value. If shared content text is not enabled or this
     * text does not qualify, the text is output as a plain text value.
     *
     * @param text content text to be written
     * @throws IOException on error writing to stream
     */
    protected final void writeText(String text) throws IOException {

        // check if a shared value
        if (m_textMap != null && text.length() <= m_sharedTextLimit) {

            // check if already defined
            int handle = m_textMap.get(text);
            if (handle > 0) {

                // write handle for previously defined value
                writeQuickValue(handle, NODE_TEXTREF_FLAG,
                    TEXTREF_HANDLE_MASK);

            } else {

                // write new handle definition
                writeByte(NODE_TEXTREF_FLAG + 1);
                writeString(text);
                m_textMap.add(text, m_textMap.size()+1);

            }
            return;
        }

        // write plain text content
        writeQuickValue(text.length(), NODE_PLAINTEXT_FLAG,
            PLAINTEXT_LENGTH_MASK);
        writeStringData(text);
    }

    /**
     * Activate a namespace. This assigns an active namespace handle to the
     * namespace, and also adds it to the table of active namespaces.
     *
     * @param ns namespace to be activated
     */
    private void activateNamespace(OutputNamespace ns) {
        if (m_activeCount >= m_activeNamespaces.length) {
            int length = m_activeNamespaces.length * 2;
            OutputNamespace[] grows = new OutputNamespace[length];
            System.arraycopy(m_activeNamespaces, 0, grows, 0, m_activeCount);
            m_activeNamespaces = grows;
        }
        m_activeNamespaces[m_activeCount] = ns;
        if (!ns.isActive()) {
            ns.setActiveHandle(m_activeCount++);
        }
        ns.incrementNesting();
    }

    /**
     * Dectivate a namespace. This clears the active namespace handle previously
     * assigned to the namespace, and also deletes it from the table of active
     * namespaces. This method must be called for namespaces reversing the order
     * of the {@link #activateNamespace} calls.
     *
     * @param ns namespace to be deactivated
     */
    private void deactivateNamespace(OutputNamespace ns) {
        int index = m_activeCount - 1;
        if (ns == m_activeNamespaces[index] && ns.getActiveHandle() == index) {
            ns.decrementNesting();
            if (!ns.isActive()) {
                m_activeNamespaces[index] = null;
                m_activeCount = index;
            }
        } else {
            throw new IllegalStateException
                ("Namespaces must be deactivated in reverse order");
        }
    }

    /**
     * Write namespace definition to stream. This is used to define a new
     * namespace (prefix+URI pair). The format is a single byte with a handle
     * value for the namespace URI and a quick length field for the prefix.
     *
     * @param ns namespace to be declared
     * @throws IOException on error writing to stream
     */
    public void writeNamespaceDef(OutputNamespace ns) throws IOException {
        
        // first check for known namespace URI
        int handle = ns.getOwner().getUriHandle();
        int length = ns.getPrefix().length();
        if (handle >= 0) {
            
            // defined URI, check if handle fits in field
            handle++;
            if (handle <= NSDEF_URIHANDLE_MASK) {

                // write URI handle combined with start of prefix length
                writeQuickValue(length, handle<<NSDEF_URIHANDLE_SHIFT,
                    NSDEF_PRELENGTH_MASK);

            } else if (length < NSDEF_PRELENGTH_MASK) {

                // write prefix length followed by URI handle value
                writeByte(length+1);
                writeValue(handle);

            } else {

                // write URI handle and prefix length as full values
                writeByte(0);
                writeValue(handle);
                writeValue(length+1);

            }
            
        } else {

            // new namespace URI definition, check prefix length size
            if (length < NSDEF_PRELENGTH_MASK) {

                // write prefix length embedded in byte
                writeByte((1<<NSDEF_URIHANDLE_SHIFT)+length+1);
                writeString(ns.getUri());

            } else {

                // write prefix length as separate value following URI
                writeByte(1<<NSDEF_URIHANDLE_SHIFT);
                writeString(ns.getUri());
                writeValue(length+1);

            }
            
            // record the handle for this URI
            ns.getOwner().setUriHandle(++m_uriCount);
        }
            
        // finish with the actual prefix text
        writeStringData(ns.getPrefix());
        
        // set the handle for this namespace
        ns.setDefinitionHandle(m_namespaceCount++);
        activateNamespace(ns);
    }

    /**
     * Write namespace declaration to stream. This allows the reuse of a
     * previously defined namespace without the need to write the component
     * information to the stream again, but can also be used to define a new
     * namespace. The format is a single byte with a handle value for the
     * namespace reference to be reused (extended to multiple bytes, if needed).
     * The scope of the namespace is the whole of the following element.
     *
     * @param ns namespace to be declared
     * @throws IOException on error writing to stream
     */
    public void writeNamespaceDecl(OutputNamespace ns) throws IOException {
        if (ns.getDefinitionHandle() >= 0) {
            writeQuickValue(ns.getDefinitionHandle()+1, NODE_NAMESPACEDECL_FLAG,
                NAMESPACEDECL_HANDLE_MASK);
            activateNamespace(ns);
        } else {
            writeByte(NODE_NAMESPACEDECL_FLAG + 1);
            writeNamespaceDef(ns);
        }
    }

	/**
	 * Write name definition to stream. This encodes the namespace definition
	 * and local name, with a leading byte specifying the format.
	 *
	 * @param local local name to be written
	 * @param ns namespace object for name
	 * @throws IOException on error writing to stream
	 */
	protected final void writeNameDef(String local, OutputNamespace ns)
		throws IOException {

		// check if namespace has been defined
		int handle = ns.getActiveHandle();
		int length = local.length();
		if (handle >= 0) {

			// defined namespace, check if handle fits in field
			handle++;
			if (handle < NAME_NS_MASK) {

				// write namespace handle combined with start of name length
				writeQuickValue(length, (handle+1)<<NAME_NS_SHIFT,
					NAME_LENGTH_MASK);

			} else if (length < NAME_LENGTH_MASK) {

				// write name length followed by namespace handle value
				writeByte(length+1);
				writeValue(handle);

			} else {

				// write namespace handle and name length as full values
				writeByte(0);
				writeValue(handle);
				writeValue(length+1);

			}
		} else {

			// new namespace definition, check name length size
			if (length < NAME_LENGTH_MASK) {

				// write name length embedded in byte
				writeByte((1<<NAME_NS_SHIFT)+length+1);
				writeNamespaceDef(ns);

			} else {

				// write name length as separate value following namespace
				writeByte(1<<NAME_NS_SHIFT);
				writeNamespaceDef(ns);
				writeValue(length+1);

			}
		}

		// write local name text
		writeStringData(local);
	}

	/**
	 * Write attribute to stream. The attribute format uses a lead byte with
	 * flags for shared attribute value and value definition combined with the
	 * attribute handle. The lead byte is followed by the continuation of the
	 * attribute handle or the attribute name definition, if necessary, and
	 * then by the attribute value, value handle, or value definition.
	 *
	 * @param obj attribute to be written
	 * @param value text of value to be written
	 * @throws IOException on error writing to stream
	 */
	protected final void writeAttribute(Object obj, String value)
		throws IOException {

		// check if this may be a shared value
		int lead = ATTRIBUTE_NOTEND_FLAG;
		int vhandle = 0;
		if (m_attrValueMap != null && value.length() <= m_sharedAttrLimit) {

			// check if already defined
			vhandle = m_attrValueMap.get(value);
			if (vhandle > 0) {
				lead = ATTRIBUTE_VALUEREF_FLAG;
			} else {
				lead = ATTRIBUTE_VALUEREF_FLAG | ATTRIBUTE_NEWREF_FLAG;
			}
		}

		// check if attribute name already defined
		int nhandle = getAttributeHandle(obj);
		if (nhandle > 0) {

			// write existing handle for name
			writeQuickValue(nhandle, lead, ATTRIBUTE_HANDLE_MASK);

		} else {

			// define new handle for name
			writeByte(lead+1);
			defineAttribute(obj);

		}

		// write the text value in appropriate format
		if ((lead & ATTRIBUTE_VALUEREF_FLAG) != 0) {
			if ((lead & ATTRIBUTE_NEWREF_FLAG) != 0) {
				writeString(value);
				m_attrValueMap.add(value, m_attrValueMap.size()+1);
			} else {
				writeValue(vhandle);
			}
		} else {
			writeString(value);
		}
	}

    /**
     * Get namespace declaration. This finds the requested namespace, creating
     * it if it does not already exist.
     *
     * @param prefix element namespace prefix
     * @param uri element namespace URI
     * @return declaration
     * @throws XBISException on namespace error
     */
    public OutputNamespace getNamespace(String prefix, String uri)
        throws XBISException {
        
        // check special case of no namespace
        if (uri == null || uri.length() == 0) {
            return m_noNamespace;
        } else {
            
            // use empty string for default namespace prefix
            if (prefix == null) {
                prefix = "";
            }
        
            // create namespace URI if not already defined
            Object obj = m_uriMap.get(uri);
            if (obj == null) {
        
                // first use of namespace, add tracking object to table
                obj = new OutputInitialNamespace(prefix, uri);
                m_uriMap.put(uri, obj);
            }
    
            // get entry for this prefix with URI
            OutputInitialNamespace ins = (OutputInitialNamespace)obj;
            return ins.get(prefix);
        }
    }

    /**
     * Close some number of active namespace declarations. Since namespaces
     * always have element scope, this can be used to close all namespace
     * declarations present on an element at the end of that element.
     *
     * @param count number of active namespace declarations to be closed
     */
    public void closeNamespaces(int count) {
        for (int i = 0; i < count; i++) {
            deactivateNamespace(m_activeNamespaces[m_activeCount-1]);
        }
    }

	/**
	 * Clear reset state. This method must be called before beginning any
	 * operations which write to the output stream.
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
	 * can be reused to serialize multiple independent documents. Subclasses
	 * overriding this method to perform their own reinitialization must call
	 * the base class method before returning to the caller.
	 */
	public void reset() {
		if (!m_isReset) {
            m_uriMap.clear();
            m_uriMap.put(NO_NAMESPACE, m_noNamespace);
            m_uriMap.put(XML_NAMESPACE, m_xmlNamespace);
            m_namespaceCount = 2;
            m_uriCount = 0;
            m_activeCount = 2;
            for (int i = m_activeCount; i < m_activeNamespaces.length; i++) {
                m_activeNamespaces[i] = null;
            }
            if (m_charsMap != null) {
                m_charsMap.clear();
            }
			if (m_textMap != null) {
				m_textMap.clear();
			}
			if (m_attrValueMap != null) {
				m_attrValueMap.clear();
			}
            m_markOffset = -1;
            m_offset = 0;
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
	 * Get handle for attribute. This abstract method must be implemented by
	 * each subclass to return the handle for an attribute.
	 *
	 * @param obj attribute object for which handle is to be found
	 * @return handle value if defined (always strictly positive), negative
	 * value if not defined
	 */
	protected abstract int getAttributeHandle(Object obj);

	/**
	 * Add attribute definition. This abstract method must be implemented by
	 * each subclass to write an attribute definition to the output stream and
	 * add it as a handle.
	 *
	 * @param obj attribute object to be defined
	 * @throws IOException on error writing to stream
	 */
	protected abstract void defineAttribute(Object obj) throws IOException;

    /**
     * Extended namespace information for output processing. This adds tracking
     * of the namespace definition and active namespace handles to the basic
     * namespace definition.
     */
    protected static class OutputNamespace extends BasicNamespace
    {
        /** Handle for namespace definition (set when written). */
        private int m_definitionHandle;
        
        /** Number of nested definitions of this namespace. */
        private int m_nestingCount;
        
        /** Active namespace handle (<code>-1</code> if namespace inactive). */
        private int m_activeHandle;
        
        /** Owning initial namespace for URI. */
        private final OutputInitialNamespace m_owner;
        
        /**
         * Constructor.
         *
         * @param prefix namespace prefix (empty string for default namespace)
         * @param uri namespace URI
         * @param owner owning initial namespace for URI
         */
        public OutputNamespace(String prefix, String uri,
            OutputInitialNamespace owner) {
            super(prefix, uri);
            m_definitionHandle = -1;
            m_activeHandle = -1;
            m_owner = (owner == null) ? (OutputInitialNamespace)this : owner;
        }
        
        /**
         * Constructor for predefined namespaces.
         *
         * @param prefix namespace prefix (empty string for default namespace)
         * @param uri namespace URI
         * @param handle namespace definition and active handle
         */
        public OutputNamespace(String prefix, String uri, int handle) {
            super(prefix, uri);
            m_definitionHandle = handle;
            m_activeHandle = handle;
            m_owner = (OutputInitialNamespace)this;
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
         * Set definition handle.
         *
         * @param handle definition handle
         */
        private void setDefinitionHandle(int handle) {
            m_definitionHandle = handle;
        }
    
        /**
         * Get definition handle.
         *
         * @return definition handle
         */
        public int getDefinitionHandle() {
            return m_definitionHandle;
        }
    
        /**
         * Set active handle.
         *
         * @param handle active handle
         */
        private void setActiveHandle(int handle) {
            m_activeHandle = handle;
        }
    
        /**
         * Get active handle.
         *
         * @return active handle
         */
        public int getActiveHandle() {
            return m_activeHandle;
        }
    
        /**
         * Get owning URI information.
         *
         * @return information for the URI used by this namespace
         */
        public OutputInitialNamespace getOwner() {
            return m_owner;
        }
    }

    /**
     * Initial extended namespace information for output processing. This is the
     * form used for the first namespace referencing a particular URI. It
     * provides extension information for access to other namespaces that
     * reference the same URI with other prefixes.
     */
    private static class OutputInitialNamespace extends OutputNamespace
    {
        /** Handle for namespace URI. */
        private int m_uriHandle;
        
        /** Flag for no other prefixes possible. */
        private boolean m_isFixed;
        
        /** Map based on prefix used for this URI (lazy contruction, only if
         multiple definitions present). */
        private HashMap m_prefixMap;
    
        /**
         * Constructor.
         *
         * @param prefix namespace prefix (empty string for default namespace)
         * @param uri namespace URI
         */
        public OutputInitialNamespace(String prefix, String uri) {
            super(prefix, uri, null);
            m_uriHandle = -1;
        }
    
        /**
         * Constructor for predefined URIs. These use fixed prefixes, so they're
         * flagged to be non-extensible.
         *
         * @param prefix namespace prefix (empty string for default namespace)
         * @param uri namespace URI
         * @param handle predefined handle value (both active namespace handle
         * and namespace handle)
         */
        public OutputInitialNamespace(String prefix, String uri, int handle) {
            super(prefix, uri, handle);
            m_uriHandle = Integer.MAX_VALUE;
            m_isFixed = true;
        }
    
        /**
         * Set URI handle.
         *
         * @param handle URI handle
         */
        private void setUriHandle(int handle) {
            m_uriHandle = handle;
        }
    
        /**
         * Get URI handle.
         *
         * @return URI handle
         */
        public int getUriHandle() {
            return m_uriHandle;
        }
    
        /**
         * Get the namespace information for a particular prefix of this
         * namespace. If the prefix has already been defined for this URI the
         * existing information is returned, otherwise a new pairing is
         * constructed and returned.
         *
         * @param prefix 
         * @return namespace information
         * @throws XBISException on namespace error
         */
        public OutputNamespace get(String prefix) throws XBISException {
            if (getPrefix().equals(prefix)) {
                return this;
            } else {
                OutputNamespace ns = null;
                if (m_prefixMap != null) {
                    ns = (OutputNamespace)m_prefixMap.get(prefix);
                }
                if (ns == null) {
                    if (m_isFixed) {
                        throw new XBISException("Cannot set prefix " + prefix +
                            " to namespace " + getUri());
                    } else {
                        ns = new OutputNamespace(prefix, getUri(), this);
                        if (m_prefixMap == null) {
                            m_prefixMap = new HashMap();
                        }
                        m_prefixMap.put(prefix, ns);
                    }
                }
                return ns;
            }
        }
    }
}