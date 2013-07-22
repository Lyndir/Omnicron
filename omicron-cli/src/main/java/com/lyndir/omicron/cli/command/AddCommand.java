package com.lyndir.omicron.cli.command;

import com.lyndir.omicron.cli.OmicronCLI;


/**
 * <i>10 07, 2012</i>
 *
 * @author lhunath
 */
@CommandGroup(name = "add", abbr = "a", desc = "Add objects to properties of a built object.")
public class AddCommand extends Command {

    public AddCommand(final OmicronCLI omicron) {
        super( omicron );
    }
}
