/*
Written by sarker.
Written at 6/30/20.
*/

import org.dase.ecii.Main;

public class TestMain {

    public void testConceptInductionM() {
        String[] argsConceptInduction = new String[2];
        argsConceptInduction[0] = "-e";
        argsConceptInduction[1] = "/Users/sarker/Workspaces/Jetbrains/ecii/ecii/ecii/src/test/resources/expr_types/induction_m_test/induction_test_2_null_error.config";


        Main.decideOp(argsConceptInduction);
    }

    public static void main(String[] args) {
        TestMain testMain = new TestMain();
        testMain.testConceptInductionM();
    }
}
