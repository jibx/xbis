/*
 * Copyright (c) 2000-2001 Sosnoski Software Solutions, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights 
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 */

package com.sosnoski.util;

import com.sosnoski.util.array.CharArray;

/**
 * Growable <code>char</code> array with added <code>StringBuffer</code>-like
 * functionality. This implementation differs from <code>StringBuffer</code> in
 * that it is unsynchronized so as to provide the best possible performance for 
 * typical usage scenarios. Explicit synchronization must be implemented by a 
 * wrapper class or directly by the application in cases where instances are 
 * modified in a multithreaded environment. See the base classes for other 
 * details of the implementation.<p>
 *
 * This class defines a number of convenience methods for working with 
 * character data (including simple arrays and <code>String</code>s). Besides
 * allowing character data to be appended/inserted/replaced in the growable 
 * array, other methods support comparisons between the several forms of 
 * character sequences and calculations of hash codes independent of the form 
 * of the sequence.
 * 
 * @author Dennis M. Sosnoski
 * @version 1.0
 */

public class CharBuffer extends CharArray
{
	/** Hash value multiplier to scramble bits in accumulation. */
	protected static final int KEY_MULTIPLIER = 517;

	/**
	 * Constructor with full specification.
	 * 
	 * @param size number of <code>char</code> values initially allowed in array
	 * @param growth maximum size increment for growing array
	 */
	
	public CharBuffer(int size, int growth) {
		super(size, growth);
	}

	/**
	 * Constructor with initial size specified.
	 * 
	 * @param size number of <code>char</code> values initially allowed in array
	 */
	
	public CharBuffer(int size) {
		super(size);
	}

	/**
	 * Default constructor.
	 */
	
	public CharBuffer() {
		this(DEFAULT_SIZE);
	}

	/**
	 * Constructor from <code>String</code>.
	 */
	
	public CharBuffer(String base) {
		this(base.length());
		append(base);
	}

	/**
	 * Constructor from <code>char[]</code>.
	 */
	
	public CharBuffer(char[] base) {
		this(base.length);
		append(base);
	}

	/**
	 * Copy (clone) constructor.
	 * 
	 * @param base instance being copied
	 */
	
	public CharBuffer(CharArray base) {
		super(base);
	}

	/**
	 * Appends the characters from a <code>String</code> to the current contents
	 * of the array.
	 * 
	 * @param text <code>String</code> of characters to be appended
	 * @return index number of first character appended
	 */
	
	public int append(String text) {
		
		// grow array if necessary
		int length = text.length();
		ensureCapacity(m_countPresent + length);
		
		// append text after existing content
		int start = m_countPresent;
		for (int i = 0; i < length; i++) {
			m_baseArray[m_countPresent++] = text.charAt(i);
		}
		return start;
	}

	/**
	 * Appends the characters from an array range to the current contents
	 * of the array.
	 * 
	 * @param text array containing characters to be appended
	 * @param offset starting offset in array
	 * @param length length of character range in array
	 * @return index number of first character appended
	 */
	
	public int append(char[] text, int offset, int length) {
		
		// grow array if necessary
		ensureCapacity(m_countPresent + length);
		
		// append text after existing content
		int start = m_countPresent;
		System.arraycopy(text, offset, m_baseArray, m_countPresent, length);
		m_countPresent += length;
		return start;
	}

	/**
	 * Appends all the characters from an array to the current contents
	 * of the array.
	 * 
	 * @param text array containing characters to be appended
	 * @return index number of first character appended
	 */
	
	public int append(char[] text) {
		return append(text, 0, text.length);
	}

	/**
	 * Appends all the characters from another growable array to the current 
	 * contents of the array.
	 * 
	 * @param text growable array containing characters to be appended
	 * @return index number of first character appended
	 */
	
	public int append(CharArray text) {
		return append((char[])getArray(text), 0, text.size());
	}

	/**
	 * Adjust the characters in the array to make room for an insertion or
	 * replacement. Depending on the relative sizes of the range being 
	 * replaced and the range being inserted, this may move characters past
	 * the start of the replacement range up or down in the array.
	 * 
	 * @param from index number of first character to be replaced
	 * @param to index number past last character to be replaced
	 * @param length length of character range being inserted
	 */
	
	protected void adjust(int from, int to, int length) {
		if (from >= 0 && to < m_countPresent && from <= to) {
			int change = from + length - to;
			if (change > 0) {
				ensureCapacity(m_countPresent+change);
			}
			if (to < m_countPresent){
				System.arraycopy(m_baseArray, to, m_baseArray, to + change,
					m_countPresent - to);
				m_countPresent += change;
			}
		} else {
			throw new ArrayIndexOutOfBoundsException("Invalid remove range");
		}
	}

	/**
	 * Replace a character range in the array with the characters from a 
	 * <code>String</code>.
	 * 
	 * @param from index number of first character to be replaced
	 * @param to index number past last character to be replaced
	 * @param text replacement text
	 */
	
	public void replace(int from, int to, String text) {
		adjust(from, to, text.length());
		text.getChars(0, text.length(), m_baseArray, from);
	}

	/**
	 * Replace a character range in the array with the characters from a 
	 * <code>char[]</code>.
	 * 
	 * @param from index number of first character to be replaced
	 * @param to index number past last character to be replaced
	 * @param text replacement text
	 */
	
	public void replace(int from, int to, char[] text) {
		adjust(from, to, text.length);
		System.arraycopy(text, 0, m_baseArray, from, text.length);
	}

	/**
	 * Insert the characters from a <code>String</code> into the array.
	 * 
	 * @param offset insert position offset in array
	 * @param text insertion text
	 */
	
	public void insert(int offset, String text) {
		adjust(offset, offset, text.length());
		text.getChars(0, text.length(), m_baseArray, offset);
	}

	/**
	 * Insert the characters from a <code>char[]</code> into the array.
	 * 
	 * @param offset insert position offset in array
	 * @param text insertion text
	 */
	
	public void insert(int offset, char[] text) {
		adjust(offset, offset, text.length);
		System.arraycopy(text, 0, m_baseArray, offset, text.length);
	}

	/**
	 * Compare the character sequence in the array with a <code>String</code>.
	 * 
	 * @param comp <code>String</code> value to be compared
	 * @return <code>true</code> if the character sequences are identical,
	 * <code>false</code> if they're different
	 */
	
	public boolean equals(String comp) {
		
		// first check for lengths the same
		if (comp != null && m_countPresent == comp.length()) {
				
			// match character by character for full compare
			for (int index = 0; index < m_countPresent; index++) {
				if (m_baseArray[index] != comp.charAt(index)) {
					return false;
				}
			}
			
			// return with all characters matched
			return true;
		}
		
		// fail if matching not done
		return false;
	}

	/**
	 * Compare the character sequence in the array with the characters in a 
	 * simple array range.
	 * 
	 * @param comp array of characters to be compared
	 * @param offset starting offset in array
	 * @param length length of character range in array
	 * @return <code>true</code> if the character sequences are identical,
	 * <code>false</code> if they're different
	 */
	
	public boolean equals(char[] comp, int offset, int length) {
		
		// first check for lengths the same
		if (comp != null && m_countPresent == length) {
				
			// match character by character for full compare
			for (int index = 0; index < m_countPresent; index++) {
				if (m_baseArray[index] != comp[index+offset]) {
					return false;
				}
			}
			
			// return with all characters matched
			return true;
		}
		
		// fail if matching not done
		return false;
	}

	/**
	 * Compare the character sequence in the array with that in a simple
	 * array.
	 * 
	 * @param comp array of characters to be compared
	 * @return <code>true</code> if the character sequences are identical,
	 * <code>false</code> if they're different
	 */
	
	public boolean equals(char[] comp) {
		return equals(comp, 0, comp.length);
	}

	/**
	 * Compare the character sequence in the array with the sequence in
	 * another growable array.
	 * 
	 * @param comp array of characters to be compared
	 * @return <code>true</code> if the character sequences are identical,
	 * <code>false</code> if they're different
	 */
	
	public boolean equals(CharArray comp) {
		return equals((char[])getArray(comp), 0, comp.size());
	}
	
	/**
	 * Compare the character sequences in a pair of arrays.
	 * 
	 * @param a first character sequence array (non-<code>null</code>)
	 * @param b second character sequence array (may be <code>null</code>)
	 * @return <code>true</code> if the character sequences are identical,
	 * <code>false</code> if they're different
	 */
	
	public static boolean equals(char[] a, char[] b) {
		
		// make sure the lengths are the same
		if (b != null && a.length == b.length) {
				
			// match character by character for full compare
			for (int i = 0; i < a.length; i++) {
				if (a[i] != b[i]) {
					return false;
				}
			}
			return true;
			
		} else {
			return false;
		}
	}
	
	/**
	 * Compare the character sequences in a simple array and a 
	 * <code>String</code>.
	 * 
	 * @param a simple array character sequence (non-<code>null</code>)
	 * @param b <code>String</code> character sequence (may be 
	 * <code>null</code>)
	 * @return <code>true</code> if the character sequences are identical, 
	 * <code>false</code> if they're different
	 */
	
	public static boolean equals(char[] a, String b) {
		
		// make sure the lengths are the same
		if (b != null && a.length == b.length()) {
				
			// match character by character for full compare
			for (int i = 0; i < a.length; i++) {
				if (a[i] != b.charAt(i)) {
					return false;
				}
			}
			return true;
			
		} else {
			return false;
		}
	}

	/**
	 * Compare the character sequence in the array with a <code>String</code> 
	 * without regard to case.
	 * 
	 * @param comp <code>String</code> value to be compared
	 * @return <code>true</code> if the character sequences are identical 
	 * except perhaps for case, <code>false</code> if they're different
	 */
	
	public boolean equalsNoCase(String comp) {
		
		// first check for lengths the same
		if (comp != null && m_countPresent == comp.length()) {
			
			// match character by character for full compare
			int index = 0;
			for (; index < m_countPresent; index++) {
				if (Character.toLowerCase(m_baseArray[index]) !=
					Character.toLowerCase(comp.charAt(index))) {
					return false;
				}
			}
			
			// return with all characters matched
			return true;
		}
		
		// fail if matching not done
		return false;
	}

	/**
	 * Compare the character sequence in the array with the characters in a 
	 * simple array range without regard to case.
	 * 
	 * @param comp array of characters to be compared
	 * @param offset starting offset in array
	 * @param length length of character range in array
	 * @return <code>true</code> if the character sequences are identical 
	 * except perhaps for case, <code>false</code> if they're different
	 */
	
	public boolean equalsNoCase(char[] comp, int offset, int length) {
		
		// first check for lengths the same
		if (comp != null && m_countPresent == length) {
				
			// match character by character for full compare
			for (int index = 0; index < m_countPresent; index++) {
				if (Character.toLowerCase(m_baseArray[index]) != 
					Character.toLowerCase(comp[index+offset])) {
					return false;
				}
			}
			
			// return with all characters matched
			return true;
		}
		
		// fail if matching not done
		return false;
	}

	/**
	 * Compare the character sequence in the array with that in a simple
	 * array without regard to case.
	 * 
	 * @param comp array of characters to be compared
	 * @return <code>true</code> if the character sequences are identical 
	 * except perhaps for case, <code>false</code> if they're different
	 */
	
	public boolean equalsNoCase(char[] comp) {
		return equalsNoCase(comp, 0, comp.length);
	}

	/**
	 * Compare the character sequence in the array with the sequence in
	 * another growable array without regard to case.
	 * 
	 * @param comp array of characters to be compared
	 * @return <code>true</code> if the character sequences are identical 
	 * except perhaps for case, <code>false</code> if they're different
	 */
	
	public boolean equalsNoCase(CharArray comp) {
		return equalsNoCase((char[])getArray(comp), 0, comp.size());
	}
	
	/**
	 * Compare the character sequences in a pair of arrays without regard to
	 * case.
	 * 
	 * @param a first character sequence array (non-<code>null</code>)
	 * @param b second character sequence array (may be <code>null</code>)
	 * @return <code>true</code> if the character sequences are identical 
	 * except perhaps for case, <code>false</code> if they're different
	 */
	
	public static boolean equalsNoCase(char[] a, char[] b) {
		
		// make sure the lengths are the same
		if (b != null && a.length == b.length) {
				
			// match character by character for full compare
			for (int i = 0; i < a.length; i++) {
				if (Character.toUpperCase(a[i]) !=
					Character.toUpperCase(b[i])) {
					return false;
				}
			}
			return true;
			
		} else {
			return false;
		}
	}
	
	/**
	 * Compare the character sequences in a simple array and a 
	 * <code>String</code> without regard to case.
	 * 
	 * @param a simple array character sequence (non-<code>null</code>)
	 * @param b <code>String</code> character sequence (may be 
	 * <code>null</code>)
	 * @return <code>true</code> if the character sequences are identical 
	 * except perhaps for case, <code>false</code> if they're different
	 */
	
	public static boolean equalsNoCase(char[] a, String b) {
		
		// make sure the lengths are the same
		if (b != null && a.length == b.length()) {
				
			// match character by character for full compare
			for (int i = 0; i < a.length; i++) {
				if (Character.toLowerCase(a[i]) !=
					Character.toLowerCase(b.charAt(i))) {
					return false;
				}
			}
			return true;
			
		} else {
			return false;
		}
	}
	
	/**
	 * Compute the compatible hash code value for character sequence in a simple
	 * array.
	 * 
	 * @param text text to be hashed
	 * @return compatible hash code value
	 */
	
	public static int hashCode(char[] text) {
		int hash = 0;
		for (int i = 0; i < text.length; i++) {
			hash = (hash * KEY_MULTIPLIER) + text[i];
		}
		return hash;
	}

	/**
	 * Compute the compatible hash code value for character sequence in a 
	 * <code>String</code>.
	 * 
	 * @param text text to be hashed
	 * @return compatible hash code value
	 */
	
	public static int hashCode(String text) {
		int hash = 0;
		for (int i = 0; i < text.length(); i++) {
			hash = (hash * KEY_MULTIPLIER) + text.charAt(i);
		}
		return hash;
	}

	/**
	 * Compute a compatible hash code value for this character sequence.
	 * 
	 * @return compatible hash code value
	 */
	
	public int hashCode() {
		int hash = 0;
		for (int i = 0; i < size(); i++) {
			hash = (hash * KEY_MULTIPLIER) + m_baseArray[i];
		}
		return hash;
	}

	/**
	 * Duplicates the object with the generic call.
	 * 
	 * @return a copy of the object
	 */
	
	public Object clone() {
		return new CharBuffer(this);
	}

	/**
	 * Construct a <code>String</code> from the character sequence present.
	 * 
	 * @return copy of data as <code>String</code>
	 */
	
	public String toString() {
		return new String(m_baseArray, 0, m_countPresent);
	}

	/**
	 * Construct a <code>String</code> from a portion of the character sequence
	 * present.
	 * 
	 * @param offset start offset in array
	 * @param length number of characters to use
	 * @return copy of data as <code>String</code>
	 */
	
	public String toString(int offset, int length) {
		if (offset + length <= m_countPresent) {
			return new String(m_baseArray, offset, length);
		} else {
			throw new ArrayIndexOutOfBoundsException();
		}
	}
}
