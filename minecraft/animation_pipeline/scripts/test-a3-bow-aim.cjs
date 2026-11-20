// Vanilla bow_pulling_2 arrow points NW in its unmodified XY sprite: (-X,+Y).
// Validate the actual draw-space sequence including R3's handedness reflection.
const assert=require('assert/strict'),fs=require('fs'),path=require('path');
const x=(v,a)=>[v[0],v[1]*Math.cos(a)-v[2]*Math.sin(a),v[1]*Math.sin(a)+v[2]*Math.cos(a)];
const y=(v,a)=>[v[0]*Math.cos(a)+v[2]*Math.sin(a),v[1],-v[0]*Math.sin(a)+v[2]*Math.cos(a)];
const z=(v,a)=>[v[0]*Math.cos(a)-v[1]*Math.sin(a),v[0]*Math.sin(a)+v[1]*Math.cos(a),v[2]];
let maxError=0,count=0;for(const yaw of [-180,-90,0,45,179])for(const pitch of [-60,-20,0,35,60])for(const offset of [-90,-25,0,30,90]){const a=yaw*Math.PI/180,p=pitch*Math.PI/180,d=offset*Math.PI/180;let v=z([-1,1,0],Math.PI/4);v=y(v,-Math.PI/2);v=x(v,-p);v=y(v,-d);v[0]*=-1;v=y(v,Math.PI-a-d);const len=Math.hypot(...v),expected=[-Math.sin(a)*Math.cos(p),-Math.sin(p),Math.cos(a)*Math.cos(p)];maxError=Math.max(maxError,...v.map((n,i)=>Math.abs(n/len-expected[i])));count++;}assert(maxError<1e-9);const result={pass:true,cases:count,maxDirectionComponentError:maxError,source:'Local unmodified vanilla 1.20.1 bow_pulling_2 sprite and R3 draw-space transforms; model attachment only, not projectile physics'};fs.writeFileSync(path.resolve(__dirname,'../reports/m002_a3/BOW_AIM.json'),JSON.stringify(result,null,2));console.log(result);
