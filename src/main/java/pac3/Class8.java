package pac3;

import java.lang.reflect.Type;

public class Class8 {
    public Class8(String str){
        int i = 0;
        System.out.println("class8" + str);
        i++;
    }

    synchronized void someMethod() {
        System.out.println("sync");
    }

    @SuppressWarnings("unused")
    @Annotation1(value = "one")
    @Annotation2(value = "one", value2 = {"two", "three"})
    void anotherMethod() {
        synchronized (this) {
            System.out.println("sync2");
        }

    }

    int outerMember;
    final int outerMember2 = 80;
    int outerMember3 = 500;

    void outerMethod() {
        int a = 10;
        final int b = 20;
        /** ローカル変数はfinalのみ使える↓×
        * static int m = 19;
        * 外部クラスのメソッド内でクラス定義するクラスを
        * ローカルクラスという
        * ローカルクラスの修飾子はfinalとabstractのみ使える
        * ローカルクラス↓
        */
        class LocalClass {
            // staticなフィールド変数やメソッドを定義できない
            // static int i = 100;NG
            final int i2 = 120;
            // abstract int i3 = 130;
            // System.out.println(a);
            // System.out.println(b);

            public void innerMethod() {
                outerMember = 2;
                System.out.println("innerMethodです");
                System.out.println(outerMember);
                System.out.println(a);
                System.out.println(b);
                System.out.println(outerMember2);
                System.out.println(outerMember3);
            }
        }
        // innerMethod();では使えない
        // 同じメソッド内ですぐに利用
        LocalClass lc = new LocalClass();
        lc.innerMethod();
    }

    public static void main(String[] args) {
        Class8 t = new Class8("");
        t.outerMethod();
    }

    public class Class8_In{

    }
}
