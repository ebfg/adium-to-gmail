package im.adium.backuptogmail;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import im.adium.backuptogmail.model.Conversation;
import im.adium.backuptogmail.model.Message;
import im.adium.backuptogmail.parser.HtmlConversationParser;
import im.adium.backuptogmail.parser.TextConversationParser;
import im.adium.backuptogmail.parser.XmlMessageParser;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public final class Main {
  private static final String LOG_BASE_PATH =
      "/Users/${USER}/Library/Application Support/Adium 2.0/Users/Default/Logs";
  private static final Address[] ADDRESS_ARRAY = new Address[0];

  public static void main(String[] args) throws Exception {
    String logBasePath = LOG_BASE_PATH.replace("${USER}", System.getenv("USER"));

    List<Path> aimPaths =
        Files.list(Paths.get(logBasePath))
            .filter((p) -> p.toFile().isDirectory() && p.getFileName().toString().startsWith("AIM"))
            .collect(Collectors.toList());
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props);

    for (Path path : aimPaths) {
      String screenname = path.getFileName().toString().split("\\.")[1];

      List<Conversation> unmergedConversations =
          Files.walk(path)
              .filter((p) -> p.toFile().isFile())
              .filter(
                  (p) ->
                      !p.toAbsolutePath().toString().contains("aolsystemmsg")
                          && !p.getFileName().endsWith(".DS_Store"))
              .map(
                  (p) -> {
                    try {
                      String filename = p.getFileName().toString();
                      if (filename.endsWith(").AdiumHTMLLog")) {
                        return new HtmlConversationParser(screenname).parse(p);
                      } else if (filename.endsWith("xml") || filename.endsWith("chatlog")) {
                        return new XmlMessageParser(screenname).parse(p);
                      } else {
                        return new TextConversationParser(screenname).parse(p);
                      }
                    } catch (Exception e) {
                      if (e.getCause() instanceof MalformedInputException) {
                        System.err.println(
                            String.format("Error processing %s, skipping: %s", p, e.getMessage()));
                      } else {
                        throw new RuntimeException(p.toString(), e);
                      }
                    }
                    return null;
                  })
              .filter((c) -> c != null)
              .collect(Collectors.toList());

      Multimap<Set<String>, Conversation> mappedConversations =
          Multimaps.index(unmergedConversations, Conversation::getPartipants);
      List<Conversation> conversations =
          mappedConversations
              .keySet()
              .stream()
              .map((k) -> Conversation.merge(mappedConversations.get(k)))
              .collect(Collectors.toList());

      System.out.println(path.getFileName().toString());

      try {
        //List<MimeMessage> messages =
        conversations
            .stream()
            .filter((c) -> c.getMessages().size() > 0)
            .map((c) -> convertToMessages(screenname, session, c))
            .forEach(
                (ms) -> {
                  ms.forEach(
                      (m) -> {
                        try {
                          m.writeTo(System.err);
                        } catch (Exception e) {
                          throw new RuntimeException(e);
                        }
                      });
                });
      } catch (Exception e) {
        throw new RuntimeException(path.toString(), e);
      }
    }
  }

  private static List<MimeMessage> convertToMessages(
      String screenname, Session session, Conversation conversation) {
    Set<String> others = new HashSet<>(conversation.getPartipants());
    others.remove(screenname);
    String subject = String.format("Conversation with %s", String.join(", ", others));

    List<MimeMessage> messages = new ArrayList<>(conversation.getMessages().size());
    try {
      for (Message message : conversation.getMessages()) {
        List<Address> recipients =
            conversation
                .getPartipants()
                .stream()
                .filter((a) -> !a.equals(message.getSender()))
                .map(Main::toAddress)
                .collect(Collectors.toList());
        if (recipients.size() == 0) {
          recipients.add(toAddress(screenname));
        }
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setRecipients(
            javax.mail.Message.RecipientType.TO, recipients.toArray(ADDRESS_ARRAY));
        mimeMessage.setFrom(new InternetAddress(message.getSender(), false));
        mimeMessage.setSubject(subject);
        mimeMessage.setSentDate(
            Date.from(message.getTime().atZone(ZoneId.systemDefault()).toInstant()));
        BodyPart body = new MimeBodyPart();
        body.setText(message.getMessage());
        Multipart content = new MimeMultipart();
        content.addBodyPart(body);
        mimeMessage.setContent(content);
        messages.add(mimeMessage);
      }
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
    return messages;
  }

  private static Address toAddress(String address) {
    try {
      //      if (address.contains("Autoreply")) {
      //        throw new RuntimeException();
      //      }
      return new InternetAddress(address.replaceAll("\\s+", ""), false);
    } catch (AddressException e) {
      throw new RuntimeException(e);
    }
  }
}
