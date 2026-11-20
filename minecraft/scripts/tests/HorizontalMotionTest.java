import com.towerofentropy.animation.HorizontalMotion;
public class HorizontalMotionTest {
 static int checks;static void eq(Object a,Object b){checks++;if(!a.equals(b))throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){var m=new HorizontalMotion();
  for(int i=0;i<20;i++)m.update(0,.10,0,false);eq(m.state(),HorizontalMotion.State.WALK);
  for(int i=0;i<20;i++)m.update(0,.216,0,false);eq(m.state(),HorizontalMotion.State.JOG);
  for(int i=0;i<20;i++)m.update(0,.281,0,true);eq(m.state(),HorizontalMotion.State.SPRINT);
  m.update(0,0,0,true);eq(m.state(),HorizontalMotion.State.IDLE);
  for(int i=0;i<20;i++)m.update(0,-.216,0,false);eq(m.state(),HorizontalMotion.State.MOVE_BACKWARD);eq(m.directionalFallback(),true);
  m.update(.216,0,0,false);eq(m.state(),HorizontalMotion.State.STRAFE_LEFT);m.update(-.216,0,0,false);eq(m.state(),HorizontalMotion.State.STRAFE_RIGHT);
  m.update(.15,.15,0,false);eq(m.state(),HorizontalMotion.State.JOG);
  m.update(-.216,0,90,false);eq(m.state(),HorizontalMotion.State.JOG);
  m.update(0,.216,180,false);eq(m.state(),HorizontalMotion.State.MOVE_BACKWARD);
  m.reset();for(int i=0;i<20;i++)m.update(0,.10,0,false);for(int i=0;i<30;i++){m.update(0,i%2==0?.125:.14,0,false);eq(m.state(),HorizontalMotion.State.WALK);}
  m.update(Double.NaN,0,0,false);eq(m.state(),HorizontalMotion.State.IDLE);m.update(5,0,0,false);eq(m.state(),HorizontalMotion.State.IDLE);
  System.out.println("PASS "+checks+" movement cases");
 }
}
