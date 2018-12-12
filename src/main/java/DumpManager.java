import components.MaskerPane;
import javafx.application.Platform;
import javafx.scene.text.Text;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class DumpManager {

    public CharBuffer data;
    public ArrayList<LineIndexesMeta> lineIndexesMetas;
    public boolean dumpLoaded = false;
    public boolean regex = false;
    public boolean caseSensitive = false;
    public boolean loadRequired = true;

    public DumpManager() {

    }

    public List<LineIndexesMeta> search(DumpManager dumpManager, String text) {
        if (text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        while (!dumpLoaded) { // waiting for dump to be loaded
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        String[] terms = text.split(" ");

        Pattern compile = null;
        if (regex) {
            try {
                compile = Pattern.compile(text);
            } catch (PatternSyntaxException e) {
                return new ArrayList<>();
            }
        }
        Pattern finalCompile = compile;

        return dumpManager.lineIndexesMetas.parallelStream()
                .filter(lineIndexesMeta -> lineIndexesMeta.secondQuote != -1)
                .filter(lineIndexesMeta -> {
                    if (!regex) {
                        return lineContainsTerms(dumpManager.data, lineIndexesMeta, terms);
                    } else {
                        if (finalCompile != null) {
                            return finalCompile.matcher(dumpManager.data.subSequence(lineIndexesMeta.firstQuote + 1,
                                    lineIndexesMeta.secondQuote)).find();
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    public boolean loadDump(File dumpFile, Text statusText, MaskerPane maskerPane) {
        try {
            Platform.runLater(() -> {
                statusText.setText("Status: Loading Dump");
                maskerPane.setText("Reading dump into memory");
            });

            FileChannel fileChannel = new RandomAccessFile(dumpFile, "r").getChannel();
            MappedByteBuffer memoryMappedFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());

            data = StandardCharsets.UTF_8.newDecoder().decode(memoryMappedFile);
            //
            // unmap(memoryMappedFile);

            Platform.runLater(() -> {
                statusText.setText("Status: Parsing Dump");
                maskerPane.setText("Parsing dump");
            });

            lineIndexesMetas = new ArrayList<>();

            parseData(data, lineIndexesMetas);

            dumpLoaded = true;

            Platform.runLater(() -> statusText.setText("Status: Ready"));

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private boolean lineContainsTerms(CharBuffer data, LineIndexesMeta lineIndexesMeta, String[] terms) {
        for (String term : terms) {
            if (indexOf(data, lineIndexesMeta.firstQuote + 1, lineIndexesMeta.secondQuote, term.toCharArray()) == -1) {
                return false;
            }
        }
        return true;
    }

    private int indexOf(CharBuffer data, int startIndex, int endIndex, char[] target) {
        label28:
        for (int i = startIndex; i < endIndex - target.length + 1; ++i) {
            for (int j = 0; j < target.length; ++j) {
                char dataC = data.get(i + j);
                char targetC = target[j];

                if (caseSensitive) {
                    if (dataC != targetC) {
                        continue label28;
                    }
                } else if (Character.toLowerCase(dataC) != Character.toLowerCase(targetC)) {
                    continue label28;
                }
            }
            return i;
        }

        return -1;
    }


    private static void parseData(CharBuffer data, ArrayList<LineIndexesMeta> lineIndexesMetas) {
        short length = 0;
        int startIndex = 0;
        int firstQuote = -1;
        int secondQuote = -1;

        for (int i = 0; i < data.length(); i++) {
            char c = data.get(i);

            if (c == '\r') {
                continue;
            }
            if (c == '\n') {
                if (length != 0) {
                    lineIndexesMetas.add(new LineIndexesMeta(startIndex, length, firstQuote, secondQuote));

                    length = 0;
                    firstQuote = -1;
                    secondQuote = -1;
                }
                startIndex = i + 1;
            } else {
                if (c == '\"') {
                    if (firstQuote == -1) {
                        firstQuote = i;
                    } else {
                        secondQuote = i;
                    }
                }
                length++;
            }
        }
    }

    public static class LineIndexesMeta {
        public int startIndex;
        public short length;
        public int firstQuote;
        public int secondQuote;

        LineIndexesMeta(int startIndex, short length, int firstQuoteIndex, int secondQuoteIndex) {
            this.startIndex = startIndex;
            this.length = length;
            this.firstQuote = firstQuoteIndex;
            this.secondQuote = secondQuoteIndex;
        }
    }

}
