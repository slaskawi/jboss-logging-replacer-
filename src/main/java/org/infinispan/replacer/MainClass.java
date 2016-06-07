package org.infinispan.replacer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainClass {

    private static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private static Pattern debugfPattern = Pattern.compile("debugf\\((\".*)\\);");
    private static Pattern tracefPattern = Pattern.compile("tracef\\((\".*)\\);");

    private static Pattern[] allPatterns = new Pattern[]{debugfPattern, tracefPattern};


    public static void main(String[] args) throws Exception {
//        args = new String[]{"/home/slaskawi/work/infinispan/infinispan/core/src/main/java/org/infinispan/interceptors/InvalidationInterceptor.java"};

        Objects.nonNull(args);
        Objects.equals(args.length, 1);

        logger.info("Parsing file: " + args[0]);

        StringBuilder output = new StringBuilder();

        Path path = Paths.get(args[0]);
        Scanner scanner = new Scanner(path);
        int lineNumber = 0;
        boolean modifiedAnything = false;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            ++lineNumber;

            String finalModifiedString = line;
            for(Pattern p : allPatterns) {
                Matcher m = p.matcher(finalModifiedString);
                if(m.find() && !line.contains("new Object")) {
                    logger.info("Modifying expression (line: " + lineNumber + "): " + m.group(0));
                    String [] decomposedExpression = m.group(1).trim().split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    StringBuilder replacement = createReplacement(decomposedExpression);

                    StringBuilder modifiedExpression = new StringBuilder(line).replace(m.start(1), m.end(1), replacement.toString());
                    finalModifiedString = modifiedExpression.toString();
                    logger.info("Modified expression " + finalModifiedString);
                    modifiedAnything = true;
                }
            }
            output.append(finalModifiedString);
            output.append(System.getProperty("line.separator"));
        }
        scanner.close();

        if(modifiedAnything) {
            Files.write(path, output.toString().getBytes("UTF-8"));
        }
    }

    private static StringBuilder createReplacement(String [] decomposedExpression) {
        StringBuilder replacement = new StringBuilder(decomposedExpression[0] + ", new Object[] {");
        for(int i = 1; i < decomposedExpression.length; ++i) {
            replacement.append(decomposedExpression[i].trim());
            if(i < decomposedExpression.length - 1) {
                replacement.append(", ");
            }
        }
        replacement.append("}");
        return replacement;
    }

}
