package org.dase.test;
/*
Written by sarker.
Written at 8/10/18.
*/

import java.util.ArrayList;
import java.util.HashSet;

public class TestSolutionComplexCustom {

    public void testHashSetEntryChange(){
        HashSet<InnerClass> hashSet = new HashSet<>();

        InnerClass innerClass = null;
        innerClass = new InnerClass();
        innerClass.aList = new ArrayList<>();
        innerClass.aList.add(1);
        innerClass.aList.add(2);
        innerClass.aList.add(3);

        hashSet.add(innerClass);

        innerClass = new InnerClass();
        innerClass.aList.add(4);
        innerClass.aList.add(5);

        for(InnerClass innerClass1: hashSet){
            for(Integer integer: innerClass1.aList) {
                System.out.print(integer+" ");
            }
        }
    }

    public static void main(String [] args){
        TestSolutionComplexCustom testSolutionComplexCustom = new TestSolutionComplexCustom();
        testSolutionComplexCustom.testHashSetEntryChange();
    }

    static class InnerClass {
       public ArrayList<Integer> aList = new ArrayList<>();
    }
}
