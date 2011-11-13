/*
Copyright (c) 2000-2004, Dennis M. Sosnoski
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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import org.xbis.eventstore.EventStore;
import org.xbis.eventstore.SAXReader;
import org.xbis.eventstore.SAXWriter;

/**
 * Test code for XSLT output to text form which is then gzipped, and parsing of
 * ungzipped text.
 *
 * @author Dennis M. Sosnoski
 * @version 0.8
 */

public class TestGZIP implements ITest
{
	/** Reader used for parsing text input document. */
	private XMLReader m_reader;

	/** Writer used for text output of documents. */
	private Transformer m_writer;

	/**
	 * Constructor.
	 */

	protected TestGZIP() throws SAXException,
        TransformerConfigurationException,
        TransformerFactoryConfigurationError {
		m_reader = XMLReaderFactory.createXMLReader();
        m_writer = TransformerFactory.newInstance().newTransformer();
        String text = m_reader.getClass().getName();
        System.out.println("Created XMLReader class is " + text);
        int split = text.lastIndexOf('.');
        if (split >= 0) {
            text = text.substring(split+1);
        }
	}

	/**
	 * Time writing and parsing documents as text. This override of the abstract
	 * base class method gives the time required by this model to save and
	 * restore the documents as text.
	 *
	 * @param ins input streams for reading document text
     * @param store parse event stream store
	 * @param out output stream for writing document text to byte array
	 * @param reps number of repetitions to time
     * @param passes number of passes to try (with best pass time saved)
     * @param times array of time values filled in by test run
	 * @throws Exception anything thrown by example code
	 */

	public void runTest(ByteArrayInputStream[] ins, EventStore store,
		ByteArrayOutputStream out, int reps, int passes, int[] times)
        throws Exception {

		// read documents from XML text
        SummaryHandler handler = new SummaryHandler();
		DocumentSummary summ1 = new DocumentSummary();
		handler.setSummary(summ1);
		m_reader.setContentHandler(handler);
        try {
            m_reader.setProperty
                ("http://xml.org/sax/properties/lexical-handler", handler);
        } catch (Exception ex) {}
        long best = Long.MAX_VALUE;
		for (int i = 0; i < passes; i++) {
            long start = System.currentTimeMillis();
			handler.getSummary().reset();
            for (int j = 0; j < reps; j++) {
                for (int k = 0; k < ins.length; k++) {
                    ins[k].reset();
                    m_reader.parse(new InputSource(ins[k]));
                }
            }
            long time = System.currentTimeMillis() - start;
            if (best > time) {
                best = time;
            }
			if (i == 0) {
				handler.setSummary(new DocumentSummary());
			}
		}
        times[RunTest.PARSE_TIME_OFFSET] = (int)best;
		System.out.println(" parsed in " + best + " ms.");
		if (!summ1.dataEquals(handler.getSummary())) {
			System.out.println("Error on document summary");
            RunTest.printSummary(" original", summ1);
            RunTest.printSummary(" last", handler.getSummary());
		} else {
            RunTest.printSummary(" document summary", summ1);
		}

        // parse with output saved to event store
        long mem = RunTest.getMemoryUsage();
        EventStore es = new EventStore();
        SAXWriter saxw = new SAXWriter(es);
        m_reader.setContentHandler(saxw);
        try {
            m_reader.setProperty
                ("http://xml.org/sax/properties/lexical-handler", saxw);
        } catch (Exception ex) {}
        best = Long.MAX_VALUE;
        for (int i = 0; i < passes; i++) {
            long start = System.currentTimeMillis();
            for (int j = 0; j < reps; j++) {
                saxw.reset();
                for (int k = 0; k < ins.length; k++) {
                    ins[k].reset();
                    m_reader.parse(new InputSource(ins[k]));
                }
            }
            long time = System.currentTimeMillis() - start;
            if (best > time) {
                best = time;
            }
        }
        times[RunTest.WRITEEVENT_TIME_OFFSET] = (int)best;
        System.out.println(" parsed to event store in " + best + " ms.");
        System.out.println(" memory usage is " +
            (RunTest.getMemoryUsage()-mem));

		// output document as gzipped XML text from event store
        SAXReader saxr = new SAXReader(es);
        saxr.setContentHandler(handler);
        try {
            saxr.setProperty
                ("http://xml.org/sax/properties/lexical-handler", handler);
        } catch (Exception ex) {}
        best = Long.MAX_VALUE;
		for (int i = 0; i < passes; i++) {
            SAXSource source = new SAXSource(new InputSource(""));
            long start = System.currentTimeMillis();
            for (int j = 0; j < reps; j++) {
                saxr.reset();
                source.setXMLReader(saxr);
                out.reset();
                if (ins.length > 1) {
                
                    // merge multiple documents with leading lengths
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    for (int k = 0; k < ins.length; k++) {
                        bos.reset();
                        GZIPOutputStream gzo = new GZIPOutputStream(bos);
                        Result result = new StreamResult(gzo);
                        m_writer.transform(source, result);
                        gzo.close();
                        byte[] doc = bos.toByteArray();
                        int length = doc.length;
                        out.write(length >> 24);
                        out.write(length >> 16);
                        out.write(length >> 8);
                        out.write(length);
                        out.write(doc);
                    }
                
                } else {
                    
                    // compress single documents directly to output
                    GZIPOutputStream gzo = new GZIPOutputStream(out);
                    Result result = new StreamResult(gzo);
                    m_writer.transform(source, result);
                    gzo.close();
                }
            }
            long time = System.currentTimeMillis() - start;
            if (best > time) {
                best = time;
            }
		}
        times[RunTest.WRITE_TIME_OFFSET] = (int)best;
		System.out.println(" wrote gzip from event store in " + best + " ms." +
            " with size " + out.size());

        // read documents from gzipped output
        m_reader.setContentHandler(handler);
        try {
            m_reader.setProperty
                ("http://xml.org/sax/properties/lexical-handler", handler);
        } catch (Exception ex) {}
        best = Long.MAX_VALUE;
        byte[] byts = out.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(byts);
        for (int i = 0; i < passes; i++) {
            long start = System.currentTimeMillis();
            handler.getSummary().reset();
            for (int j = 0; j < reps; j++) {
                if (ins.length > 1) {
                
                    // break apart multiple documents with leading lengths
                    int offset = 0;
                    for (int k = 0; k < ins.length; k++) {
                        int length = byts[offset++] << 24;
                        length += (byts[offset++] & 0xFF) << 16;
                        length += (byts[offset++] & 0xFF) << 8;
                        length += byts[offset++] & 0xFF;
                        bis = new ByteArrayInputStream(byts, offset, length);
                        GZIPInputStream gzi = new GZIPInputStream(bis);
                        InputSource source = new InputSource(gzi);
                        m_reader.parse(source);
                        offset += length;
                    }
                
                } else {
                    
                    // parse single documents directly from compressed form
                    bis.reset();
                    GZIPInputStream gzi = new GZIPInputStream(bis);
                    InputSource source = new InputSource(gzi);
                    m_reader.parse(source);
                    
                }
            }
            long time = System.currentTimeMillis() - start;
            if (best > time) {
                best = time;
            }
        }
        times[RunTest.READ_TIME_OFFSET] = (int)best;
        System.out.println(" read from gzip in " + best + " ms.");
        if (!summ1.dataEquals(handler.getSummary())) {
            System.out.println("Error on document summary");
            RunTest.printSummary(" original", summ1);
            RunTest.printSummary(" last", handler.getSummary());
        } else {
            RunTest.printSummary(" document summary", summ1);
        }
	}
}