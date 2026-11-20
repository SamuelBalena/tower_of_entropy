import com.towerofentropy.animation.VisualActionWindow;
import static com.towerofentropy.animation.VisualActionWindow.Action.*;

public class VisualActionWindowTest {
    static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
    public static void main(String[] args){
        var w=new VisualActionWindow();check(w.request(ROLL),"first request accepted");
        check(!w.request(SWORD_ATTACK),"pending first request wins");
        check(w.start(10)==ROLL,"Roll starts once");check(w.start(11)==null,"no per-tick restart");
        for(int i=11;i<42;i++){check(!w.request(ROLL),"spam must not extend action");check(w.finishIfDue(i)==null,"do not truncate source duration");}
        check(w.finishIfDue(42)==ROLL,"Roll finishes with bounded transport grace");
        check(w.request(SWORD_ATTACK),"can attack after Roll");check(w.start(50)==SWORD_ATTACK,"attack starts");
        check(!w.request(ROLL),"no unsafe Roll interruption");check(w.finishIfDue(60)==null,"attack duration retained");
        check(w.finishIfDue(61)==SWORD_ATTACK,"attack finishes");
        w.request(ROLL);w.start(70);check(w.cancel()==ROLL,"fallback/off cancels active action");
        check(!w.busy(),"no residual action");w.request(SWORD_ATTACK);w.cancel();check(!w.busy(),"pending request cleared");
        check(!new VisualActionWindow().busy(),"no action after new session");
        System.out.println("PASS: first wins, bounded one-shot, no restart/spam extension, off/fallback/pending cleanup");
    }
}
