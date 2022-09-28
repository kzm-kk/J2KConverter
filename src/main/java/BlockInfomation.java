import com.github.javaparser.Range;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

public class BlockInfomation extends BaseInfomation{
    String name;
    String kind;
    boolean isStatic;
    HashMap<String, FieldInfomation> memoryF = new HashMap<>();
    TwinKeyDataList<String, Range, BlockInfomation> memoryB = new TwinKeyDataList<>();

    public BlockInfomation(String name, String kind, boolean isStatic, String structure, String pathDir, Range range, ArrayDeque<Range> rangeStructure){
        super(structure, pathDir, range, rangeStructure);
        this.name = name;
        this.kind = kind;
        this.isStatic = isStatic;
    }

    public void setMemoryF(FieldInfomation infomation){
        memoryF.put(infomation.name, infomation);
    }

    public void setMemoryF(ArrayList<FieldInfomation> infomations){
        for(FieldInfomation FI:infomations){
            setMemoryF(FI);
        }
    }

    public HashMap<String, FieldInfomation> getMemoryF(){
        return this.memoryF;
    }

    public void setMemory(BlockInfomation infomation){
        memoryB.setData(infomation.blockStructure, infomation.range, infomation);
    }

    public BlockInfomation getMemoryData(String key1, Range key2){
        return memoryB.getData(key1, key2);
    }

    public TwinKeyDataList<String, Range, BlockInfomation> getMemory(){
        return memoryB;
    }

    public ArrayList<Triplets<String, Range, BlockInfomation>> getMemoryKind(String kindKey){
        ArrayList<Triplets<String, Range, BlockInfomation>> retList = new ArrayList<>();
        for(Triplets<String, Range, BlockInfomation> triplets:memoryB.getDataList()){
            String kind = triplets.getRightValue().kind;
            if(kind.equals(kindKey)) retList.add(triplets);
        }
        return retList;
    }
}
