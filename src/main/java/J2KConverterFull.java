import java.io.File;
import java.io.IOException;

public class J2KConverterFull {

    public static void main(String[] args) throws IOException {

        if (args.length == 0)
            DataStore.pathName = "/Users/kzm0308/IdeaProjects/J2KConverter/src/main/java/numer0n.java";
        else DataStore.pathName = args[0];
        //System.out.println(args[0]);
        J2KConverterSupporter.main(args);
        J2KConverter.main(args);
    }
}
