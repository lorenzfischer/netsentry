/**
 * 
 */
package com.googlecode.netsentry.backend;

/**
 * <p>
 * This class serves as a data transfer object for the statistics gathered by
 * the {@link Updater}.
 * </p>
 * <p>
 * <b>This class is thread safe.</b>
 * </p>
 * 
 * TODO there must be a better solution than just to synchronize on all methods.
 * Maybe make the values volatile and setters private?
 * 
 * @author lorenz fischer
 */
public class InterfaceStats {

	/** The name of the interface the counter values are for. */
	private String interfaceName;

	/** The bytes received counter. */
	private long bytesReceived;

	/** The bytes sent counter. */
	private long bytesSent;

	/**
	 * Creates a new instance of {@link InterfaceStats} setting
	 * <code>interfaceName</code> as this objects value for
	 * <code>interfaceName</code>.
	 * 
	 * @param interfaceName
	 *            the name of the interface this object's statistics are for.
	 */
	public InterfaceStats(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	/**
	 * @return the bytesSent counter at the time of object construction.
	 */
	public synchronized long getBytesSent() {
		return bytesSent;
	}

	/**
	 * @return the bytesReceived counter at the time of object construction.
	 */
	public synchronized long getBytesReceived() {
		return bytesReceived;
	}

	/**
	 * @param bytesSent
	 *            the bytesSent to set
	 */
	public synchronized void setBytesSent(long bytesSent) {
		this.bytesSent = bytesSent;
	}

	/**
	 * @param bytesReceived
	 *            the bytesReceived to set
	 */
	public synchronized void setBytesReceived(long bytesReceived) {
		this.bytesReceived = bytesReceived;
	}

	/**
	 * @return the interfaceName
	 */
	public synchronized String getInterfaceName() {
		return interfaceName;
	}

}
