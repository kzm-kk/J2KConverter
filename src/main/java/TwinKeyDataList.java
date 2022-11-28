import java.util.ArrayList;

public class TwinKeyDataList <K1, K2, V>{
    ArrayList<Triplets<K1, K2, V>> dataList;

    public TwinKeyDataList(){
        dataList = new ArrayList<>();
    }

    public void setData(K1 key1, K2 key2, V value){
        boolean newFlag = true;
        for(Triplets<K1, K2, V> triplets:dataList){
            if(triplets.getLeftValue().equals(key1) && triplets.getCenterValue().equals(key2)){
                int index = dataList.indexOf(triplets);
                dataList.remove(index);
                dataList.add(index, new Triplets<K1, K2, V>(key1, key2, value));
                newFlag = false;
                break;
            }
        }
        if(newFlag){
            dataList.add(new Triplets<K1, K2, V>(key1, key2, value));
        }
    }

    public Triplets<K1, K2, V> getDataTriplets(K1 key1, K2 key2){
        for(Triplets<K1, K2, V> triplets:dataList){
            if(triplets.getLeftValue().equals(key1) && triplets.getCenterValue().equals(key2)){
                return triplets;
            }
        }
        return null;
    }

    public V getData(K1 key1, K2 key2){
        Triplets<K1, K2, V> triplets = getDataTriplets(key1, key2);
        if (triplets == null) return null;
        else return triplets.getRightValue();
    }

    /*public V getData(K1 key1, K2 key2){
        V ret = null;
        try{
            Triplets<K1, K2, V> triplets = getDataTriplets(key1, key2);
            //if(triplets == null) return null;
            //else return triplets.getRightValue();
             ret = triplets.getRightValue();
        } catch (NullPointerException e){
            System.out.println("no-infomation Error:" + key1 + " " + key2);
            System.out.println("key1-search");
            ArrayList<Triplets<K1, K2, V>> retList1 = getDataListKey1(key1);
            for(Triplets<K1, K2, V> triplets:retList1){
                System.out.println("key1:" + triplets.getLeftValue() + " key2:" + triplets.getCenterValue());
            }
            System.out.println("key2-search");
            ArrayList<Triplets<K1, K2, V>> retList2 = getDataListKey2(key2);
            for(Triplets<K1, K2, V> triplets:retList2){
                System.out.println("key1:" + triplets.getLeftValue() + " key2:" + triplets.getCenterValue());
            }
        }
        finally {
            return ret;
        }
    }*/

    public ArrayList<Triplets<K1, K2, V>> getDataListKey1(K1 key1){
        ArrayList<Triplets<K1, K2, V>> retList = new ArrayList<>();
        for(Triplets<K1, K2, V> triplets:dataList){
            if(triplets.getLeftValue().equals(key1)){
                retList.add(triplets);
            }
        }
        return retList;
    }

    public ArrayList<Triplets<K1, K2, V>> getDataListKey2(K2 key2){
        ArrayList<Triplets<K1, K2, V>> retList = new ArrayList<>();
        for(Triplets<K1, K2, V> triplets:dataList){
            if(triplets.getCenterValue().equals(key2)){
                retList.add(triplets);
            }
        }
        return retList;
    }

    public ArrayList<Triplets<K1, K2, V>> getDataList(){
        return this.dataList;
    }
}
