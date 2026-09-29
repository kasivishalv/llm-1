import java.util.regex.*;

public class TestRegex {
    public static void main(String[] args) {
        String text = "\\*Net return = market gain minus* fees";
        // Java string literal for (?<!\\) is "(?<!\\\\)"
        String patternStr = "(?<!\\\\)(\\*\\*\\*(.+?)(?<!\\\\)\\*\\*\\*|(?<!\\\\)___(.+?)(?<!\\\\)___|(?<!\\\\)\\*\\*(.+?)(?<!\\\\)\\*\\*|(?<!\\\\)__(.+?)(?<!\\\\)__|(?<!\\\\)\\*(.+?)(?<!\\\\)\\*|(?<!\\\\)_(.+?)(?<!\\\\)_|(?<!\\\\)~~(.+?)(?<!\\\\)~~|(?<!\\\\)`([^`]+)(?<!\\\\)`)";
        Pattern pattern = Pattern.compile(patternStr, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        while(matcher.find()) {
            System.out.println("Match: " + matcher.group(0));
        }
        
        String text2 = "This is *italic* and \\**this is not**";
        matcher = pattern.matcher(text2);
        while(matcher.find()) {
            System.out.println("Match2: " + matcher.group(0));
        }
    }
}
