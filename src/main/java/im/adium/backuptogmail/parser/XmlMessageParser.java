package im.adium.backuptogmail.parser;

import im.adium.backuptogmail.model.Conversation;
import im.adium.backuptogmail.model.Message;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class XmlMessageParser {

  private static final DateTimeFormatter ISO_OFFSET_DATE_TIME =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
          .appendOffset("+HHMM", "")
          .toFormatter();

  public static Conversation parse(Path path) throws IOException {
    Document document = Jsoup.parse(path.toFile(), "UTF8");
    List<Message> messages =
        document
            .select("message")
            .stream()
            .map(XmlMessageParser::getMessage)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    Set<String> particpants = messages.stream().map(Message::getSender).collect(Collectors.toSet());
    return Conversation.create(particpants, messages);
  }

  private static Optional<Message> getMessage(Element messageElem) {
    Element message = messageElem.select("span").first();
    if (message == null) {
      return Optional.empty();
    }
    String sender = messageElem.attr("sender");
    LocalDateTime timestamp;
    try {
      timestamp = LocalDateTime.parse(messageElem.attr("time"), ISO_OFFSET_DATE_TIME);
    } catch (DateTimeParseException e) {
      timestamp =
          LocalDateTime.parse(messageElem.attr("time"), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    return Optional.of(Message.create(sender, timestamp, message.text()));
  }
}
