import com.github.javaparser.Range;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class MethodInfomation extends BlockInfomation{
    //name:名前
    //static:staticか否か
    //access:アクセサメソッドか否か
    //fix:get/setだとして、暗黙的なアクセサもしくはそのカスタムアクセサに直せるか否か
    //accessField:どのフィールドのアクセサか
    //lines:メソッドの中身の行数。2以上ならfixがtrueでカスタムアクセサにできる可能性がある
    //nullable:メソッドの返り値がnullになり得るか
    //type:メソッドの返り値
    //range:このメソッドの記述範囲、localvalueを引き出すために使う
    boolean access;
    boolean fix;
    String accessField;
    int lines;
    boolean nullable;
    String type;

    public MethodInfomation(String name, String structure, String pathDir, boolean isStatic,
                            boolean access, boolean fix, String accessField, int lines,
                            boolean nullable, String type, Range range, ArrayDeque<Range> rangeStructure){
        super(name, "Method", isStatic, structure, pathDir, range, rangeStructure);
        this.access = access;
        this.fix = fix;
        this.accessField = accessField;
        this.lines = lines;
        this.nullable = nullable;
        this.type = type;
    }
}
