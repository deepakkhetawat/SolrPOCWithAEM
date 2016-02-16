package com.solrpoc.core.util

/**
 * Utility to simplify some groovy-isms
 */
@groovy.transform.CompileStatic
final class GroovyUtil {

    /**
     * Set an object's properties via a map
     * @param obj the object to set
     * @param properties the properties to set on the object
     */
    public static void setProperties(GroovyObject obj, Map<String, Object> properties) {
        if (!properties)
           return
        if (obj == null)
            throw new IllegalArgumentException('obj is null')
        for (Map.Entry<String, Object> prop : properties.entrySet()) {
            obj.setProperty(prop.key, prop.value)
        }
    }

    /**
     * Set an object's properties via a map
     * @param obj the object to set
     * @param properties the properties to set on the object
     */
    public static void setProperties(Object obj, Map<String, Object> properties) {
        if (!(obj instanceof GroovyObject))
            throw new IllegalArgumentException('obj must be a groovy object')
        setProperties(obj as GroovyObject, properties)
    }
}
