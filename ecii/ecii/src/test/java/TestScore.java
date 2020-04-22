/*
Written by sarker.
Written at 4/20/20.
*/

import org.dase.ecii.core.Score;
import org.dase.ecii.core.ScoreType;

public class TestScore {


    public void testgetDefaultScoreValue() {
        Score score = new Score();
        Score.defaultScoreType = ScoreType.PRECISION;

        score.setPrecision(0.5);
        score.setCoverage(0.4);

        System.out.println("Default score value: " + score.getDefaultScoreValue());
    }

    public static void main(String[] args) {
        TestScore testScore = new TestScore();
    }
}
