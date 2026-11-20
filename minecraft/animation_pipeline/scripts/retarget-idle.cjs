// M001.1: UAL1 Idle_Loop -> articulated classic Steve. Node core modules only.
// Uses measured source poses; does not copy source geometry or edit source files.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),zlib=require('node:zlib');
const roll=process.argv.includes('--roll'),sword=process.argv.includes('--sword'),action=roll||sword;
const base=path.resolve(__dirname,'..'),src=path.join(base,sword?'input/UAL2/UAL2_Standard.glb':'input/UAL1/UAL1_Standard.glb');
const b=fs.readFileSync(src),sha=crypto.createHash('sha256').update(b).digest('hex');
if(sha!==(sword?'8cee20ab1bc55130092447e810e26df22dd2803eccc54f52137a7d54d7ab88a8':'69591853d817488edaa8fd9bf8fc1d821eaeaf789f8627b3cd23b41c4ed67997'))throw Error('Unexpected UAL1 source');
let g,bin;for(let o=12;o<b.length;){const n=b.readUInt32LE(o),t=b.readUInt32LE(o+4);o+=8;if(t===0x4e4f534a)g=JSON.parse(b.subarray(o,o+n));if(t===0x004e4942)bin=b.subarray(o,o+n);o+=n;}
const jog=process.argv.includes('--jog');
const clip=roll?'Roll':sword?'Sword_Regular_A':jog?'Jog_Fwd_Loop':'Idle_Loop';
const anim=g.animations.find(a=>a.name===clip);if(!anim)throw Error(clip+' missing');
const mul=(a,b)=>[a[3]*b[0]+a[0]*b[3]+a[1]*b[2]-a[2]*b[1],a[3]*b[1]-a[0]*b[2]+a[1]*b[3]+a[2]*b[0],a[3]*b[2]+a[0]*b[1]-a[1]*b[0]+a[2]*b[3],a[3]*b[3]-a[0]*b[0]-a[1]*b[1]-a[2]*b[2]];
const norm=q=>{const n=Math.hypot(...q);return q.map(v=>v/n);},inv=q=>[-q[0],-q[1],-q[2],q[3]],rotate=(q,v)=>mul(mul(q,[...v,0]),inv(q)).slice(0,3);
const sub=(a,b)=>a.map((v,i)=>v-b[i]),add=(a,b)=>a.map((v,i)=>v+b[i]);
function slerp(a,b,t){let d=a.reduce((s,v,i)=>s+v*b[i],0);if(d<0){b=b.map(v=>-v);d=-d;}if(d>.9995)return norm(a.map((v,i)=>v+(b[i]-v)*t));const th=Math.acos(Math.min(1,d));return a.map((v,i)=>(v*Math.sin((1-t)*th)+b[i]*Math.sin(t*th))/Math.sin(th));}
function fromTo(a,b){a=norm(a);b=norm(b);const dot=a.reduce((s,v,i)=>s+v*b[i],0);if(dot<-.999999)return [1,0,0,0];return norm([a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0],1+dot]);}
// CPM Rotation uses ZYX: quaternion = qZ * qY * qX. Output degrees.
function euler(q){const [x,y,z,w]=norm(q);return [Math.atan2(2*(w*x+y*z),1-2*(x*x+y*y)),Math.asin(Math.max(-1,Math.min(1,2*(w*y-z*x)))),Math.atan2(2*(w*z+x*y),1-2*(y*y+z*z))].map(v=>v*180/Math.PI);}
const vec=a=>({x:a[0],y:a[1],z:a[2]});
function accessor(id){const a=g.accessors[id],v=g.bufferViews[a.bufferView],n={SCALAR:1,VEC3:3,VEC4:4}[a.type];if(a.componentType!==5126||a.sparse||v.buffer!==0)throw Error('Unsupported accessor');return Array.from({length:a.count},(_,i)=>Array.from({length:n},(_,j)=>bin.readFloatLE((v.byteOffset||0)+(a.byteOffset||0)+i*(v.byteStride||n*4)+j*4)));}
const channels=anim.channels.map(c=>{const s=anim.samplers[c.sampler];if(!['LINEAR','STEP',undefined].includes(s.interpolation))throw Error('Unsupported interpolation');return {node:c.target.node,path:c.target.path,times:accessor(s.input).map(a=>a[0]),values:accessor(s.output),interpolation:s.interpolation||'LINEAR'};});
const byName=new Map(g.nodes.map((n,i)=>[n.name,i])),parents={};g.nodes.forEach((n,i)=>(n.children||[]).forEach(c=>parents[c]=i));
function sample(c,t){let i=0;while(i+1<c.times.length&&c.times[i+1]<=t)i++;if(i===c.times.length-1||c.interpolation==='STEP')return c.values[i];const u=(t-c.times[i])/(c.times[i+1]-c.times[i]);return c.path==='rotation'?slerp(c.values[i],c.values[i+1],u):c.values[i].map((v,j)=>v+(c.values[i+1][j]-v)*u);}
function pose(t){const local=g.nodes.map(n=>({q:n.rotation||[0,0,0,1],p:n.translation||[0,0,0],s:n.scale||[1,1,1]}));if(t!==null)for(const c of channels)local[c.node][{rotation:'q',translation:'p',scale:'s'}[c.path]]=sample(c,t);
 const world=[];function get(i){if(world[i])return world[i];const l=local[i],p=parents[i]===undefined?null:get(parents[i]);return world[i]=p?{q:norm(mul(p.q,l.q)),p:add(p.p,rotate(p.q,l.p.map((v,j)=>v*p.s[j]))),s:l.s.map((v,j)=>v*p.s[j])}:{...l};}g.nodes.forEach((_,i)=>get(i));return world;}
const rest=pose(null),C=[1,0,0,0]; // glTF Y-up/Z-forward -> Minecraft model Y-down/Z-back.
const mappings=[
 {id:0,name:'head',bone:'Head'}, {id:1,name:'body',bone:'spine_03'},
 {id:2,name:'left_arm',bone:'upperarm_l',end:'lowerarm_l'}, {id:3,name:'right_arm',bone:'upperarm_r',end:'lowerarm_r'},
 {id:4,name:'left_leg',bone:'thigh_l',end:'calf_l'}, {id:5,name:'right_leg',bone:'thigh_r',end:'calf_r'},
 {id:102,name:'left_elbow',bone:'lowerarm_l',end:'hand_l',parent:2}, {id:103,name:'right_elbow',bone:'lowerarm_r',end:'hand_r',parent:3},
 {id:104,name:'left_knee',bone:'calf_l',end:'foot_l',parent:4}, {id:105,name:'right_knee',bone:'calf_r',end:'foot_r',parent:5}
];
for(const m of mappings){const i=byName.get(m.bone);if(i===undefined)throw Error('Missing '+m.bone);m.index=i;m.bind=m.end?fromTo([0,1,0],rotate(C,sub(rest[byName.get(m.end)].p,rest[i].p))):[0,0,0,1];}
const rootPivots=[[0,0,0],[0,0,0],[5,2,0],[-5,2,0],[1.9,12,0],[-1.9,12,0]];
function rotations(t){
 const p=pose(t),world={};for(const m of mappings)world[m.id]=norm(mul(mul(mul(mul(C,p[m.index].q),inv(rest[m.index].q)),inv(C)),m.bind));
 const hip=[0,12,0];
 const components=mappings.map(m=>{
   const rot=euler(m.parent===undefined?world[m.id]:mul(inv(world[m.parent]),world[m.id]));
   // Block elbows/knees are hinges. Remove forearm/shin axial twist, preserve measured flexion.
   if(m.parent!==undefined){rot[1]=0;rot[2]=0;}
   const pivot=m.id<6?rootPivots[m.id]:[0,m.id<104?4:6,0];
   const pos=m.id<4?add(hip,rotate(world[1],sub(pivot,hip))):pivot;
   // CPM's angle writer uses unsigned 0..360 degrees. Negative angles otherwise clamp to zero.
   return {storeID:m.id,rotation:vec(rot.map(v=>((v%360)+360)%360)),pos:vec(pos),scale:vec([1,1,1]),color:'ffffff',show:!(m.id>=2&&m.id<6)};
 });
 if(action){
   // Invisible CPM item slots under the forearms, with source wrist orientation.
   const qx=deg=>[Math.sin(deg*Math.PI/360),0,0,Math.cos(deg*Math.PI/360)];
   for(const [side,id,armID]of [['l',102,2],['r',103,3]]){
     const forearm=mul(world[armID],qx(components.find(c=>c.storeID===id).rotation.x));
     const hand=byName.get('hand_'+side),m=mappings.find(m=>m.id===id);
     const desired=mul(mul(mul(mul(C,p[hand].q),inv(rest[hand].q)),inv(C)),m.bind);
     const wrist=mul(inv(forearm),desired),x=side==='l'?1:-1;
     const pos=sub([x,6,-2],rotate(wrist,[x,10,-2]));
     components.push({storeID:200+id,rotation:vec(euler(wrist).map(v=>((v%360)+360)%360)),pos:vec(pos),scale:vec([1,1,1]),color:'ffffff',show:true});
   }
 }
 if(roll){
   // Fit visual cuboids above the local sole plane, without touching the entity's position.
   const quat=r=>{const [x,y,z]=['x','y','z'].map(k=>r[k]*Math.PI/360);return mul(mul([0,0,Math.sin(z),Math.cos(z)],[0,Math.sin(y),0,Math.cos(y)]),[Math.sin(x),0,0,Math.cos(x)]);};
   let maxY=-Infinity;
   for(const m of mappings.filter(m=>m.id<6)){
     const c=components.find(c=>c.storeID===m.id),q=quat(c.rotation),pos=Object.values(c.pos);
     const boxes=m.id===0?[{o:[-4,-8,-4],s:[8,8,8],q,p:pos}]:m.id===1?[{o:[-4,0,-2],s:[8,12,4],q,p:pos}]:[
       {o:[m.id===2?-1:m.id===3?-3:-2,m.id<4?-2:0,-2],s:[4,6,4],q,p:pos},
       (()=>{const k=components.find(c=>c.storeID===100+m.id);return{o:[m.id===2?-1:m.id===3?-3:-2,0,-2],s:[4,6,4],q:mul(q,quat(k.rotation)),p:add(pos,rotate(q,Object.values(k.pos)))};})()
     ];
     for(const box of boxes)for(let bits=0;bits<8;bits++){
       const point=box.o.map((v,j)=>v+((bits>>j)&1?box.s[j]+.25:-.25));
       maxY=Math.max(maxY,add(box.p,rotate(box.q,point))[1]);
     }
   }
   const lift=24-maxY;
   for(const c of components.filter(c=>c.storeID<6))c.pos.y+=lift;
 }
 return components;
}
const times=[...new Set(channels.flatMap(c=>c.times))].sort((a,b)=>a-b),duration=times.at(-1)-times[0];if(Math.abs(duration-(roll?44/30:sword?13/30:jog?28/30:2.5))>1e-5)throw Error('Unexpected duration');
// 75 uniformly spaced samples at 30 Hz; endpoint excluded for CPM linear_loop.
const count=roll?45:sword?14:jog?28:75,frames=Array.from({length:count},(_,i)=>({components:rotations(i*duration/(action?count-1:count))}));
const face=(sx,sy,ex,ey)=>({sx,sy,ex,ey,rot:'0',autoUV:false});
function uv(u,v,lo){const y=v+4+(lo?6:0);return {up:face(u+4,v,u+8,v+4),down:face(u+8,v+4,u+12,v),west:face(u,y,u+4,y+6),north:face(u+4,y,u+8,y+6),east:face(u+8,y,u+12,y+6),south:face(u+12,y,u+16,y+6)};}
let seq=200;const cube=(name,pos,offset,size,u,v,lo,inflate=0)=>({name,storeID:seq++,show:true,texture:true,textureSize:1,pos:vec(pos),offset:vec(offset),size:vec(size),rotation:vec([0,0,0]),rscale:vec([1,1,1]),scale:vec([1,1,1]),u,v,color:'ffffff',mirror:false,mcScale:inflate,faceUV:uv(u,v,lo)});
const elements=mappings.filter(m=>m.id<6).map(m=>({id:m.name,name:m.name,show:m.id<2,pos:vec([0,0,0]),rotation:vec([0,0,0]),disableVanillaAnim:false,children:[]}));
for(const [id,u,v,ou,ov,x,oy] of [[2,32,48,48,48,-1,-2],[3,40,16,40,32,-3,-2],[4,16,48,0,48,-2,0],[5,0,16,0,32,-2,0]]){
 const root=elements[id],arm=id<4,hingeY=arm?4:6;
 root.children.push(cube(root.id+'_upper',[0,0,0],[x,oy,-2],[4,6,4],u,v,false));
 root.children.push(cube(root.id+'_upper_layer',[0,0,0],[x,oy,-2],[4,6,4],ou,ov,false,.25));
 const lower=cube(root.id+'_lower',[0,hingeY,0],[x,0,-2],[4,6,4],u,v,true);lower.storeID=100+id;lower.children=[cube(root.id+'_lower_layer',[0,0,0],[x,0,-2],[4,6,4],ou,ov,true,.25)];root.children.push(lower);
}
const config={version:1,skinType:'default',skinSize:{x:64,y:64},hideHeadIfSkull:true,removeArmorOffset:true,elements};
if(action)for(const id of[2,3]){
 const lower=elements[id].children.find(c=>c.storeID===100+id);
 const anchor=cube(id===2?'left_hand_item':'right_hand_item',[0,-4,0],[0,0,0],[0,0,0],0,0,false);
 anchor.storeID=300+id;anchor.itemRenderer=id===2?'left_hand':'right_hand';delete anchor.faceUV;
 lower.children.push(anchor);
}
const animation={name:(sword?'UAL2 ':'UAL1 ')+clip,additive:false,duration:Math.round(duration*1000),priority:action?10:0,loop:!action,interpolator:action?'linear_single':'linear_loop',frames};
const out=path.join(base,action?'converted/m001_3':jog?'converted/m001_2':'converted/m001_1');fs.mkdirSync(out,{recursive:true});
// Minimal ZIP writer (deflated) for the CPM project, using standard ZIP CRC-32.
const {zip}=require('./zip-project.cjs');
const modelName=roll?'steve-ual1-roll':sword?'steve-ual2-sword':jog?'steve-ual1-jog':'steve-ual1-idle';
fs.writeFileSync(path.join(out,modelName+'.cpmproject'),zip({'config.json':config,[action?'animations/g_'+(roll?'toe_roll':'toe_sword')+'.json':jog?'animations/v_walking_ual1_jog.json':'animations/v_standing_ual1_idle.json']:animation}));
fs.writeFileSync(path.join(out,roll?'roll-data.json':sword?'sword-data.json':jog?'jog-data.json':'retarget-data.json'),JSON.stringify({config,animation},null,2));
const ranges=channels.map(c=>({bone:g.nodes[c.node].name,path:c.path,keys:c.times.length,start:c.times[0],end:c.times.at(-1),interpolation:c.interpolation,min:c.values[0].map((_,j)=>Math.min(...c.values.map(v=>v[j]))),max:c.values[0].map((_,j)=>Math.max(...c.values.map(v=>v[j])))}));
const stats={source:src,sha256:sha,clip,duration,sourceTimes:times.length,sourceDeltas:[...new Set(times.slice(1).map((t,i)=>(t-times[i]).toFixed(7)))],sampleRate:30,frames:count,mappings:mappings.map(({index,bind,...m})=>m),channels:ranges,rootTranslation:ranges.filter(c=>['root','pelvis'].includes(c.bone)&&c.path==='translation'),policy:{translation:'Target pivots follow torso rotation; source translations discarded; no player position writes',scale:'Unit scale on target, source scale inspected',geometry:'Classic Steve; split vanilla cuboids, 64x64 skin UVs; no source mesh'},maxLoopEulerDifferenceDegrees:Math.max(...mappings.map((m,i)=>{const a=Object.values(rotations(0)[i].rotation),b=Object.values(rotations(duration)[i].rotation);return Math.max(...a.map((v,j)=>Math.abs(((v-b[j]+540)%360)-180)));})),targetRotationRanges:mappings.map((m,j)=>({bone:m.name,min:['x','y','z'].map(k=>Math.min(...frames.map(f=>f.components[j].rotation[k]))),max:['x','y','z'].map(k=>Math.max(...frames.map(f=>f.components[j].rotation[k])))}))};
fs.writeFileSync(path.join(base,roll?'reports/M001_3_ROLL_SOURCE.json':sword?'reports/M001_3_SWORD_SOURCE.json':jog?'reports/M001_2_SOURCE_ANALYSIS.json':'reports/M001_1_SOURCE_ANALYSIS.json'),JSON.stringify(stats,null,2));
if(crypto.createHash('sha256').update(fs.readFileSync(src)).digest('hex')!==sha)throw Error('Source changed');
console.log(JSON.stringify({output:out,clip:stats.clip,duration,frames:count,channels:channels.length,projectBytes:fs.statSync(path.join(out,modelName+'.cpmproject')).size}));
