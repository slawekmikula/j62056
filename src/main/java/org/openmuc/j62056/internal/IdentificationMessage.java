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

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Format: '/' X X X Z Identification 'CR' 'LF'
 * <p>
 * X X X = manufacturer identification (three characters)
 * <p>
 * Z = baud rate identification, is also used to select the mode, e.g. if Z='A'...'F' then mode B is selected
 * <p>
 * Identification = manufacturer specific device ID that has a maximum length of 16. It may contain the escape character
 * '\' followed by W which is the enhanced baud rate and mode identification character.
 */
public class IdentificationMessage {

    private final String manufacturerId;
    private final ProtocolMode protocolMode;
    private final int baudRate;
    private final String meterId;
    private final String enhancedId;

    public IdentificationMessage(DataInputStream is) throws IOException {

        byte b = is.readByte();
        if (b != '/') {
            throw new IOException(
                    "Received unexpected identification message start byte: " + HexConverter.toShortHexString(b));
        }

        byte[] manufacturerIdBytes = new byte[3];
        is.readFully(manufacturerIdBytes);
        manufacturerId = new String(manufacturerIdBytes, Helper.ASCII_CHARSET);

        byte baudRateByte = is.readByte();
        switch (baudRateByte) {
        case 'A':
            baudRate = 600;
            protocolMode = ProtocolMode.B;
            break;
        case 'B':
            baudRate = 1200;
            protocolMode = ProtocolMode.B;
            break;
        case 'C':
            baudRate = 2400;
            protocolMode = ProtocolMode.B;
            break;
        case 'D':
            baudRate = 4800;
            protocolMode = ProtocolMode.B;
            break;
        case 'E':
            baudRate = 9600;
            protocolMode = ProtocolMode.B;
            break;
        case 'F':
            baudRate = 19200;
            protocolMode = ProtocolMode.B;
            break;
        case '0':
            baudRate = 300;
            protocolMode = ProtocolMode.C;
            break;
        case '1':
            baudRate = 600;
            protocolMode = ProtocolMode.C;
            break;
        case '2':
            baudRate = 1200;
            protocolMode = ProtocolMode.C;
            break;
        case '3':
            baudRate = 2400;
            protocolMode = ProtocolMode.C;
            break;
        case '4':
            baudRate = 4800;
            protocolMode = ProtocolMode.C;
            break;
        case '5':
            baudRate = 9600;
            protocolMode = ProtocolMode.C;
            break;
        case '6':
            baudRate = 19200;
            protocolMode = ProtocolMode.C;
            break;
        default:
            baudRate = -1;
            protocolMode = ProtocolMode.A;
        }

        b = is.readByte();
        String tempEnhancedId = "";
        while (b == 0x5c) {
            tempEnhancedId += (char) is.readByte();
            b = is.readByte();
        }
        enhancedId = tempEnhancedId;

        byte[] identificationBytes = new byte[16];
        int i = 0;
        while (b != '\r') {
            if (i == 16) {
                throw new IOException("Expected carriage return character not received");
            }
            identificationBytes[i] = b;
            i++;
            b = is.readByte();
        }
        meterId = new String(identificationBytes, 0, i, Helper.ASCII_CHARSET);

        b = is.readByte();
        if (b != '\n') {
            throw new IOException(
                    "Received unexpected identification message end byte: " + HexConverter.toShortHexString(b));
        }

    }

    public String getManufactureId() {
        return manufacturerId;
    }

    public String getMeterId() {
        return meterId;
    }

    public String getEnhancedId() {
        return enhancedId;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public ProtocolMode getProtocolMode() {
        return protocolMode;
    }

    @Override
    public String toString() {
        return "{\"identification message\": {\"manufacturer ID\": \"" + manufacturerId + "\", \"protocol mode\": \""
                + protocolMode + "\", \"baud rate\": " + baudRate + ", \"meter ID\": \"" + meterId
                + "\", \"enhanced ID/capability\": \"" + enhancedId + "\"" + getEnhancedIdDescription(enhancedId)
                + "}}";
    }

    public static String getEnhancedIdDescription(String enhancedId) {
        if (enhancedId.equals("2")) {
            return "(HDLC)";
        }
        return "";
    }
}
