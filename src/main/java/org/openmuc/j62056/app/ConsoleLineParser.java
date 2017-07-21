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

import java.util.ArrayList;
import java.util.List;

import org.openmuc.j62056.internal.cli.CliParameter;
import org.openmuc.j62056.internal.cli.CliParameterBuilder;
import org.openmuc.j62056.internal.cli.CliParseException;
import org.openmuc.j62056.internal.cli.CliParser;
import org.openmuc.j62056.internal.cli.FlagCliParameter;
import org.openmuc.j62056.internal.cli.IntCliParameter;
import org.openmuc.j62056.internal.cli.StringCliParameter;

class ConsoleLineParser {
    private final CliParser cliParser;

    public final StringCliParameter serialPortName = new CliParameterBuilder("-p")
            .setDescription(
                    "The serial port used for communication. Examples are /dev/ttyS0 (Linux) or COM1 (Windows).")
            .setMandatory()
            .buildStringParameter("serial_port");

    public final IntCliParameter initialBaudRate = new CliParameterBuilder("-b")
            .setDescription("Initial baud rate of the serial port.").buildIntParameter("baud_rate", 300);

    public final IntCliParameter baudRateChangeDelay = new CliParameterBuilder("-d")
            .setDescription(
                    "Delay of baud rate change in ms. USB to serial converters often require a delay of up to 250ms.")
            .buildIntParameter("baud_rate_change_delay", 0);

    public final IntCliParameter timeout = new CliParameterBuilder("-t").setDescription("The timeout in ms.")
            .buildIntParameter("timeout", 5000);

    public final FlagCliParameter verbose = new CliParameterBuilder("-v")
            .setDescription("Enable verbose mode to print debug messages to standard out.").buildFlagParameter();

    public final FlagCliParameter fixedBaudRate = new CliParameterBuilder("-f")
            .setDescription(
                    "Fixed baud rate. In mode C the baud rate starts with 300 and then by default changes to a value suggested by the meter. The baud rate will NOT be changed with this flag enabled.")
            .buildFlagParameter();

    public final FlagCliParameter listen = new CliParameterBuilder("-l")
            .setDescription("Listen for mode D messages instead of reading the meter using mode A, B or C.")
            .buildFlagParameter();

    public final StringCliParameter deviceAddress = new CliParameterBuilder("-a")
            .setDescription("The device address that is sent with the request message in modes A, B, and C.")
            .buildStringParameter("device_address", "");

    public final StringCliParameter requestStartCharacters = new CliParameterBuilder("-rsc")
            .setDescription("Set the request message start characters.")
            .buildStringParameter("request_start_character", "/?");

    ConsoleLineParser() {
        List<CliParameter> parameters = new ArrayList<>();
        parameters.add(serialPortName);
        parameters.add(initialBaudRate);
        parameters.add(baudRateChangeDelay);
        parameters.add(timeout);
        parameters.add(verbose);
        parameters.add(fixedBaudRate);
        parameters.add(listen);
        parameters.add(deviceAddress);
        parameters.add(requestStartCharacters);

        cliParser = new CliParser("j62056-console-client",
                "IEC 62056-21 client application to read meters using modes A, B, C or D");
        cliParser.addParameters(parameters);
    }

    public void parse(String[] args) throws CliParseException {
        cliParser.parseArguments(args);
    }

    public void printUsage() {
        System.out.println(cliParser.getUsageString());
    }

}
