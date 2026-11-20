// R3: preserve previous assets and extract the existing UAL pelvis/spine motion.
const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert/strict');
const root=path.resolve(__dirname,'../..'),out=path.join(root,'animation_pipeline/converted/m002_r3');fs.mkdirSync(out,{recursive:true});
const hash=p=>crypto.createHash('sha256').update(fs.readFileSync(path.join(root,p))).digest('hex');
const baselinePath=path.join(root,'animation_pipeline/reports/M002_R3_BASELINE.json');
if(!fs.existsSync(baselinePath)){
 const b=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/reports/M002_R2_BASELINE.json')));
 function add(dir){for(const e of fs.readdirSync(path.join(root,dir),{withFileTypes:true})){const p=dir+'/'+e.name;if(e.isDirectory())add(p);else b[p]=hash(p);}}
 add('experiments/m002_r2');for(const p of ['animation_pipeline/scripts/prepare-r2.cjs','animation_pipeline/scripts/validate-r2.cjs','scripts/start-r2.ps1','docs/M002_R2_SKINNED_RENDERER_REPORT.md'])b[p]=hash(p);
 fs.writeFileSync(baselinePath,JSON.stringify(b,null,2));for(const p of Object.keys(b)){const d=path.join(root,'.downloads/m002_r3/backup',p);fs.mkdirSync(path.dirname(d),{recursive:true});fs.copyFileSync(path.join(root,p),d);}
}
const baseline=JSON.parse(fs.readFileSync(baselinePath));for(const[p,h]of Object.entries(baseline))assert.equal(hash(p),h,p);
const mul=(a,b)=>[a[3]*b[0]+a[0]*b[3]+a[1]*b[2]-a[2]*b[1],a[3]*b[1]-a[0]*b[2]+a[1]*b[3]+a[2]*b[0],a[3]*b[2]+a[0]*b[1]-a[1]*b[0]+a[2]*b[3],a[3]*b[3]-a[0]*b[0]-a[1]*b[1]-a[2]*b[2]];
const inv=q=>[-q[0],-q[1],-q[2],q[3]],norm=q=>{const l=Math.hypot(...q);return q.map(v=>v/l)},C=[1,0,0,0];
function slerp(a,b,t){let d=a.reduce((s,x,i)=>s+x*b[i],0);if(d<0){b=b.map(x=>-x);d=-d;}if(d>.9995)return norm(a.map((x,i)=>x+(b[i]-x)*t));let h=Math.acos(Math.min(1,d));return a.map((x,i)=>(x*Math.sin((1-t)*h)+b[i]*Math.sin(t*h))/Math.sin(h));}
const angle=q=>2*Math.acos(Math.min(1,Math.abs(q[3])))*180/Math.PI;
const oldPoses=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/converted/m002_r1/poses.json'))),result={};
for(const [clip,source,name]of [['idle','UAL1','Idle_Loop'],['jog','UAL1','Jog_Fwd_Loop'],['roll','UAL1','Roll'],['sword','UAL2','Sword_Regular_A']]){
 const rel='animation_pipeline/input/'+source+'/'+source+'_Standard.glb',b=fs.readFileSync(path.join(root,rel));let gltf,bin;
 for(let o=12;o<b.length;){const n=b.readUInt32LE(o),t=b.readUInt32LE(o+4);o+=8;if(t===0x4e4f534a)gltf=JSON.parse(b.subarray(o,o+n));if(t===0x004e4942)bin=b.subarray(o,o+n);o+=n;}
 const g=gltf,anim=g.animations.find(a=>a.name===name);assert(anim);const parent={};g.nodes.forEach((n,i)=>(n.children||[]).forEach(j=>parent[j]=i));
 function acc(id){const a=g.accessors[id],v=g.bufferViews[a.bufferView],width={SCALAR:1,VEC4:4,VEC3:3}[a.type];assert.equal(a.componentType,5126);return Array.from({length:a.count},(_,i)=>Array.from({length:width},(_,k)=>bin.readFloatLE((v.byteOffset||0)+(a.byteOffset||0)+i*(v.byteStride||width*4)+k*4)));}
 const channels=anim.channels.filter(c=>c.target.path==='rotation').map(c=>{const s=anim.samplers[c.sampler];return {id:c.target.node,t:acc(s.input).flat(),q:acc(s.output)};});
 function pose(t){const local=g.nodes.map(n=>n.rotation||[0,0,0,1]);if(t!==null)for(const c of channels){let i=0;while(i+1<c.t.length&&c.t[i+1]<=t)i++;local[c.id]=i===c.t.length-1?c.q[i]:slerp(c.q[i],c.q[i+1],(t-c.t[i])/(c.t[i+1]-c.t[i]));}const world=[];function get(i){return world[i]||(world[i]=parent[i]===undefined?local[i]:norm(mul(get(parent[i]),local[i])));}g.nodes.forEach((_,i)=>get(i));return world;}
 const rest=pose(null),indices=Object.fromEntries(['pelvis','spine_01','spine_02','spine_03'].map(n=>[n,g.nodes.findIndex(x=>x.name===n)]));assert(Object.values(indices).every(i=>i>=0));
 const duration=oldPoses[clip].duration/1000,n=oldPoses[clip].frames.length,loop=clip==='idle'||clip==='jog';let maxRelative=0;
 const frames=Array.from({length:n},(_,i)=>{const t=i*(clip==='roll'?44/30:clip==='sword'?13/30:clip==='jog'?28/30:2.5)/(loop?n:n-1),p=pose(t),q={};for(const [name,id]of Object.entries(indices))q[name]=norm(mul(mul(mul(C,p[id]),inv(rest[id])),inv(C)));const rel=mul(inv(q.pelvis),q.spine_03);maxRelative=Math.max(maxRelative,angle(rel));return {time:t,quaternions:q};});
 result[clip]={source:rel,sourceSha256:hash(rel),clip:name,durationMs:oldPoses[clip].duration,frames,maxPelvisChestRelativeDegrees:maxRelative};
}
fs.writeFileSync(path.join(out,'spine-source.json'),JSON.stringify(result));
fs.writeFileSync(path.join(root,'animation_pipeline/reports/M002_R3_SPINE_SOURCE.json'),JSON.stringify(Object.fromEntries(Object.entries(result).map(([k,v])=>[k,{...v,frames:v.frames.length}])),null,2));
const resources=path.join(out,'resources/assets/towerofentropy');fs.mkdirSync(path.join(resources,'models/gltf'),{recursive:true});
fs.copyFileSync(path.join(root,'animation_pipeline/converted/m002_r1/steve-r1-weighted.glb'),path.join(resources,'models/gltf/r3_reference.glb'));
fs.copyFileSync(path.join(root,'animation_pipeline/converted/m002_r1/poses.json'),path.join(resources,'r3-poses.json'));
if(fs.existsSync(path.join(out,'steve-r3-refined.glb')))fs.copyFileSync(path.join(out,'steve-r3-refined.glb'),path.join(resources,'models/gltf/r3_steve.glb'));
console.log('Preserved files:',Object.keys(baseline).length);for(const[k,v]of Object.entries(result))console.log(k,'relative pelvis/chest degrees',v.maxPelvisChestRelativeDegrees.toFixed(3));
