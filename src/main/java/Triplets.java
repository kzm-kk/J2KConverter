public class Triplets <T1, T2, T3>{
    T1 leftValue;
    T2 centerValue;
    T3 rightValue;

    public Triplets(T1 leftValue, T2 centerValue, T3 rightValue){
        this.leftValue = leftValue;
        this.centerValue = centerValue;
        this.rightValue = rightValue;
    }

    public Triplets(){
        this(null, null, null);
    }

    public T1 getLeftValue(){
        return this.leftValue;
    }

    public void setLeftValue(T1 leftValue) {
        this.leftValue = leftValue;
    }

    public T2 getCenterValue() {
        return this.centerValue;
    }

    public void setCenterValue(T2 centerValue) {
        this.centerValue = centerValue;
    }

    public T3 getRightValue() {
        return this.rightValue;
    }

    public void setRightValue(T3 rightValue) {
        this.rightValue = rightValue;
    }
}
