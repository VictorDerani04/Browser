import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextRenderer {
    private static final Pattern LINK_PATTERN = Pattern.compile("<a\\s+[^>]*?href\\s*=\\s*['\"]?([^'\"\\s>]+)['\"]?[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern HASH_TARGET_PATTERN = Pattern.compile("<(h[1-6]|a)\\s+[^>]*?(name|id)\\s*=\\s*['\"]?([^'\"\\s>]+)['\"]?[^>]*>", Pattern.CASE_INSENSITIVE);

    private BufferedReader reader;
    final private int rows;

    private boolean renderedUrl = false;

    private Queue<String> processedLines = new ArrayQueue<String>(1000);

    public TextRenderer(int rows) {
        this.rows = rows;
    }

    public List<String> links = new ArrayList<>();

    public boolean newPage(UrlInfo urlInfo) {
        try {
            Path filePath = Paths.get(urlInfo.path);
            List<String> rawLines = Files.readAllLines(filePath, StandardCharsets.ISO_8859_1);
            StringBuilder buffer = new StringBuilder();
            boolean foundHashTarget = false;

            // At this point we've successfully read the file. Clear existing state.
            links.clear();
            links.add(urlInfo.path);
            renderedUrl = false;
            processedLines.clear();

            // Combine all lines for processing
            for (String line : rawLines) {
                if (!foundHashTarget && urlInfo.hash != null) {
                    Matcher hashMatcher = HASH_TARGET_PATTERN.matcher(line);
                    while (hashMatcher.find()) {
                        String hashTarget = hashMatcher.group(3);
                        if (hashTarget.equals(urlInfo.hash)) {
                            foundHashTarget = true;
                            processedLines.clear();
                            buffer.setLength(0);
                            break;
                        }
                    }
                }

                if (foundHashTarget || urlInfo.hash == null) {
                    buffer.append(line).append(" ");
                }
            }

            // Process all content at once and split into lines
            String[] lines = processLine(buffer.toString()).split("\n");

            // Add non-empty lines to the queue
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    processedLines.enqueue(line);
                }
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean hasNextFrame() {
        return !processedLines.isEmpty();
    }

    public String nextFrame() {
        StringBuilder contentBuilder = new StringBuilder();
        int lineCount = 0;

        // Handle URL display first
        if (!renderedUrl && links.size() > 0) {
            String newUrl = links.get(0);
            String formattedURL = "\u001B[1m\u001B[32m" + newUrl + "\u001B[0m\n";
            contentBuilder.append(formattedURL);
            renderedUrl = true;
            lineCount++;
        }

        // Get up to 'rows' lines from the queue
        while (!processedLines.isEmpty() && lineCount < rows) {
            contentBuilder.append(processedLines.dequeue()).append("\n");
            lineCount++;
        }

        return contentBuilder.toString();
    }

    // Method to process a single line (handling tags, links, etc.)
    private String processLine(String line) {
        Matcher linkMatcher = LINK_PATTERN.matcher(line);
        StringBuilder processedLine = new StringBuilder();
        int linkCount = links.size();
        int lastMatchEnd = 0;  // Track the end of the last match

        while (linkMatcher.find()) {
            String url = linkMatcher.group(1);
            String linkText = linkMatcher.group(2);

            // Format the link text in blue with a numbered reference
            String blueLink = "\u001B[34m" + linkText + "\u001B[0m";
            String numberedLink = blueLink + "[" + linkCount + "]";

            // Append everything before the link and the numbered link itself
            processedLine.append(line, lastMatchEnd, linkMatcher.start()).append(numberedLink);

            // Update the last match end to the end of this match
            lastMatchEnd = linkMatcher.end();

            // Store the link for reference
            links.add(url);
            linkCount++;
        }

        // Append the remaining part of the line after the last match
        if (lastMatchEnd < line.length()) {
            processedLine.append(line.substring(lastMatchEnd));
        }

        // Replace paragraph and header tags with appropriate formatting
        processedLine = new StringBuilder(processedLine.toString()
                .replaceAll("(?i)<title>", "\u001B[45m\u001B[37;1m")
                .replaceAll("(?i)</title>", "\u001B[0m\n")
                .replaceAll("(?i)<p>", "\n\n")
                .replaceAll("(?i)<h1>", "\n* ")
                .replaceAll("(?i)<h2>", "\n** ")
                .replaceAll("(?i)<h3>", "\n*** ")
                .replaceAll("(?i)</h[1-6]>", "\n")
                .replaceAll("(?i)<dd>", "\n")
                .replaceAll("(?i)<dt>", "\t\n")
                .replaceAll("(?i)<ul>", "\n")
                .replaceAll("(?i)<li>", "\n - ")
        );

        // Remove any remaining HTML tags
        return TAG_PATTERN.matcher(processedLine.toString()).replaceAll("");
    }
}
