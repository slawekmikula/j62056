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

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;

import org.openmuc.j62056.internal.AcknowledgeMessage;
import org.openmuc.j62056.internal.AcknowledgeMode;
import org.openmuc.j62056.internal.Helper;
import org.openmuc.j62056.internal.IdentificationMessage;
import org.openmuc.j62056.internal.ProtocolControlCharacter;
import org.openmuc.j62056.internal.ProtocolMode;
import org.openmuc.j62056.internal.RequestMessage;
import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.SerialPort;
import org.openmuc.jrxtx.SerialPortBuilder;
import org.openmuc.jrxtx.StopBits;

/**
 * Represents a serial communication port that can be used to read meters using IEC 62056-21 modes A, B, C or D. Create
 * and open a port using {@link Builder}.
 * 
 */
public class Iec21Port {

    private final int baudRateChangeDelay;
    private final int initialBaudRateModeABC;
    private final int initialBaudRateModeD;
    private final int timeout;
    private final boolean verbose;
    private final boolean fixedBaudRate;

    private final SerialPort serialPort;
    private final DataOutputStream os;
    private final DataInputStream is;
    private final RequestMessage requestMessage;

    private ModeDListener listener = null;
    private boolean closed = false;

    private class ModeDReceiver extends Thread {

        @Override
        public void run() {
            while (!isClosed()) {
                try {
                    IdentificationMessage identificationMessage = new IdentificationMessage(is);
                    listener.newDataMessage(DataMessage.readModeDDataMessage(is, identificationMessage, serialPort));
                } catch (Exception e) {
                    if (isClosed()) {
                        break;
                    }
                    listener.exceptionWhileListening(e);

                    int numBytesInStream;
                    try {
                        numBytesInStream = is.available();
                        if (numBytesInStream > 0) {
                            byte[] bytesInStream = new byte[numBytesInStream];
                            is.read(bytesInStream);
                            if (verbose) {
                                Helper.debug("Cleared input stream because of exception. Bytes read from stream: ",
                                        bytesInStream);
                            }
                        }
                    } catch (IOException e1) {
                    }
                }
            }
        }

    }

    /**
     * A builder for Iec21Ports.
     * 
     */
    public static class Builder {

        private int baudRateChangeDelay = 0;
        // -1 indicates that the default initial baud rate should be used (i.e. 300 for modes A, B and C and 2400 for
        // mode D
        private int initialBaudrate = -1;
        private int timeout = 5000;
        private boolean verbose = false;
        private String deviceAddress = "";
        private boolean fixedBaudRate = false;
        private String requestStartCharacters = null;

        private final String serialPortName;

        /**
         * Create an Iec21Port builder.
         * 
         * @param serialPortName
         *            examples for serial port identifiers on Linux are "/dev/ttyS0" or "/dev/ttyUSB0" and on Windows
         *            "COM1"
         */
        public Builder(String serialPortName) {
            if (serialPortName == null) {
                throw new IllegalArgumentException("serialPort may not be NULL");
            }
            this.serialPortName = serialPortName;
        }

        /**
         * Set the time in ms to wait before changing the baud rate during message exchange. This parameter can usually
         * be set to zero for regular serial ports. If a USB to serial converter is used, you might have to use a delay
         * of around 250ms because otherwise the baud rate is changed before the previous message (i.e. the
         * acknowledgment) has been completely sent.
         * <p>
         * The default value is 0.
         * 
         * @param baudRateChangeDelay
         *            the baud rate change delay
         * @return the builder
         */
        public Builder setBaudRateChangeDelay(int baudRateChangeDelay) {
            this.baudRateChangeDelay = baudRateChangeDelay;
            return this;
        }

        /**
         * Set the initial baud rate.
         * <p>
         * The default is 300 baud for modes A, B, and C and 2400 baud for mode D. This function allows to change the
         * initial baud rate in case the meter does not use the default initial baud rate.
         * 
         * @param initialBaudrate
         *            the initial baud rate
         * @return the builder
         */
        public Builder setInitialBaudrate(int initialBaudrate) {
            this.initialBaudrate = initialBaudrate;
            return this;
        }

        /**
         * Set the maximum time in ms to wait for new data from the remote device. A timeout of zero is interpreted as
         * an infinite timeout.
         * <p>
         * The default value is 5000 (= 5 seconds).
         * 
         * @param timeout
         *            the maximum time in ms to wait for new data.
         * @return the builder
         */
        public Builder setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Set the device address which is transmitted as part of the request message that is sent to the meter.
         * <p>
         * The default value is the empty string.
         * 
         * @param deviceAddress
         *            the device address
         * @return the builder
         */
        public Builder setDeviceAddress(String deviceAddress) {
            this.deviceAddress = deviceAddress;
            return this;
        }

        /**
         * Sets the RequestMessage start characters.
         * <p>
         * Default value is: /? <br>
         * 
         * @param requestStartCharacters
         *            characters at the start of a RequestMessage
         * @return the builder
         */
        public Builder setRequestStartCharacters(String requestStartCharacters) {
            this.requestStartCharacters = requestStartCharacters;
            return this;
        }

        /**
         * Enable or disable verbose output to standard out.
         * <p>
         * Default is disabled.
         * 
         * @param verbose
         *            if true enable verbose mode
         * @return the builder
         */
        public Builder enableVerboseMode(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        /**
         * Enable a fixed baud rate.
         * <p>
         * In mode C communication starts with baud rate 300 and then by default changes to a baud rate suggested by the
         * meter. Enable a fixed baud rate if the baud rate shall NOT change.
         * 
         * @param fixedBaudRate
         *            if true enable fixed baud rate
         * @return the builder
         */
        public Builder enableFixedBaudrate(boolean fixedBaudRate) {
            this.fixedBaudRate = fixedBaudRate;
            return this;
        }

        /**
         * Build and open the Iec21Port.
         * 
         * @return the opened Iec21Port
         * 
         * @throws IOException
         *             if an error occurs while opening the associated serial port (e.g. when the serial port is
         *             occupied).
         */
        public Iec21Port buildAndOpen() throws IOException {
            return new Iec21Port(this);
        }

    }

    private Iec21Port(Builder builder) throws IOException {

        if (builder.initialBaudrate == -1) {
            initialBaudRateModeABC = 300;
            initialBaudRateModeD = 2400;
        }
        else {
            initialBaudRateModeABC = builder.initialBaudrate;
            initialBaudRateModeD = builder.initialBaudrate;
        }

        baudRateChangeDelay = builder.baudRateChangeDelay;
        timeout = builder.timeout;
        verbose = builder.verbose;
        requestMessage = new RequestMessage(builder.deviceAddress, builder.requestStartCharacters);
        fixedBaudRate = builder.fixedBaudRate;

        serialPort = SerialPortBuilder.newBuilder(builder.serialPortName)
                .setDataBits(DataBits.DATABITS_7)
                .setStopBits(StopBits.STOPBITS_1)
                .setParity(Parity.EVEN)
                .setBaudRate(initialBaudRateModeABC)
                .build();
        serialPort.setSerialPortTimeout(timeout);

        is = new DataInputStream(serialPort.getInputStream());
        os = new DataOutputStream(new BufferedOutputStream(serialPort.getOutputStream()));
    }

    /**
     * Closes this communication port. Also closes the associated serial port, input stream and output stream.
     * <p>
     * The port cannot be opened again but has to be recreated.
     */
    public void close() {
        closed = true;
        try {
            serialPort.close();
        } catch (IOException e) {
        }
    }

    /**
     * Requests meter data and returns the response.
     * <p>
     * Requests a data message from the remote device using IEC 62056-21 Mode A, B or C. The data message received is
     * parsed and returned. The returned data message also contains some information fields from the identification
     * message sent by the meter.
     * 
     * @return The response data message.
     * @throws IOException
     *             if any kind of IO error occurs
     * @throws InterruptedIOException
     *             if a timeout is thrown while waiting for the meter response
     */
    public DataMessage read() throws IOException, InterruptedIOException {

        if (isClosed()) {
            throw new IOException("Port is closed");
        }

        if (serialPort.getBaudRate() != initialBaudRateModeABC) {
            if (verbose) {
                Helper.debug("Changing baud rate from ", serialPort.getBaudRate(), " to ", initialBaudRateModeABC);
            }
            serialPort.setBaudRate(initialBaudRateModeABC);
        }

        int numBytesInStream = is.available();
        if (numBytesInStream > 0) {
            byte[] bytesInStream = new byte[numBytesInStream];
            is.read(bytesInStream);
            if (verbose) {
                Helper.debug("Cleared input stream. Bytes read from stream: ", bytesInStream);
            }
        }

        requestMessage.send(os);
        if (verbose) {
            Helper.debug("Sending ", requestMessage.toString());
        }

        IdentificationMessage identificationMessage = new IdentificationMessage(is);
        if (verbose) {
            Helper.debug("Received ", identificationMessage.toString());
        }

        if (identificationMessage.getProtocolMode() == ProtocolMode.C) {
            int baudRate = identificationMessage.getBaudRate();
            if (fixedBaudRate) {
                baudRate = serialPort.getBaudRate();
            }
            AcknowledgeMessage acknowledgeMessage = new AcknowledgeMessage(baudRate, ProtocolControlCharacter.NORMAL,
                    AcknowledgeMode.DATA_READOUT);

            if (verbose) {
                Helper.debug("Sending ", acknowledgeMessage.toString());
            }
            acknowledgeMessage.send(os);
            if (!fixedBaudRate && baudRateChangeDelay > 0) {
                if (verbose) {
                    Helper.debug("Sleeping for : ", baudRateChangeDelay, "ms before changing the baud rate");
                }
                try {
                    Thread.sleep(baudRateChangeDelay);
                } catch (InterruptedException e) {
                }
            }
        }

        if (identificationMessage.getProtocolMode() == ProtocolMode.B
                || (identificationMessage.getProtocolMode() == ProtocolMode.C && !fixedBaudRate)) {
            if (verbose) {
                Helper.debug("Changing baud rate from ", serialPort.getBaudRate(), " to ",
                        identificationMessage.getBaudRate());
            }
            serialPort.setBaudRate(identificationMessage.getBaudRate());
        }

        DataMessage dataMessage = DataMessage.readModeAbcDataMessage(is, identificationMessage);
        if (verbose) {
            Helper.debug("Received data message.");
        }

        if (serialPort.getBaudRate() != initialBaudRateModeABC) {
            if (verbose) {
                Helper.debug("Changing baud rate from ", serialPort.getBaudRate(), " to ", initialBaudRateModeABC);
            }
            serialPort.setBaudRate(initialBaudRateModeABC);
        }

        return dataMessage;
    }

    /**
     * Returns true if this port has been closed.
     * 
     * @return true if this port has been closed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Listen for mode D messages.
     * 
     * @param listener
     *            A listener for mode D messages
     * @throws IOException
     *             throws IOException
     */
    public void listen(ModeDListener listener) throws IOException {

        serialPort.setSerialPortTimeout(0);
        if (serialPort.getBaudRate() != initialBaudRateModeD) {
            if (verbose) {
                Helper.debug("Changing baud rate from ", serialPort.getBaudRate(), " to ", initialBaudRateModeD);
            }
            serialPort.setBaudRate(initialBaudRateModeD);
        }

        this.listener = listener;

        if (verbose) {
            Helper.debug("Starting to listen for mode D messages");
        }
        new ModeDReceiver().start();
    }

}
