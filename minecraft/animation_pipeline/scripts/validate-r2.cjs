const fs=require('fs'),path=require('path'),assert=require('assert/strict'),crypto=require('crypto');
const root=path.resolve(__dirname,'../..'),hash=p=>crypto.createHash('sha256').update(fs.readFileSync(path.join(root,p))).digest('hex');
const baseline=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/reports/M002_R2_BASELINE.json')));
for(const[p,h]of Object.entries(baseline))assert.equal(hash(p),h,'Approved file changed: '+p);
const src='animation_pipeline/converted/m002_r1/steve-r1-weighted.glb',copy='animation_pipeline/converted/m002_r2/resources/assets/towerofentropy/models/gltf/r2_steve.glb';
assert.equal(hash(src),hash(copy),'Experimental GLB differs from R1 source');
const poses=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/converted/m002_r1/poses.json')));
const mul=(a,b)=>[a[3]*b[0]+a[0]*b[3]+a[1]*b[2]-a[2]*b[1],a[3]*b[1]-a[0]*b[2]+a[1]*b[3]+a[2]*b[0],a[3]*b[2]+a[0]*b[1]-a[1]*b[0]+a[2]*b[3],a[3]*b[3]-a[0]*b[0]-a[1]*b[1]-a[2]*b[2]];
function q(r){const a=['x','y','z'].map(k=>r[k]*Math.PI/360);return mul(mul([0,0,Math.sin(a[2]),Math.cos(a[2])],[0,Math.sin(a[1]),0,Math.cos(a[1])]),[Math.sin(a[0]),0,0,Math.cos(a[0])]);}
function rotate(q,v){return mul(mul(q,[...v,0]),[-q[0],-q[1],-q[2],q[3]]).slice(0,3);}
let samples=0,maxError=0;
for(const info of Object.values(poses))for(const frame of info.frames){const a=frame.components.find(c=>c.storeID===303);assert(a);const pos=rotate(q(a.rotation),[-1,10,-2]).map((v,i)=>v+Object.values(a.pos)[i]);maxError=Math.max(maxError,Math.hypot(...pos.map((v,i)=>v-[-1,6,-2][i])));++samples;}
assert(maxError<1e-5,'Wrist compensation error');
const result={status:'PASS',preservedFiles:Object.keys(baseline).length,originalGlbSha256:hash(src),glbCopiedByteIdentically:true,wristSamples:samples,maxLocalWristErrorPixels:maxError,limit:'This geometric anchor check does not prove every rendered weapon frame or remote synchronization.'};
fs.writeFileSync(path.join(root,'animation_pipeline/reports/M002_R2_PRESERVATION.json'),JSON.stringify(result,null,2));console.log(result);
