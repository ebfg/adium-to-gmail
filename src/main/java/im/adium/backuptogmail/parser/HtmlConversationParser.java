package im.adium.backuptogmail.parser;

import com.google.common.base.Preconditions;
import im.adium.backuptogmail.model.Conversation;
import im.adium.backuptogmail.model.Message;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class HtmlConversationParser {
  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      new DateTimeFormatterBuilder().appendPattern("h:mm:ss a").toFormatter();

  private final String screenname;

  public HtmlConversationParser(String screenname) {
    this.screenname = Preconditions.checkNotNull(screenname);
  }

  public Conversation parse(Path path) throws IOException {
    Document document = Jsoup.parse(path.toFile(), "UTF8");
    LocalDate date = Util.getDate(path);
    List<Message> messages =
        document
            .select("div")
            .stream()
            .filter((e) -> e.hasClass("send") || e.hasClass("receive"))
            .map((e) -> getMessage(date, e))
            .collect(Collectors.toList());

    Set<String> participants =
        messages.stream().map(Message::getSender).collect(Collectors.toSet());
    participants.add(screenname);
    if (participants.size() == 1) {
      participants.add(Util.getReceipient(screenname, path));
    }

    return Conversation.create(participants, messages);
  }

  private Message getMessage(LocalDate date, Element div) {
    String senderVal = div.select(".sender").first().text();
    String sender = senderVal.substring(0, senderVal.indexOf(":"));
    if (sender.contains("(Autoreply")) {
      sender = sender.replace(" (Autoreply)", "");
    }
    LocalDateTime timestamp =
        date.atTime(LocalTime.parse(div.select(".timestamp").first().text(), TIMESTAMP_FORMATTER));
    String message = div.select(".message").first().text();
    return Message.create(sender, timestamp, message);
  }
}
