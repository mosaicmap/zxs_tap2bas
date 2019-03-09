/*
 * Version.java
 *
 *  created: 1.10.2017
 *  charset: UTF-8
 */

package cz.mp.zxs.tools.tap2bas;

/**
 * Verze programu. Zde se bere verze z manifest.mf, který je generovaný pom.xml.
 *
 * @author Martin Pokorný
 */
public final class Version {
    
    public static final String VERSION_SPEC = 
            Version.class.getPackage().getSpecificationVersion(); // viz pom.xml -> manifest.mf
    public static final String VERSION_IMPL = 
            Version.class.getPackage().getImplementationVersion(); // viz pom.xml -> manifest.mf
    public static final String VERSION;
    static {
        if (VERSION_SPEC == null || VERSION_SPEC.length() == 0) {
            VERSION = "DEVEL";
        }
        else if (VERSION_IMPL == null || VERSION_IMPL.length() == 0) {
            VERSION = VERSION_SPEC;
        }
        else {
            VERSION = VERSION_SPEC + " (" + VERSION_IMPL + ")";
        }
    }    
    
    private Version() {
    }

}   // Version.java
