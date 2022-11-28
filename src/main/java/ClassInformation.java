import com.github.javaparser.Range;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class ClassInformation extends BlockInformation {
    String classEx;
    ArrayList<String> classIm;
    boolean isEnum;
    ArrayList<MethodInformation> memoryM = new ArrayList<>();
    boolean isContainStatic = false;


    public ClassInformation(String name, String kind, String structure, String pathDir,
                            Range range, ArrayDeque<Range> rangeStructure, String classEx,
                            ArrayList<String> classIm, boolean isEnum, boolean isStatic,
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
        this(name, kind, structure, pathDir, range, rangeStructure, "", new ArrayList<>(), isEnum, isStatic, isKotlinPrivate);

    }

    public void setMemoryM(MethodInformation information){
        memoryM.add(information);
    }



    public boolean pathMatching(String pathDir){
        return this.pathDir.equals(pathDir);
    }
}
