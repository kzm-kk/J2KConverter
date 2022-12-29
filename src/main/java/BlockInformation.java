import com.github.javaparser.Range;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.Type;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

public class BlockInformation extends BaseInformation {
    String name;
    String kind;
    HashMap<String, FieldInformation> memoryF = new HashMap<>();
    TwinKeyDataList<String, Range, BlockInformation> memoryB = new TwinKeyDataList<>();
    Type[] paramTypes;

    public BlockInformation(String name, String kind, boolean isStatic, boolean isKotlinPrivate,
                            String structure, String pathDir, Range range, ArrayDeque<Range> rangeStructure){
        super(structure, pathDir, range, rangeStructure, isStatic, isKotlinPrivate);
        this.name = name;
        this.kind = kind;
    }

    public BlockInformation(String name, String kind, boolean isStatic, String structure, String pathDir, Range range,
                            ArrayDeque<Range> rangeStructure){
        this(name, kind, isStatic, false, structure, pathDir, range, rangeStructure);
    }


    public BlockInformation(String name, String kind, String structure, String pathDir, Range range,
                            ArrayDeque<Range> rangeStructure){
        this(name, kind, false, false, structure, pathDir, range, rangeStructure);
    }



    public void setParamTypes(NodeList<Parameter> parameters){
        int size = parameters.size();
        paramTypes = new Type[size];
        for(int i = 0; i < size ;i++){
            paramTypes[i] = parameters.get(i).getType();
        }
    }

    public void setMemoryF(FieldInformation information){
        memoryF.put(information.name, information);
    }

    public void setMemoryF(ArrayList<FieldInformation> informations){
        for(FieldInformation FI:informations){
            setMemoryF(FI);
        }
    }

    public HashMap<String, FieldInformation> getMemoryF(){
        return this.memoryF;
    }

    public void setMemory(BlockInformation information){
        memoryB.setData(information.blockStructure, information.range, information);
    }

    public BlockInformation getMemoryData(String key1, Range key2){
        return memoryB.getData(key1, key2);
    }

    public TwinKeyDataList<String, Range, BlockInformation> getMemory(){
        return memoryB;
    }

    public ArrayList<Triplets<String, Range, BlockInformation>> getMemoryKind(String kindKey){
        ArrayList<Triplets<String, Range, BlockInformation>> retList = new ArrayList<>();
        for(Triplets<String, Range, BlockInformation> triplets:memoryB.getDataList()){
            String kind = triplets.getRightValue().kind;
            if(kind.equals(kindKey)) retList.add(triplets);
        }
        return retList;
    }
}
