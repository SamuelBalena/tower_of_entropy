package com.towerofentropy.animation;

/** Visual arbitration only: first request wins; no queue, cooldown, stamina or physics. */
public final class VisualActionWindow {
    public enum Action {
        ROLL("toe_roll",1467), SWORD_ATTACK("toe_sword",433);
        public final String animation;
        public final int durationMs;
        Action(String animation,int durationMs){this.animation=animation;this.durationMs=durationMs;}
    }
    private Action pending,active;
    private int deadline;
    public boolean request(Action action){if(busy())return false;pending=action;return true;}
    public boolean busy(){return pending!=null||active!=null;}
    public Action pending(){return pending;}
    public Action active(){return active;}
    public Action start(int tick){
        if(pending==null||active!=null)return null;
        active=pending;pending=null;
        // CPM stops the non-looping gesture at its own millisecond duration.
        // The server adds two ticks of transport grace before accepting another action.
        deadline=tick+(active.durationMs+49)/50+2;
        return active;
    }
    public Action finishIfDue(int tick){if(active==null||tick<deadline)return null;Action ended=active;active=null;return ended;}
    public Action cancel(){Action old=active;active=null;pending=null;return old;}
}
