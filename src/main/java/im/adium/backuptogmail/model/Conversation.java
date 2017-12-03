package im.adium.backuptogmail.model;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
public abstract class Conversation {

  @SuppressWarnings("unchecked")
  private static final Set<String>[] SET_OF_STRING_ARRAY = new Set[0];

  public static Conversation create(Set<String> participants, List<Message> messages) {
    return new AutoValue_Conversation(participants, messages);
  }

  public static Conversation merge(Collection<Conversation> conversations) {
    Set<Set<String>> particpants =
        conversations.stream().map(Conversation::getPartipants).collect(Collectors.toSet());
    Preconditions.checkArgument(particpants.size() == 1);
    List<Message> messages = new ArrayList<>();
    conversations.forEach((c) -> messages.addAll(c.getMessages()));
    messages.sort(Comparator.comparing(Message::getTime));
    return new AutoValue_Conversation(particpants.toArray(SET_OF_STRING_ARRAY)[0], messages);
  }

  public abstract Set<String> getPartipants();

  public abstract List<Message> getMessages();
}
