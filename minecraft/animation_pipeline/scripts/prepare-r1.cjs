// R1: immutable baseline, isolated inputs and conservative CPM surface correction.
const fs=require('fs'),path=require('path'),crypto=require('crypto');
const {zip}=require('./zip-project.cjs');
const root=path.resolve(__dirname,'../..'),out=path.join(root,'animation_pipeline/converted/m002_r1');
fs.mkdirSync(out,{recursive:true});
const hash=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
const original=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/converted/m001_3/actions-data.json')));
const fixed=structuredClone(original),changes=[];
for(const e of fixed['config.json'].elements){
  e.disableVanillaAnim=true;
  if(!e.children?.length)continue;
  for(const c of e.children){
    if(c.name?.includes('upper')){
      // A split is not the original wrist/ankle: use the adjoining side texels at the cut.
      const v=c.faceUV.north.sy+5,u=c.faceUV.north.sx;
      c.faceUV.down={sx:u,sy:v+1,ex:u+4,ey:v,rot:'0',autoUV:false};
    }
    if(c.name?.includes('lower')&&!c.name.includes('layer')){
      const u=c.faceUV.north.sx,v=c.faceUV.north.sy;
      c.faceUV.up={sx:u,sy:v,ex:u+4,ey:v+1,rot:'0',autoUV:false};
      const layer=c.children?.find(x=>x.name?.includes('layer'));
      if(layer){const u=layer.faceUV.north.sx,v=layer.faceUV.north.sy;layer.faceUV.up={sx:u,sy:v,ex:u+4,ey:v+1,rot:'0',autoUV:false};}
    }
  }
}
// Retain inflation: reducing the entire outer layer would silently change skin semantics.
changes.push('Cut caps sample adjacent skin rows instead of shoulder/wrist/ankle caps; skin pixels and lateral UVs unchanged.');
changes.push('Disable vanilla root transforms while experimental absolute poses are applied. No mesh weights invented.');
fs.writeFileSync(path.join(out,'fixed-data.json'),JSON.stringify(fixed));
fs.writeFileSync(path.join(out,'steve-r1-fixed.cpmproject'),zip(fixed));
const clips={idle:['c_toe_idle',25],jog:['c_toe_jog',7],sword:['g_toe_sword',6],roll:['g_toe_roll',22]};
const poses={};
for(const[name,[key,index]]of Object.entries(clips)){
  const a=original['animations/'+key+'.json'];poses[name]={duration:a.duration,index,frame:a.frames[index],frames:a.frames};
  for(const [variant,data]of[['baseline',original],['fixed',fixed]]){
    const anim={...a,name:'toe_r1',loop:true,command:true,layerControlled:false,duration:1000,
      frames:[structuredClone(a.frames[index])],interpolator:'linear_loop',priority:20,mustFinish:false};
    fs.writeFileSync(path.join(out,variant+'-'+name+'.cpmproject'),zip({'config.json':data['config.json'],'animations/c_toe_r1.json':anim}));
  }
}
fs.writeFileSync(path.join(out,'poses.json'),JSON.stringify(poses));
const baselinePath=path.join(root,'animation_pipeline/reports/M002_R1_BASELINE.json');
if(!fs.existsSync(baselinePath)){
 const files=[];function walk(dir){for(const f of fs.readdirSync(dir,{withFileTypes:true})){const p=path.join(dir,f.name);if(f.isDirectory())walk(p);else files.push(p);}}
 for(const dir of ['src/main/java','animation_pipeline/converted/m001_1','animation_pipeline/converted/m001_2','animation_pipeline/converted/m001_3'])walk(path.join(root,dir));
 const baseline=Object.fromEntries(files.map(p=>[path.relative(root,p).replaceAll('\\','/'),hash(p)]));
 fs.writeFileSync(baselinePath,JSON.stringify(baseline,null,2));
 const backup=path.join(root,'.downloads/m002_r1/backup');
 for(const p of files){const dst=path.join(backup,path.relative(root,p));fs.mkdirSync(path.dirname(dst),{recursive:true});fs.copyFileSync(p,dst);}
}
const baseline=JSON.parse(fs.readFileSync(baselinePath));for(const[p,h]of Object.entries(baseline))if(hash(path.join(root,p))!==h)throw Error('Approved file changed: '+p);
fs.writeFileSync(path.join(root,'animation_pipeline/reports/M002_R1_GEOMETRY_DIAGNOSIS.json'),JSON.stringify({approvedFiles:Object.keys(baseline).length,
  limbGeometry:'2 closed rigid cuboids per limb; no vertex weights; outer cuboids inflated 0.25 px',
  cutCaps:'Original upper bottom and lower top reuse end-of-limb UV islands, not the skin at the cut',
  unchanged:'4 animation arrays and timings, all side UVs, skin64x64, elbow/knee pivots, wrist item anchors',changes,
  limitation:'Cap/transform correction cannot turn two rigid closed boxes into a continuous weighted mesh.'},null,2));
console.log('R1 prepared; approved files unchanged:',Object.keys(baseline).length);
