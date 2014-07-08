package gr.csdashes.formattransform;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.GraphParseException;
import org.graphstream.stream.file.FileSink;
import org.graphstream.stream.file.FileSinkGML;

/**
 *
 * @author tasos
 */
public class Main {

    private static String join(Collection<Edge> s, String delimiter, Node n) {
        StringBuilder builder = new StringBuilder(50);
        Iterator<Edge> iter = s.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next().getOpposite(n).getId());
            if (!iter.hasNext()) {
                break;
            }
            builder.append(delimiter);
        }
        return builder.toString();
    }

    @SuppressWarnings("static-access")
    public static Options GetMenuOptions() {
        Options options = new Options();

        // TODO: Group input and output sections and make them mandatory
        // TODO: Add <format-transform [INPUT] [OUTPUT]> -like menu
        options.addOption(OptionBuilder.withLongOpt("input-gml")
                .withDescription("Set a GML file as an input source")
                .hasArg()
                .withArgName("file")
                .create("ig"));
        options.addOption(OptionBuilder.withLongOpt("input-hama-txt")
                .withDescription("Set a Hama text file as an input source")
                .hasArg()
                .withArgName("file")
                .create("ih"));

        options.addOption(OptionBuilder.withLongOpt("output-gml")
                .withDescription("Set a GML file as an output target")
                .hasArg()
                .withArgName("file")
                .create("og"));
        options.addOption(OptionBuilder.withLongOpt("output-hama-txt")
                .withDescription("Set a Hama text file as an output target")
                .hasArg()
                .withArgName("file")
                .create("oh"));

        options.addOption("h", "help", false, "Print this message");

        return options;
    }

    public static void main(String[] args) throws IOException, GraphParseException {
        CommandLineParser parser = new GnuParser();
        try {
            // parse the command line arguments
            CommandLine cliArgs = parser.parse(GetMenuOptions(), args);

            // Print help and exit
            if (args.length == 0 || cliArgs.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("format-transform", GetMenuOptions());
                System.exit(0);
            }

            // TODO: create an input and output interface and generalize the
            // framework
            if (cliArgs.hasOption("ig") && cliArgs.hasOption("oh")) {
                // Init input
                Graph graph = new DefaultGraph("tmp graph");
                graph.read(cliArgs.getOptionValue("ig"));

                // Init output
                Path filePath = Paths.get(cliArgs.getOptionValue("oh"));
                Charset charset = Charset.forName("utf-8");

                try (BufferedWriter writer = Files.newBufferedWriter(filePath, charset)) {
                    // Start exchange
                    for (Node n : graph) {
                        String line = n.getId() + " " + join(n.getEdgeSet(), " ", n);
                        writer.write(line, 0, line.length());
                        writer.newLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace(System.out);
                }
            }

            if (cliArgs.hasOption("ih") && cliArgs.hasOption("og")) {
                // Init output
                Graph graph = new DefaultGraph("tmp graph", false, true);

                // Init input
                Path filePath = Paths.get(cliArgs.getOptionValue("ih"));
                // TODO: would be great to have a data stream that would be
                // tokenized on the fly, and not lines and then split
                Files.lines(filePath).forEach((line) -> {
                    // Start exchange
                    String[] tokens = line.split(" ");
                    for (int i = 1; i < tokens.length; i++) {
                        graph.addEdge(tokens[0] + " " + tokens[i], tokens[0], tokens[i]);
                    }
                });

                // Write output
                FileSink fs = new FileSinkGML();
                fs.writeAll(graph, cliArgs.getOptionValue("og"));
            }
        } catch (ParseException exp) {
            // oops, something went wrong
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        }
    }
}
