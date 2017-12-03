package im.adium.backuptogmail;

import im.adium.backuptogmail.parser.HtmlConversationParser;
import im.adium.backuptogmail.parser.TextConversationParser;
import im.adium.backuptogmail.parser.XmlMessageParser;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public final class Main {
  private static final String LOG_BASE_PATH =
      "/Users/${USER}/Library/Application Support/Adium 2.0/Users/Default/Logs";

  public static void main(String[] args) throws Exception {
    String logBasePath = LOG_BASE_PATH.replace("${USER}", System.getenv("USER"));

    List<Path> screennames =
        Files.list(Paths.get(logBasePath))
            .filter((p) -> p.toFile().isDirectory() && p.getFileName().toString().startsWith("AIM"))
            .collect(Collectors.toList());
    for (Path path : screennames) {
      Files.walk(path)
          .filter((p) -> p.toFile().isFile())
          .filter(
              (p) ->
                  !p.toAbsolutePath().toString().contains("aolsystemmsg")
                      && !p.getFileName().endsWith(".DS_Store"))
          .forEach(
              (p) -> {
                try {
                  String filename = p.getFileName().toString();
                  if (filename.endsWith(").AdiumHTMLLog")) {
                    HtmlConversationParser.parse(p);
                  } else if (filename.endsWith("xml") || filename.endsWith("chatlog")) {
                    XmlMessageParser.parse(p);
                  } else {
                    TextConversationParser.parse(p);
                  }
                } catch (Exception e) {
                  if (e.getCause() instanceof MalformedInputException) {
                    System.err.println(
                        String.format("Error processing %s, skipping: %s", p, e.getMessage()));
                  } else {
                    throw new RuntimeException(p.toString(), e);
                  }
                }
              });
    }
  }
}
