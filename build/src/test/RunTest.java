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
import java.util.ArrayList;

import org.xbis.eventstore.EventStore;
import org.xbis.eventstore.SAXWriter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Base class for XML Binary Information Set testing. Contains the method used to read
 * the list of files from the command line, which sets up the XML documents and
 * calls the methods implemented in the subclass for actual processing.
 *
 * @author Dennis M. Sosnoski
 * @version 0.8
 */

public class RunTest
{
    //
    // Constants for result times array filled in by test runs
    
    public static final int PARSE_TIME_OFFSET = 0;
    public static final int WRITEEVENT_TIME_OFFSET = 1;
    public static final int READEVENT_TIME_OFFSET = 2;
    public static final int READ_TIME_OFFSET = 3;
    public static final int WRITE_TIME_OFFSET = 4;
    
    // Additional information tracked by base code
    public static final int INPUT_SIZE_OFFSET = 5;
    public static final int OUTPUT_SIZE_OFFSET = 6;
    public static final int RESULTS_ARRAY_SIZE = 7;
    
    // Names of fields in array
    public static final String[] RESULT_NAMES =
    {
        "Parse", "Write-event", "Read-event", "Read", "Write", "Text size",
        "Out size"
    };

	/**
	 * Constructor. Just initializes and prints the JVM information.
	 */

	protected RunTest() {
    }
    
	/**
	 * Read contents of file into byte array.
	 *
	 * @param file file to be read
	 * @return array of bytes containing all data from file
	 * @throws IOException on file access error
	 */

	private static byte[] getFileBytes(File file) throws IOException {
		int length = (int)file.length();
		byte[]data = new byte[length];
		FileInputStream in = new FileInputStream(file);
		int offset = 0;
		do {
			offset += in.read(data, offset, length-offset);
		} while (offset < data.length);
		return data;
	}

	/**
	 * Load data for file or directory argument. If the supplied path is a
	 * file, the returned array of arrays consists of a single byte array
	 * containing the data from that file. If the path is a directory, the 
	 * returned array contains one value array for each file in the
	 * target directory, with the value arrays sorted by file name (ignoring
	 * case).
	 *
	 * @param path file or directory path
	 * @return array of arrays of bytes containing all data from file(s)
	 * @throws IOException on file access error
	 */

	private static byte[][] getPathData(String path) throws IOException {
		
		// check type of path supplied
		File file = new File(path);
		ArrayList data = new ArrayList();
		if (file.isDirectory()) {
			
			// get array of files in this directory
			File[] files = file.listFiles();
			byte[][] arrays = new byte[files.length][];
			String[] names = new String[files.length];
			int count = 0;
			for (int i = 0; i < files.length; i++) {
				if (files[i].isFile()) {
					arrays[count] = getFileBytes(files[i]);
					names[count++] = files[i].getName();
				}
			}
			
			// (bubble) sort results by file name
			for (int i = 0; i < count; i++) {
				for (int j = i+1; j < count; j++) {
					if (names[j].compareToIgnoreCase(names[i]) < 0) {
						byte[] array = arrays[j];
						arrays[j] = arrays[i];
						arrays[i] = array;
						String name = names[j];
						names[j] = names[i];
						names[i] = name;
					}
				}
//				System.out.println("  sorted " + names[i]);
			}
			
			// convert results to sized array
			byte[][] results = new byte[count][];
			System.arraycopy(arrays, 0, results, 0, count);
			return results;
			
		} else {
			byte[][] results = new byte[1][];
			results[0] = getFileBytes(file);
			return results;
		}
	}

    /**
     * Calculates the current memory usage. This method first loops through
     * multiple garbage collection requests and waits in order to encourage the
     * JVM to do a full collection.
     * 
     * @return computed memory usage
     */
    
    public static long getMemoryUsage() {
        Runtime rt = Runtime.getRuntime();
        for (int i = 0; i < 4; i++) {
            rt.gc();
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {}
        }
        return rt.totalMemory() - rt.freeMemory();
    }

    /**
     * Print document summary information. Prints the information with a
     * supplied lead phrase.
     *
     * @param lead lead text phrase for document summary
     * @param info document summary information
     */

    public static void printSummary(String lead, DocumentSummary info) {
        System.out.println(lead + " has " + info.getElementCount() +
            " elements, " + info.getAttributeCount() +
            " attributes with " + info.getAttrCharCount() +
            " characters of data, " + info.getContentCount() +
            " content text segments with " + info.getTextCharCount() +
            " characters of text, and " + info.getCommentCharCount() +
            " characters of comment text");
    }

	/**
	 * Test driver, just reads the input files and executes the processing
	 * code.
	 *
	 * @param argv command line arguments
	 */

	public static void main(String[] argv) {

		// handle list of files to be used for test
		if (argv.length >= 2) {
            
            // print basic JVM and Java information
            System.out.println("Java version " +
                System.getProperty("java.version"));
            String text = System.getProperty("java.vm.name");
            if (text != null) {
                System.out.println(text);
            }
            text = System.getProperty("java.vm.version");
            if (text != null) {
                System.out.println(text);
            }
            text = System.getProperty("java.vm.vendor");
            if (text == null) {
                text = System.getProperty("java.vendor");
            }
            System.out.println(text);
            int split = text.indexOf(' ');
            if (split >= 0) {
                text = text.substring(0, split);
            }
            String jvm = text;
            
            // initialize for test run
            System.out.println("Test variation " + argv[0]);
            String[] names = new String[argv.length-1];
            int[][] summs = new int[argv.length-1][];
            int fill = 0;
			for (int i = 1; i < argv.length; i++) {

				// make sure we have a non-empty file name
				String path = argv[i].trim();
				if (path.length() > 0) {

					// read file data into byte array
					byte[][] data = null;
					try {
						data = getPathData(argv[i].trim());
					} catch (IOException ex) {
						System.err.println("Error reading file " + path + ':');
						ex.printStackTrace(System.err);
						return;
					}
                    
                    // set up for document input and output
                    ByteArrayInputStream[] ins =
                        new ByteArrayInputStream[data.length];
                    int length = 0;
                    for (int j = 0; j < data.length; j++) {
                        ins[j] = new ByteArrayInputStream(data[j]);
                        length += data[j].length;
                    }
                    ByteArrayOutputStream out =
                        new ByteArrayOutputStream(length);

					// process file data with subclass implementation
                    int[] results = new int[RESULTS_ARRAY_SIZE];
					try {

                        // parse with output saved to event store
                        EventStore es = new EventStore();
                        try {
                            SAXWriter saxw = new SAXWriter(es);
                            XMLReader reader =
                                XMLReaderFactory.createXMLReader();
                            reader.setContentHandler(saxw);
                            try {
                                reader.setProperty
                                    ("http://xml.org/sax/properties/lexical-handler",
                                    saxw);
                            } catch (Exception ex) {}
                            for (int j = 0; j < ins.length; j++) {
                                reader.parse(new InputSource(ins[j]));
                            }
                        } catch (SAXException e) {
                            System.err.println("Error parsing data:");
                            e.printStackTrace();
                            System.exit(0);
                        }
                    
                        // initialize tracking information
                        names[fill] = new File(path).getName();
                        summs[fill++] = results;
                        results[INPUT_SIZE_OFFSET] = length;
                        
                        // load the actual test implementation
                        String name = "test.Test" + argv[0];
                        ITest inst = null;
                        try {
                            Class clas = Class.forName(name);
                            inst = (ITest)clas.newInstance();
                        } catch (ClassNotFoundException e) {
                            System.err.println("Class " + name + " not found");
                        }
                    
                        // run initial parse pass with pause after
                        inst.runTest(ins, es, out, 1, 2, results);
                        for (int j = 0; j < 5; j++) {
                            try {
                                 System.gc();
                                 Thread.sleep(200);
                             } catch (InterruptedException ex) {}
                        }
                        
                        // perform the actual timed tests
                        System.out.println("Processing " + path + " (" +
                            length + " bytes)");
//                        inst.runTest(ins, es, out, 3, 2, results);
                        inst.runTest(ins, es, out, 10, 5, results);
						System.out.println(" output size is " + out.size());
                        RandomAccessFile save =
                            new RandomAccessFile("temp.xml", "rw");
                        save.setLength(0);
                        save.write(out.toByteArray());
                        save.close();
                        
					} catch (Exception ex) {
						ex.printStackTrace(System.err);
						return;
					}
                    
                    // finish tracking information with output size
                    results[OUTPUT_SIZE_OFFSET] = out.size();
				}
			}
            
            // finish with summary information in CSV form
            System.out.println();
            System.out.println("Summary information for all documents:");
            System.out.print(",");
            for (int i = 0; i < RESULTS_ARRAY_SIZE; i++) {
                System.out.print(",");
                for (int j = 0; j < fill; j++) {
                    System.out.print("," + names[j]);
                }
            }
            System.out.println();
            System.out.print(jvm + "-" + argv[0] + ",");
            for (int i = 0; i < RESULTS_ARRAY_SIZE; i++) {
                System.out.print("," + RESULT_NAMES[i]);
                for (int j = 0; j < fill; j++) {
                    System.out.print("," + summs[j][i]);
                }
            }
            System.out.println();
            
		} else {
			System.err.println("Usage: java test.RunTest " +
                "TEXT|XBIS|ZIP|GZIP files-or-directories");
		}
	}
}