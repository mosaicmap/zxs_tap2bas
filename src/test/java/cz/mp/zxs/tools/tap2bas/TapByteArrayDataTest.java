/*
 * TapByteArrayDataTest.java
 *
 *  created: 20.9.2017
 *  charset: UTF-8
 */

package cz.mp.zxs.tools.tap2bas;


import java.math.BigDecimal;
import org.junit.*;
import static org.junit.Assert.*;


/**
 *
 * @author Martin Pokorný
 * @see ByteArrayDataTest
 */
public class TapByteArrayDataTest {

    @Test
    public void testReadBlockAndReturnAsEscapedString01() {
        byte[] rawData;
        TapByteArrayData tData;
        String text;
        
        rawData = new byte[] {(byte)0xF5, (byte)0x22, (byte)0x48, 
                (byte)0x65, (byte)0x6C, (byte)0x6C, (byte)0x6F,
                (byte)0x20, (byte)0xA2, (byte)0x22};
        tData = new TapByteArrayData(rawData);
        try {
            text = tData.readBlockAndReturnAsEscapedString(rawData.length);
            assertEquals("PRINT \"Hello {S}\"", text);
        } catch (InvalidTapException ex) {
            fail(ex.getMessage());
        }
        
        rawData = new byte[] {(byte)0x48, 
                (byte)0x65, (byte)0x6C, (byte)0x6C, (byte)0x6F,
                (byte)0x20, (byte)0x7F, (byte)0x20, 
                (byte)0x80, (byte)0x81, (byte)0x88};
        tData = new TapByteArrayData(rawData);
        try {
            text = tData.readBlockAndReturnAsEscapedString(rawData.length);
            //System.out.println("\"" + text + "\"");
            assertEquals("Hello {(C)} {-1}{-2}{+1}", text);
        } catch (InvalidTapException ex) {
            fail(ex.getMessage());
        }   
        
        rawData = new byte[] {(byte)0x14, (byte)0x01};
        tData = new TapByteArrayData(rawData);
        try {
            text = tData.readBlockAndReturnAsEscapedString(rawData.length);
            //System.out.println("\"" + text + "\"");
            assertEquals("{INVERSE 1}", text);
        } catch (InvalidTapException ex) {
            fail(ex.getMessage());
        }         
    }

    @Test
    public void testReadAndParseBasicNumber01Integer() {
        byte[] rawData;
        TapByteArrayData tData;
        BigDecimal number;
        
        rawData = new byte[] {(byte)0, (byte)0, (byte)0x80, (byte)0x01, (byte)0, (byte)0xAB};
        tData = new TapByteArrayData(rawData);
        number = tData.readBlockAndParseBasicNumber();
        assertTrue(new BigDecimal("384").compareTo(number) == 0);

        rawData = new byte[] {(byte)0, (byte)0xFF, (byte)0x80, (byte)0x01, (byte)0, (byte)0xAB};
        tData = new TapByteArrayData(rawData);        
        number = tData.readBlockAndParseBasicNumber();
        assertTrue(new BigDecimal("-384").compareTo(number) == 0);
    }
        
    @Test
    public void testReadAndParseBasicNumber02Float() {
        byte[] rawData;
        TapByteArrayData tData;
        BigDecimal number;
        
        rawData = new byte[] {(byte)126, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0xAB};
        tData = new TapByteArrayData(rawData);        
        number = tData.readBlockAndParseBasicNumber();
        assertTrue(new BigDecimal("0.125").compareTo(number) == 0);

        rawData = new byte[] {(byte)128, (byte)76, (byte)0, (byte)0, (byte)0, (byte)0xAB};
        tData = new TapByteArrayData(rawData);        
        number = tData.readBlockAndParseBasicNumber();
        assertTrue(new BigDecimal("0.796875").compareTo(number) == 0);

        rawData = new byte[] {(byte)128, (byte)204, (byte)0, (byte)0, (byte)0, (byte)0xAB};
        tData = new TapByteArrayData(rawData);        
        number = tData.readBlockAndParseBasicNumber();
        assertTrue(new BigDecimal("-0.796875").compareTo(number) == 0);

        rawData = new byte[] {(byte)128, (byte)0, (byte)0, (byte)0, (byte)0};
        tData = new TapByteArrayData(rawData);        
        number = tData.readBlockAndParseBasicNumber();
        assertTrue(new BigDecimal("0.5").compareTo(number) == 0);
        
        rawData = new byte[] {(byte)128, (byte)128, (byte)0, (byte)0, (byte)0};
        tData = new TapByteArrayData(rawData);        
        number = tData.readBlockAndParseBasicNumber();
        assertTrue(new BigDecimal("-0.5").compareTo(number) == 0);        

//        rawData = new byte[] {(byte)127, (byte)0x7f, (byte)0xff, (byte)0xff, (byte)0xff};
//        tData = new TapByteArrayData(rawData);        
//        number = tData.readBlockAndParseBasicNumber();
//        assertTrue(new BigDecimal("0.499999999883584678173380").compareTo(number) == 0);        
    }
    
    
    // -- static:

    @Test
    public void testIsValidVariableName() {
        assertTrue(TapByteArrayData.isValidVariableName("c"));
        assertTrue(TapByteArrayData.isValidVariableName("C"));
        assertTrue(TapByteArrayData.isValidVariableName("char"));
        assertTrue(TapByteArrayData.isValidVariableName("z2"));
        assertFalse(TapByteArrayData.isValidVariableName("2"));
        assertFalse(TapByteArrayData.isValidVariableName("_Z"));
    }
    
    @Test
    public void testIsEol() {
        assertTrue(TapByteArrayData.isEolChar(0x0D));
        assertFalse(TapByteArrayData.isEolChar(0x0A));
    }

    @Test
    public void testIsBeginOfNumberRepresentation() {
        assertTrue(TapByteArrayData.isBeginOfNumberRepresentation(0x0E));
        assertFalse(TapByteArrayData.isBeginOfNumberRepresentation(0x0D));
    }

    @Test
    public void testIsUdgChar() {
        assertTrue(TapByteArrayData.isUdgChar(0x90));   // {A}
        assertTrue(TapByteArrayData.isUdgChar(0xA2));   // {S}
        assertTrue(TapByteArrayData.isUdgChar(0xA3));   // {T}
        assertTrue(TapByteArrayData.isUdgChar(0xA4));   // {U}
        assertFalse(TapByteArrayData.isUdgChar(0xA5));
        assertFalse(TapByteArrayData.isUdgChar(0x89));
        assertFalse(TapByteArrayData.isUdgChar(0xFFF));
    }

    @Test
    public void testIsMosaicGraphicChar() {
        assertTrue(TapByteArrayData.isMosaicGraphicChar(0x80));
        assertTrue(TapByteArrayData.isMosaicGraphicChar(0x88));
        assertTrue(TapByteArrayData.isMosaicGraphicChar(0x8F));
        assertFalse(TapByteArrayData.isMosaicGraphicChar(0x90));
        assertFalse(TapByteArrayData.isMosaicGraphicChar(0x9));
    }
    
    @Test
    public void testIsPrintableChar() {
        assertTrue(TapByteArrayData.isPrintableChar('a'));
        assertTrue(TapByteArrayData.isPrintableChar('A'));
        assertTrue(TapByteArrayData.isPrintableChar('z'));
        assertTrue(TapByteArrayData.isPrintableChar('Z'));
        assertTrue(TapByteArrayData.isPrintableChar('-'));
        assertTrue(TapByteArrayData.isPrintableChar('>'));
        assertTrue(TapByteArrayData.isPrintableChar(' '));
        assertTrue(TapByteArrayData.isPrintableChar(0x20));
        assertTrue(TapByteArrayData.isPrintableChar(0x7F));    // (c)
        assertFalse(TapByteArrayData.isPrintableChar(0x90));
        assertFalse(TapByteArrayData.isPrintableChar(0x0));
    }

    @Test
    public void testIsKeyword() {
        assertTrue(TapByteArrayData.isKeyword(0xA5));    // RND
        assertTrue(TapByteArrayData.isKeyword(0xF5));    // PRINT
        assertTrue(TapByteArrayData.isKeyword(0xFF));    // COPY
        assertFalse(TapByteArrayData.isKeyword(0x0));
        assertFalse(TapByteArrayData.isKeyword(0x90));
        assertFalse(TapByteArrayData.isKeyword(0x101));
        assertFalse(TapByteArrayData.isKeyword('a'));
    }

    @Test
    public void testGetKeyword() {
        assertEquals("RND ", TapByteArrayData.getKeyword(0xA5));    // RND
        assertEquals("PRINT ", TapByteArrayData.getKeyword(0xF5));    // PRINT
        assertEquals("COPY ", TapByteArrayData.getKeyword(0xFF));    // COPY
        assertEquals("GO TO ", TapByteArrayData.getKeyword(0xEC));    // GO TO
        assertNull(TapByteArrayData.getKeyword(0x100));
        assertNull(TapByteArrayData.getKeyword(0xA4));
        assertNull(TapByteArrayData.getKeyword(0x10));
    }

    @Test
    public void testIsControlCharToSetTextAttribute() {
        assertTrue(TapByteArrayData.isControlCharToSetTextAttribute(0x10));
        assertTrue(TapByteArrayData.isControlCharToSetTextAttribute(0x17));
        assertFalse(TapByteArrayData.isControlCharToSetTextAttribute(0x1));
    }

    @Test
    public void testGetControlAsCharText() {
        assertEquals("INK", TapByteArrayData.getControlAsCharText(0x10));
        assertEquals("PAPER", TapByteArrayData.getControlAsCharText(0x11));
        assertEquals("TAB", TapByteArrayData.getControlAsCharText(0x17));
        assertNull(TapByteArrayData.getControlAsCharText(0x100));
        assertNull(TapByteArrayData.getControlAsCharText(0x18));
        assertNull(TapByteArrayData.getControlAsCharText(0x9));
    }

}   // TapByteArrayDataTest.java
