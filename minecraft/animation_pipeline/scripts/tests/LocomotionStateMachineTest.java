import com.towerofentropy.animation.LocomotionStateMachine;
import static com.towerofentropy.animation.LocomotionStateMachine.State.*;

public class LocomotionStateMachineTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static LocomotionStateMachine.Motion motion(double speed, double forward, boolean unsupported) {
        return new LocomotionStateMachine.Motion(speed, forward, unsupported);
    }
    public static void main(String[] args) {
        var m = new LocomotionStateMachine();
        check(m.update(motion(0, 1, false)) == VANILLA_FALLBACK, "Do not enter Idle on one sample");
        m.update(motion(0, 1, false));
        check(m.update(motion(0, 1, false)) == IDLE, "Stable ground stop enters Idle");
        for (int i=0;i<100;i++) check(m.update(motion(i%2==0?.011:.020,1,false))==IDLE,"Speed jitter must not toggle Jog");
        for (int i=0;i<3;i++) m.update(motion(.21,1,false));
        check(m.state()==JOG,"Forward movement enters Jog");
        for (int i=0;i<100;i++) check(m.update(motion(i%2==0?.020:.030,.55,false))==JOG,"Speed/direction hysteresis must hold");
        check(m.update(motion(.21,-1,false))==VANILLA_FALLBACK,"Reverse must fall back immediately");
        for (int i=0;i<3;i++) m.update(motion(.21,1,false));
        check(m.state()==JOG,"Resume after reverse");
        check(m.update(motion(.21,1,true))==VANILLA_FALLBACK,"Air/crouch/swim/riding must fall back immediately");
        check(m.update(motion(Double.NaN,1,false))==VANILLA_FALLBACK,"Reject non-finite motion");
        for (int i=0;i<3;i++) m.update(motion(0,1,false));
        check(m.state()==IDLE,"Landing and stopping returns to Idle");
        check(new LocomotionStateMachine().state()==VANILLA_FALLBACK,"Fresh session must not retain a pose");
        System.out.println("PASS: stable entry, speed/direction hysteresis, reverse, unsupported states, finite inputs, fresh session");
    }
}
