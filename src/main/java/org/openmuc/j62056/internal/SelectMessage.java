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
 * / <manufacturer data> CR LF
 * <p>
 * Device address is optional
 */
public class SelectMessage {

    private final byte[] selectMessageBytes;
    private final String manufacturerData;

    /**
     * Constructor for select message with manufacturer data.
     *
     * @param manufacturerData specific manufacturer data
     */
    public SelectMessage(String manufacturerData) {
        this.manufacturerData = manufacturerData;
        selectMessageBytes = (manufacturerData + "\r\n").getBytes(Helper.ASCII_CHARSET);
    }

    public void send(DataOutputStream os) throws IOException {
        os.write(selectMessageBytes);
        os.flush();
    }

    @Override
    public String toString() {
        return "{\"select message\": {\"manufacturer data\": \"" + manufacturerData + "\"}}";
    }

    public boolean isEmpty() {
        return manufacturerData == null;
    }
}
