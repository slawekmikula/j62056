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
import java.util.ArrayList;
import java.util.List;

import org.openmuc.j62056.internal.Bcc;
import org.openmuc.j62056.internal.Helper;
import org.openmuc.j62056.internal.HexConverter;
import org.openmuc.j62056.internal.IdentificationMessage;
import org.openmuc.jrxtx.SerialPort;

/**
 * Represents the data sent by the meter.
 * 
 * The data consists of the manufacturer ID, the meter ID (optional), the enhanced ID/capability (optional), and a list
 * of data sets.
 *
 */
public class DataMessage {

    private final String manufacturerId;
    private final String meterId;
    private final String enhancedId;
    private final List<DataSet> dataSets;

    private DataMessage(String manufacturerId, String meterId, String enhancedId, List<DataSet> dataSets) {
        this.manufacturerId = manufacturerId;
        this.meterId = meterId;
        this.enhancedId = enhancedId;
        this.dataSets = dataSets;
    }

    // Mode A,B or C Data message ::= 'STX'(0x02) <data block> '!' '\r'(0x0D) '\n'(0x0A) 'ETX'(0x03) BCC
    // Data block ::= List of data sets separated by CR and LF, Optionally the data block ends with a CR and LF
    // Data set ::= Address '(' Value(optional) ('*' unit)(optional) ')'
    // BCC = block check character

    static DataMessage readModeAbcDataMessage(DataInputStream is, IdentificationMessage identificationMessage)
            throws IOException {

        byte b = is.readByte();
        if (b != 0x02) {
            throw new IOException("Received unexpected data message start byte: " + HexConverter.toShortHexString(b));
        }

        Bcc bcc = new Bcc();

        List<DataSet> dataSets = new ArrayList<>();
        DataSet dataSet;
        while ((dataSet = DataSet.readDataSet(is, bcc)) != null) {
            dataSets.add(dataSet);
        }

        b = is.readByte();
        if (b != '\r') {
            throw new IOException("Received unexpected byte at end of data message: " + HexConverter.toShortHexString(b)
                    + ", expected: '\r'(");
        }
        b = is.readByte();
        if (b != '\n') {
            throw new IOException("Received unexpected byte at end of data message: " + HexConverter.toShortHexString(b)
                    + ", expected: '\n'");
        }
        b = is.readByte();
        if (b != 0x03) {
            throw new IOException("Received unexpected byte at end of data message: " + HexConverter.toShortHexString(b)
                    + ", expected: 0x03");
        }

        bcc.value ^= '\r' ^ '\n' ^ 0x03;

        b = is.readByte();

        if (b != bcc.value) {
            throw new IOException("Block check character (BCC) does not match. Received: " + HexConverter.toHexString(b)
                    + ", expected: " + HexConverter.toHexString(bcc.value));
        }

        return new DataMessage(identificationMessage.getManufactureId(), identificationMessage.getMeterId(),
                identificationMessage.getEnhancedId(), dataSets);

    }

    // Mode D data message: '\r' '\n' <data block> '!' '\r' '\n'

    static DataMessage readModeDDataMessage(DataInputStream is, IdentificationMessage identificationMessage,
            SerialPort serialPort) throws IOException {
        byte b = is.readByte();
        if (b != '\r') {
            throw new IOException("Received unexpected byte at beginning of data message: "
                    + HexConverter.toShortHexString(b) + ", expected: '\r'(");
        }

        serialPort.setSerialPortTimeout(Helper.FRAGMENT_TIMEOUT);
        try {

            b = is.readByte();
            if (b != '\n') {
                throw new IOException("Received unexpected byte at beginning of data message: "
                        + HexConverter.toShortHexString(b) + ", expected: '\n'");
            }
            List<DataSet> dataSets = new ArrayList<>();
            DataSet dataSet;
            while ((dataSet = DataSet.readDataSet(is, new Bcc())) != null) {
                dataSets.add(dataSet);
            }
            b = is.readByte();
            if (b != '\r') {
                throw new IOException("Received unexpected byte at end of data message: "
                        + HexConverter.toShortHexString(b) + ", expected: '\r'(");
            }
            b = is.readByte();
            if (b != '\n') {
                throw new IOException("Received unexpected byte at end of data message: "
                        + HexConverter.toShortHexString(b) + ", expected: '\n'");
            }
            return new DataMessage(identificationMessage.getManufactureId(), identificationMessage.getMeterId(),
                    identificationMessage.getEnhancedId(), dataSets);

        } finally {
            serialPort.setSerialPortTimeout(0);
        }
    }

    /**
     * Returns the manufacturer identification of this data message.
     * 
     * @return the manufacturer identification
     */
    public String getManufacturerId() {
        return manufacturerId;
    }

    /**
     * Returns the identification string (except for the enhanced identification characters).
     * 
     * @return the identification string
     */
    public String getMeterId() {
        return meterId;
    }

    /**
     * Returns the enhanced identification/capability characters as a string.
     * 
     * @return the enhanced identification/capability characters
     */
    public String getEnhancedId() {
        return enhancedId;
    }

    /**
     * Returns the data sets of this data message.
     * 
     * @return the data sets
     */
    public List<DataSet> getDataSets() {
        return dataSets;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{\n\t\"data message\": {\n\t\t\"manufacturer ID\": \"")
                .append(manufacturerId)
                .append("\",\n\t\t\"meter ID\": \"")
                .append(meterId)
                .append("\",\n\t\t\"enhanced ID/capability\": \"")
                .append(enhancedId)
                .append("\"")
                .append(IdentificationMessage.getEnhancedIdDescription(enhancedId))
                .append(",\n\t\t\"data block\": {");
        for (DataSet dataSet : dataSets) {
            sb.append("\n\t\t\t").append(dataSet.toString()).append(',');
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("\n\t\t}\n\t}\n}");
        return sb.toString();
    }

}
