import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Machine {
    private final Pattern numbers = Pattern.compile("\\d+");
    private final Pattern identifiers = Pattern.compile("[a-zA-Z]\\w*");
    private final Pattern operators = Pattern.compile("[+\\-*/]");
    private final Pattern delimiters = Pattern.compile("[,;(){}]");
    private final Pattern comments = Pattern.compile("//.*|/\\*(.|\\R)*?\\*/");
    private final Pattern strings = Pattern.compile("\".*?\"");
    private final Pattern whitespaces = Pattern.compile("\\s+");

    public Machine() {
        //
    }

    public void readCode(String code) {
        while (code.length() > 0) {
            Matcher matcher;

            if ((matcher = numbers.matcher(code)).lookingAt()) {
                System.out.println("Number: " + matcher.group());
                code = code.substring(matcher.end());
            } else if ((matcher = identifiers.matcher(code)).lookingAt()) {
                System.out.println("Identifier: " + matcher.group());
                code = code.substring(matcher.end());
            } else if ((matcher = operators.matcher(code)).lookingAt()) {
                System.out.println("Operator: " + matcher.group());
                code = code.substring(matcher.end());
            } else if ((matcher = delimiters.matcher(code)).lookingAt()) {
                System.out.println("Delimiter: " + matcher.group());
                code = code.substring(matcher.end());
            } else if ((matcher = comments.matcher(code)).lookingAt()) {
                System.out.println("Comment: " + matcher.group());
                code = code.substring(matcher.end());
            } else if ((matcher = strings.matcher(code)).lookingAt()) {
                System.out.println("String: " + matcher.group());
                code = code.substring(matcher.end());
            } else if ((matcher = whitespaces.matcher(code)).lookingAt()) {
                code = code.substring(matcher.end()); // Ignorar espaços em branco
            } else {
                System.out.println("Invalid character: " + code.charAt(0));
                code = code.substring(1);
            }
        }
    }

    public static void main(String[] args) {
        Machine machine = new Machine();
        machine.readCode("int x = 42; // exemplo");
    }
}
