import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.IOException;

public class ASTViewer {

    public static void main(String[] args) throws IOException {

        String pathname
                = "/Users/kzm0308/IdeaProjects/J2KConverter/src/main/java/pac3/Class18.java";
        File file;
        if (args.length == 0)
            file = new File(pathname);
        else file = new File(args[0]);
        //file = new File(DataStore.pathName);
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(file).getResult().get();

        VoidVisitor<?> visitor = new DummyVisitor();
        cu.accept(visitor, null);

    }

    public static class DummyVisitor extends VoidVisitorAdapter<Void>{

    }
}