// New animation-only derivative. Never writes approved models or source libraries.
const fs=require('fs'),path=require('path'),vm=require('vm');const base=path.resolve(__dirname,'..'),out=path.join(base,'converted/m002_a2');fs.mkdirSync(out,{recursive:true});
const source=fs.readFileSync(path.join(__dirname,'retarget-idle.cjs'),'utf8'),result={};
for(const [name,clip]of [['walk','Walk_Loop'],['sprint','Sprint_Loop']]){
 let code=source.slice(0,source.indexOf('const face=')).replace("sword?'Sword_Regular_A'","sword?'"+clip+"'").replace("sword?'input/UAL2/UAL2_Standard.glb'","sword?'input/UAL1/UAL1_Standard.glb'");
 // UAL1 source with action=true retains the approved source-derived wrist anchors.
 code=code.replace(/if\(sha!==.*?throw Error\('Unexpected UAL1 source'\);/,"if(sha!=='69591853d817488edaa8fd9bf8fc1d821eaeaf789f8627b3cd23b41c4ed67997')throw Error('Unexpected UAL1 source');");
 code=code.replace(/if\(Math\.abs\(duration-.*?throw Error\('Unexpected duration'\);/,'').replace('sword?14','sword?Math.round(duration*30)+1');
 code+=`\nresult.value={duration:Math.round(duration*1000),frames,spine:frames.map((_,i)=>{const p=pose(i*duration/(count-1));const quaternions={};for(const name of ['pelvis','spine_02']){const id=byName.get(name);quaternions[name]=norm(mul(mul(mul(C,p[id].q),inv(rest[id].q)),inv(C)));}return {quaternions};}),sourceClip:clip};`;
 const box={require,process:{argv:['node','script','--sword']},__dirname,console,result:{}};vm.runInNewContext(code,box);result[name]=box.result.value;
}
fs.writeFileSync(path.join(out,'poses-new.json'),JSON.stringify(result));console.log(Object.fromEntries(Object.entries(result).map(([k,v])=>[k,{clip:v.sourceClip,duration:v.duration,frames:v.frames.length}])));
