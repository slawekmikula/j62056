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
import java.nio.charset.Charset;

public class Helper {
    public static final Charset ASCII_CHARSET = Charset.forName("US-ASCII");
    public static final int FRAGMENT_TIMEOUT = 500;

    public static void debug(Object... objects) {
        StringBuilder sb = new StringBuilder();
        sb.append("DEBUG: ");
        for (Object object : objects) {
            if (object.getClass().equals(byte[].class)) {
                sb.append(HexConverter.toHexString((byte[]) object));
            }
            else {
                sb.append(object);
            }

        }
        System.out.println(sb.toString());
    }

    public static byte readByteAndCalculateBcc(DataInputStream is, Bcc bcc) throws IOException {
        byte b = is.readByte();
        bcc.value ^= b;
        return b;
    }
}
