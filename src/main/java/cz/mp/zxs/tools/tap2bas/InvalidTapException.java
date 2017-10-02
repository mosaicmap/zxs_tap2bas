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

    public InvalidTapException(String message) {
        super(message);
    }

    public InvalidTapException(String message, Throwable cause) {
        super(message, cause);
    }
    
}   // InvalidTapException