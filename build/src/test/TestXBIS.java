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

package test;

import java.io.*;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import org.xbis.SAXToXBISAdapter;
import org.xbis.XBISToSAXAdapter;
import org.xbis.eventstore.EventStore;
import org.xbis.eventstore.SAXReader;
import org.xbis.eventstore.SAXWriter;

/**
 * Test code for XBIS encoding and decoding.
 *
 * @author Dennis M. Sosnoski
 */
public class TestXBIS implements ITest
{
	/** Reader used for parsing text input document. */
	private XMLReader m_reader;
	
	/** XMLS input handler. */
	private XBISToSAXAdapter m_input;

	/**
	 * Constructor.
	 * 
	 * @throws SAXException 
	 */
	protected TestXBIS() throws SAXException {
		m_reader = XMLReaderFactory.createXMLReader();
        String text = m_reader.getClass().getName();
        System.out.println("Created XMLReader class is " + text);
        int split = text.lastIndexOf('.');
        if (split >= 0) {
            text = text.substring(split+1);
        }
		m_input = new XBISToSAXAdapter();
	}

	/**
	 * Time writing and reading documents in XBIS encoding.
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

		// output document as XBIS from event store
        SAXReader saxr = new SAXReader(es);
		SAXToXBISAdapter saxhand = new SAXToXBISAdapter();
		saxr.setContentHandler(saxhand);
        try {
            saxr.setProperty
                ("http://xml.org/sax/properties/lexical-handler", saxhand);
        } catch (Exception ex) {}
        best = Long.MAX_VALUE;
		for (int i = 0; i < passes; i++) {
            long start = System.currentTimeMillis();
            handler.getSummary().reset();
            for (int j = 0; j < reps; j++) {
                saxr.reset();
                out.reset();
                saxhand.setStream(out);
                for (int k = 0; k < ins.length; k++) {
                    saxr.read();
                }
                saxhand.flush();
                out.flush();
            }
            long time = System.currentTimeMillis() - start;
            if (best > time) {
                best = time;
            }
		}
        times[RunTest.WRITE_TIME_OFFSET] = (int)best;
		System.out.println(" wrote XBIS from event store in " + best + " ms." +
            " with size " + out.size());

        // read documents from XBIS output
		m_input.setContentHandler(handler);
        try {
            m_input.setProperty
                ("http://xml.org/sax/properties/lexical-handler", handler);
        } catch (Exception ex) {
            System.out.println("***Error setting lexical handler on XBIS***");
        }
        best = Long.MAX_VALUE;
        byte[] byts = out.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(byts);
        for (int i = 0; i < passes; i++) {
            long start = System.currentTimeMillis();
            handler.getSummary().reset();
            for (int j = 0; j < reps; j++) {
                bis.reset();
                m_input.setStream(bis);
                for (int k = 0; k < ins.length; k++) {
                    m_input.readDocument();
                }
            }
            long time = System.currentTimeMillis() - start;
            if (best > time) {
                best = time;
            }
        }
        times[RunTest.READ_TIME_OFFSET] = (int)best;
        System.out.println(" read from XBIS in " + best + " ms.");
        if (!summ1.dataEquals(handler.getSummary())) {
            System.out.println("Error on document summary");
            RunTest.printSummary(" original", summ1);
            RunTest.printSummary(" last", handler.getSummary());
        } else {
            RunTest.printSummary(" document summary", summ1);
        }
	}
}