import com.github.javaparser.Range;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class ClassInfomation extends BlockInfomation{
    String classEx;
    ArrayList<String> classIm;
    boolean isEnum;
    ArrayList<MethodInfomation> memoryM = new ArrayList<>();
    boolean isContainStatic = false;


    public ClassInfomation(String name, String kind, String structure, String pathDir, Range range, ArrayDeque<Range> rangeStructure,
                           String classEx, ArrayList<String> classIm, boolean isEnum, boolean isStatic){
        super(name, kind, isStatic, structure, pathDir, range, rangeStructure);
        this.range = range;
        this.classEx = classEx;
        this.classIm = classIm;
        this.isEnum = isEnum;
    }

    public ClassInfomation(String name, String kind, String structure, String pathDir, Range range, ArrayDeque<Range> rangeStructure, boolean isEnum, boolean isStatic){
        this(name, kind, structure, pathDir, range, rangeStructure, "", new ArrayList<>(), isEnum, isStatic);

    }

    public void setMemoryM(MethodInfomation infomation){
        memoryM.add(infomation);
    }



    public boolean pathMatching(String pathDir){
        return this.pathDir.equals(pathDir);
    }
}
