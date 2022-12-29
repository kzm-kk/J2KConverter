import com.github.javaparser.Range;
import com.github.javaparser.ast.type.Type;

import java.util.ArrayDeque;

public class FieldInformation extends BaseInformation {
    //name:名前、type:型、dim:配列の深さなければ0、assign:代入の有無、nullable:nullになり得るか？ static:staticか否か
    //initializable:初期値の有無、formerRW:ReadとWriteどちらが先(former)か
    String name;
    Type type;
    int dim;
    boolean assign;
    boolean nullable;
    boolean initializable;
    String formerRW;
    boolean isAnonymous;
    boolean isLambda;
    Type valueType;

    boolean isConvertValueType;


    public FieldInformation(String name, String structure, String pathDir, Type type, int dim,
                            boolean assign, boolean nullable, boolean isStatic, boolean isKotlinPrivate,
                            boolean initializable, Type valueType, String formerRW, Range range, ArrayDeque<Range> rangeStructure){
        super(structure, pathDir, range, rangeStructure, isStatic, isKotlinPrivate);
        this.name = name;
        this.type = type;
        this.dim = dim;
        this.assign = assign;
        this.nullable = nullable;
        this.initializable = initializable;
        this.valueType = valueType;
        this.formerRW = formerRW;
        this.isAnonymous = false;
        this.isLambda = false;
        this.isConvertValueType = false;
    }

    public FieldInformation(String name, String structure, String pathDir, Type type, int dim,
                            boolean assign, boolean nullable, boolean initializable, Type valueType,
                            String formerRW, Range range, ArrayDeque<Range> rangeStructure){
        this(name, structure, pathDir, type, dim, assign, nullable,
                false, false, initializable, valueType, formerRW, range, rangeStructure);
    }
}
