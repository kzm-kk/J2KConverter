package pac3;

public class Class17 {
    private String path, code;

    public Class17() {
        this.path = "";
    }

    public Class17(String path) {
        this.path = path;
        code = path;
    }

    private int num = 0;
    private String mStr = "";
    public int getNum(){
        return this.num;
    }
    public void setNum(int num){
        this.num = num;
    }
    public String getStr(){
        return this.mStr + "/";
    }
    public void setStr(String str){
        this.mStr = this.mStr + "str";
    }


    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path
                = this.path.concat("/" + path);
    }

    public String getCode() {
        return "code:" + this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getLayer() {
        return path != null ? path.split("/").length : 0;
    }

}
