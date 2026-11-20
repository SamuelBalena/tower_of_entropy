// Animation-only extension. Approved models and source GLBs are immutable.
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert/strict');
const base=path.resolve(__dirname,'..'),out=path.join(base,'converted/m002_a1');fs.mkdirSync(out,{recursive:true});
const source=fs.readFileSync(path.join(__dirname,'retarget-idle.cjs'),'utf8');
const result={};
for(const [name,clip] of [['sword_a','Sword_Regular_A'],['sword_b','Sword_Regular_B'],['sword_c','Sword_Regular_C'],['heavy_source','Sword_Heavy_Combo']]){
 let code=source.slice(0,source.indexOf('const face='));
 code=code.replace("sword?'Sword_Regular_A'","sword?'"+clip+"'");
 code=code.replace(/if\(Math\.abs\(duration-.*?throw Error\('Unexpected duration'\);/,'');
 code=code.replace('sword?14','sword?Math.round(duration*30)+1');
 code+=`\nresult.value={duration:Math.round(duration*1000),frames,spine:frames.map((_,i)=>{const p=pose(i*duration/(count-1));const quaternions={};for(const name of ['pelvis','spine_02']){const id=byName.get(name);quaternions[name]=norm(mul(mul(mul(C,p[id].q),inv(rest[id].q)),inv(C)));}return {quaternions};}),sourceClip:clip};`;
 const box={require,process:{argv:['node','script','--sword']},__dirname,console,result:{}};vm.runInNewContext(code,box);result[name]=box.result.value;
}
// Final overhead phrase: source frames 60..130, including windup and neutral recovery.
// The first 0..24 trial was rejected visually because it ended inside a turn.
result.heavy={...result.heavy_source,duration:2333,frames:result.heavy_source.frames.slice(60,131),spine:result.heavy_source.spine.slice(60,131),sourceRange:[60,130]};
fs.writeFileSync(path.join(out,'poses-new.json'),JSON.stringify(result));
console.log(Object.fromEntries(Object.entries(result).map(([k,v])=>[k,{clip:v.sourceClip,duration:v.duration,frames:v.frames.length}])));
const analysis={};for(const [key,v]of Object.entries(result)){
 const deltas=v.frames.slice(1).map((f,i)=>({frame:i+1,ms:Math.round((i+1)*1000/30),armDegrees:Math.hypot(...[3,103,303].flatMap(id=>['x','y','z'].map(k=>{const a=f.components.find(c=>c.storeID===id).rotation[k],b=v.frames[i].components.find(c=>c.storeID===id).rotation[k];return ((a-b+540)%360)-180;})))}));
 analysis[key]={durationMs:v.duration,sourceClip:v.sourceClip,sourceRange:v.sourceRange||[0,v.frames.length-1],fastestSamples:[...deltas].sort((a,b)=>b.armDegrees-a.armDegrees).slice(0,8),samples:deltas};
}fs.writeFileSync(path.join(base,'reports/m002_a1/KEYFRAME_ANALYSIS.json'),JSON.stringify(analysis,null,2));
