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

package test;

import java.io.*;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import org.xbis.SAXToXBISAdapter;
import org.xbis.XBISToSAXAdapter;

/**
 * Test code for XBIS encoding and decoding. Roundtrips a document to XBIS
 * format and back out as text, comparing the result with the same document
 * output as text directly from the parser.
 *
 * @author Dennis M. Sosnoski
 * @version 0.8
 */

public class RoundTrip
{
    /**
     * Compare a pair of byte arrays for differences.
     *
     * @param array1 first array to be compared
     * @param array2 second array to be compared
     * @return <code>true</code> if identical, <code>false</code> if different
     */

    private static boolean compArrays(byte[] array1, byte[] array2) {
        boolean match = true;
        if (array1.length != array2.length) {
            match = false;
            System.err.println("Different text lengths: " + array1.length +
                " and " + array2.length);
        }
        int length = Math.min(array1.length, array2.length);
        for (int i = 0; i < length; i++) {
            if (array1[i] != array2[i]) {
                System.err.println("Data mismatch at offset " + i + ":");
                int start = Math.max(i-20, 0);
                int use = Math.min(length-start, 40);
                System.err.println(" " + new String(array1, start, use));
                System.err.println(" " + new String(array2, start, use));
                return false;
            }
        }
        return match;
    }

    /**
     * Parse input document and output again as text.
     *
     * @param in input stream for reading document text
     * @return output text as byte array
     * @throws Exception anything thrown by example code
     */
     
    public static byte[] runParseOut(InputStream in) throws Exception {
        
        // output XML text from document parse
        XMLReader reader = XMLReaderFactory.createXMLReader();
        Transformer transformer =
            TransformerFactory.newInstance().newTransformer();
        SAXSource source = new SAXSource(new InputSource(in));
        source.setXMLReader(reader);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Result result = new StreamResult(out);
        transformer.transform(source, result);
        return out.toByteArray();
    }

	/**
	 * Parse input document to generate XBIS then output again as text.
	 *
	 * @param in input stream for reading document text
     * @return output text as byte array
	 * @throws Exception anything thrown by example code
	 */
     
    public static byte[] runRoundTrip(InputStream in) throws Exception {

		// parse XML text to generate XBIS
        XMLReader reader = XMLReaderFactory.createXMLReader();
		SAXToXBISAdapter toxbis = new SAXToXBISAdapter();
		reader.setContentHandler(toxbis);
        try {
            reader.setProperty
                ("http://xml.org/sax/properties/lexical-handler", toxbis);
            reader.setProperty
                ("http://xml.org/sax/properties/declaration-handler", toxbis);
        } catch (Exception ex) {
            System.out.println("***Error setting XBIS handler on parser***");
            throw ex;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        toxbis.setStream(out);
        reader.parse(new InputSource(in));
        out.flush();
        
        // output XML text from XBIS
        XBISToSAXAdapter tosax = new XBISToSAXAdapter();
        Transformer transformer =
            TransformerFactory.newInstance().newTransformer();
		ByteArrayInputStream bis = new ByteArrayInputStream(out.toByteArray());
        SAXSource source = new SAXSource(new InputSource(bis));
        source.setXMLReader(tosax);
        out.reset();
        Result result = new StreamResult(out);
        transformer.transform(source, result);
        return out.toByteArray();
    }

	/**
	 * Test driver, reads an input file and writes out the roundtripped version
     * to an output file.
	 *
	 * @param args command line arguments
     * @throws Exception anything thrown by example code
	 */

	public static void main(String[] args) throws Exception {

		// handle roundtrip test
		if (args.length == 3) {
            InputStream in = new FileInputStream(args[0]);
            byte[] byts1 = runParseOut(in);
            OutputStream out = new FileOutputStream(args[1]);
            out.write(byts1);
            out.close();
            in = new FileInputStream(args[0]);
            byte[] byts2 = runRoundTrip(in);
            out = new FileOutputStream(args[2]);
            out.write(byts2);
            out.close();
            if (compArrays(byts1, byts2)) {
                System.out.println
                    ("Output from parse matched output from XBIS roundtrip");
            }
		} else {
			System.err.println("Usage: java test.RoundTrip in-file out-file1 " +                "out-file2");
		}
	}
}