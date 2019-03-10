/*
 * TapBlockType.java
 *
 *  created: 25.4.2017
 *  charset: UTF-8
 */

package cz.mp.zxs.tools.tap2bas;


/**
 * Typ bloku v TAP souboru.
 *
 * @author Martin Pokorný
 * @see Tap2bas
 */
public enum TapBlockType {
    /** BASIC program. */
    BASIC(0, "BASIC program"),
    NUMBERS(1, "Data: numbers"),
    TEXTS(2, "Data: texts"),
    /** Binární data. Tj:  SCREEN$, MC rutiny, UDG, Fonty, Sprity, cokoliv jiného. */
    BINARY_DATA(3, "Data: binary data "),
    ;
    
    int num;
    String description;

    private TapBlockType(int num, String description) {
        this.num = num;
        this.description = description;
    }

    public int getNum() {
        return num;
    }

    public String getDescription() {
        return description;
    }
    
    /**
     * 
     * @param num
     * @return  {@code TapBlockType} nebo {@code null}, pokud pro zadané číslo 
     *      neodpovídá žídnému blok.
     */
    public static TapBlockType getByNum(int num) {
        for (TapBlockType type : values()) {
            if (type.getNum() == num) {
                return type;
            }
        }
        return null;
    }
}   // TapBlockType
