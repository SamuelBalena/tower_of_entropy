// Read-only inspection of GLB metadata/key times and binary FBX node metadata.
// No import, retargeting, extraction of animation clips, or conversion is performed.
// Usage: node scripts/inspect-assets.cjs <Godot root> [JSON report path]
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const root = path.resolve(process.argv[2] || '../../../tower-of-entroy');
const relative = p => path.relative(root, p).split(path.sep).join('/');
function walk(p) {
  return fs.existsSync(p) ? fs.readdirSync(p, {withFileTypes:true}).flatMap(e =>
    e.isDirectory() ? walk(path.join(p,e.name)) : [path.join(p,e.name)]) : [];
}
function hash(p) { return crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex'); }
function glb(p) {
  const b=fs.readFileSync(p);
  if(b.readUInt32LE(0)!==0x46546c67 || b.readUInt32LE(4)!==2 || b.readUInt32LE(8)!==b.length) throw Error('Invalid GLB: '+p);
  let doc, bin;
  for(let o=12;o<b.length;) {
    const n=b.readUInt32LE(o), type=b.readUInt32LE(o+4); o+=8;
    if(type===0x4e4f534a) doc=JSON.parse(b.subarray(o,o+n).toString('utf8').trim());
    if(type===0x004e4942) bin=b.subarray(o,o+n);
    o+=n;
  }
  function times(index) {
    const a=doc.accessors[index], v=doc.bufferViews[a.bufferView];
    if(a.componentType!==5126 || a.type!=='SCALAR' || a.sparse || v.buffer!==0) throw Error('Unsupported time accessor');
    return Array.from({length:a.count},(_,i)=>bin.readFloatLE((v.byteOffset||0)+(a.byteOffset||0)+i*(v.byteStride||4)));
  }
  const parents={};
  (doc.nodes||[]).forEach((n,i)=>(n.children||[]).forEach(c=>parents[c]=i));
  const nodeName=i=>doc.nodes[i].name || '#'+i;
  return {
    asset:doc.asset, buffers:doc.buffers, sceneRoots:(doc.scenes||[]).map(s=>(s.nodes||[]).map(nodeName)),
    skins:(doc.skins||[]).map(s=>({name:s.name, skeleton:s.skeleton===undefined?null:nodeName(s.skeleton), joints:s.joints.map(i=>({index:i,name:nodeName(i),parent:parents[i]===undefined?null:nodeName(parents[i]),translation:doc.nodes[i].translation,rotation:doc.nodes[i].rotation,scale:doc.nodes[i].scale,matrix:doc.nodes[i].matrix}))})),
    clips:(doc.animations||[]).map(a=>{
      const all=a.samplers.flatMap(s=>times(s.input));
      const start=Math.min(...all),end=Math.max(...all);
      return {name:a.name,start,end,duration:end-start,channels:a.channels.length,
        interpolation:[...new Set(a.samplers.map(s=>s.interpolation||'LINEAR'))],
        targets:[...new Set(a.channels.map(c=>nodeName(c.target.node)))],
        paths:[...new Set(a.channels.map(c=>c.target.path))]};
    })
  };
}
function fbx(p) {
  const b=fs.readFileSync(p);
  if(b.subarray(0,20).toString()!=='Kaydara FBX Binary  ') return {unsupported:'Not binary FBX'};
  const version=b.readUInt32LE(23),wide=version>=7500, header=wide?25:13;
  function node(start) {
    const end=wide?Number(b.readBigUInt64LE(start)):b.readUInt32LE(start);
    if(!end)return null;
    const count=wide?Number(b.readBigUInt64LE(start+8)):b.readUInt32LE(start+4);
    const nameSize=b[start+header-1];let o=start+header;
    const name=b.subarray(o,o+nameSize).toString();o+=nameSize;
    const props=[];
    for(let i=0;i<count;i++) {
      const t=String.fromCharCode(b[o++]);let val;
      if(t==='S'||t==='R'){const n=b.readUInt32LE(o);o+=4;val=t==='S'?b.subarray(o,o+n).toString():null;o+=n;}
      else if('fdilbc'.includes(t)){const n=b.readUInt32LE(o+8);o+=12+n;val=null;}
      else if(t==='L'){val=b.readBigInt64LE(o).toString();o+=8;}
      else if(t==='I'){val=b.readInt32LE(o);o+=4;}
      else if(t==='D'){val=b.readDoubleLE(o);o+=8;}
      else if(t==='F'){val=b.readFloatLE(o);o+=4;}
      else if(t==='Y'){val=b.readInt16LE(o);o+=2;}
      else if(t==='C'){val=b[o++];}
      else throw Error('Unsupported FBX property '+t+' in '+p);
      props.push(val);
    }
    const children=[];
    while(o+header<=end){const c=node(o);if(!c)break;children.push(c);o=c.end;}
    return {name,props,children,end};
  }
  const top=[];let o=27;
  while(o+header<b.length){const n=node(o);if(!n)break;top.push(n);o=n.end;}
  const objects=top.find(n=>n.name==='Objects')?.children||[];
  const settings=top.find(n=>n.name==='GlobalSettings')?.children.find(n=>n.name==='Properties70')?.children||[];
  const models=objects.filter(n=>n.name==='Model');
  const names=new Map(models.map(n=>[n.props[0],String(n.props[1]).split('\0')[0]]));
  const links=top.find(n=>n.name==='Connections')?.children||[];
  return {version,settings:settings.map(n=>n.props),
    bones:models.filter(n=>n.props[2]==='LimbNode'||n.props[2]==='Root').map(n=>({name:names.get(n.props[0]),type:n.props[2],parent:names.get(links.find(c=>c.props[0]==='OO'&&c.props[1]===n.props[0])?.props[2])||null})),
    clips:objects.filter(n=>n.name==='AnimationStack').map(n=>{
      const ps=n.children.find(c=>c.name==='Properties70')?.children||[];
      const get=k=>ps.find(c=>c.props[0]===k)?.props.at(-1);
      const name=String(n.props[1]).split('\0')[0];
      const take=top.find(t=>t.name==='Takes')?.children.find(t=>t.name==='Take'&&t.props[0]===name);
      const local=take?.children.find(t=>t.name==='LocalTime')?.props;
      const start=get('LocalStart')??local?.[0],end=get('LocalStop')??local?.[1];
      return {name,startTicks:start,endTicks:end,duration:start===undefined||end===undefined?null:Number(BigInt(end)-BigInt(start))/46186158000};
    })};
}
const packages=['Universal Animation Library[Standard]','Universal Animation Library 2[Standard]','Human Archer Animations FREE'];
const files=packages.flatMap(n=>walk(path.join(root,'assets/source',n)));
const prototype=path.join(root,'assets/prototypes_3d/ual1_standard.glb');
if(fs.existsSync(prototype))files.push(prototype);
const report={root,date:'2026-11-12',method:'Read-only binary metadata inspection. No conversion.',files:files.map(p=>{
  const ext=path.extname(p).toLowerCase();
  return {path:relative(p),bytes:fs.statSync(p).size,sha256:hash(p),...(ext==='.glb'?{glb:glb(p)}:ext==='.fbx'?{fbx:fbx(p)}:{})};
})};
if(process.argv[3]) {
  const out=path.resolve(process.argv[3]);
  // Never write a report back into the source Godot project.
  const rel=path.relative(root,out);
  if(!rel.startsWith('..'+path.sep)&&rel!=='..'&&!path.isAbsolute(rel))throw Error('Report must be outside Godot root');
  fs.writeFileSync(out,JSON.stringify(report,null,2)+'\n');
} else process.stdout.write(JSON.stringify(report,null,2)+'\n');
