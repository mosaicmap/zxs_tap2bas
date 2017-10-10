/*
 * Tap2basCli.java
 *
 *  created: 30.9.2017
 *  charset: UTF-8
 */

package cz.mp.zxs.tools.tap2bas;

import static cz.mp.zxs.tools.tap2bas.Version.VERSION;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * CLI (Command Line Interface) pro vykonání {@code Tap2bas}.
 * 
 * @author Martin Pokorný
 * @see Tap2bas
 */
public final class Tap2basCli {
    private static final Logger log = LoggerFactory.getLogger(Tap2basCli.class);

    private Options options = new Options();
    private HelpFormatter helpFormatter = new HelpFormatter();

    // hodnoty z parametrů programu
    private boolean optHelp = false;
    private boolean optVersion = false;
    private String optInputFileName = null;
    private String optOutFileName = null;        
    private boolean optOnlyBasic = false;

    private Tap2bas tap2bas = new Tap2bas();

    public static final int RESULT_OK = 0;
    public static final int RESULT_ERR_GENERAL = 1;
    public static final int RESULT_ERR_OPTS = 2;
    public static final int RESULT_ERR_TAP_FORMAT = 3;
        
    /**
     * 
     */
    private void printHelp() {
        log.info("");
        pout("Does elementary analysis of a TAP file for Sinclair ZX Spectrum.");
        pout("Converts BASIC blocks to listing readable for humans.");
        pout("Usage:");
        pout("  java -jar zxs_tap2bas.jar [options...]");
        pout("Options:");
        StringWriter sw = new StringWriter();
        helpFormatter.printOptions(new PrintWriter(sw), 79, options, 2, 2);
        poutNoEol(sw.toString());
        pout("Examples:");
        pout("  java -jar zxs_tap2bas.jar -i gold.tap -o gold.txt");
        pout("  java -jar zxs_tap2bas.jar -i gold.tap --onlyBasic -o gold.bas");
    }
    
    /**
     * 
     */
    private void printVersion() {
        log.info("");
        pout(VERSION); 
    }
    
    private static void pout(String text) {
        System.out.println(text);
    }

    private static void poutNoEol(String text) {
        System.out.print(text);
    }

    private static void perr(String text) {
        System.err.println(text);
    }
    
    /**
     * 
     * @param msg
     * @param errCode 
     */
    private static void exitWithError(String msg, int errCode) {
        log.error("Error: " + msg);
        perr("Error: " + msg);
        System.exit(errCode);
    }

    /**
     * 
     * @param ex
     * @param errCode 
     */
    private static void exitWithError(Exception ex, int errCode) {
        log.error("Error: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
        perr("Error: " + ex.getClass().getName() + ": " + ex.getMessage());
        System.exit(errCode);
    }

    /** */
    public Tap2basCli() {
        createOptions();
    }

    
    private void createOptions() {
        Option help = Option.builder("h")
                .longOpt("help")
                .hasArg(false)
                .required(false)
                .desc("prints this help and exit")
                .build();
        options.addOption(help);
        
        Option version = Option.builder("v")
                .longOpt("version")
                .hasArg(false)
                .required(false)
                .desc("prints version number and exit")
                .build();
        options.addOption(version);

        Option inFileName = Option.builder("i")
                .hasArg(true)
                .required(false)
                .desc("input TAP file name. Mandatory")
                .build();
        options.addOption(inFileName);

        Option outFileName = Option.builder("o")
                .hasArg(true)
                .required(false)
                .desc("output file name. If it is not specified, output is made to stdout.")
                .build();
        options.addOption(outFileName);

        Option onlyBasic = Option.builder()
                .longOpt("onlyBasic")
                .hasArg(false)
                .required(false)
                .desc("output should contain only listings of BASIC programs")
                .build();
        options.addOption(onlyBasic);                
        
        // --extractScr file
    }
    
    /**
     * 
     * @param args 
     */
    private void parseArgs(String[] args) {
        try {
            CommandLineParser parser = new DefaultParser();
            
            CommandLine commandLine = parser.parse(options, args);
                       
            if (commandLine.getOptions().length == 0) {     // žádné parametry
                printHelp();
                exitWithError("no program options", RESULT_ERR_OPTS);
            }
            
            if (commandLine.hasOption("h")) {
                log.info("--help");
                optHelp = true;
            }
            if (commandLine.hasOption("v")) {
                log.info("--version");
                optVersion = true;
            }
            if (commandLine.hasOption("i")) {
                optInputFileName = commandLine.getOptionValue("i");
                log.info("-i = " + optInputFileName);
            } 
            if (commandLine.hasOption("o")) {
                optOutFileName = commandLine.getOptionValue("o");
                log.info("-o = " + optOutFileName);
            }
            if (commandLine.hasOption("onlyBasic")) {
                log.info("--onlyBasic");
                optOnlyBasic = true;
            }
        }
        catch (ParseException pex) {
            exitWithError(pex, RESULT_ERR_OPTS);
        }
    }

    /**
     * Obslouží parametry {@code --help}, {@code --version} a pokud byl 
     * nějaký takový parametr zadán, tak i ukončí program.
     */
    private void executeInfoOptsAndExit() {
        if (optHelp) {
            printHelp();
            System.exit(RESULT_OK);
        }
        if (optVersion) {
            printVersion();
            System.exit(RESULT_OK);            
        }        
    }

    /**
     * Validuje hodnoty parametrů pro {@code Tap2bas}.
     * Pokud byly parametry zadány špatně, ukončí program s chybou.
     */
    private void validateOptValuesForTap2bas() {
        // zadání vstupního souboru je povinné
        if (optInputFileName == null) {
            exitWithError("input file not defined", RESULT_ERR_OPTS);
        }
        log.info("inFileName  = " + optInputFileName);
              
        // pojistka pro nechtěnému přepsání originálního souboru
        if (optInputFileName.equals(optOutFileName)) {
            exitWithError("input and output file have same name", RESULT_ERR_OPTS);
        }
        
        File inFile = new File(optInputFileName);
        if (!inFile.exists() || inFile.isDirectory()) {
            exitWithError("input file not found", RESULT_ERR_OPTS);
        }

        log.info("outFileName = " + optOutFileName);
        if (optOutFileName != null) {
            // jednoduchá pojistka proti nechtěnému přepsání programu a/nebo originálního souboru
            // (asi není nutná)
            if (hasFileForbiddedExt(optOutFileName.toLowerCase())) {
                exitWithError("output file has forbidden extension", RESULT_ERR_OPTS);
            }                
        }        
    }
    
    private static final String[] FORBIDDEN_EXTS = new String[] {
        ".tap", ".jar", ".bat", ".sh", ".exe", ".dll", ".xml", ".properties"        
    };
    /**
     * Zjistí zda zadané jméno souboru nemá nepovolenou příponu.
     * Pomocná metoda pro {@linkplain #validateOptValuesForTap2bas()}.
     * 
     * @param fileName
     * @return 
     * @see #validateOptValuesForTap2bas()
     */
    private boolean hasFileForbiddedExt(final String fileName) {
        for (String forbiddenExt : FORBIDDEN_EXTS) {
            if (fileName.endsWith(forbiddenExt)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Vykoná {@code Tap2bas} se zadanými parametry.
     * 
     */
    private void executeTap2basWithOpts() {        
        validateOptValuesForTap2bas();  // (pokud jsou parametry zadány špatně, tak ukončí program)
        
        try {        
            tap2bas.setInFile(new File(optInputFileName));
            if (optOutFileName == null) {
                tap2bas.setOutFile(null);
            }
            else {
                tap2bas.setOutFile(new File(optOutFileName));
            }

            if (optOnlyBasic) {
                tap2bas.analyzeAndExtractBasic();
            }
            else {
                tap2bas.analyzeAll();
            }            
            
            closeTap2basOutWriter();
        } catch (InvalidTapException ex) {
            closeTap2basOutWriter();
            exitWithError(ex, RESULT_ERR_TAP_FORMAT);
        } catch (Exception ex) {
            closeTap2basOutWriter();
            exitWithError(ex, RESULT_ERR_GENERAL);
        } finally {
            // NE! Po System.exit(num) by nenastalo!
        }        
    }
    
    /** 
     * Pomocná metoda pro {@linkplain #executeTap2basWithOpts()}. 
     * @see Tap2bas#closeOutWriter()
     */
    private void closeTap2basOutWriter() {
        try {
            tap2bas.closeOutWriter();
        } catch (IOException ioex) {
            log.error(ioex.getMessage(), ioex);
        }        
    }
    
    /**
     * 
     * @param args 
     */
    public void executeWithArgs(String[] args) {
        parseArgs(args);

        if (optHelp || optVersion) {    // (Tap2bas se zde nevykonává...)
            executeInfoOptsAndExit();
        }
        else {
            executeTap2basWithOpts();
        }
    }

}   // Tap2basCli.java
