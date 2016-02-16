package com.solrpoc.core.model

/**
 * interface indicating the object implies a string constant value.
 * This is primarily used for enumerated types
 */
public interface StringConstant {

    /**
     * Retrieve the string value stored by the constant
     * @return string value the constant represents
     */
    public String getValue()
}
