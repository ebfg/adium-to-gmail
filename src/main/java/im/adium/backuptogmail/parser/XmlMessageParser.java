package im.adium.backuptogmail.parser;

import com.google.common.base.Preconditions;
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

  private final String screenname;

  public XmlMessageParser(String screenname) {
    this.screenname = Preconditions.checkNotNull(screenname);
  }

  public Conversation parse(Path path) throws IOException {
    Document document = Jsoup.parse(path.toFile(), "UTF8");
    List<Message> messages =
        document
            .select("message")
            .stream()
            .map(this::getMessage)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    Set<String> participants =
        messages.stream().map(Message::getSender).collect(Collectors.toSet());
    participants.add(screenname);
    if (participants.size() == 1) {
      participants.add(Util.getReceipient(screenname, path));
    }
    return Conversation.create(participants, messages);
  }

  private Optional<Message> getMessage(Element messageElem) {
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
