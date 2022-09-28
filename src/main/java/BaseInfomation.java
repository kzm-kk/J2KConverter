import com.github.javaparser.Range;

import java.util.ArrayDeque;

public class BaseInfomation {
    String blockStructure;
    String pathDir;
    Range range;
    ArrayDeque<Range> rangeStructure;

    public BaseInfomation(String blockStructure, String pathDir, Range range, ArrayDeque<Range> rangeStructure){
        this.blockStructure = blockStructure;
        this.pathDir = pathDir;
        this.range = range;
        this.rangeStructure = rangeStructure;
    }
}
