package im.adium.backuptogmail.parser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Util {
  private static final List<String> REGEXES =
      Arrays.asList(".*(\\d{4}\\|\\d{2}\\|\\d{2}).*", ".*(\\d{4}-\\d{2}-\\d{2}).*");
  private static final List<DateTimeFormatter> DATE_FORMATTERS =
      Arrays.asList(
          new DateTimeFormatterBuilder().appendPattern("yyyy|MM|dd").toFormatter(),
          new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd").toFormatter());

  static LocalDate getDate(Path path) {
    try {
      return getDate(path.toString());
    } catch (IllegalArgumentException e) {
      // Fall back to trying to read the first line
      try {
        String firstLine = Files.lines(path, StandardCharsets.ISO_8859_1).findFirst().get();
        return getDate(firstLine);
      } catch (Exception inner) {
        throw e;
      }
    }
  }

  private static LocalDate getDate(String string) {
    Matcher matcher =
        REGEXES
            .stream()
            .map((r) -> Pattern.compile(r).matcher(string))
            .filter(Matcher::matches)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Cannot extract date from " + string));

    for (DateTimeFormatter formatter : DATE_FORMATTERS) {
      try {
        return LocalDate.parse(matcher.group(1), formatter);
      } catch (DateTimeParseException ignored) {

      }
    }
    throw new IllegalArgumentException("Failed to extract date from " + string);
  }
}
