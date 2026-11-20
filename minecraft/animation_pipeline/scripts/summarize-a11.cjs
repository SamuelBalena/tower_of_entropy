const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const root=path.resolve(__dirname,'../..'),dir=path.join(root,'animation_pipeline/reports/m002_a1_1');
const json=p=>JSON.parse(fs.readFileSync(path.join(dir,p),'utf8').replace(/^\uFEFF/,''));
const suites={};for(const [name,file]of Object.entries({automatic:'RUNTIME.json',restart:'RESTART.json',previousA1:'regression_a1/RUNTIME.json',roll:'ROLL_REGRESSION.json',playback:'PLAYBACK_AUDIT.json',browser:'VIEWER_TEST.json'})){
 const r=json(file);assert(r.checks.length>0&&r.checks.every(c=>c.pass),name+' failed');assert(!r.renderErrors,name+' render error');suites[name]={passed:r.checks.length,total:r.checks.length};
}
const builds={};for(const [name,file]of Object.entries({normal:'m002-a11-final-build.log',experimental:'m002-a11-experimental-build.log',r4FixtureCompile:'m002-a11-r4-compile.log'})){
 const b=fs.readFileSync(path.join(root,'.logs',file));const text=b.toString(b[0]===255&&b[1]===254?'utf16le':'utf8'),match=text.match(/BUILD SUCCESSFUL in ([^\r\n]+)/);assert(match,name+' failed');builds[name]={status:'PASS',duration:match[1]};
}
const result={status:'TECHNICAL_PASS_VISUAL_REVIEW_REQUIRED',suites,builds,evidence:json('EVIDENCE.json'),preservation:json('PRESERVATION.json'),artifact:json('ARTIFACT.json'),aestheticApproval:'PENDING_USER',findings:['Heavy initial pose differs sharply from Idle','C to Jog changes pose sharply','One Idle frame (50 ms nominal) before Roll after attack cancellation','Visual ground separation, especially Sword B'],limits:['Integrated server; no dedicated multiplayer latency certification','PNG sample playback at 20 Hz; no continuous video recorded','CPM fallback has no new B/C/heavy gestures','Existing targeting remains eligible vanilla hostile monsters only']};
fs.writeFileSync(path.join(dir,'RESULTS.json'),JSON.stringify(result,null,2));console.log(result);
