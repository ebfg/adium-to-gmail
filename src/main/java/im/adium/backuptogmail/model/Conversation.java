package im.adium.backuptogmail.model;

import com.google.auto.value.AutoValue;
import java.util.List;
import java.util.Set;

@AutoValue
public abstract class Conversation {

  public static Conversation create(Set<String> participants, List<Message> messages) {
    return new AutoValue_Conversation(participants, messages);
  }

  public abstract Set<String> getPartipants();

  public abstract List<Message> getMessages();
}
