//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.12 at 10:43:37 PM CET 
//


package eu.fbk.dkm.pikes.resources.util.fnlu;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for sentenceType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="sentenceType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="text" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="annotationSet" type="{http://framenet.icsi.berkeley.edu}annotationSetType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="ID" type="{http://framenet.icsi.berkeley.edu}IDType" />
 *       &lt;attribute name="aPos" type="{http://framenet.icsi.berkeley.edu}extSentRefType" />
 *       &lt;attribute name="paragNo" type="{http://framenet.icsi.berkeley.edu}orderType" />
 *       &lt;attribute name="sentNo" type="{http://framenet.icsi.berkeley.edu}orderType" />
 *       &lt;attribute name="docID" type="{http://framenet.icsi.berkeley.edu}IDType" />
 *       &lt;attribute name="corpID" type="{http://framenet.icsi.berkeley.edu}IDType" />
 *       &lt;attribute name="externalID" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sentenceType", propOrder = {
    "text",
    "annotationSet"
})
public class SentenceType {

    @XmlElement(required = true)
    protected String text;
    protected List<AnnotationSetType> annotationSet;
    @XmlAttribute(name = "ID")
    protected Integer id;
    @XmlAttribute(name = "aPos")
    protected Integer aPos;
    @XmlAttribute(name = "paragNo")
    protected Integer paragNo;
    @XmlAttribute(name = "sentNo")
    protected Integer sentNo;
    @XmlAttribute(name = "docID")
    protected Integer docID;
    @XmlAttribute(name = "corpID")
    protected Integer corpID;
    @XmlAttribute(name = "externalID")
    protected String externalID;

    /**
     * Gets the value of the text property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the value of the text property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setText(String value) {
        this.text = value;
    }

    /**
     * Gets the value of the annotationSet property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the annotationSet property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAnnotationSet().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AnnotationSetType }
     * 
     * 
     */
    public List<AnnotationSetType> getAnnotationSet() {
        if (annotationSet == null) {
            annotationSet = new ArrayList<AnnotationSetType>();
        }
        return this.annotationSet;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getID() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setID(Integer value) {
        this.id = value;
    }

    /**
     * Gets the value of the aPos property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getAPos() {
        return aPos;
    }

    /**
     * Sets the value of the aPos property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setAPos(Integer value) {
        this.aPos = value;
    }

    /**
     * Gets the value of the paragNo property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getParagNo() {
        return paragNo;
    }

    /**
     * Sets the value of the paragNo property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setParagNo(Integer value) {
        this.paragNo = value;
    }

    /**
     * Gets the value of the sentNo property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getSentNo() {
        return sentNo;
    }

    /**
     * Sets the value of the sentNo property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setSentNo(Integer value) {
        this.sentNo = value;
    }

    /**
     * Gets the value of the docID property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getDocID() {
        return docID;
    }

    /**
     * Sets the value of the docID property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setDocID(Integer value) {
        this.docID = value;
    }

    /**
     * Gets the value of the corpID property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getCorpID() {
        return corpID;
    }

    /**
     * Sets the value of the corpID property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setCorpID(Integer value) {
        this.corpID = value;
    }

    /**
     * Gets the value of the externalID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExternalID() {
        return externalID;
    }

    /**
     * Sets the value of the externalID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExternalID(String value) {
        this.externalID = value;
    }

}
