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
package org.openmuc.j62056;

import java.io.DataInputStream;
import java.io.IOException;

import org.openmuc.j62056.internal.Bcc;
import org.openmuc.j62056.internal.Helper;
import org.openmuc.j62056.internal.HexConverter;

/**
 * 
 * A data message contains a list of data sets. Each data set consists of 3 fields "address", "value", and "unit". Each
 * of these fields is optional an may thus be equal to the empty string.
 * 
 */
public class DataSet {

    // Data set ::= Address '(' Value(optional) ('*' unit)(optional) ')'

    private final String address;
    private final String value;
    private final String unit;

    private static final int BUFFER_LENGTH = 100;

    private DataSet(String id, String value, String unit) {
        this.address = id;
        this.value = value;
        this.unit = unit;
    }

    static DataSet readDataSet(DataInputStream is, Bcc bcc) throws IOException {

        byte b = Helper.readByteAndCalculateBcc(is, bcc);
        if (b == '\r') {
            b = Helper.readByteAndCalculateBcc(is, bcc);
            if (b != '\n') {
                throw new IOException(
                        "Received unexpected data message start byte: " + HexConverter.toShortHexString(b));
            }
            b = Helper.readByteAndCalculateBcc(is, bcc);
        }
        if (b == '!') {
            return null;
        }

        byte[] buffer = new byte[BUFFER_LENGTH];

        int i = 0;
        while (b != '(') {
            if (i == BUFFER_LENGTH) {
                throw new IOException("Expected '(' character not received.");
            }
            buffer[i] = b;
            i++;
            b = Helper.readByteAndCalculateBcc(is, bcc);
        }
        String address = new String(buffer, 0, i, Helper.ASCII_CHARSET);

        i = 0;
        while ((b = Helper.readByteAndCalculateBcc(is, bcc)) != '*' && b != ')') {
            if (i == BUFFER_LENGTH) {
                throw new IOException("Expected '*' or ')' character not received.");
            }
            buffer[i] = b;
            i++;
        }
        String value = new String(buffer, 0, i, Helper.ASCII_CHARSET);

        String unit;
        if (b == ')') {
            unit = "";
        }
        else {
            i = 0;
            while ((b = Helper.readByteAndCalculateBcc(is, bcc)) != ')') {
                if (i == BUFFER_LENGTH) {
                    throw new IOException("Expected ')' character not received.");
                }
                buffer[i] = b;
                i++;
            }
            unit = new String(buffer, 0, i, Helper.ASCII_CHARSET);
        }

        return new DataSet(address, value, unit);

    }

    /**
     * Returns the address/ID of this data set.
     * <p>
     * The address is usually an OBIS code of the format A-B:C.D.E*F or on older EDIS code of the format C.D.E. that
     * specifies exactly what the value of this data set represents. C is the type of the measured quantity (e.g 1 =
     * positive active power), D describes the measurement mode and E is the tariff (e.g. 0 for total or 1 for tariff 1
     * only) associated with this value.
     * <p>
     * If this data set contains no address this function returns the empty string.
     * 
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Returns the value of this data set as a string.
     * <p>
     * The value is usually a decimal number that can be converted to a Double using
     * {@link java.lang.Double#parseDouble(String)}. But the value may also be a date or have some other format.
     * <p>
     * If this data set contains no value this function returns the empty string.
     * 
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the unit of this data set as a string.
     * <p>
     * If this data set contains no unit this function returns the empty string.
     * 
     * @return the unit
     */
    public String getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return "\"data set\": {\"address\": \"" + address + "\", \"value\": \"" + value + "\", \"unit\": \"" + unit
                + "\"}";
    }

}
