package de.nierbeck.camel.exam.demo.control;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "order")
@XmlAccessorType(XmlAccessType.FIELD)
public class CamelMessageBean implements Serializable {

	private static final long serialVersionUID = -1L;
	@XmlElement(required = true)
	private String message;
	@XmlElement
	private String tmstamp;

	/**
	 * @return
	 */
	public String getTmstamp() {
		return tmstamp;
	}

	/**
	 * @param tmstamp
	 */
	public void setTmstamp(String tmstamp) {
		this.tmstamp = tmstamp;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "CamelMessageBean [message=" + message + ", tmstamp=" + tmstamp
				+ "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((tmstamp == null) ? 0 : tmstamp.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof CamelMessageBean)) {
			return false;
		}
		CamelMessageBean other = (CamelMessageBean) obj;
		if (message == null) {
			if (other.message != null) {
				return false;
			}
		} else if (!message.equals(other.message)) {
			return false;
		}
		if (tmstamp == null) {
			if (other.tmstamp != null) {
				return false;
			}
		} else if (!tmstamp.equals(other.tmstamp)) {
			return false;
		}
		return true;
	}

}
