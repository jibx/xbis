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

/**
 * XML Binary Information Set constant definitions. These definitions are used by both
 * the input and output sides.
 *
 * @author Dennis M. Sosnoski
 */
public interface XBISConstants
{
    // URI for no-namespace namespace
    public static final String NO_NAMESPACE = "";
    
    // URI for XML namespace
    public static final String XML_NAMESPACE =
        "http://www.w3.org/XML/1998/namespace";
    
	// format version identifier
	public static final int HEADER_VERSION_ID = 1;

	// source adapter identifier
	public static final int SAX2_SOURCE_ID = 1;
	public static final int DOM_SOURCE_ID = 2;
	public static final int DOM4J_SOURCE_ID = 3;
	public static final int JDOM_SOURCE_ID = 4;
	public static final int JIBX_SOURCE_ID = 5;

	// High bit of node lead byte indicates element definition when set
	public static final int NODE_ELEMENT_FLAG = 0x80;
	public static final int ELEMENT_HASATTRIBUTES_FLAG = 0x40;
	public static final int ELEMENT_HASCHILDREN_FLAG = 0x20;
	public static final int ELEMENT_HANDLE_MASK = 0x1F;

	// If not an element definition, next bit indicates plain text content
	public static final int NODE_PLAINTEXT_FLAG = 0x40;
	public static final int PLAINTEXT_LENGTH_MASK = 0x3F;

	// If not plain text content, next bit indicates text reference
	public static final int NODE_TEXTREF_FLAG = 0x20;
	public static final int TEXTREF_HANDLE_MASK = 0x1F;

	// If not text reference, next bit indicates namespace declaration
	public static final int NODE_NAMESPACEDECL_FLAG = 0x10;
	public static final int NAMESPACEDECL_HANDLE_MASK = 0x0F;

	// If not a namespace reference, remaining bits specify type
	public static final int NODE_TYPE_MASK = 0x0F;

	// type values for general nodes
	public static final int NODE_TYPE_END = 0;
	public static final int NODE_TYPE_DOCUMENT = 1;
	public static final int NODE_TYPE_COMMENT = 2;
	public static final int NODE_TYPE_CDATA = 3;
	public static final int NODE_TYPE_PI = 4;
    public static final int NODE_TYPE_DOCTYPE = 5;
	public static final int NODE_TYPE_NOTATION = 6;
	public static final int NODE_TYPE_UNPARSEDENTITY = 7;
	public static final int NODE_TYPE_SKIPPEDENTITY = 8;
    public static final int NODE_TYPE_ELEMENTDECL = 9;
    public static final int NODE_TYPE_ATTRIBUTEDECL = 10;
    public static final int NODE_TYPE_EXTERNALENTITYDECL = 11;

	// attribute lead byte format
	public static final int ATTRIBUTE_VALUEREF_FLAG = 0x80;
	public static final int ATTRIBUTE_NEWREF_FLAG = 0x40;
	public static final int ATTRIBUTE_NOTEND_FLAG = 0x40;
	public static final int ATTRIBUTE_HANDLE_MASK = 0x3F;

	// name definition lead byte format
	public static final int NAME_NS_MASK = 0x07;
	public static final int NAME_NS_SHIFT = 5;
	public static final int NAME_LENGTH_MASK = 0x1F;
    
    // namespace definition lead byte format
    public static final int NSDEF_URIHANDLE_MASK = 0x1F;
    public static final int NSDEF_URIHANDLE_SHIFT = 3;
    public static final int NSDEF_PRELENGTH_MASK = 0x07;

	// Predefined namespace handle values.
	public static final int NS_HANDLE_NONE = 1;
	public static final int NS_HANDLE_XML = 2;
}