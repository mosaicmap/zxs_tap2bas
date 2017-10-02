/*
 * ByteArrayDataTest.java
 *
 *  created: 28.9.2017
 *  charset: UTF-8
 */

package cz.mp.zxs.tools.tap2bas;

import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author Martin Pokorný
 */
public class ByteArrayDataTest {


    @Test
    public void testLength01() {
        byte[] rawData = new byte[] {(byte) 0x41, (byte) 0x68, (byte) 0x6F, (byte) 0x6A}; // Ahoj
        ByteArrayData baData = new ByteArrayData(rawData);        
        assertEquals(rawData.length, baData.length());
    }

    @Test
    public void testIsEmpty01() {
        ByteArrayData baData;
        
        baData = new ByteArrayData(new byte[] {});         
        assertTrue(baData.isEmpty());
        
        byte[] rawData = new byte[] {(byte) 0x41, (byte) 0x68, (byte) 0x6F, (byte) 0x6A}; // Ahoj
        baData = new ByteArrayData(rawData);
        assertFalse(baData.isEmpty());
    }

    @Test
    public void testChangeIdxMethods01() {
        byte[] rawData = new byte[] {(byte) 0x41, (byte) 0x68, (byte) 0x6F, (byte) 0x6A}; // Ahoj
        ByteArrayData baData = new ByteArrayData(rawData);        

        assertEquals(0, baData.getIdx());
        baData.begin();
        assertEquals(0, baData.getIdx());
        
        baData.skip(1);
        assertEquals(1, baData.getIdx());
        baData.begin();
        assertEquals(0, baData.getIdx());

        baData.skip(2);
        baData.back(1);
        assertEquals(1, baData.getIdx());
        
        baData.begin();
        baData.skip(5);     // nezpůsobí výjimku (!) (to by způsobilo až read()
        assertEquals(5, baData.getIdx());   
    }

    @Test
    public void testRead01() {
        byte[] rawData = new byte[] {(byte) 0x41, (byte) 0x68, (byte) 0x6F, (byte) 0x6A}; // Ahoj
        ByteArrayData baData = new ByteArrayData(rawData);

        assertEquals(0x41, baData.read());
        assertEquals(0x68, baData.read());
        baData.back(1);
        assertEquals(0x68, baData.read());
        assertEquals(0x6F, baData.read());
        assertEquals(0x6A, baData.read());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRead02Err() {
        byte[] rawData = new byte[] {(byte) 0x41, (byte) 0x68, (byte) 0x6F, (byte) 0x6A}; // Ahoj
        ByteArrayData baData = new ByteArrayData(rawData);

        for(int i=0; i<rawData.length; i++) {
            assertEquals(rawData[i], (byte) baData.read());
        }
        baData.read();  // --> IndexOutOfBoundsException
    }
    
    @Test
    public void testReadAndReturnAsHex01() {
        byte[] rawData = new byte[] {(byte) 0x41, (byte) 0x68, (byte) 0x6F, (byte) 0x6A}; // Ahoj
        ByteArrayData baData = new ByteArrayData(rawData);

        assertEquals("41", baData.readAndReturnAsHex());
        assertEquals("68", baData.readAndReturnAsHex());
        assertEquals("6f", baData.readAndReturnAsHex());
        assertEquals("6a", baData.readAndReturnAsHex());
    }

    @Test
    public void testReadLsbMSB01() {
        byte[] rawData = new byte[] {(byte) 0x0, (byte) 0xf0, (byte) 0x03, (byte) 0x0};
        ByteArrayData baData = new ByteArrayData(rawData);
        baData.skip(1);
        // 240 + 3*256 = 240 + 768 = 1008
        assertEquals(1008, baData.readLsbMSB());
    }

    @Test
    public void testReadMSBLsb01() {
        byte[] rawData = new byte[] {(byte) 0x0, (byte) 0x03, (byte) 0xf0, (byte) 0x0};
        ByteArrayData baData = new ByteArrayData(rawData);
        baData.skip(1);
        // 240 + 3*256 = 240 + 768 = 1008
        assertEquals(1008, baData.readMSBlsb());
    }

    @Test
    public void testLastWasReaded01() {
        byte[] rawData = new byte[] {(byte) 0x41, (byte) 0x68, (byte) 0x6F, (byte) 0x6A }; // Ahoj
        ByteArrayData baData = new ByteArrayData(rawData);
        assertFalse(baData.lastWasReaded());
        while(! baData.lastWasReaded()) {
            baData.read();
        }
        assertTrue(baData.lastWasReaded());        
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testLastWasReaded02ErrRead() {
        byte[] rawData = new byte[] {(byte) 0x41, (byte) 0x68, (byte) 0x6F, (byte) 0x6A }; // Ahoj
        ByteArrayData baData = new ByteArrayData(rawData);
        while(! baData.lastWasReaded()) {
            baData.read();
        }
        baData.read();
    }
    
    @Test
    public void testIsLast01() {
        byte[] rawData = new byte[] {(byte) 0x41, (byte) 0x68, (byte) 0x6F, (byte) 0x6A }; // Ahoj
        ByteArrayData baData = new ByteArrayData(rawData);
        assertFalse(baData.isLast());
        while(! baData.isLast()) {
            assertFalse(baData.isLast());
            baData.read();
        }
        assertTrue(baData.isLast());
        baData.read();
        assertTrue(baData.lastWasReaded());        
    }
    
    @Test
    public void testReadBlock01() {
        byte[] rawData = new byte[] {(byte) 0x41, (byte) 0x68, (byte) 0x6F, (byte) 0x6A }; // Ahoj
        ByteArrayData tapContent = new ByteArrayData(rawData);
        assertEquals(rawData.length, tapContent.length());
        assertArrayEquals(rawData, tapContent.readBlock(rawData.length));
    }

    @Test
    public void testReadBlockAndReturnAsString01() {
        byte[] rawData = new byte[] {(byte) 0x41, (byte) 0x68, (byte) 0x6F, (byte) 0x6A }; // Ahoj
        ByteArrayData tapContent = new ByteArrayData(rawData);
        assertEquals(rawData.length, tapContent.length());
        assertEquals("Ahoj", tapContent.readBlockAndReturnAsString(rawData.length));
    }
    
    @Test
    public void testReadBlockAndReturnAsInts01() {
        byte[] rawData = new byte[] {(byte) 0x41, (byte) 0x68, (byte) 0x6F, (byte) 0x6A }; // Ahoj
        ByteArrayData tapContent = new ByteArrayData(rawData);
        assertEquals(rawData.length, tapContent.length());
        int[] expected = new int[] {0x41, 0x68, 0x6F, 0x6A};
        assertArrayEquals(expected, tapContent.readBlockAndReturnAsInts(rawData.length));
    }
    
}   // ByteArrayDataTest