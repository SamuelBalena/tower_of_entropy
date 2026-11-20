import com.towerofentropy.combat.AttackGeometry;
import com.towerofentropy.combat.AttackState;
import com.towerofentropy.combat.CombatMove;

public final class CombatRulesTest {
    private static int checks;
    private static void check(boolean pass,String name){++checks;if(!pass)throw new AssertionError(name);}
    public static void main(String[] args){
        check(AttackGeometry.inRegion(0,.9,2,0,3,90),"front");
        check(!AttackGeometry.inRegion(0,.9,-2,0,3,90),"behind");
        check(!AttackGeometry.inRegion(0,0,3.001,0,3,90),"reach");
        check(AttackGeometry.inRegion(0,0,3,0,3,90),"exact reach");
        check(AttackGeometry.inRegion(1,0,1,0,3,90),"arc boundary");
        check(!AttackGeometry.inRegion(1.01,0,1,0,3,90),"outside arc");
        check(AttackGeometry.inRegion(-2,0,0,90,3,90),"yaw rotated");
        check(!AttackGeometry.inRegion(0,4,0,0,3,90),"vertical range");
        check(!AttackGeometry.inRegion(0,0,0,0,3,90),"undefined heading");
        check(!AttackGeometry.inRegion(Double.NaN,0,1,0,3,90),"nonfinite");
        var a=new AttackState();
        check(!a.consumeImpact(1000,4),"pending cannot damage");
        a.start(100);a.start(103);
        check(a.startedAt()==100,"duplicate start cannot postpone");
        check(!a.consumeImpact(103,4),"no early impact");
        check(a.consumeImpact(104,4),"impact exactly four ticks");
        check(!a.consumeImpact(104,4)&&!a.consumeImpact(110,4),"one attempt only");
        check(!a.finished(108,9)&&a.finished(109,9),"independent end");
        var lag=new AttackState();lag.start(10);
        check(lag.consumeImpact(16,4)&&!lag.consumeImpact(17,4),"late tick once");
        var changed=new AttackState();changed.start(0);
        check(!changed.consumeImpact(5,6)&&changed.consumeImpact(6,6),"configurable impact");
        recovery(CombatMove.LIGHT_ATTACK_C, 14, 17, 35, 2f);
        recovery(CombatMove.HEAVY_ATTACK, 12, 16, 34, 70f / 30);
        check(CombatMove.LIGHT_ATTACK_A.next() == CombatMove.LIGHT_ATTACK_B
                && CombatMove.LIGHT_ATTACK_B.next() == CombatMove.LIGHT_ATTACK_C
                && CombatMove.LIGHT_ATTACK_C.next() == CombatMove.LIGHT_ATTACK_A, "light combo order");
        System.out.println("PASS: "+checks+" combat geometry/timeline rules");
    }

    private static void recovery(CombatMove move, int impact, int start, int previousEnd, float sourceEnd) {
        check(move.impactTicks == impact, move + " impact");
        check((move.durationTicks-start)*2 == previousEnd-start, move + " half recovery");
        for (int tick=0; tick<=start; tick++) {
            float time=tick/20f;
            check(CombatMove.visualTime(move.clip,time)==time, move + " preserved phase " + tick);
        }
        float last=0;
        for (int sample=1; sample<=move.durationTicks*10; sample++) {
            float time=CombatMove.visualTime(move.clip,sample/200f);
            if (time<last || time>sourceEnd+.001f) throw new AssertionError(move + " monotonic playback at " + sample);
            last=time;
        }
        check(Math.abs(last-sourceEnd)<.001f, move + " reaches last pose at unlock");
    }
}