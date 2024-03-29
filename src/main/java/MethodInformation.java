import com.github.javaparser.Range;
import com.github.javaparser.ast.type.Type;

import java.util.ArrayDeque;

public class MethodInformation extends BlockInformation {
    //name:名前
    //static:staticか否か
    //access:アクセサメソッドか否か
    //fix:get/setだとして、暗黙的なアクセサもしくはそのカスタムアクセサに直せるか否か
    //accessField:どのフィールドのアクセサか
    //lines:メソッドの中身の行数。2以上ならfixがtrueでカスタムアクセサにできる可能性がある
    //nullable:メソッドの返り値がnullになり得るか
    //type:メソッドの返り値
    //range:このメソッドの記述範囲、localValueを引き出すために使う
    boolean getAccess;
    boolean setAccess;
    boolean fix;
    String accessField;
    int lines;
    boolean nullable;
    Type type;
    Type returnType;

    boolean isConvertReturnType;

    public MethodInformation(String name, String structure, String pathDir, boolean isStatic,
                             boolean isKotlinPrivate, boolean getAccess, boolean setAccess, boolean fix, String accessField, int lines,
                             boolean nullable, Type type, Range range, ArrayDeque<Range> rangeStructure){
        super(name, "Method", isStatic, isKotlinPrivate, structure, pathDir, range, rangeStructure);
        this.getAccess = getAccess;
        this.setAccess = setAccess;
        this.fix = fix;
        this.accessField = accessField;
        this.lines = lines;
        this.nullable = nullable;
        this.type = type;
        this.returnType = type;
        this.isConvertReturnType = false;
    }
}
