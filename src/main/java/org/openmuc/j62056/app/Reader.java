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
package org.openmuc.j62056.app;

import java.io.IOException;
import java.io.InterruptedIOException;

import org.openmuc.j62056.DataMessage;
import org.openmuc.j62056.Iec21Port;
import org.openmuc.j62056.ModeDListener;
import org.openmuc.j62056.internal.cli.CliParseException;

/**
 * Application to read IEC 62056-21 meters (using modes A, B and C).
 *
 */
public class Reader {

    private static class ModeDListenerImpl implements ModeDListener {

        @Override
        public void newDataMessage(DataMessage dataMessage) {
            System.out.println("Received " + dataMessage);
        }

        @Override
        public void exceptionWhileListening(Exception e) {
            System.err.println("IOException while listening for messages: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {

        ConsoleLineParser cliParser = new ConsoleLineParser();
        try {
            cliParser.parse(args);
        } catch (CliParseException e) {
            System.err.println("Error parsing command line parameters: " + e.getMessage());
            cliParser.printUsage();
            System.exit(1);
        }

        Iec21Port iec21Port = null;
        try {
            iec21Port = new Iec21Port.Builder(cliParser.serialPortName.getValue())
                    .setBaudRateChangeDelay(cliParser.baudRateChangeDelay.getValue())
                    .setTimeout(cliParser.timeout.getValue())
                    .setInitialBaudrate(cliParser.initialBaudRate.getValue())
                    .enableVerboseMode(cliParser.verbose.isSelected())
                    .enableFixedBaudrate(cliParser.fixedBaudRate.isSelected())
                    .setDeviceAddress(cliParser.deviceAddress.getValue())
                    .setRequestStartCharacters(cliParser.requestStartCharacters.getValue())
                    .buildAndOpen();
        } catch (IOException e) {
            System.err.println("Failed to open serial port: " + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("Illegal parameter value: " + e.getMessage());
            cliParser.printUsage();
            System.exit(1);
        }

        final Iec21Port iec21PortFinalRef = iec21Port;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                iec21PortFinalRef.close();
            }
        });

        DataMessage dataMessage = null;

        if (cliParser.listen.isSelected()) {
            try {
                iec21Port.listen(new ModeDListenerImpl());
            } catch (IOException e) {
                System.err.println("IOException while starting to listen: " + e.getMessage());
                System.exit(1);
            }
            return;
        }

        try {
            dataMessage = iec21Port.read();
        } catch (InterruptedIOException e) {
            System.err.println("Read attempt timed out.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("IOException while trying to read: " + e.getMessage());
            System.exit(1);
        }
        System.out.println("Received \n" + dataMessage);

    }

}
