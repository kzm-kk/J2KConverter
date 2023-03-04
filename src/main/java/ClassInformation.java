import com.github.javaparser.Range;
import com.github.javaparser.ast.type.Type;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

public class ClassInformation extends BlockInformation {
    Type classEx;
    ArrayList<Type> classIm;
    boolean isEnum;
    boolean isContainStatic = false;
    boolean isInner = false;


    public ClassInformation(String name, String kind, String structure, String pathDir,
                            Range range, ArrayDeque<Range> rangeStructure, Type classEx,
                            ArrayList<Type> classIm, boolean isEnum, boolean isStatic,
                            boolean isKotlinPrivate){
        super(name, kind, isStatic, isKotlinPrivate, structure, pathDir, range, rangeStructure);
        this.range = range;
        this.classEx = classEx;
        this.classIm = classIm;
        this.isEnum = isEnum;
    }

    public ClassInformation(String name, String kind, String structure, String pathDir,
                            Range range, ArrayDeque<Range> rangeStructure, boolean isEnum,
                            boolean isStatic, boolean isKotlinPrivate){
        this(name, kind, structure, pathDir, range, rangeStructure, null, new ArrayList<>(), isEnum, isStatic, isKotlinPrivate);

    }




    public boolean pathMatching(String pathDir){
        return this.pathDir.equals(pathDir);
    }
}
