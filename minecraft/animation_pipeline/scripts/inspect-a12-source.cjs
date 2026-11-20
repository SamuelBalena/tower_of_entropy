// Read-only source inspection. Executes only the existing sampler, never its file writers.
const fs=require('fs'),path=require('path'),vm=require('vm');
const base=path.resolve(__dirname,'..'),source=fs.readFileSync(path.join(__dirname,'retarget-idle.cjs'),'utf8'),result={};
for(const clip of ['Sword_Regular_B','Sword_Regular_C']){
 let code=source.slice(0,source.indexOf('const face=')).replace("sword?'Sword_Regular_A'","sword?'"+clip+"'").replace(/if\(Math\.abs\(duration-.*?throw Error\('Unexpected duration'\);/,'').replace('sword?14','sword?Math.round(duration*30)+1');
 code+=`\nconst names=['pelvis','foot_l','foot_r','ball_l','ball_r'];result.value={clip,duration,rest:Object.fromEntries(names.filter(n=>byName.has(n)).map(n=>[n,rest[byName.get(n)].p])),frames:Array.from({length:count},(_,i)=>{const time=i*duration/(count-1),p=pose(time);return {time,positions:Object.fromEntries(names.filter(n=>byName.has(n)).map(n=>[n,p[byName.get(n)].p]))};})};`;
 const box={require,process:{argv:['node','script','--sword']},__dirname,console,result:{}};vm.runInNewContext(code,box);result[clip]=box.result.value;
}
const out=path.join(base,'reports/m002_a1_2');fs.mkdirSync(out,{recursive:true});fs.writeFileSync(path.join(out,'SOURCE_CONTACT.json'),JSON.stringify(result,null,2));
for(const [name,v]of Object.entries(result)){console.log(name,'rest',v.rest);for(const bone of Object.keys(v.rest))console.log(bone,'Y range',Math.min(...v.frames.map(f=>f.positions[bone][1])),Math.max(...v.frames.map(f=>f.positions[bone][1])));}
