/*
 * TapByteArrayData.java
 *
 *  created: 18.9.2017
 *  charset: UTF-8
 */

package cz.mp.zxs.tools.tap2bas;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;
import java.util.TreeMap;


/**
 * Třída pro práci s polem dat. 
 * Oproti {@linkplain ByteArrayData} obsahuje navíc metody použitelné 
 * pravděpodobně pouze (nebo hlavně) pro analýzu Tap souboru pro ZX Spectrum.
 * <p>
 * <em>Pozor, neprovádí kontroly na přetečení!</em> Např. 
 * v metodách: {@code read*()}, {@code skip()}, {@code back()}!
 * Hlídat ručně metodami {@linkplain #lastWasReaded()}, 
 * {@linkplain #isLast()}, {@linkplain #getIdx()} a {@linkplain #length() }.
 *
 * @author Martin Pokorný
 * @see ByteArrayData
 */
public class TapByteArrayData extends ByteArrayData {

    private static Map<Integer,String> keywords = 
            new TreeMap<Integer,String>();
    private static Map<Integer,String> controlCharNames = 
            new TreeMap<Integer,String>();

    static {
        initZxsKeywordsMap();
        initZxsControlCharNames();
    }

    /** každé číslo je v Basicu v paměti reprezentováno 5B.
     * V Basic programu je těchto 5B uvozeno znakem 0x0E ("number identifier")
     * a po čísle následuje ještě textová reprezentace, která se vypisuje 
     * uživateli. */
    public static final int NUMBER_REPRESENTATION_LEN = 5;
    
    /**
     * 
     * @param data 
     */
    public TapByteArrayData(byte[] data) {
        super(data);    
    }

    /**
     * Načte data zadané délky a vrátí je formátované jako "decadicDump".
     * <p>
     * Hodí se pro případ, když blok obsahuje data jako UDG.
     * 
     * @param length
     * @return 
     */
    public String readBlockReturnAsDecadicDump(int length) {
        StringBuilder sb = new StringBuilder(8192); // 4096
        
        final int LINE_LEN = 8;
        int i = 0;
        sb.append(String.format("%04x: ", i));
        //sb.append("  ");
        
        while(i < length) {
            int num = read();            
            sb.append(String.format("%3d", num));
            
            i++;
            sb.append(",");
            if (i % LINE_LEN == 0) {                
                //sb.append("\n  ");
                sb.append(String.format("\n%04x: ", i));
            }
            else if (i == length) {   // poslední znak
                sb.append("\n  ");                
            }            
        }
        
        return sb.toString();
    }
    
    /**
     * Načte data zadané délky a vrátí je formátované jako "hexDump".
     * <p>
     * př: <pre><tt>
     * 01 00 00 e6 00 02 62 00  00 03 00 00 a8 f3 00 00  ......b.........
     * 00 00 00 69 00 00 01 00  00 6c 00 00 09 00 00 b3  ...i.....l......
     * atd...
     * </tt></pre>
     * 
     * @param length
     * @return 
     */
    public String readBlockReturnAsHexDump(int length) {
        StringBuilder sb = new StringBuilder(4096);
        
        final int LINE_LEN = 16;
        int i = 0;
        sb.append(String.format("%04x: ", i));
        
        while(i < length) {
            sb.append(readAndReturnAsHex());
            i++;
            if (i % LINE_LEN == LINE_LEN/2) {    
                sb.append("  ");          // mezera navíc pro lepší čitelnost 
            }            
            else {
                sb.append(" ");
            }
            if (i % LINE_LEN == 0) {                
                sb.append(" ");
                // jako text vypsat vše, co jako text vypsat lze; začít nový řádek
                
                back(LINE_LEN);
                sb.append(getReadableColumnsForDump(LINE_LEN));
                
                //sb.append("\n");                
                sb.append(String.format("\n%04x: ", i));
            }
            else if (i == length) {   // poslední znak => zapsat ještě poslední řádek
                sb.append(" ");
                int lastLineLen = i % LINE_LEN;
                
                // nahradit prázdnou část v hexa, aby čitelný text byl zarovnán ve sloupci s textem nad ním
                int iToEol = LINE_LEN - lastLineLen;
                if (iToEol >= LINE_LEN/2) {
                    sb.append(" "); // za mezeru navíc pro lepší čitelnost
                }                
                for (int j=0; j<iToEol; j++) {
                    sb.append("   ");
                }
                
                // jako text vypsat vše, co jako text vypsat lze; začít nový řádek
                back(lastLineLen);
                sb.append(getReadableColumnsForDump(lastLineLen));

                sb.append("\n");                
            }            
        }
        
        return sb.toString();
    }
    
    /**
     * Vrací text o délce {@code lineLength}, který obsahuje čitelné znaky.
     * Tento text se vypíše v pravém sloupci, vedle hexadecilálních čísel.
     * <p>
     * Pomocná metoda pro {@linkplain #readBlockReturnAsHexDump(int)}.
     * Před touto metodou volat {@linkplain #back(lineLength)}...
     * <p>
     * př: <pre><tt>
     * b1 00 00 0f 00 00 42 01  00 7a 43 02 00 32 30 44  
     *      -->  ......B..zC..20D  (pro lineLength=16)
     * </tt></pre>
     * 
     * @param lineLength
     * @return 
     * 
     */       
    private String getReadableColumnsForDump(int lineLength) {
        StringBuilder sb = new StringBuilder(lineLength + 1);
        for (int j=0; j<lineLength; j++) {
            int ch = read();
            if(ch >= 0x20 && ch <= 0x7E) {  // 0x7F ne, v ASCII je DEL
                sb.append((char)ch);
            }
            else {
                sb.append(".");    // jiné znaky nahradit za "."
            }          
        }         
        return sb.toString();
    }          

    /**
     * 
     * @param length
     * @return 
     * @throws InvalidTapException
     */
    public String readBlockAndReturnAsEscapedString(int length) 
            throws InvalidTapException {
        if(length < 0) {
            throw new IllegalArgumentException("length");
        }
        StringBuilder sb = new StringBuilder(256);
        int i=0;
        while (i < length) {
            int dataItem = read();
            i++;
            
            // číslo je tam jako text následovaný vnitřní reprezentací čísla
            // 0x0E + 5B. To přeskakovat.
            if (isBeginOfNumberRepresentation(dataItem)) {
                skip(NUMBER_REPRESENTATION_LEN);
                i += NUMBER_REPRESENTATION_LEN;
            }
            // 'libra' (£)  -->  {pound}  (Není v bas2tap)
            else if (isPoundChar(dataItem)) {       
                sb.append("{pound}");
            }          
            // character 7F = copyright sign  -->  {(C)}
            else if (isCopyrightChar(dataItem)) {
                sb.append("{(C)}");
            }     
            else if (isPrintableChar(dataItem)
                    || isEolChar(dataItem)) {
                sb.append((char)dataItem);
            }
            // např. {INVERSE 1} (Není v bas2tap)
            else if (isControlCharToSetTextAttribute(dataItem)) {
                String charName = getControlAsCharText(dataItem);
                if (charName == null) {
                    // (nemělo by nastat)
                    throw new InvalidTapException("charName = null");
                }
                int value = read();
                i++;        
                sb.append("{").append(charName).append(" ")
                        .append(String.valueOf(value));
                if (dataItem == 0x16) {   //  AT má dva parametry: (y,x)
                    int value2 = read();
                    i++;
                    sb.append(" ");
                    sb.append(String.valueOf(value2));
                }
                sb.append("}");
            }
            else if (isControlChar(dataItem)) {
                // nic! tyto znaky prostě přeskakovat
            }
            else if (isKeyword(dataItem)) {
                String keyword = getKeyword(dataItem);
                if (keyword == null) {
                    // (nemělo by nastat)
                    throw new InvalidTapException("keyword = null");
                }
                sb.append(keyword);
            }            
            // {-X} X is 1-8, characters 80-87 (block graphics without shift)
            // {+X} X is 1-8, characters 88-8F (block graphics with shift)            
            else if (isMosaicGraphicChar(dataItem)) {
                if (dataItem >= 0x80 && dataItem <= 0x87) {
                    sb.append("{-");
                    sb.append((char)('1'+dataItem-0x80));  // 1-8, ne 0-7 !
                    sb.append("}");                        
                }
                if (dataItem >= 0x88 && dataItem <= 0x8F) {
                    sb.append("{+");
                    sb.append((char)('1'+dataItem-0x88));  // 1-8, ne 0-7 !
                    sb.append("}");                        
                }
            }            
            // {X} X is 'A'-'U', converts to the UDG Spectrum ASCII value
            else if (isUdgChar(dataItem)) {
                sb.append("{").append((char)('A'+dataItem-0x90)).append("}");
            }
            else {
                // (nemělo by nastat)
                throw new InvalidTapException("unknown data " + dataItem);
            }
        }
        
        return sb.toString();
    }

    /**
     * 
     * @return 
     */
    public BigDecimal readBlockAndParseBasicNumber() {
        return (parseBasicNumber(
                readBlockAndReturnAsInts(NUMBER_REPRESENTATION_LEN)));
    }
            
    /**
     * Parsuje 5B číslo ve vnitřní reprezentaci Basicu ZX Spectra.
     * 
     * @param rawValue  5B číslo ve vnitřní reprezentaci Basicu ZX Spectra
     * @return  číslo v javě
     */
    protected static BigDecimal parseBasicNumber(int[] rawValue) {
        if (rawValue.length != NUMBER_REPRESENTATION_LEN) {
            throw new IllegalArgumentException("rawValue.length != " + NUMBER_REPRESENTATION_LEN);
        }
        
//        for (int i : rawValue) {
//            System.out.print(Integer.toHexString(i) + " ");
//        }
//        System.out.println();
        
        // --- integer
        if (rawValue[0] == 0) {
            // reprezentováno:   0x0  0x0|0xFF  LsB  MsB  0x0   
            //   1.B = 0x0 --> integer
            //   2.B = 0x0|0xFF -- 0xFF -> záporné číslo
            // pro hodnoty  -65535 až +65535
            int intResult = rawValue[2];
            intResult += 256 * rawValue[3];
            if (rawValue[1] == 0xFF) {
                intResult = -intResult;
            }
            else if (rawValue[1] != 0x0) {
                throw new IllegalArgumentException("number is not a valid integer");
            }
            if (rawValue[4] != 0x0) {
                throw new IllegalArgumentException("number is not a valid integer");
            }
            return new BigDecimal(intResult);
        }
        // --- float
        else {
            // reprezentováno:  
            //   1.B = exponent + 0x80(=128)  (hodnota -127 až 126)
            //   4 B = mantisa -- "binární zlomek"  (hodnota 0.5 až 1)
            //         první bit je znaménko.
            //         mantisa je automaticky = 0.5 (1 * 2^-1)
            // hodnota = mantisa * 2^exponent
            // např.
            //   01111110 00000000 00000000 00000000 00000000
            //     mantisa = 0.5
            //     exp = 126-128 = -2
            //   = 0.5 * 2^(-2) =  0.125
            // např.
            //   10000000 01001100 00000000 00000000 00000000   (01001100 = 76)
            //     mantisa = 0.5 + (1 * 2^-2) + (1 * 2^-5) + (1 * 2^-6) =
            //             = 0.5 + 0.25 + 0.03125 + 0.015625 =  0,796875
            //     exp = 128-128 = 0
            //   = 0.796925 * 2^(0) =  0.796875
            
            int exp = rawValue[0] - 128;
//            System.out.println("  exp=" + exp);
            if (exp <= -128 || exp >= 127) {
                throw new IllegalArgumentException("number is not a valid float");
            }  
            int sign = rawValue[1] >> 7;
//            System.out.println("  sign=" + sign);
            
            BigDecimal mantisa = new BigDecimal("0.5");
            mantisa = mantisa.add(new BigDecimal(rawValue[1] & 0x7F).divide(new BigDecimal("256"), MathContext.DECIMAL64));
            mantisa = mantisa.add(new BigDecimal(rawValue[2]).divide(new BigDecimal("65536"), MathContext.DECIMAL64));
            mantisa = mantisa.add(new BigDecimal(rawValue[3]).divide(new BigDecimal("16777216"), MathContext.DECIMAL64));
            mantisa = mantisa.add(new BigDecimal(rawValue[4]).divide(new BigDecimal("4294967296"), MathContext.DECIMAL64));
            BigDecimal result = mantisa;
            
//            double mantisa = 0.5d;
//            mantisa += (rawValue[1] & 0x7F) / 256d;
//            mantisa += rawValue[2] / 65536d;           // = 0xFFFF = 256^2
//            mantisa += rawValue[3] / 16777216d;        // = 0xFFFFFF = 256^3
//            mantisa += rawValue[4] / 4294967296d;      // = 0xFFFFFFFF = 256^4
////            System.out.println("  mantisa=" + mantisa);
//            BigDecimal result = new BigDecimal(mantisa);
            
            // (pozn. 0x7f 0x7f 0xff 0xff 0xff není přesně 0.5!   0.5 = 0x80 0x0 0x0 0x0 0x0!)
            
            result = result.multiply(new BigDecimal("2").pow(exp, MathContext.DECIMAL64));
            if (sign != 0) {
                result = result.negate();
            }
            
            return result;
        }
    }

    
    /**
     * 
     * @param name
     * @return 
     */
    public static final boolean isValidVariableName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        char firstChar = name.charAt(0);
        if (! Character.isAlphabetic(firstChar)) {
            return false;
        }
        for (int i=1; i<name.length(); i++) {
            if (! Character.isAlphabetic(name.charAt(i)) && 
                    ! Character.isDigit(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     *
     * @param data
     * @return
     */
    public static final boolean isEolChar(int data) {
        return data == 0x0D;
    }

    /**
     * Řídící znak pro nastavení barvy a pozice přímo v BASICu.
     * <p>
     * Znaky: {@code 0x10 INK, 0x11 PAPER, 0x12 FLASH, 0x13 BRIGHT,
     * 0x14 INVERSE, 0x15 OVER, 
     * 0x16 AT, 0x17 TAB}
     * 
     * @param data  
     * @return
     * @see #getControlAsCharText(int) 
     */
    public static final boolean isControlCharToSetTextAttribute(int data) {
        return data >= 0x10 && data <= 0x17;
    }
    
    /**
     * 
     * @param data  
     * @return  text nebo {@code null}
     * @see #isControlCharToSetTextAttribute(int) 
     */
    public static String getControlAsCharText(int data) {
        return controlCharNames.get(data);
    }
    
    /**
     * Řídící znak.
     * <p>
     * Pozn: {@code 0x10 INK, 0x11 PAPER, 0x12 FLASH, 0x13 BRIGHT,
     * 0x14 INVERSE, 0x15 OVER, 0x16 AT, 0x17 TAB}
     * <br> a také:
     * {@code 0x08 LEFT, 0x09 RIGTH, 0x0A DOWN, 0x0B UP, Ox0C DELETE,
     * 0x0D ENTER}
     * <br> a také:
     * {@code 0x0E - začátek čísla}
     * 
     * @param data
     * @return 
     * @see #isControlCharToSetTextAttribute(int) 
     * @see #isPrintableControlChar(int) 
     * @see #isBeginOfNumberRepresentation(int) 
     */
    public static final boolean isControlChar(int data) {
        return data >= 0 && data <= 0x1F;
    }
    
    /**
     * Znak je řídící znak pro začátek čísla ve vnitřní reprezentaci basicu 
     * (0x0E + 5B).
     * 
     * @param data
     * @return 
     */
    public static final boolean isBeginOfNumberRepresentation(int data) {
        return data == 0x0E;
    }
    
    /**
     * Běžný tisknutelný znak jako písmeno,číslice,znaménko.
     * 
     * @param data
     * @return 
     */
    public static final boolean isPrintableChar(int data) {
        return data >= 0x20 && data <= 0x7F;
    }

    /**
     * Znak 'copyright' (c). 
     * V US-ASCII je 'DEL'.
     * 
     * @param data
     * @return 
     */
    public static final boolean isCopyrightChar(int data) {
        return data == 0x7F;
    }
    
    /**
     * Znak 'libra' (£). 
     * V US-ASCII je znak "backtick" '`'.
     * 
     * @param data
     * @return 
     */
    public static final  boolean isPoundChar(int data) {
        return data == 0x60;
    }
    
    /**
     * znak "čtverečky".
     * 
     * @param data
     * @return 
     */
    public static final boolean isMosaicGraphicChar(int data) {
        return data >= 0x80 && data <= 0x8F;
    }
    
    /**
     * uživatelem definovaný znak -- UDG.
     * 
     * @param data
     * @return 
     */
    public static final boolean isUdgChar(int data) {
        return data >= 0x90 && data <= 0xA4;
    }

    /**
     * klíčové slovo basicu.
     * 
     * @param data
     * @return 
     */
    public static final boolean isKeyword(int data) {
        return data >= 0xA5 && data <= 0xFF;
    }    

    /**
     * 
     * @param data
     * @return text nebo {@code null}
     */
    public static final String getKeyword(int data) {
        return keywords.get(data);
    }

    /**
     */
    private static void initZxsControlCharNames() {
        controlCharNames.put(0x10, "INK");
        controlCharNames.put(0x11, "PAPER");
        controlCharNames.put(0x12, "FLASH");
        controlCharNames.put(0x13, "BRIGHT");
        controlCharNames.put(0x14, "INVERSE");
        controlCharNames.put(0x15, "OVER");
        controlCharNames.put(0x16, "AT");
        controlCharNames.put(0x17, "TAB");        
    }

    // (pozor v tokenech obsahující mezeru (např. v GO TO apod) nesmí být nezalomitelné mezery)
    /**
     */
    private static final void initZxsKeywordsMap() {
        keywords.put(0xA5,"RND ");
        keywords.put(0xA6,"INKEY$ ");
        keywords.put(0xA7,"PI ");
        keywords.put(0xA8,"FN ");
        keywords.put(0xA9,"POINT ");
        keywords.put(0xAA,"SCREEN$ ");
        keywords.put(0xAB,"ATTR ");
        keywords.put(0xAC,"AT ");
        keywords.put(0xAD,"TAB ");
        keywords.put(0xAE,"VAL$	");
        keywords.put(0xAF,"CODE ");
        keywords.put(0xB0,"VAL ");
        keywords.put(0xB1,"LEN ");
        keywords.put(0xB2,"SIN ");
        keywords.put(0xB3,"COS ");
        keywords.put(0xB4,"TAN ");
        keywords.put(0xB5,"ASN ");
        keywords.put(0xB6,"ACS ");
        keywords.put(0xB7,"ATN ");
        keywords.put(0xB8,"LN ");
        keywords.put(0xB9,"EXP ");
        keywords.put(0xBA,"INT ");
        keywords.put(0xBB,"SQR ");
        keywords.put(0xBC,"SGN ");
        keywords.put(0xBD,"ABS ");
        keywords.put(0xBE,"PEEK ");
        keywords.put(0xBF,"IN ");
        keywords.put(0xC0,"USR ");
        keywords.put(0xC1,"STR$ ");
        keywords.put(0xC2,"CHR$ ");
        keywords.put(0xC3," NOT ");
        keywords.put(0xC4,"BIN ");
        keywords.put(0xC5," OR ");
        keywords.put(0xC6," AND ");
        keywords.put(0xC7,"<=");
        keywords.put(0xC8,">=");
        keywords.put(0xC9,"<>");
        keywords.put(0xCA,"LINE ");
        keywords.put(0xCB," THEN ");
        keywords.put(0xCC," TO ");
        keywords.put(0xCD," STEP ");
        keywords.put(0xCE,"DEF FN ");
        keywords.put(0xCF,"CAT ");
        keywords.put(0xD0,"FORMAT ");
        keywords.put(0xD1,"MOVE ");
        keywords.put(0xD2,"ERASE ");
        keywords.put(0xD3,"OPEN# ");
        keywords.put(0xD4,"CLOSE# ");
        keywords.put(0xD5,"MERGE ");
        keywords.put(0xD6,"VERIFY ");
        keywords.put(0xD7,"BEEP ");
        keywords.put(0xD8,"CIRCLE ");
        keywords.put(0xD9,"INK ");
        keywords.put(0xDA,"PAPER ");
        keywords.put(0xDB,"FLASH ");
        keywords.put(0xDC,"BRIGHT ");
        keywords.put(0xDD,"INVERSE ");
        keywords.put(0xDE,"OVER ");
        keywords.put(0xDF,"OUT ");
        keywords.put(0xE0,"LPRINT ");
        keywords.put(0xE1,"LLIST ");
        keywords.put(0xE2,"STOP ");
        keywords.put(0xE3,"READ ");
        keywords.put(0xE4,"DATA ");
        keywords.put(0xE5,"RESTORE ");
        keywords.put(0xE6,"NEW ");
        keywords.put(0xE7,"BORDER ");
        keywords.put(0xE8,"CONTINUE ");
        keywords.put(0xE9,"DIM ");
        keywords.put(0xEA,"REM ");
        keywords.put(0xEB,"FOR ");
        keywords.put(0xEC,"GO TO ");
        keywords.put(0xED,"GO SUB ");
        keywords.put(0xEE,"INPUT ");
        keywords.put(0xEF,"LOAD ");
        keywords.put(0xF0,"LIST ");
        keywords.put(0xF1,"LET ");
        keywords.put(0xF2,"PAUSE ");
        keywords.put(0xF3,"NEXT ");
        keywords.put(0xF4,"POKE ");
        keywords.put(0xF5,"PRINT ");
        keywords.put(0xF6,"PLOT ");
        keywords.put(0xF7,"RUN ");
        keywords.put(0xF8,"SAVE ");
        keywords.put(0xF9,"RANDOMIZE ");
        keywords.put(0xFA,"IF ");
        keywords.put(0xFB,"CLS ");
        keywords.put(0xFC,"DRAW ");
        keywords.put(0xFD,"CLEAR ");
        keywords.put(0xFE,"RETURN ");
        keywords.put(0xFF,"COPY ");
    }
    
    
}   // TapByteArrayData.java
