import com.github.javaparser.Range;

import java.util.ArrayDeque;

public class BaseInformation {
    String blockStructure;
    String pathDir;
    Range range;
    ArrayDeque<Range> rangeStructure;
    boolean isStatic;
    boolean isKotlinPrivate;
    boolean isOpen = false;
    boolean isOverride = false;

    public BaseInformation(String blockStructure, String pathDir, Range range, ArrayDeque<Range> rangeStructure,
                           boolean isStatic, boolean isKotlinPrivate){
        this.blockStructure = blockStructure;
        this.pathDir = pathDir;
        this.range = range;
        this.rangeStructure = rangeStructure;
        this.isStatic = isStatic;
        this.isKotlinPrivate = isKotlinPrivate;

    }
}
