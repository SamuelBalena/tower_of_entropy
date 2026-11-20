// Contact confidence metadata only, no authored transforms or new animation clips.
const fs=require('fs'),path=require('path');const root=path.resolve(__dirname,'../..');
const source=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/reports/m002_a1_2/SOURCE_CONTACT.json'))),result={};
const smooth=t=>{t=Math.max(0,Math.min(1,t));return t*t*(3-2*t)};
for(const [key,name]of [['sword_b','Sword_Regular_B'],['sword_c','Sword_Regular_C']]){
 const s=source[name];const samples=s.frames.map(f=>{const height=Math.min(f.positions.ball_l[1]-s.rest.ball_l[1],f.positions.ball_r[1]-s.rest.ball_r[1]);return 1-smooth((height-.015)/.03);});result[key]={duration:s.duration,confidence:samples};
 console.log(key,{samples:samples.length,stance:samples.filter(x=>x>.99).length,air:samples.filter(x=>x<.01).length});
}
const target=path.join(root,'src/main/resources/assets/towerofentropy/visual-support.json');fs.writeFileSync(target,JSON.stringify(result));
