import java.io.IOException;

public class J2KConverterFullMulti {

    public static void main(String[] args) throws IOException {

        if (args.length == 0)
            DataStore.pathName = "/Users/kzm0308/IdeaProjects/J2KConverter/src/main/java/pac3";
        else DataStore.pathName = args[0];
        //System.out.println(args[0]);
        J2KConverterSupporterMulti.main(args);
        J2KConverter.main(args);
    }
}
