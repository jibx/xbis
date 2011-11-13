/*
Copyright (c) 2000-2003, Dennis M. Sosnoski
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

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * XML Binary Information Set data handling exception.
 *
 * @author Dennis M. Sosnoski
 * @version 1.0
 */

public class XBISException extends Exception
{
	private Throwable m_rootCause;
	
	/**
	 * Constructor from other exception.
	 *
	 * @param msg error message text
	 */

	 public XBISException(String msg) {
		super(msg);
	}
	
	/**
	 * Constructor from message and wrapped exception.
	 * 
	 * @param msg message describing the exception condition
	 * @param root exception which caused this exception
	 */
	
	public XBISException(String msg, Throwable root) {
		super(msg);
		m_rootCause = root;
	}
	
	/**
	 * Get root cause exception.
	 *
	 * @return exception that caused this exception
	 */
	
	public Throwable getRootCause() {
		return m_rootCause;
	}
	
	/**
	 * Build string representation. If there's no wrapped exception this
	 * just returns the usual text, otherwise it appends the wrapped
	 * exception information to the text generated from this one.
	 *
	 * @return string representation
	 */
	
	public String toString() {
		if (m_rootCause == null) {
			return super.toString();
		} else {
			return super.toString() + "\nRoot cause: " + 
				m_rootCause.toString();
		}
	}

	/**
	 * Print stack trace to standard error. This is an override of the base
	 * class method to implement exception chaining.
	 */
	
	public void printStackTrace() {
		super.printStackTrace();
		if (m_rootCause != null) {
			System.err.print("Cause: ");
			m_rootCause.printStackTrace();
		}
	}

	/**
	 * Print stack trace to stream. This is an override of the base class method
	 * to implement exception chaining.
	 * 
	 * @param s stream for printing stack trace
	 */
	
	public void printStackTrace(PrintStream s) {
		super.printStackTrace(s);
		if (m_rootCause != null) {
			s.print("Caused by: ");
			m_rootCause.printStackTrace(s);
		}
	}

	/**
	 * Print stack trace to writer. This is an override of the base class method
	 * to implement exception chaining.
	 * 
	 * @param s writer for printing stack trace
	 */
	
	public void printStackTrace(PrintWriter s) {
		super.printStackTrace(s);
		if (m_rootCause != null) {
			s.print("Caused by: ");
			m_rootCause.printStackTrace(s);
		}
	}
}
