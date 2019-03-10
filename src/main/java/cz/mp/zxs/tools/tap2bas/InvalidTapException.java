/*
 * InvalidTapException.java
 *
 *  created: 25.4.2017
 *  charset: UTF-8
 */

package cz.mp.zxs.tools.tap2bas;


/**
 *
 * @author Martin Pokorný
 */
public class InvalidTapException extends Exception {

    private static final long serialVersionUID = -1026870137787052087L;

    public InvalidTapException(String message) {
        super(message);
    }

    public InvalidTapException(String message, Throwable cause) {
        super(message, cause);
    }
    
}   // InvalidTapException