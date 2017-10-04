
package cz.mp.zxs.tools.tap2bas;

import static cz.mp.zxs.tools.tap2bas.Version.VERSION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martin Pokorný
 */
public class Main {
    private static final Logger log;
    static {
        if (System.getProperty("java.util.logging.config.file") == null) {
            System.setProperty("java.util.logging.config.file", "logging.properties");
        }
        log = LoggerFactory.getLogger(Main.class);
    }    

    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        log.info("version: " + VERSION);
        
        new Tap2basCli().executeWithArgs(args);
        
        log.info("end");
    }
}

