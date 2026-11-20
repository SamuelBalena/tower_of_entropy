const fs=require('fs'),path=require('path'),assert=require('assert/strict');const dir=path.resolve(__dirname,'../reports/m002_a1_2');
const before=JSON.parse(fs.readFileSync(path.join(dir,'before/RUNTIME.json'))),after=JSON.parse(fs.readFileSync(path.join(dir,'after/RUNTIME.json'))),checks=[];
const avg=v=>v.reduce((s,x)=>s+x,0)/v.length;
function check(name,pass,detail){checks.push({name,pass,detail});assert(pass,name+': '+JSON.stringify(detail));}
for(const [name,data]of [['before',before],['after',after]])check(name+'_runtime',data.checks.every(c=>c.pass)&&data.renderErrors===0,{passed:data.checks.filter(c=>c.pass).length,total:data.checks.length,errors:data.renderErrors});
const all=after.series.flatMap(s=>s.frames);check('finite_matrices',all.every(f=>f.visual.finite),{samples:all.length});
const scale=Math.max(...all.map(f=>f.visual.scaleError)),length=Math.max(...all.map(f=>f.visual.boneLengthError));
check('no_new_scaling_or_bone_stretch',scale<1e-4&&length<1e-4,{maxScaleError:scale,maxBoneLengthError:length});
check('rigid_grip_constraint',all.every(f=>typeof f.visual.gripError==='number'&&f.visual.gripError<1e-5),{maxGripError:Math.max(...all.map(f=>f.visual.gripError||0))});
const gaps=[];for(const s of after.series.filter(s=>s.name.startsWith('cancel'))){const clips=s.frames.filter((f,i)=>!i||f.clip!==s.frames[i-1].clip).map(f=>f.clip);const roll=clips.indexOf('UAL_roll');check(s.name+'_'+s.angle+'_direct_roll',roll>0&&clips[roll-1]!=='UAL_idle',clips);gaps.push({scene:s.name,angle:s.angle,intermediateIdle:false});}
const feet={};for(const clip of ['UAL_sword_b','UAL_sword_c']){
 const collect=data=>data.series.flatMap(s=>s.frames).filter(f=>f.clip===clip&&f.clipTime>=.12&&f.visual.sourceContact>.99&&f.visual.blend>.999);
 const a=collect(before),b=collect(after),av=avg(a.map(f=>Math.max(0,f.visual.soleBlocks))),bv=avg(b.map(f=>Math.max(0,f.visual.soleBlocks)));
 feet[clip]={beforeMeanPositiveGapBlocks:av,afterMeanPositiveGapBlocks:bv,reductionPercent:(1-bv/av)*100,afterMinimumSole:Math.min(...b.map(f=>f.visual.soleBlocks)),afterMaximumSole:Math.max(...b.map(f=>f.visual.soleBlocks)),samplesBefore:a.length,samplesAfter:b.length};
 check(clip+'_support_improved',bv<av*.5,feet[clip]);
}
const air=all.filter(f=>f.clip==='UAL_sword_c'&&f.visual.sourceContact<.01);check('air_phases_not_planted',air.length>0&&air.every(f=>Math.abs(f.visual.groundBlocks)<.01),{samples:air.length,maxResidualCorrectionBlocks:Math.max(...air.map(f=>Math.abs(f.visual.groundBlocks)))});
function angle(a,b){const dot=a.reduce((s,x,i)=>s+x*b[i],0),norm=Math.hypot(...a)*Math.hypot(...b);return 2*Math.acos(Math.min(1,Math.abs(dot/norm)))*180/Math.PI;}
function jump(p,f){return Math.max(...Object.keys(f.visual.bones).map(k=>angle(p.visual.bones[k].q,f.visual.bones[k].q)));}
const cancellationPoses=[];
for(const s of after.series.filter(s=>s.name.startsWith('cancel'))){
 const old=before.series.find(x=>x.name===s.name&&x.angle===s.angle);
 const gapIdle=old.frames.find((f,i)=>i>0&&f.clip==='UAL_idle'&&/^UAL_(sword|heavy)/.test(old.frames[i-1].clip));const idle=gapIdle||old.frames.find(f=>f.clip==='UAL_idle'),roll=s.frames.find(f=>f.clip==='UAL_roll');
 const deviation=jump(idle,roll);check(s.name+'_'+s.angle+'_not_idle_pose',deviation>5,{degreesFromOldIdle:deviation});cancellationPoses.push({scene:s.name,angle:s.angle,oldGapCaptured:!!gapIdle,degreesFromOldIdle:deviation,rollSourceTime:roll.clipTime,blend:roll.visual.blend});
}
const transitions=[];
for(const scene of ['heavy','jog_heavy','combo_jog','heavy_jog','combo'])for(const a of before.series.filter(s=>s.name===scene)){
 const b=after.series.find(s=>s.name===scene&&s.angle===a.angle);
 const target=scene==='combo_jog'||scene==='heavy_jog'?'UAL_jog':scene==='combo'?'UAL_sword_b':'UAL_heavy';
 function metric(s){const i=s.frames.findIndex((f,i)=>i>0&&f.clip===target&&s.frames[i-1].clip!==target);assert(i>0);const first=jump(s.frames[i-1],s.frames[i]),window=[];for(let j=i;j<Math.min(s.frames.length,i+4);j++)window.push(jump(s.frames[j-1],s.frames[j]));return {firstStepDegrees:first,maximumStepFirst200ms:Math.max(...window),index:i,sourcePhase:s.frames[i].clipTime};}
 const x=metric(a),y=metric(b);transitions.push({scene,angle:a.angle,before:x,after:y});
 if(scene!=='combo')check(scene+'_'+a.angle+'_entry_step_reduced',y.firstStepDegrees<x.firstStepDegrees,{before:x.firstStepDegrees,after:y.firstStepDegrees});
}
// Phase-matched verification: A/B/C orientation curves are preserved outside targeted blends.
const vm=require('vm'),sampler=fs.readFileSync(path.join(__dirname,'audit-a11-transitions.cjs'),'utf8');const box={require,__dirname,console,result:{}};
vm.runInNewContext(sampler.slice(0,sampler.indexOf('const pairs='))+'\nresult.pose=pose;result.worldQuat=worldQuat;result.g=g;',box);
let maxLightOrientationError=0,orientationSamples=0;
for(const f of all.filter(f=>['UAL_sword','UAL_sword_b','UAL_sword_c'].includes(f.clip)&&f.visual.blend>.999999)){
 const expected=box.result.pose(f.clip.replace('UAL_',''),f.clipTime);
 for(const [bone,actual]of Object.entries(f.visual.bones)){const i=box.result.g.nodes.findIndex(n=>n.name===bone);maxLightOrientationError=Math.max(maxLightOrientationError,angle(actual.q,box.result.worldQuat(expected.world[i])));orientationSamples++;}
}
check('original_light_rotation_curves_preserved',orientationSamples>0&&maxLightOrientationError<.1,{samples:orientationSamples,maxDeviationDegrees:maxLightOrientationError});
const report={checks,feet,transitions,directRoll:gaps,cancellationPoses,originalLightRotationErrorDegrees:maxLightOrientationError,notes:['Angles are largest global bone orientation differences between successive captured 50 ms samples; not GPU frame timing.','Contact correction only on source-supported B/C phases; not terrain IK or XY foot locking.','Captured peaks differ with sampling phase; phase-matched light orientations are checked against the immutable GLB.'],aestheticApproval:'PENDING_USER'};
fs.writeFileSync(path.join(dir,'COMPARISON.json'),JSON.stringify(report,null,2));console.log(report);
