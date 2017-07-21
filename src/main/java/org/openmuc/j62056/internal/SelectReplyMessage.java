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
 * Format: '/' <manufacturer reply> 'CR' 'LF'
 */
public class SelectReplyMessage {

    private final String selectReplyMessage;

    public SelectReplyMessage(DataInputStream is) throws IOException {

        byte b = is.readByte();
        if (b != '/') {
            throw new IOException(
                    "Received unexpected select reply message start byte: " + HexConverter.toShortHexString(b));
        }

        String tempReplyMessage = "";
        b = is.readByte();
        while (b != '\r') {
            tempReplyMessage += (char) b;
            b = is.readByte();
        }
        selectReplyMessage = tempReplyMessage;

        b = is.readByte();
        if (b != '\n') {
            throw new IOException(
                    "Received unexpected identification message end byte: " + HexConverter.toShortHexString(b));
        }

    }

    public String getReplyMessage() {
        return selectReplyMessage;
    }

    @Override
    public String toString() {
        return "{\"reply message\": {\"reply\": \"" + selectReplyMessage + "}}";
    }
}
