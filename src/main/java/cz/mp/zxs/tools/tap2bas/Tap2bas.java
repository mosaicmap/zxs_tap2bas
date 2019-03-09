/*
 * Tap2bas.java
 *
 *  created: 25.4.2017
 *  charset: UTF-8
 */

package cz.mp.zxs.tools.tap2bas;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analyzuje TAP soubor a část s BASICem pro ZX Spectrum převede do čitelné formy.
 * Převod TAP souboru pro jiné systémy/platformy/značky počítačů  nebude fungovat.
 * <p>
 * Viz popis TAP souboru, např:<ul>
 * <li><a href="https://faqwiki.zxnet.co.uk/wiki/TAP_format">TAP format na ZXS FAQ Wiki</a>,</li>
 * <li><a href="http://faqwiki.zxnet.co.uk/wiki/Spectrum_tape_interface">tape format na ZXS FAQ Wiki</a></li>
 * <li><a href="http://www.zx-modules.de/fileformats/tapformat.html">TAP format</a></li>
 * </ul><p>
 * Viz též knihy: <ul>
 * <li><i><b>ZX ROM II - poznámky [CZ] -- Daniel Jenne</b></i> (kapitoly 2, 3, 4)</li>
 * <li><i>Understanding Your Spectrum [EN] -- Ian Logan</i> (strany 14, 17, 32)</li>
 * <li><i>Rutiny ROM ZX Spectrum [CZ] -- Jan Šritter, Marcel Dauth</i> (tabulka 6 na str. 21)</li>
 * </ul>
 *
 *
 * @author Martin Pokorný
 * @see InvalidTapException
 * @see ByteArrayData
 */
public class Tap2bas {
    private static final Logger log = LoggerFactory.getLogger(Tap2bas.class);
    
    protected TapByteArrayData tapContent;
    protected Writer fout;
    
    protected String fileNameInHeader;
    
    protected static final int NAME_IN_HEADER_LEN = 10;
    protected static final int DEFAULT_HEADER_SIZE = 19;
    protected static final int MIN_TAP_SIZE = 2 + DEFAULT_HEADER_SIZE + 5;  // (2B za blocklen na začátku) (5 = 2B za blocklen na začátku dalšího bloku + 1B flag + 1B data + 1B parita) 
    
    public static final int FLAG_HEADER = 0;
    public static final int FLAG_DATA = 0xFF;

    public static final int MIN_LINE_NUM_IN_BASIC = 0;  // (existují způsoby jak lze zapsat řádek 0; jinak běžně je min řádek 1)
    public static final int MAX_LINE_NUM_IN_BASIC = 9999;
    
    public static final int MAX_LINE_LENGTH = 1024;
    
    /** Jen bitmapa (bez barevných atributů) */
//    private static final int SCREEN_BITMAP_LENGTH = 6144;   // 24 * 32 * 8
    /** Jen atributy (barvy) (bez bitmapy) . */
//    private static final int SCREEN_ATTRIB_LENGTH = 768;   // 24 * 32
    /** Velikost obrazovky v bytech */
    public static final int SCREEN_LENGTH = 6912;   // = SCREEN_BITMAP_LENGTH + SCREEN_ATTRIB_LENGTH
    public static final int SCREEN_WIDTH = 256;     // px ... = 32 * 8
    public static final int SCREEN_HEIGHT = 192;    // px     = 24 * 8
    
    private static final int MAX_VARS_DATA_LEGTH = 49152; // 48*1024 = 48kiB = celá RAM ZX Spectra
    // (? je toto opravdu omezeno, nebo jsem si to dříve vymyslel ? Aneb příště psát komentář ...)
    private static final int MAX_VARS_ARRAY_DIM = 4;

    
    /** */
    public Tap2bas() {
    }


    /**
     * Provede analýzu TAP, do výstupu zapíše pouze výpisy BASIC programů.
     * 
     * @throws IOException
     * @throws InvalidTapException 
     * @throws IllegalStateException
     * @see #setInFile(java.io.File) 
     * @see #setOutWriter(java.io.Writer) 
     */
    public void analyzeAndExtractOnlyBasic()
            throws IOException, InvalidTapException {
        boolean onlyBasic = true;
        boolean analyzeVars = false;
        processTap(onlyBasic, analyzeVars);      
    }
    
    /**
     * Provede analýzu TAP, do výstupu vypíše všechny bloky TAP.
     * BASIC bloky převede na výpis programu v Basicu ZXS,
     * pokud je v některém BASIC bloku tabulka proměných, vypíše ji jako
     * {@code hexdump} a pokusí ji analyzovat. Ostatní typu bloků vypíše
     * jako {@code hexdump} a {@code 'decadic' dump}.
     * Ke každému bloku vypíše také hlavičku.
     * 
     * @return
     * @throws IOException 
     * @throws InvalidTapException
     * @throws IllegalStateException
     * @see #setInFile(java.io.File) 
     * @see #setOutWriter(java.io.Writer) 
     */
    public void analyzeAll()
            throws IOException, InvalidTapException {
        boolean onlyBasic = false;
        boolean analyzeVars = true;
        processTap(onlyBasic, analyzeVars);
    }
    
    /**
     * Provede analýzu TAP, do výstupu vypíše všechny bloky TAP.
     * BASIC bloky převede na výpis programu v Basicu ZXS,
     * pokud je v některém BASIC bloku tabulka proměných, vypíše ji pouze jako
     * {@code hexdump}. Ostatní typu bloků vypíše
     * jako {@code hexdump} a {@code 'decadic' dump}.
     * Ke každému bloku vypíše také hlavičku.
     * 
     * @return
     * @throws IOException 
     * @throws InvalidTapException
     * @throws IllegalStateException
     * @see #setInFile(java.io.File) 
     * @see #setOutWriter(java.io.Writer) 
     */
    public void analyzeWithoutVars()
            throws IOException, InvalidTapException {
        boolean onlyBasic = false;
        boolean analyzeVars = false;
        processTap(onlyBasic, analyzeVars);
    }    
    
    /**
     * 
     * @param onlyBasic  pokud je {@code true}, zapíše se na výstup jen výpis
     *      BASIC programů. Pro {@code false} se na výstup zapíše calá analýza.
     * @param analyzeVars  
     * 
     * @throws IOException
     * @throws InvalidTapException 
     * @throws IllegalStateException
     * @see #setInFile(java.io.File) 
     * @see #setOutWriter(java.io.Writer) 
     * @see #analyzeBasicBlock(int, boolean, boolean) 
     */
    private void processTap(boolean onlyBasic, boolean analyzeVars) 
            throws IOException, InvalidTapException {
        if (tapContent == null || tapContent.isEmpty()) {
            throw new IllegalStateException("tapContent is blank");
        }

        tapContent.begin();
        while (! tapContent.lastWasReaded()) {
            int blockLen = tapContent.readLsbMSB();
            //log.debug("blockLen = " + blockLen);
            if (blockLen != DEFAULT_HEADER_SIZE) {
                // (chyba mi většinou vznikala pokud se do analýzy VARS 
                //  začlenila i následují data. Teď se při chybě VARS 
                //  posunuje index v tapConten na správné místo)
                log.warn("Invalid header size. blockLen = " + blockLen);
                if (!onlyBasic) {
                    writeToOut("\nERROR: Invalid header size. blockLen = " + blockLen);
                }
                throw new InvalidTapException("Invalid header size. blockLen = " + blockLen);
            }
                    
            TapBlockType typeFromHeader;
            int flag = tapContent.read();            
            if (flag == FLAG_HEADER) {

                int typeNum = tapContent.read();
                typeFromHeader = TapBlockType.getByNum(typeNum);
                log.debug("--- block type = " + typeFromHeader.description);
                if (typeFromHeader == null) {
                    throw new InvalidTapException(
                            "unknown type: 0x" + Integer.toHexString(typeNum));
                }

                // zbylý obsah hlavičky:
                //  10B jméno, 6B header info, 1B parity
                if (onlyBasic) {
                    tapContent.skip(17);
                }
                else {
                    String headerInfo = readHeaderDataAndFormat(typeFromHeader);
                    //logDebug(headerInfo);
                    writeToOut("\n=== ");
                    writeToOut(headerInfo + "\n");
                    log.info("=== " + headerInfo);
                }

                // ---- Data následující po hlavičce:

                int dataBlockLen = tapContent.readLsbMSB();
                log.info("dataBlockLen = " + dataBlockLen);
                
                flag = tapContent.read();
                if (flag != FLAG_DATA) {
                    throw new InvalidTapException(
                            "wrong flag: 0x" + Integer.toHexString(flag));
                }

                if (typeFromHeader == TapBlockType.BASIC) {
                    analyzeBasicBlock(dataBlockLen - 1, onlyBasic, analyzeVars);  // 1B za již načtený flag
                }
                else if (onlyBasic) {
                    tapContent.skip(dataBlockLen - 1);
                }
                else {      // (např. Code or SCREEN$)
                    String hexDump = tapContent.readBlockReturnAsHexDump(dataBlockLen - 2); // -1 za flag a -1 za paritu na konci
                    writeToOut(hexDump);

                    //writeToOut("\n"); // (ne \n, to už zapíše readBlockReturnAsHexDump)
                    writeToOut("--- same data -- \"decimal\" dump: \n");
                    tapContent.back(dataBlockLen - 2);
                    String decDump = tapContent.readBlockReturnAsDecadicDump(dataBlockLen - 2);
                    writeToOut(decDump);

                    tapContent.skip(1);     // parita ("checksum")
                            
                    writeToOut("\n");                    
                    if (fout != null) {
                        fout.flush();
                    }                    
                }
            }
            else {
                throw new InvalidTapException(
                        "wrong flag: 0x" + Integer.toHexString(flag));
            }
        }            
    }
    
    // (typ se předává jako parametr, protože je potřeba i dále, po volání této metody)
    /**
     * Čte část hlavičky bez čísla typu, tj:
     * 10 B jméno, 2 B délka dat, 2 B param 1, 2 B param 2, 1 B parity.
     * <p>
     * např.: <pre><tt>BASIC program  name="myprog"  dataLength=17808  p1=9005  p2=15931
     * </tt></pre>
     * 
     * @param typeFromHeader  předtím načtený typ
     * @return  hlavičku jako formátovaný text (viz kód)
     * @throws IllegalArgumentException
     */
    private String readHeaderDataAndFormat(TapBlockType typeFromHeader) {
        if (typeFromHeader == null) {
            throw new IllegalArgumentException("typeFromHeader=null");
        }
        String name = tapContent
                .readBlockAndReturnAsString(NAME_IN_HEADER_LEN).trim();                

        // zbylý obsah hlavičky: (6B header info, 1B checksum)
        //tapContent.skip(blockLen - 2 - NAME_IN_HEADER_LEN);  // 1B za flag, 1B za typ, 10B jméno                
        int dataLen = tapContent.readLsbMSB();

        String param1text = "";
        if (typeFromHeader == TapBlockType.NUMBERS || typeFromHeader == TapBlockType.TEXTS) {
            // pole čísel | pole znaků:   parameter 1 =  "0 , jméno_proměnné"
            int B1 = tapContent.read();
            int B2 = tapContent.read();
            StringBuilder sb = new StringBuilder();
            sb.append(B1);
            if (TapByteArrayData.isPrintableChar(B1)) {
                sb.append("(").append(String.valueOf((char) B1)).append(")");
            }
            sb.append(" ");
            sb.append(B2);
            if (TapByteArrayData.isPrintableChar(B2)) {
                sb.append("(").append(String.valueOf((char) B2)).append(")");
            }
            param1text = sb.toString();
        }
        else {        
            // BASIC: parameter 1 = 1 - 9999  -- řádek pro autostart 
            //        parameter 1 >= 32768  -- řádek pro autostart není definován
            //        parameter 1 = 0 | 10000 - 32767  -- ??
            // Code:  parameter 1 = * (>16kB)  -- adresa v paměti kam se má zapsat kód
            param1text = String.valueOf(tapContent.readLsbMSB());
        }

        // BASIC: parameter 2 = *  -- adresa začátku oblasti proměnných
        // pole čísel | pole znaků:  parameter 2 = X -- nedefinováno
        // Code:  parameter 2 = 32768  -- jen identifikace, že blok je kód
        int param2 = tapContent.readLsbMSB();

        // parita (checksum). (Počítá se jako XOR postupně přes všechny byty)
        // ("bitwise XOR of all bytes including the flag byte")
        tapContent.read();
        //String parity = tapContent.readAndReturnAsHex();

        StringBuilder sbHeader = new StringBuilder();
        sbHeader.append(typeFromHeader.getDescription());
        sbHeader.append("  name=\"").append(name).append("\"");
        sbHeader.append("  dataLength=").append(dataLen);
        sbHeader.append("  p1=").append(param1text);
        sbHeader.append("  p2=").append(param2);
        //sbHeader.append("  parity=0x").append(parity);

        return sbHeader.toString();
    }
    
    /**
     * Analyzuje blok dat v hlavičce označený jako program v BASICu.
     * 
     * @param dataLen
     * @param onlyBasic  pokud je {@code true}, zapíše se na výstup jen výpis
     *      BASIC programů. Pro {@code false} se na výstup zapíše i 
     *      tabulka proměnných, pokud ji BASIC blok obsahuje.
     * @param analyzeVars
     * @throws IOException
     * @throws InvalidTapException
     * @see #processTap(boolean, boolean) 
     */
    private void analyzeBasicBlock(int dataLen, boolean onlyBasic, boolean analyzeVars) 
            throws IOException, InvalidTapException {
        log.debug("dataLen = " + dataLen);
        
        int dataLenWoParity = dataLen - 1;
        int startIdx = tapContent.getIdx();
        // (tapContent.getIdx() - startIdx  = počet načtených bytů)
        while (tapContent.getIdx() - startIdx < dataLenWoParity) {            // dataLen-1 ... 1B za "checksum" na konci, který (zatím) nechci zpracovat, viz konec while...
            // jedna řádka v Basicu:
            
            // --- číslo řádky a zbývající délka řádky, kterou je třeba analyzovat
            int lineNum = tapContent.readMSBlsb();
            //log.debug("lineNum = " + lineNum + "   0x" + Integer.toHexString(lineNum));
            
            // Řádka 0 se někdy vyskytuje. 
            //  (Často jde o nesmazatelný (běžným způsobem) komentář s Copyright info)

            // 0 - 9999 (=0x270F)  -->  PROG -- BASIC program line number
            // 0x41xx - 0xFAxx  -->  VARS -- tabulka proměnných - definice typu, hodnoty atd.
            //  (0x4100 = 16640; 0xFAFF = 64255)
            
            if (lineNum < MIN_LINE_NUM_IN_BASIC) {
                throw new InvalidTapException("illegal begin of line (lineNum < " + MIN_LINE_NUM_IN_BASIC + ")");
            }
            // na konci BASIC bloku, po výpisu programu,
            // může někdy být uložena tabulka proměnných
            // (ukazuje na ni systémová proměnná VARS)            
            if (lineNum > MAX_LINE_NUM_IN_BASIC) {
                tapContent.back(2);  // (protože lineNum není číslo řádky, ale definice proměnné v tabulce proměnných)
                
                int varsLength = 
                        dataLenWoParity - (tapContent.getIdx() - startIdx);
                if (onlyBasic) {    // přeskočit VARS
                    tapContent.skip(varsLength);
                    
                    if (fout != null) {
                        fout.flush();
                    }                
                    break;
                }
                
                writeToOut("--- table of variables (VARS) -- hexdump: \n");                
                // VARS jen jako hexdump;  (decdump není užitečný)
                String hexDump = tapContent.readBlockReturnAsHexDump(varsLength);
                writeToOut("    length = ");
                writeIntToOut(varsLength);
                writeToOut("\n");
                writeToOut(hexDump);
                if (fout != null) {
                    fout.flush();
                }                
                
                if (analyzeVars) {
                    log.info("analyzeVars");
                    // VARS znovu, ale tentokrát jako analýza
                    writeToOut("--- table of variables (VARS) -- analyzed: \n");
                    tapContent.back(varsLength);

                    boolean validTable = analyzeVarsTable(varsLength);
                    if (!validTable) {                    
                        log.info("!validTable");
                        // NE: throw new InvalidTapException("Invalid table of variables");
                        writeToOut("ERROR: Invalid table of variables");
                        // (posun idx v tapContent v metodě analyzeVarsTable)
                    }

                    if (fout != null) {
                        fout.flush();
                    }                
                }
                
                break;   // (tabulka proměnných je na konci, po analýze skončit)
            }
            
            int remainingLineLen = tapContent.readLsbMSB();
            //log.debug("remainingLineLen = " + remainingLineLen + "   0x" + Integer.toHexString(remainingLineLen));
            //log.debug("remainingLineLen = " + remainingLineLen);
            
            writeIntToOut(lineNum);
            writeToOut(" ");

            // --- zbytek řádky po čísle řádky jsou BASIC příkazy
            String basicLine = tapContent
                    .readBlockAndReturnAsEscapedString(remainingLineLen);
            writeToOut(basicLine);
            writeToOut("\n");
            
            if (fout != null) {
                fout.flush();
            }
        }   // while
        
        tapContent.skip(1);     // ! a ten 1B za "checksum" na konci
    }

    
    /**
     * Analyzuje tabulku proměnných na konci BASIC bloku v TAP.
     * Tabulka proměnných se v TAP nevyskytuje často. Někdy obsahuje data,
     * která v BASIC kódu nejsou, která jsou jen v paměti.
     * <p>
     * Př. hexdump začátku oblasti s tabulkou proměnných v Zlatkop.tap:
     * <pre><tt>
     * ec 00 00 04 00 00 00 00  03 00 00 00 00 01 00 00  ................
     * 84 03 03 a2 65 65 f0 00  00 07 00 00 ea 00 00 16  ....ee..........
     * 00 00 00 00 15 00 00 00  00 01 00 00 b8 01 03 56  ...............V
     * e0 02 56 20 6a 65 64 6e  6f 6d 20 73 74 61 72 65  ..V jednom stare
     * </tt></pre>
     * <p> Na záčátku jsou byty:
     * <pre><tt>
     * ec  --> proměnná pro cyklus; jméno: 0xec-0x80 = 0x6c = "l"
     * 00 00 04 00 00  -- hodnota = 4
     * 00 00 03 00 00  -- konečná hodnota = 3
     * 00 00 01 00 00  -- hodnota kroku = 1
     * 84 03  -- číslo řádku na který se smyčka vrací po next = 132+768 = 900
     * 03  -- číslo příkazu pro next
     * </tt></pre>
     * V BASIC kódu lze dohledat i korespondující řádek kódu:
     * <pre><tt>
     * 900 IF NOT gn THEN FOR l=0 TO 3:NEXT l:RETURN 
     * </tt></pre>
     * Poté následuje:
     * <pre><tt>
     * a2  --> proměnná označená více písmeny; první písmeno jména = "b"
     * 65 65  -- "ee"
     * f0  -- 0xf0-0x80 = "p";  celé jméno: "beep" 
     * 00 00 07 00 00  -- hodnota = 7
     * </tt></pre>
     * a tak dále ...
     * 
     * @param dataLen
     * @return {@code true}, pokud se podaří tabulku přijatelně analyzovat; 
     *      jinak {@code false}
     * @throws IOException
     * @throws InvalidTapException  (nemělo by nastat)
     * @throws IllegalArgumentException
     * @see #analyzeVarForLoop(int) 
     * @see #analyzeVarMultiCharName(int) 
     * @see #analyzeVarNumericArray(int) 
     * @see #analyzeVarOneCharName(int) 
     * @see #analyzeVarString(int) 
     * @see #analyzeVarStringArray(int) 
     */
    private boolean analyzeVarsTable(int dataLen) 
            throws IOException, InvalidTapException {
        if (dataLen <= 0) {
            throw new IllegalArgumentException("dataLen <= 0 !");
        }
        log.info("dataLen = " + dataLen);
        
        boolean valid = true;
        
        int startIdx = tapContent.getIdx();
        // (tapContent.getIdx() - startIdx  = počet načtených bytů)
        int lenToEnd;
        while (valid && 
                (lenToEnd = dataLen - (tapContent.getIdx() - startIdx)) > 0) {
                    
            int varId = tapContent.read();  // (z identifikace se i odvozuje první znak proměnné, viz analyzeVar*)
            // ----- řetězec (počet znaků v řetězci + 3 B)
            if (varId >= 0x41 && varId <= 0x5A) {       // = 'A' - 'Z'
                valid = analyzeVarString(varId);                
            }
            // ----- proměnná označená jedním písmenem (6 B)
            else if (varId >= 0x61 && varId <= 0x7A) {
                if (lenToEnd < 6) {
                    valid = false;
                }
                else {
                    valid = analyzeVarOneCharName(varId);
                }
            }
            // ----- číslicové pole (počet prvků * 5  + počet rozměrů * 2  + 4)
            else if (varId >= 0x81 && varId <= 0x9A) {
                valid = analyzeVarNumericArray(varId);
            }
            // ----- proměnná označená více písmeny (počet B jména + 5 B)
            else if (varId >= 0xA1 && varId <= 0xBA) {
                valid = analyzeVarMultiCharName(varId);
            }
            // ----- řetězcové pole (počet prvků +  počet rozměrů * 2  + 4
            else if (varId >= 0xC1 && varId <= 0xDA) {
                valid = analyzeVarStringArray(varId);
            }
            // ----- řídící proměnná cyklu for (19 B)
            else if (varId >= 0xE1 && varId <= 0xFA) {
                if (lenToEnd < 19) {
                    valid = false;
                }
                else {
                    valid = analyzeVarForLoop(varId);                
                }
            }
            // ----- (cokoliv jiného = chyba)
            else {
                valid = false;
            }
        } 
        
        if (!valid) {
            writeToOut("Invalid data. Table of variables probably contains a machine code. ");
            writeToOut("Byte at: 0x" + Integer.toHexString(tapContent.getIdx()) + "\n");
            log.warn("Invalid data. Table of variables probably contains a machine code. ");
            log.warn("Byte at: 0x" + Integer.toHexString(tapContent.getIdx()));
            int readed = tapContent.getIdx() - startIdx;
            log.info("readed: " + readed + " Bytes");

            // skočit až na konec
            log.info("Skip Table of variables");
            tapContent.setIdx(startIdx + dataLen);            
        }
        
        return valid;
    }
    
    /**
     * 
     * @param varId
     * @retrurn 
     * @throws IOException
     * @throws InvalidTapException 
     * @see #analyzeVarsTable(int) 
     */    
    private boolean analyzeVarString(int varId) 
            throws IOException, InvalidTapException {
        char name = (char)(varId + 0x20); // (= malé písmeno)
        writeToOut(name);
        if (! TapByteArrayData.isValidVariableName(String.valueOf(name))) {
            log.warn("name = \"" + String.valueOf(name) + "\"");
            return false;
        }         
        writeToOut(" = ");

        int slen = tapContent.readLsbMSB();

        String text = tapContent.readBlockAndReturnAsEscapedString(slen);

        writeToOut("\"");
        writeToOut(text);
        writeToOut("\"");
        writeToOut("   -- string\n");
        return true;
    }
    
    /**
     * 
     * @param varId
     * @retrurn 
     * @throws IOException
     * @see #analyzeVarsTable(int) 
     */
    private boolean analyzeVarOneCharName(int varId) 
            throws IOException {
        char name = (char)varId;
        if (! TapByteArrayData.isValidVariableName(String.valueOf(name))) {
            log.warn("name = \"" + String.valueOf(name) + "\"");
            return false;
        }        
        writeToOut(name);
        writeToOut(" = ");

        BigDecimal val = tapContent.readBlockAndParseBasicNumber();
        writeToOut(val.toPlainString());
        writeToOut("   -- numeric (one character name)\n");
        return true;
    }
    
    /**
     * 
     * @param varId
     * @retrurn 
     * @throws IOException
     * @see #analyzeVarsTable(int) 
     */
    private boolean analyzeVarNumericArray(int varId)
            throws IOException {
        // např. 2D  pole 4x3  -- 12 prvků
        //  1  2  3  4        čte se po řádcích (řádek, sloupec "row-major")
        //  5  6  7  8
        //  9 10 11 12                
        //  v paměti jako  -->  1  2  3  4    5  6  7  8    9 10 11 12

        char name = (char)(varId - 0x20);
        if (! TapByteArrayData.isValidVariableName(String.valueOf(name))) {
            log.warn("name = \"" + String.valueOf(name) + "\"");
            return false;
        }
        writeToOut(name);
        writeToOut(":  ");

        int datalen = tapContent.readLsbMSB();
        if (datalen > MAX_VARS_DATA_LEGTH) {
            return false;
        }
        
        // počet rozměrů
        int dimensions = tapContent.read();
        if (dimensions <= 0 || dimensions > MAX_VARS_ARRAY_DIM) {
            log.warn("dimensions = \"" + dimensions + "\"");
            return false;
        }
        writeToOut("dimensions = ");
        writeIntToOut(dimensions);

        // velikost rozměrů
        int[] dimSizes = new int[dimensions];

        writeToOut("\n");
        int totalItemsCount = 1;
        for (int k=0; k<dimensions; k++) {
            int size = tapContent.readLsbMSB();
            writeToOut("    dim ");
            writeIntToOut(k+1);
            writeToOut(":  size = ");
            writeIntToOut(size);
            writeToOut("\n");
            dimSizes[k] = size;
            totalItemsCount = totalItemsCount * size;
        }
        if (totalItemsCount > MAX_VARS_DATA_LEGTH) {
            log.warn("totalItemsCount = \"" + totalItemsCount + "\"");
            return false;
        }

        int[] limits = new int[dimensions];
        limits[0] = 1;
        for (int k=1; k<dimensions; k++) {
            limits[k] =  limits[k-1] * dimSizes[k-1];
        }

        if (totalItemsCount > 0) {
            writeToOut("    values = \n");
            BigDecimal item = tapContent.readBlockAndParseBasicNumber();
            writeToOut("      ");
            writeToOut(item.toPlainString());
            writeToOut(", ");
            for (int idx=1; idx<totalItemsCount; idx++) {
                for (int j=1; j<limits.length; j++) {
                    if (idx % limits[j] == 0) {
                        writeToOut("\n      ");
                    }
                }
                item = tapContent.readBlockAndParseBasicNumber();
                writeToOut(item.toPlainString());
                writeToOut(", ");
            }
        }
        writeToOut("\n    -- numeric array\n");
        return true;
    }
         
    /**
     * 
     * @param varId
     * @return 
     * @throws IOException
     * @see #analyzeVarsTable(int) 
     */    
    private boolean analyzeVarMultiCharName(int varId)
            throws IOException {
        StringBuilder name = new StringBuilder();
        // prvni pismeno
        name.append((char)(varId - 0x40));
        // písmena mezi                
        int ch;                
        while ((ch = tapContent.read()) < 0x80) {            
            name.append((char) ch);
        }
        // posledni pismeno
        name.append((char) (ch-0x80));        
        if (! TapByteArrayData.isValidVariableName(name.toString())) {
            log.warn("name = \"" + name.toString() + "\"");
            return false;
        }
        writeToOut(name.toString());
        writeToOut(" = ");

        BigDecimal val = tapContent.readBlockAndParseBasicNumber();
        writeToOut(val.toPlainString()); 
        writeToOut("   -- numeric (multi character name)\n");
        return true;
    }
    
    /**
     * 
     * @param varId
     * @return
     * @throws IOException
     * @throws InvalidTapException 
     * @see #analyzeVarsTable(int) 
     */    
    private boolean analyzeVarStringArray(int varId)
            throws IOException, InvalidTapException {
        char name = (char)(varId - 0x60);
        if (! TapByteArrayData.isValidVariableName(String.valueOf(name))) {
            log.warn("name = \"" + String.valueOf(name) + "\"");
            return false;
        }
        writeToOut(name);
        writeToOut(" = ");

        int datalen = tapContent.readLsbMSB();
        if (datalen > MAX_VARS_DATA_LEGTH) {
            log.warn("datalen = \"" + datalen + "\"");
            return false;
        }
        
        // počet rozměrů
        int dimensions = tapContent.read();
        if (dimensions <= 0 || dimensions > MAX_VARS_ARRAY_DIM) {
            log.warn("dimensions = \"" + dimensions + "\"");
        //    return false;
        }
        writeToOut("dimensions = ");
        writeIntToOut(dimensions);

        // velikost rozměrů
        int[] dimSizes = new int[dimensions];

        writeToOut("\n");
        int itemsCount = 1;
        for (int k=0; k<dimensions; k++) {
            int size = tapContent.readLsbMSB();
            if (size > MAX_VARS_DATA_LEGTH) {
                return false;
            }
            writeToOut("    dim ");
            writeIntToOut(k+1);
            writeToOut(":  size = ");
            writeIntToOut(size);            
            writeToOut("\n");
            dimSizes[k] = size;
            itemsCount = itemsCount * size;
        }

        writeToOut("    values = \n");
        for (int k=0; k<dimensions; k++) {
            String text = tapContent.readBlockAndReturnAsEscapedString(dimSizes[k]);
            writeToOut("      \"");
            writeToOut(text);
            writeToOut("\", ");
            if (k != dimensions-1) {
                writeToOut("\n      ");
            }
        }
        writeToOut("\n    -- string array\n");
        return true;
    }

    /**
     * 
     * @param varId
     * @return
     * @throws IOException
     * @throws InvalidTapException 
     * @see #analyzeVarsTable(int) 
     */
    private boolean analyzeVarForLoop(int varId)
            throws IOException {
        // 1. B  -- kód názvu řídící proměnné + 0x80
        char name = (char)(varId - 0x80);
        if (! TapByteArrayData.isValidVariableName(String.valueOf(name))) {
            log.warn("name = \"" + String.valueOf(name) + "\"");
            return false;
        }        
        writeToOut(name);
        writeToOut(" = ");

        // 5 B  -- (počáteční ?) hodnota řídící prom.
        BigDecimal val = tapContent.readBlockAndParseBasicNumber();
        writeToOut(val.toPlainString());

        // 5 B  --  konečná hodnota řídící prom.
        BigDecimal finalVal = tapContent.readBlockAndParseBasicNumber();
        writeToOut("  to = ");
        writeToOut(finalVal.toPlainString());

        // 5 B  --  hodnota velikosti kroku řídící prom.
        BigDecimal stepVal = tapContent.readBlockAndParseBasicNumber();
        writeToOut("  step = ");
        writeToOut(stepVal.toPlainString());

        // 2 B  --  číslo řádku na který se cyklus vrací po příkaze NEXT
        int lineNumForNext = tapContent.readLsbMSB();
        writeToOut("  lineNumForNext = ");
        writeIntToOut(lineNumForNext);

        // 1 B  --  číslo příkazu na řádce, kam se cyklus vrací po NEXT
        int cmdNumAfterNext = tapContent.read();
        writeToOut("  cmdNumAfterNext = ");
        writeIntToOut(cmdNumAfterNext);
        
        writeToOut("   -- variable for loop\n");
        return true;
    }
            
    /**
     * 
     * @param inFile  soubor tap, který má být analyzován
     * @throws FileNotFoundException
     * @throws IOException 
     * @see #setTapContent(byte[])
     */
    public void setInFile(File inFile) 
            throws FileNotFoundException, IOException {
        if (inFile == null) {
            throw new IllegalArgumentException("inFile=null");
        }
        if (!inFile.exists() || inFile.isDirectory()) {
            throw new FileNotFoundException(inFile.getAbsolutePath());
        }
        setTapContent(Files.readAllBytes(inFile.toPath()));
    }
    
    /**
     * 
     * @param tapContent  toto bude analyzováno
     * @see #setInFile(java.io.File)
     */
    public void setTapContent(byte[] tapContent) {
        if (tapContent.length < MIN_TAP_SIZE) {
            throw new IllegalArgumentException("tapContent.length < MIN_TAP_SIZE");
        }
        this.tapContent = new TapByteArrayData(tapContent);
    }
    
    /**
     * 
     * @param outFile  může být {@code null}. 
     *      V případě {@code null} se bude zapisovat na {@code stdout}.
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public void setOutFile(File outFile) 
            throws FileNotFoundException, IOException {
        if (outFile == null) {
            return;
        }
                
        OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(outFile), 
                Charset.forName("US-ASCII"));
        setOutWriter(writer);
    }
    
    /**
     * .
     * <p>
     * Kódování by mělo být {@code US-ASCII}.
     * 
     * @param fout  může být {@code null}. 
     *      V případě {@code null} se bude zapisovat na {@code stdout}.
     */
    public void setOutWriter(Writer fout) {
        this.fout = fout;
    }        

    /**
     * 
     * @return 
     */
    public Writer getOutWriter() {
        return fout;
    }

    /**
     * Vypíše se jako znak.
     * 
     * @param dataByte
     * @throws IOException 
     */
    private void writeCharToOut(int dataByte) throws IOException {
        writeToOut((char)dataByte);
    }

    /**
     * Vypíše se jako číslo.
     * 
     * @param dataByte
     * @throws IOException 
     */
    private void writeIntToOut(int dataByte) throws IOException {
        writeToOut(String.valueOf(dataByte));
    }

    /**
     * 
     * @param dataCh
     * @throws IOException 
     */
    private void writeToOut(char dataCh) throws IOException {
        if (fout == null) {
            System.out.print(dataCh);
            return;
        }
        fout.write(dataCh);
    }
    
    /**
     * 
     * @param data
     * @throws IOException 
     */
    private void writeToOut(String data) throws IOException {
        if (fout == null) {
            System.out.print(data);
            return;
        }
        fout.write(data);
    }

    /**
     * Uzavře "OutputWriter".
     * Pokud OutputWriter nebyl zadán, neprovede nic.
     *
     * @throws IOException
     */
    public void closeOutWriter() throws IOException {
        if (fout == null) {
            return;
        }

        try {
            fout.flush();
        }
        finally {
            fout.close();
        }
    }

}   // Tap2bas.java
