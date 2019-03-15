/*
 * Tap2basTest.java
 *
 *  created: 15.3.2019
 *  charset: UTF-8
 */

package cz.mp.zxs.tools.tap2bas;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import org.junit.Assert;
import org.junit.Test;


/**
 *
 * @author Martin Pokorný
 */
public class Tap2basTest {

    // Obsah usr_char.bas_ :
    //11 DATA BIN 00011000
    //12 DATA BIN 00111100
    //13 DATA BIN 01111110
    //14 DATA BIN 11011011
    //15 DATA BIN 11111111
    //16 DATA BIN 00100100
    //17 DATA BIN 01011010
    //18 DATA BIN 10100101
    //  -- pozor "  " místo " " :
    //19 FOR I=0  TO 7:READ B:POKE USR "A"+I,B:NEXT I   
    //20 PRINT CHR$ 144

    // pro volbu  --onlyBasic
    @Test
    public void testAnalyzeAndExtractOnlyBasic01() {
        Tap2bas tap2bas = new Tap2bas();
        File inFile = new File("src/test/resources/tap_files/usr_char.tap");
                
        try {
            tap2bas.setInFile(inFile);

            StringWriter sw = new StringWriter();            
            tap2bas.setOutWriter(sw);
            tap2bas.analyzeAndExtractOnlyBasic();
            sw.close();
            String result = sw.toString().replaceAll("  ", " ");
            
            File expectedFile = new File("src/test/resources/tap_files/usr_char.bas_");
            String expected = new String(
                    Files.readAllBytes(expectedFile.toPath()) );
            expected = expected.replaceAll("  ", " "); 
                    
            Assert.assertEquals(expected, result);
            
        } catch (InvalidTapException | IOException ex) {            
            ex.printStackTrace(System.err);
            Assert.fail(ex.getMessage());
        }        
    }
            
}   // Tap2basTest.java
 