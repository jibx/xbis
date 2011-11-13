/*
 * Copyright (c) 2008, Dennis M. Sosnoski. All rights reserved.
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
import java.io.OutputStream;

import org.jibx.runtime.IXMLWriter;
import org.jibx.runtime.impl.OutByteBuffer;

/**
 * JiBX writer implementation for output to a normal stream. This just extends the base class with stream handling.
 * 
 * @author Dennis M. Sosnoski
 */
public class JibxStreamWriter extends JibxWriter
{
    /** Parent writer if created as child. */
    private final JibxStreamWriter m_parent;
    
    /** Actual output buffer passed to base class. */
    private final OutByteBuffer m_byteBuffer;
    
    /**
     * Constructor with buffer supplied.
     * 
     * @param uris ordered array of URIs for namespaces used in document (must be constant; the value in position 0 must
     * always be the empty string "", and the value in position 1 must always be the XML namespace
     * "http://www.w3.org/XML/1998/namespace")
     * @param buff
     */
    public JibxStreamWriter(String[] uris, OutByteBuffer buff) {
        super(uris, buff);
        m_byteBuffer = buff;
        m_parent = null;
    }
    
    /**
     * Constructor. This creates a byte buffer of the default size.
     * 
     * @param uris ordered array of URIs for namespaces used in document (must be constant; the value in position 0 must
     * always be the empty string "", and the value in position 1 must always be the XML namespace
     * "http://www.w3.org/XML/1998/namespace")
     */
    public JibxStreamWriter(String[] uris) {
        this(uris, new OutByteBuffer(XBISWriter.DEFAULT_BUFFER_SIZE));
    }
    
    /**
     * Copy constructor. This initializes the writer and extension namespace information from an existing instance.
     * 
     * @param base existing instance
     * @param uris ordered array of URIs for namespaces used in document
     */
    public JibxStreamWriter(JibxStreamWriter base, String[] uris) {
        super(base, uris);
        m_byteBuffer = base.m_byteBuffer;
        m_parent = base;
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
        flush();
        reset();
        m_byteBuffer.setOutput(os);
        m_writer.initWrite(XBISConstants.JIBX_SOURCE_ID);
    }
    
    /**
     * Create a child writer instance to be used for a separate binding. The child writer inherits the output handling
     * from this writer, while using the supplied namespace URIs.
     * 
     * @param uris ordered array of URIs for namespaces used in document (see {@link #setNamespaceUris(String[])})
     * @return child writer
     */
    public IXMLWriter createChildWriter(String[] uris) {
        return new JibxStreamWriter(this, uris);
    }
}