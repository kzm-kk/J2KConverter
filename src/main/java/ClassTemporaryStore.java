import com.github.javaparser.Range;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class ClassTemporaryStore extends BaseInformation {
    String name;
    List<FieldDeclaration> listFD = new ArrayList<>();
    List<MethodDeclaration> listMD = new ArrayList<>();
    List<ConstructorDeclaration> listCD = new ArrayList<>();
    List<InitializerDeclaration> listID = new ArrayList<>();

    public ClassTemporaryStore(String name, String structure,
                               String pathDir, Range range,
                               ArrayDeque<Range> rangeStructure,
                               List<FieldDeclaration> listFD,
                               List<MethodDeclaration> listMD,
                               List<ConstructorDeclaration> listCD,
                               List<InitializerDeclaration> listID){
        super(structure, pathDir, range, rangeStructure, false, false);
        this.name = name;
        this.listFD = listFD;
        this.listMD = listMD;
        this.listCD = listCD;
        this.listID = listID;
    }
}
