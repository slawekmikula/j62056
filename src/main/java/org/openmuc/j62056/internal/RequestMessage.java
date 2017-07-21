/*
 * Copyright 2013-17 Fraunhofer ISE
 *
 * This file is part of j62056.
 * For more information visit http://www.openmuc.org
 *
 * j62056 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * j62056 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with j62056.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.j62056.internal;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * / ? Device address ! CR LF
 * <p>
 * Device address is optional
 */
public class RequestMessage {

    private final byte[] requestMessageBytes;
    private final String deviceAddress;

    /**
     * Constructor for request message with device address.
     * 
     * @param deviceAddress
     *            the device address
     */
    public RequestMessage(String deviceAddress) {
        this(deviceAddress, null);
    }

    /**
     * Constructor for request message with device address and with specific start characters. <br>
     * If startCharacters is null or empty the default value '/?' will used.
     * 
     * @param deviceAddress
     *            the device address
     * @param startCharacters
     *            specific start characters, default is '/?'
     */

    public RequestMessage(String deviceAddress, String startCharacters) {
        if (deviceAddress.length() > 32) {
            throw new IllegalArgumentException("Device address is longer than 32 characters");
        }
        if (startCharacters == null || startCharacters.isEmpty()) {
            startCharacters = "/?";
        }

        this.deviceAddress = deviceAddress;
        requestMessageBytes = (startCharacters + deviceAddress + "!\r\n").getBytes(Helper.ASCII_CHARSET);
    }

    public void send(DataOutputStream os) throws IOException {
        os.write(requestMessageBytes);
        os.flush();
    }

    @Override
    public String toString() {
        return "{\"request message\": {\"device address\": \"" + deviceAddress + "\"}}";
    }
}
