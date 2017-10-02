/*
 * ByteArrayData.java
 *
 *  charset: UTF-8
 */

package cz.mp.zxs.tools.tap2bas;

/**
 * Třída pro práci s polem dat.
 * <p>
 * <em>Pozor, neprovádí kontroly na přetečení!</em> Např. 
 * v metodách: {@code read*()}, {@code skip()}, {@code back()}!
 * Hlídat ručně metodami {@linkplain #lastWasReaded()}, 
 * {@linkplain #isLast()}, {@linkplain #getIdx()} a {@linkplain #length()}.
 * 
 * @author Martin Pokorný
 * @see ZxsByteArrayData
 */
public class ByteArrayData {
    
    protected byte[] data;
    protected int idx = 0;

    /**
     *
     * @param data
     */
    public ByteArrayData(byte[] data) {
        // kopie dat:
        this.data = new byte[data.length];
        System.arraycopy(data, 0, this.data, 0, data.length);
    }

    public void begin() {
        this.idx = 0;
    }
    
    public int getIdx() {
        return idx;
    }
    
    public void back(int i) {
        idx-=i;
    }
    
    public void skip(int i) {
        idx+=i;
    }
    
//    public void gotoIdx(int i) {
//        idx = i;
//    }
    
    public int read() {
        return data[idx++] & 0xFF;
    }
    
    public int readLsbMSB() {
        return read() + read() * 256;
    }
    
    public int readMSBlsb() {
        return read() * 256 + read();
    }
    
    /**
     * 
     * @return  "00" - "ff" (používá malá písmena)
     */
    public String readAndReturnAsHex() {
        int val = read();
        if (val <= 15) {
            return "0" + Integer.toHexString(val);      // (protože toHexString ořezává nuly (0F -> F))
        }
        else {
            return Integer.toHexString(val);
        }
    }
    
    public byte[] readBlock(int length) {
        byte[] result = new byte[length];
        System.arraycopy(data, idx, result, 0, length);
        idx += length;
        return result;
    }
    
    public String readBlockAndReturnAsString(int length) {
        return new String(readBlock(length));
    }
    
    public int[] readBlockAndReturnAsInts(int length) {
        int[] result = new int[length];
        for (int i=0; i<length; i++) {
            result[i] = read();
        }
        return result;        
    }
    
    public boolean lastWasReaded() {
        return idx >= data.length;
    }
    
    public boolean isLast() {
        return idx == data.length-1;
    }
    
    public int length() {
        return data.length;
    }
    
    public boolean isEmpty() {
        return data.length == 0;
    }

}   // ByteArrayData.java
