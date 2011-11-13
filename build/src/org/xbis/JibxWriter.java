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

import org.jibx.runtime.IExtensibleWriter;
import org.jibx.runtime.IXMLWriter;
import org.jibx.runtime.impl.IOutByteBuffer;
import org.jibx.runtime.impl.OutByteBuffer;
import org.jibx.runtime.impl.XMLWriterNamespaceBase;

/**
 * JiBX writer implementation. This allows JiBX to directly write XBIS representations of documents.
 * 
 * @author Dennis M. Sosnoski
 */
public class JibxWriter extends XMLWriterNamespaceBase implements IExtensibleWriter
{
    /** XBIS writer used for output. */
    protected final XBISEventWriter m_writer;
    
    /** Parent writer if created as child. */
    private final JibxWriter m_parent;
    
    /** Flag for start of element last reported. */
    private boolean m_isStart;
    
    /** Flag for element start tag written. */
    private boolean m_isWritten;
    
    /** Namespace index for element to be written. */
    private int m_namespaceIndex;
    
    /** Local name for element to be written. */
    private String m_elementName;
    
    /**
     * Constructor with writer supplied.
     * 
     * @param uris ordered array of URIs for namespaces used in document (must be constant; the value in position 0 must
     * always be the empty string "", and the value in position 1 must always be the XML namespace
     * "http://www.w3.org/XML/1998/namespace")
     * @param wrtr
     */
    public JibxWriter(String[] uris, XBISEventWriter wrtr) {
        super(uris);
        m_writer = wrtr;
        m_writer.initState();
        m_parent = null;
    }
    
    /**
     * Constructor with buffer supplied. This creates a default writer using the supplied buffer.
     * 
     * @param uris ordered array of URIs for namespaces used in document (must be constant; the value in position 0 must
     * always be the empty string "", and the value in position 1 must always be the XML namespace
     * "http://www.w3.org/XML/1998/namespace")
     * @param buff
     */
    public JibxWriter(String[] uris, IOutByteBuffer buff) {
        this(uris, new XBISEventWriter(buff));
        m_writer.setSharedContent(6);
        m_writer.setSharedAttributes(6);
        m_writer.initState();
    }
    
    /**
     * Constructor.
     * 
     * @param uris ordered array of URIs for namespaces used in document (must be constant; the value in position 0 must
     * always be the empty string "", and the value in position 1 must always be the XML namespace
     * "http://www.w3.org/XML/1998/namespace")
     */
    public JibxWriter(String[] uris) {
        this(uris, new OutByteBuffer(XBISWriter.DEFAULT_BUFFER_SIZE));
    }
    
    /**
     * Copy constructor. This initializes the writer and extension namespace information from an existing instance.
     * 
     * @param base existing instance
     * @param uris ordered array of URIs for namespaces used in document
     */
    public JibxWriter(JibxWriter base, String[] uris) {
        super(base, uris);
        m_writer = base.m_writer;
        m_parent = base;
    }
    
    /**
     * Initialize writer.
     * 
     * @throws IOException 
     */
    public void init() throws IOException {
        m_writer.initWrite(XBISConstants.JIBX_SOURCE_ID);
    }
    
    /**
     * Set the byte buffer.
     *
     * @param buff
     */
    public void setBuffer(IOutByteBuffer buff) {
        m_writer.setBuffer(buff);
    }
    
    /**
     * Set namespace URIs. It is intended to be used only for reconfiguring an existing writer for reuse with the same
     * output stream.
     * 
     * @param uris ordered array of URIs for namespaces used in document
     */
    public void setNamespaceUris(String[] uris) {
        internalSetUris(uris);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.impl.XMLWriterNamespaceBase#defineNamespace(int, java.lang.String)
     */
    protected void defineNamespace(int index, String prefix) {}
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.impl.XMLWriterNamespaceBase#undefineNamespace(int)
     */
    protected void undefineNamespace(int index) {}
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#setIndentSpaces(int, java.lang.String, char)
     */
    public void setIndentSpaces(int count, String newline, char indent) {}
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#writeXMLDecl(java.lang.String, java.lang.String, java.lang.String)
     */
    public void writeXMLDecl(String version, String encoding, String standalone) throws IOException {
        m_writer.writeDocumentStart();
    }
    
    private void writeStart(boolean attr) throws IOException {
        init();
        try {
            // write start element, without or with namespace
            if (m_namespaceIndex == 0) {
                m_writer.writeElementStart("", "", m_elementName, attr);
            } else {
                m_writer.writeElementStart(getNamespacePrefix(m_namespaceIndex), getNamespaceUri(m_namespaceIndex),
                    m_elementName, attr);
            }
            m_isWritten = true;
        } catch (XBISException e) {
            throw new IOException("Error writing document: " + e.getMessage());
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#startTagOpen(int, java.lang.String)
     */
    public void startTagOpen(int index, String name) throws IOException {
        
        // save information for writing when attribute status known
        m_namespaceIndex = index;
        m_elementName = name;
        m_isStart = true;
        m_isWritten = false;
        
        // increment nesting for any possible content
        incrementNesting();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#startTagNamespaces(int, java.lang.String, int[], java.lang.String[])
     */
    public void startTagNamespaces(int index, String name, int[] nums, String[] prefs) throws IOException {
        startTagOpen(index, name);
        try {
            
            // find the namespaces actually being declared
            int[] deltas = openNamespaces(nums, prefs);
            
            // write the namespace declarations
            for (int i = 0; i < deltas.length; i++) {
                int slot = deltas[i];
                String prefix = getNamespacePrefix(slot);
                String uri = getNamespaceUri(slot);
                m_writer.beginNamespaceMapping(prefix, uri);
            }
            
            // open the start tag
            startTagOpen(index, name);
            
        } catch (XBISException e) {
            throw new IOException("Error writing to stream: " + e.getMessage());
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#addAttribute(int, java.lang.String, java.lang.String)
     */
    public void addAttribute(int index, String name, String value) throws IOException {
        if (m_isStart) {
            if (!m_isWritten) {
                writeStart(true);
            }
            try {
                if (index == 0) {
                    m_writer.writeElementAttribute("", "", name, value);
                } else {
                    m_writer.writeElementAttribute(getNamespacePrefix(index), getNamespaceUri(index), name, value);
                }
            } catch (XBISException e) {
                throw new IOException("Error writing to stream: " + e.getMessage());
            }
        } else {
            throw new IllegalStateException("Internal error - not in start tag");
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#closeStartTag()
     */
    public void closeStartTag() throws IOException {
        if (m_isWritten) {
            m_writer.writeEndAttribute();
        } else {
            writeStart(false);
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#closeEmptyTag()
     */
    public void closeEmptyTag() throws IOException {
        closeStartTag();
        m_writer.writeElementEnd();
        decrementNesting();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#startTagClosed(int, java.lang.String)
     */
    public void startTagClosed(int index, String name) throws IOException {
        startTagOpen(index, name);
        closeStartTag();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#endTag(int, java.lang.String)
     */
    public void endTag(int index, String name) throws IOException {
        m_writer.writeElementEnd();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#writeTextContent(java.lang.String)
     */
    public void writeTextContent(String text) throws IOException {
        m_writer.writeText(text);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#writeCData(java.lang.String)
     */
    public void writeCData(String text) throws IOException {
        char[] chars = text.toCharArray();
        m_writer.writeCDATA(chars, 0, chars.length);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#writeComment(java.lang.String)
     */
    public void writeComment(String text) throws IOException {
        char[] chars = text.toCharArray();
        m_writer.writeComment(chars, 0, chars.length);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#writeEntityRef(java.lang.String)
     */
    public void writeEntityRef(String name) throws IOException {
        throw new IllegalStateException("Internal error - unsupported operation");
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#writeDocType(java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String)
     */
    public void writeDocType(String name, String sys, String pub, String subset) throws IOException {
        throw new IllegalStateException("Internal error - unsupported operation");
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#writePI(java.lang.String, java.lang.String)
     */
    public void writePI(String target, String data) throws IOException {
        throw new IllegalStateException("Internal error - unsupported operation");
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#indent()
     */
    public void indent() throws IOException {}
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#flush()
     */
    public void flush() throws IOException {
        if (m_parent == null) {
            m_writer.setHasContent(true);
            m_writer.updateBuffer();
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jibx.runtime.IXMLWriter#close()
     */
    public void close() throws IOException {
        m_writer.writeDocumentEnd();
    }
    
    /**
     * Reset output. This resets both the JiBX state information and the XBIS state information.
     */
    public void reset() {
        super.reset();
        m_writer.reset();
    }
    
    /**
     * Create a child writer instance to be used for a separate binding. The child writer inherits the output handling
     * from this writer, while using the supplied namespace URIs.
     * 
     * @param uris ordered array of URIs for namespaces used in document (see {@link #setNamespaceUris(String[])})
     * @return child writer
     */
    public IXMLWriter createChildWriter(String[] uris) {
        return new JibxWriter(this, uris);
    }
}