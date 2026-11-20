import com.towerofentropy.combat.RollRules;
public class RollRulesTest {
 private static int checks;
 private static void check(boolean ok){checks++;if(!ok)throw new AssertionError("Roll rule "+checks);}
 public static void main(String[]args){
  for(float yaw:new float[]{0,90,180,-90,360})for(float left:new float[]{-1,0,1})for(float forward:new float[]{-1,0,1}){var d=RollRules.direction(left,forward,yaw);check(d!=null&&Math.abs(Math.hypot(d.x(),d.z())-1)<1e-10);check(Math.abs(Math.hypot(d.x()*RollRules.SPEED*14,d.z()*RollRules.SPEED*14)-2.5)<1e-10);}
  check(RollRules.direction(0,1,0).z()==1);check(RollRules.direction(1,0,0).x()==1);check(RollRules.direction(0,1,90).x()<-.9999);
  check(RollRules.direction(Float.NaN,1,0)==null);check(RollRules.direction(0,Float.POSITIVE_INFINITY,0)==null);check(RollRules.direction(2,0,0)==null);check(RollRules.direction(0,0,Float.NaN)==null);
  check(RollRules.movementAllowed(2.5,0,.179,-.5));check(!RollRules.movementAllowed(2.7,0,.1,0));check(!RollRules.movementAllowed(1,.4,.1,0));check(!RollRules.movementAllowed(1,0,1,0));check(!RollRules.movementAllowed(1,0,.1,.8));check(!RollRules.movementAllowed(Double.NaN,0,0,0));check(!RollRules.movementAllowed(1,Double.NaN,0,0));
  System.out.println("PASS: "+checks+" Roll direction/distance/bounds checks");
 }
}
