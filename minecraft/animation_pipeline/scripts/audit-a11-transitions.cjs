// Read-only sampling of the approved runtime GLB: no retargeting, export or pose changes.
const fs=require('fs'),path=require('path');
const root=path.resolve(__dirname,'../..'),file=path.join(root,'animation_pipeline/converted/m002_r4/resources/assets/towerofentropy/models/gltf/steve_r3.glb');
const b=fs.readFileSync(file);let g,bin;for(let o=12;o<b.length;){const n=b.readUInt32LE(o),t=b.readUInt32LE(o+4);o+=8;if(t===0x4e4f534a)g=JSON.parse(b.subarray(o,o+n));else if(t===0x004e4942)bin=b.subarray(o,o+n);o+=n;}
const widths={SCALAR:1,VEC2:2,VEC3:3,VEC4:4,MAT4:16},accCache=new Map();
function acc(id){if(accCache.has(id))return accCache.get(id);const a=g.accessors[id],v=g.bufferViews[a.bufferView],width=widths[a.type],bytes={5126:4,5123:2,5121:1}[a.componentType];const read={5126:'readFloatLE',5123:'readUInt16LE',5121:'readUInt8'}[a.componentType];const data=Array.from({length:a.count},(_,i)=>Array.from({length:width},(_,j)=>bin[read]((v.byteOffset||0)+(a.byteOffset||0)+i*(v.byteStride||width*bytes)+j*bytes)));accCache.set(id,data);return data;}
const dot=(a,b)=>a.reduce((n,x,i)=>n+x*b[i],0),norm=q=>{const n=Math.hypot(...q);return q.map(x=>x/n);};
function slerp(a,b,t){let d=dot(a,b);if(d<0){b=b.map(x=>-x);d=-d;}if(d>.9995)return norm(a.map((v,i)=>v+(b[i]-v)*t));let h=Math.acos(Math.min(1,d));return a.map((v,i)=>(v*Math.sin((1-t)*h)+b[i]*Math.sin(t*h))/Math.sin(h));}
const identity=()=>[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1];
function mul(a,b){const c=Array(16).fill(0);for(let col=0;col<4;col++)for(let row=0;row<4;row++)for(let k=0;k<4;k++)c[col*4+row]+=a[k*4+row]*b[col*4+k];return c;}
function trs(p,q,s){const [x,y,z,w]=q;return [(1-2*y*y-2*z*z)*s[0],(2*x*y+2*w*z)*s[0],(2*x*z-2*w*y)*s[0],0,(2*x*y-2*w*z)*s[1],(1-2*x*x-2*z*z)*s[1],(2*y*z+2*w*x)*s[1],0,(2*x*z+2*w*y)*s[2],(2*y*z-2*w*x)*s[2],(1-2*x*x-2*y*y)*s[2],0,...p,1];}
const parents={};g.nodes.forEach((n,i)=>(n.children||[]).forEach(j=>parents[j]=i));
const clips=Object.fromEntries(g.animations.map(a=>[a.name.replace('UAL_',''),{duration:Math.max(...a.samplers.map(s=>acc(s.input).at(-1)[0])),channels:a.channels.map(c=>{const s=a.samplers[c.sampler];return {node:c.target.node,path:c.target.path,times:acc(s.input).flat(),values:acc(s.output)};})}]));
function pose(name,time){const local=g.nodes.map(n=>({translation:n.translation||[0,0,0],rotation:n.rotation||[0,0,0,1],scale:n.scale||[1,1,1],matrix:n.matrix}));
 for(const c of clips[name].channels){let i=0;while(i+1<c.times.length&&c.times[i+1]<=time)i++;let v=c.values[i];if(i+1<c.times.length){const t=Math.max(0,Math.min(1,(time-c.times[i])/(c.times[i+1]-c.times[i])));v=c.path==='rotation'?slerp(v,c.values[i+1],t):v.map((x,k)=>x+(c.values[i+1][k]-x)*t);}local[c.node][c.path]=v;}
 const world=[];function global(i){if(world[i])return world[i];const n=local[i],m=n.matrix||trs(n.translation,n.rotation,n.scale);return world[i]=parents[i]===undefined?m:mul(global(parents[i]),m);}g.nodes.forEach((_,i)=>global(i));return {local,world};
}
const joints=new Set(g.skins.flatMap(s=>s.joints));
function worldQuat(m){const a=m.slice();for(let c=0;c<3;c++){const n=Math.hypot(m[c*4],m[c*4+1],m[c*4+2]);for(let r=0;r<3;r++)a[c*4+r]/=n;}const tr=a[0]+a[5]+a[10];let x,y,z,w,s;if(tr>0){s=Math.sqrt(tr+1)*2;w=s/4;x=(a[6]-a[9])/s;y=(a[8]-a[2])/s;z=(a[1]-a[4])/s;}else if(a[0]>a[5]&&a[0]>a[10]){s=Math.sqrt(1+a[0]-a[5]-a[10])*2;w=(a[6]-a[9])/s;x=s/4;y=(a[4]+a[1])/s;z=(a[8]+a[2])/s;}else if(a[5]>a[10]){s=Math.sqrt(1+a[5]-a[0]-a[10])*2;w=(a[8]-a[2])/s;x=(a[4]+a[1])/s;y=s/4;z=(a[9]+a[6])/s;}else{s=Math.sqrt(1+a[10]-a[0]-a[5])*2;w=(a[1]-a[4])/s;x=(a[8]+a[2])/s;y=(a[9]+a[6])/s;z=s/4;}return norm([x,y,z,w]);}
const pairs=[['idle',0,'sword',0],['sword',clips.sword.duration,'sword_b',0],['sword_b',clips.sword_b.duration,'sword_c',0],['sword_c',clips.sword_c.duration,'idle',0],['sword_c',clips.sword_c.duration,'jog',0],['idle',0,'heavy',0],['heavy',clips.heavy.duration,'idle',0],['heavy',clips.heavy.duration,'jog',0],['sword',.1,'roll',0],['sword_b',.1,'roll',0],['sword_c',.15,'roll',0],['heavy',.4,'roll',0]];
const boundaries=pairs.map(([from,t1,to,t2])=>{const a=pose(from,t1),b=pose(to,t2);const bones=[...joints].map(i=>({bone:g.nodes[i].name,degrees:2*Math.acos(Math.min(1,Math.abs(dot(norm(a.local[i].rotation),norm(b.local[i].rotation)))))*180/Math.PI})).sort((a,b)=>b.degrees-a.degrees);const world=[...joints].map(i=>({bone:g.nodes[i].name,degrees:2*Math.acos(Math.min(1,Math.abs(dot(worldQuat(a.world[i]),worldQuat(b.world[i])))))*180/Math.PI,pivotDistanceBlocks:Math.hypot(...[12,13,14].map(k=>a.world[i][k]-b.world[i][k]))*.9375})).sort((a,b)=>b.degrees-a.degrees);return {from,fromSeconds:t1,to,toSeconds:t2,largestLocalBoneRotationDeltas:bones.slice(0,4),largestGlobalBoneRotationDeltas:world.slice(0,4)};});
const feet={};for(const name of ['idle','jog','sword','sword_b','sword_c','heavy','roll']){
 const rows=[];for(let frame=0;frame<=Math.round(clips[name].duration*30);frame++){
  const time=Math.min(frame/30,clips[name].duration),p=pose(name,time);let min=Infinity;
  for(const n of g.nodes){if(!n.name?.startsWith('limb_4')&&!n.name?.startsWith('limb_5'))continue;if(n.mesh===undefined||n.skin===undefined)continue;const s=g.skins[n.skin],ibm=acc(s.inverseBindMatrices),matrices=s.joints.map((j,k)=>mul(p.world[j],ibm[k]));
   for(const primitive of g.meshes[n.mesh].primitives){const positions=acc(primitive.attributes.POSITION),indices=acc(primitive.attributes.JOINTS_0),weights=acc(primitive.attributes.WEIGHTS_0);
    for(let v=0;v<positions.length;v++){const [x,y,z]=positions[v];let height=0;for(let k=0;k<4;k++){const m=matrices[indices[v][k]];height+=(m[1]*x+m[5]*y+m[9]*z+m[13])*weights[v][k];}min=Math.min(min,height*.9375);}
   }
  }rows.push({time,lowestLegVertexBlocks:min});
 }feet[name]={minimum:Math.min(...rows.map(r=>r.lowestLegVertexBlocks)),maximum:Math.max(...rows.map(r=>r.lowestLegVertexBlocks)),samples:rows};
}
const report={method:'Read-only glTF quaternion boundaries and LBS lower-limb minimum Y, including outer skin layer, renderer scale .9375; diagnostic geometry metrics, not aesthetic pass/fail',boundaries,feet};
fs.writeFileSync(path.join(root,'animation_pipeline/reports/m002_a1_1/TRANSITION_ANALYSIS.json'),JSON.stringify(report,null,2));
console.log(boundaries.map(b=>({from:b.from,to:b.to,maxGlobal:b.largestGlobalBoneRotationDeltas[0]})));console.log(Object.fromEntries(Object.entries(feet).map(([k,v])=>[k,{minimum:v.minimum,maximum:v.maximum}])));
