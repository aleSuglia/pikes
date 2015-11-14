//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.12 at 10:43:37 PM CET 
//


package eu.fbk.dkm.pikes.resources.util.fnlu;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for coreType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="coreType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Core"/>
 *     &lt;enumeration value="Peripheral"/>
 *     &lt;enumeration value="Extra-Thematic"/>
 *     &lt;enumeration value="Core-Unexpressed"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "coreType")
@XmlEnum
public enum CoreType {

    @XmlEnumValue("Core")
    CORE("Core"),
    @XmlEnumValue("Peripheral")
    PERIPHERAL("Peripheral"),
    @XmlEnumValue("Extra-Thematic")
    EXTRA_THEMATIC("Extra-Thematic"),
    @XmlEnumValue("Core-Unexpressed")
    CORE_UNEXPRESSED("Core-Unexpressed");
    private final String value;

    CoreType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CoreType fromValue(String v) {
        for (CoreType c: CoreType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
