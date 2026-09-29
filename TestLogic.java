import java.util.regex.*;

public class TestLogic {
    public static String unescapeMarkdown(String text) {
        return text.replace("\\*", "*")
                   .replace("\\_", "_")
                   .replace("\\~", "~")
                   .replace("\\`", "`")
                   .replace("\\[", "[")
                   .replace("\\]", "]")
                   .replace("\\(", "(")
                   .replace("\\)", ")")
                   .replace("\\\\", "\\");
    }

    public static void main(String[] args) {
        String text = "Take on high-visibility projects. <br>• Consider switching\\*";
        text = text.replaceAll("(?i)<br\\s*/?>", "\n");
        
        String patternStr = "(?<!\\\\)(\\*\\*\\*(.+?)(?<!\\\\)\\*\\*\\*|(?<!\\\\)___(.+?)(?<!\\\\)___|(?<!\\\\)\\*\\*(.+?)(?<!\\\\)\\*\\*|(?<!\\\\)__(.+?)(?<!\\\\)__|(?<!\\\\)\\*(.+?)(?<!\\\\)\\*|(?<!\\\\)_(.+?)(?<!\\\\)_|(?<!\\\\)~~(.+?)(?<!\\\\)~~|(?<!\\\\)`([^`]+)(?<!\\\\)`)";
        Pattern pattern = Pattern.compile(patternStr, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        
        int lastIndex = 0;
        StringBuilder sb = new StringBuilder();
        while(matcher.find()) {
            if (matcher.start() > lastIndex) {
                sb.append(unescapeMarkdown(text.substring(lastIndex, matcher.start())));
            }
            String fullMatch = matcher.group(0);
            if (fullMatch.startsWith("*") && fullMatch.endsWith("*")) {
                String inner = fullMatch.substring(1, fullMatch.length() - 1);
                sb.append("[ITALIC:");
                sb.append(unescapeMarkdown(inner));
                sb.append("]");
            }
            lastIndex = matcher.end();
        }
        if (lastIndex < text.length()) {
            sb.append(unescapeMarkdown(text.substring(lastIndex)));
        }
        System.out.println(sb.toString());
    }
}
