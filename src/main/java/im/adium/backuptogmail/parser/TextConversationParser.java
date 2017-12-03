package im.adium.backuptogmail.parser;

import im.adium.backuptogmail.model.Conversation;
import im.adium.backuptogmail.model.Message;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextConversationParser {
  public static Conversation parse(Path path) throws IOException {
    LocalDate date = Util.getDate(path);
    Set<String> participants = new HashSet<>();
    List<Message> messages;
    try (Stream<String> stream = Files.lines(path, StandardCharsets.ISO_8859_1)) {
      Pattern pattern = Pattern.compile("\\((\\d{2}:\\d{2}:\\d{2})\\)(\\w*)(:)(.*)");
      messages =
          stream
              .map(pattern::matcher)
              .filter((p) -> p.matches())
              .map(
                  (p) -> {
                    String sender = p.group(2);
                    participants.add(sender);
                    LocalDateTime time = date.atTime(LocalTime.parse(p.group(1)));
                    String message = p.group(4);
                    return Message.create(sender, time, message);
                  })
              .collect(Collectors.toList());
    }

    return Conversation.create(participants, messages);
  }
}
