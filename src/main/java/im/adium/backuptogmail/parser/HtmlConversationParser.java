package im.adium.backuptogmail.parser;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class HtmlConversationParser {
  private static final DateTimeFormatter DATE_FORMATTER =
      new DateTimeFormatterBuilder().appendPattern("yyy|MM|dd").toFormatter();
  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      new DateTimeFormatterBuilder().appendPattern("h:mm:ss a").toFormatter();

  public static Conversation parse(Path path) throws IOException {
    Document document = Jsoup.parse(path.toFile(), "UTF8");
    LocalDate date = getDate(path);
    List<Message> messages =
        document
            .select("div")
            .stream()
            .filter((e) -> e.hasClass("send") || e.hasClass("receive"))
            .map((e) -> getMessage(date, e))
            .collect(Collectors.toList());

    Set<String> particpants = messages.stream().map(Message::getSender).collect(Collectors.toSet());

    return Conversation.create(particpants, messages);
  }

  private static LocalDate getDate(Path path) {
    Matcher matcher =
        Pattern.compile(".*\\((\\d{4}\\|\\d{2}\\|\\d{2})\\).*").matcher(path.toString());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Cannot extract date from " + path.toString());
    }
    return LocalDate.parse(matcher.group(1), DATE_FORMATTER);
  }

  private static Message getMessage(LocalDate date, Element div) {
    String senderVal = div.select(".sender").first().text();
    String sender = senderVal.substring(0, senderVal.indexOf(":"));
    LocalDateTime timestamp =
        date.atTime(LocalTime.parse(div.select(".timestamp").first().text(), TIMESTAMP_FORMATTER));
    String message = div.select(".message").first().text();
    return Message.create(sender, timestamp, message);
  }
}
