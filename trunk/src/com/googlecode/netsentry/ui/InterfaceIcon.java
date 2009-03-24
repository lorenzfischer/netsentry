package com.googlecode.netsentry.ui;

import com.googlecode.netsentry.R;
import com.googlecode.netsentry.backend.InterfaceStatsProvider;

/**
 * This class provides resource ids for the icons that represent the interface
 * types handled by the network traffic meter application.
 * 
 * @author lorenz fischer
 */
public final class InterfaceIcon {

	/**
	 * This is a utility class, therefore we do not provide a public constructor
	 * (This is also the reason why this class is declared as final).
	 */
	private InterfaceIcon() {
	}

	/**
	 * This method returns the resource id for a given interface name.
	 * 
	 * @param interfaceName
	 *            the name of the interface we want the resource id for.
	 * @return the id of the apropriate drawable as specified in {@link R}.
	 */
	public static int getResourceIdForInterface(String interfaceName) {
		
		if (interfaceName.startsWith(InterfaceStatsProvider.INTERFACE_NAME_TYPE_WIFI)) {
			return R.drawable.interface_type_wifi;
		} else if (interfaceName.startsWith(InterfaceStatsProvider.INTERFACE_NAME_TYPE_3G)) {
			return R.drawable.interface_type_3g;
		} else if (interfaceName.startsWith(InterfaceStatsProvider.INTERFACE_NAME_TYPE_ETHERNET)) {
			return R.drawable.interface_type_ethernet;
		}

		return R.drawable.interface_type_unknown;
	}
}
