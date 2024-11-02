import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UrlInfo {
    final public String path;
    final public String hash;

    // Constructor to initialize path and hash
    public UrlInfo(String path, String hash) {
        this.path = path;
        this.hash = hash;
    }

    public String toString() {
        return path + "#" + hash;
    }

    public static UrlInfo navigate(String currentPath, String link) {
        String page = link;
        String hash = null;

        // Check if the link is absolute and starts with "http://"
        if (link.startsWith("http://")) {
            page = link.substring(7); // Remove "http://"
        } else if (link.startsWith("https://")) {
            page = link.substring(8); // Remove "http://"
        } else if (link.startsWith("info.cern.ch")) {
            // It's an absolute URL, but no need to change it
            page = link;
        } else {
            // It's a relative URL, so build the full URL
            String[] currentParts = currentPath.split("/");
            String[] linkParts = link.split("/");
            List<String> resultParts = new ArrayList<>(Arrays.asList(currentParts));
            resultParts.remove(resultParts.size() - 1); // Remove the current page from the path

            for (String part : linkParts) {
                if (part.equals("..")) {
                    if (!resultParts.isEmpty()) {
                        resultParts.remove(resultParts.size() - 1);
                    }
                } else if (!part.equals(".")) {
                    resultParts.add(part);
                }
            }

            page = String.join("/", resultParts);
        }

        // Check if the URL contains a hash part (e.g., "People.html#BernersLee")
        int hashIndex = page.indexOf("#");
        if (hashIndex != -1) {
            // Split the page into the URL part and the hash part
            hash = page.substring(hashIndex + 1); // Get the part after '#'
            page = page.substring(0, hashIndex);  // Get the part before '#'
        }

        // Return the UrlInfo object containing the page and hash
        return new UrlInfo(page, hash);
    }
}
