package im.adium.backuptogmail.parser;

import im.adium.backuptogmail.model.Conversation;
import im.adium.backuptogmail.model.Message;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextConversationParser {
  public static Conversation parse(Path path) throws IOException {
    LocalDate date;
    Set<String> particpants = new HashSet<>();
    List<Message> messages;
    try (Stream<String> stream = Files.lines(path)) {
      date = LocalDate.parse(stream.findFirst().get().split(" ")[1]);
    }
    try (Stream<String> stream = Files.lines(path)) {
      messages =
          stream
              .filter((l) -> l.startsWith("("))
              .map(
                  (l) -> {
                    String sender = getSender(l);
                    particpants.add(sender);
                    LocalDateTime time = getMessageTime(date, l);
                    String message = getMessage(l);
                    return Message.create(sender, time, message);
                  })
              .collect(Collectors.toList());
    }

    return Conversation.create(particpants, messages);
  }

  private static String getSender(String line) {
    return line.substring(10, line.indexOf(':', 11));
  }

  private static LocalDateTime getMessageTime(LocalDate date, String line) {
    String time = line.substring(1, 9);
    return date.atTime(LocalTime.parse(time));
  }

  private static String getMessage(String line) {
    return line.substring(line.indexOf(':', 11) + 1);
  }
}
