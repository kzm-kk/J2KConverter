import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ConvertOutputer {

    public static String convertDirName = "/outkt";

    public static void CreateDir(String path){
        File file = new File(path);
        if(!file.exists()){
            file.mkdir();
        }
    }

    public static FileOutputStream CreateConvertFile(String rootPath, String path) throws IOException {
        String underConvertPath = path.replace(rootPath, rootPath + convertDirName);
        String createPath = underConvertPath.replace(".java",  ".kt");
        File file = new File(createPath);
        if(!file.exists()) file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        return fos;
    }

    public static ArrayList<String> dumpFile(String path, String pathKt, int level){
        String convertPath = pathKt;
        if(level == 0){
            convertPath = convertPath + convertDirName;
            CreateDir(convertPath);
        }
        // ファイル一覧取得
        File[] files = new File(path).listFiles();
        ArrayList<String> filePathList = new ArrayList<>();
        if(files == null){
            return new ArrayList<>();
        }
        for (File tmpFile : files) {
            String name = tmpFile.getName();
            if(tmpFile.isDirectory()){// ディレクトリの場合
                if(!name.equals("outkt")) {
                    String newPath = path + "/" + name;
                    String newConvertPath = convertPath + "/" + name;
                    //CreateDir(newConvertPath);
                    ArrayList<String> tmpList = dumpFile(newPath, newConvertPath, level + 1);
                    filePathList.addAll(tmpList);
                }
            }else{// ファイルの場合
                //if(name.substring(tmpFile.name.lastIndexOf(".") + 1).equals("java"))
                    filePathList.add(path + "/" + name);
            }
        }
        return filePathList;
    }

}
