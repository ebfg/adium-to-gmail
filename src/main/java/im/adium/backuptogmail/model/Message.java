package im.adium.backuptogmail.model;

import com.google.auto.value.AutoValue;
import java.time.LocalDateTime;

@AutoValue
public abstract class Message {
  public static Message create(String sender, LocalDateTime time, String message) {
    return new AutoValue_Message(sender.toLowerCase(), time, message);
  }

  public abstract String getSender();

  public abstract LocalDateTime getTime();

  public abstract String getMessage();
}
